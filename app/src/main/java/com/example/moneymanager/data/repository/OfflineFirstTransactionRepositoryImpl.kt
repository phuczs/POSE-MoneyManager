package com.example.moneymanager.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.moneymanager.data.local.TransactionDao
import com.example.moneymanager.data.local.TransactionEntity
import com.example.moneymanager.data.mapper.toDomain
import com.example.moneymanager.data.mapper.toEntity
import com.example.moneymanager.data.mapper.toFirestoreMap
import com.example.moneymanager.data.mapper.toTransactionDomain
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.SyncRepository
import com.example.moneymanager.domain.repository.SyncState
import com.example.moneymanager.domain.repository.TransactionRepository
import com.example.moneymanager.util.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first implementation of [TransactionRepository] and [SyncRepository].
 *
 * Architecture: Single Source of Truth (SSoT) + Two-Way Sync Engine.
 * - Room is the local source of truth for all reads and immediate writes.
 * - Local writes succeed immediately with pending status, ensuring 0% UI freeze/failure offline.
 * - Firestore is the remote cloud authority.
 * - Automatic background sync via [SyncRepository] and WorkManager.
 * - Conflict resolution: Last-Write-Wins based on [updatedAt] timestamps.
 */
@Singleton
class OfflineFirstTransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val networkMonitor: NetworkMonitor
) : TransactionRepository, SyncRepository {

    companion object {
        private const val TAG = "OfflineFirstRepo"
        private const val PAGE_SIZE = 30
        private const val COLLECTION = "transactions"
    }

    private val transactionsCollection = firestore.collection(COLLECTION)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncListener: ListenerRegistration? = null
    private var currentSyncUserId: String? = null

    private val _syncState = MutableStateFlow<SyncState>(SyncState.IDLE)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        syncScope.launch {
            networkMonitor.isOnline.collectLatest { isOnline ->
                if (isOnline) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        syncAll()
                    } else {
                        _syncState.value = SyncState.IDLE
                    }
                } else {
                    _syncState.value = SyncState.OFFLINE
                }
            }
        }
    }

    private fun getCurrentUserId(): String =
        auth.currentUser?.uid ?: "local_user"

    // ─── Reads: Room → UI, Firebase → Room (background sync) ─────────────────

    override fun getAllTransactions(): Flow<List<Transaction>> {
        val userId = getCurrentUserId()
        syncFromFirebase(userId)
        return dao.getAll(userId).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getTransactionsByType(type: String): Flow<List<Transaction>> {
        val userId = getCurrentUserId()
        syncFromFirebase(userId)
        return dao.getByType(userId, type).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getTransactionsByMonth(month: Int, year: Int): Flow<List<Transaction>> {
        val userId = getCurrentUserId()
        syncFromFirebase(userId)
        return dao.getByMonth(userId, month, year).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getTransactionsByTypeAndMonth(
        type: String,
        yearMonth: YearMonth
    ): Flow<List<Transaction>> {
        val userId = getCurrentUserId()
        syncFromFirebase(userId)
        return dao.getByTypeAndMonth(userId, type, yearMonth.monthValue, yearMonth.year)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        val userId = getCurrentUserId()
        syncFromFirebase(userId)
        return dao.getRecent(userId, limit).map { entities -> entities.map { it.toDomain() } }
    }

    override fun searchTransactions(
        query: String,
        type: String,
        category: String,
        minAmount: Double?,
        maxAmount: Double?,
        startDate: Long?,
        endDate: Long?
    ): Flow<List<Transaction>> {
        val userId = getCurrentUserId()
        syncFromFirebase(userId)
        return dao.searchTransactions(
            userId = userId,
            query = query,
            type = type,
            category = category,
            minAmount = minAmount,
            maxAmount = maxAmount,
            startDate = startDate,
            endDate = endDate
        ).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getPagedTransactions(
        type: String?,
        month: Int?,
        year: Int?
    ): Flow<PagingData<Transaction>> {
        val userId = getCurrentUserId()
        syncFromFirebase(userId)
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = 10
            ),
            pagingSourceFactory = {
                when {
                    type != null && month != null && year != null ->
                        dao.getPagedByTypeAndMonth(userId, type, month, year)
                    type != null ->
                        dao.getPagedByType(userId, type)
                    month != null && year != null ->
                        dao.getPagedByMonth(userId, month, year)
                    else ->
                        dao.getPagedAll(userId)
                }
            }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    // ─── Firebase → Room Sync (Real-time Snapshot Listener) ────────────────────

    private fun syncFromFirebase(userId: String) {
        if (userId == "local_user") return
        synchronized(this) {
            if (syncListener != null && currentSyncUserId == userId) return
            syncListener?.remove()
            currentSyncUserId = userId
            syncListener = transactionsCollection
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Real-time sync listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        syncScope.launch {
                            try {
                                val entitiesToUpsert = mutableListOf<TransactionEntity>()
                                for (doc in snapshot.documents) {
                                    val data = doc.data ?: continue
                                    val remoteUpdatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
                                    val localItem = dao.getById(doc.id, userId)

                                    if (localItem == null || localItem.syncStatus == "SYNCED") {
                                        data.toTransactionDomain(doc.id).let {
                                            entitiesToUpsert.add(it.toEntity(syncStatus = "SYNCED", updatedAt = remoteUpdatedAt))
                                        }
                                    } else if (remoteUpdatedAt > localItem.updatedAt) {
                                        // Remote is newer: conflict resolved in favor of remote
                                        data.toTransactionDomain(doc.id).let {
                                            entitiesToUpsert.add(it.toEntity(syncStatus = "SYNCED", updatedAt = remoteUpdatedAt))
                                        }
                                    }
                                }
                                if (entitiesToUpsert.isNotEmpty()) {
                                    dao.upsertAll(entitiesToUpsert)
                                }
                                for (change in snapshot.documentChanges) {
                                    if (change.type == DocumentChange.Type.REMOVED) {
                                        dao.deleteById(change.document.id, userId)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error applying real-time Firestore changes to Room", e)
                            }
                        }
                    }
                }
        }
    }

    // ─── Write Operations (Optimistic Local-First) ─────────────────────────────

    override suspend fun addTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            val userId = getCurrentUserId()
            val now = System.currentTimeMillis()
            val txId = if (transaction.id.isNotBlank()) transaction.id else UUID.randomUUID().toString()
            val prepared = transaction.copy(id = txId, userId = userId)

            // Local save with PENDING_INSERT
            val entity = prepared.toEntity(syncStatus = "PENDING_INSERT", updatedAt = now)
            dao.upsert(entity)

            // Push to cloud if online
            if (networkMonitor.isOnline.value && userId != "local_user") {
                syncScope.launch { pushItem(entity) }
            }

            Result.success(prepared)
        } catch (e: Exception) {
            Log.e(TAG, "addTransaction local write error", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            if (transaction.id.isBlank()) {
                return Result.failure(IllegalArgumentException("Transaction ID cannot be empty"))
            }
            val userId = getCurrentUserId()
            val now = System.currentTimeMillis()
            val updated = transaction.copy(userId = userId)

            val existing = dao.getById(transaction.id, userId)
            val targetStatus = if (existing?.syncStatus == "PENDING_INSERT") "PENDING_INSERT" else "PENDING_UPDATE"

            val entity = updated.toEntity(syncStatus = targetStatus, updatedAt = now)
            dao.upsert(entity)

            if (networkMonitor.isOnline.value && userId != "local_user") {
                syncScope.launch { pushItem(entity) }
            }

            Result.success(updated)
        } catch (e: Exception) {
            Log.e(TAG, "updateTransaction local write error", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            val now = System.currentTimeMillis()
            val existing = dao.getById(transactionId, userId)

            if (existing == null || existing.syncStatus == "PENDING_INSERT") {
                // Not on remote cloud, delete locally
                dao.deleteById(transactionId, userId)
            } else {
                // Soft delete to hide from UI queries until cloud deletion completes
                dao.markForDeletion(transactionId, userId, now)
                if (networkMonitor.isOnline.value && userId != "local_user") {
                    syncScope.launch {
                        try {
                            transactionsCollection.document(transactionId).delete().await()
                            dao.deleteById(transactionId, userId)
                        } catch (e: Exception) {
                            Log.e(TAG, "Cloud delete failed for $transactionId, will retry in syncAll", e)
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteTransaction error", e)
            Result.failure(e)
        }
    }

    override suspend fun getTransactionById(transactionId: String): Result<Transaction> {
        return try {
            val userId = getCurrentUserId()
            val local = dao.getById(transactionId, userId)
            if (local != null && local.syncStatus != "PENDING_DELETE") {
                return Result.success(local.toDomain())
            }

            val doc = transactionsCollection.document(transactionId).get().await()
            val transaction = doc.data?.toTransactionDomain(doc.id)
                ?: return Result.failure(Exception("Transaction not found"))

            dao.upsert(transaction.toEntity())
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Two-Way Sync Engine Implementation ───────────────────────────────────

    private suspend fun pushItem(item: TransactionEntity) {
        try {
            val docMap = item.toDomain().toFirestoreMap(updatedAt = item.updatedAt)
            transactionsCollection.document(item.id).set(docMap).await()
            dao.markAsSynced(item.id, item.userId, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.w(TAG, "Push single item failed, will catch up in syncAll: ${e.message}")
        }
    }

    override suspend fun syncAll(): Result<Int> {
        val user = auth.currentUser
        if (user == null) {
            _syncState.value = SyncState.IDLE
            return Result.success(0)
        }
        if (!networkMonitor.isOnline.value) {
            _syncState.value = SyncState.OFFLINE
            return Result.success(0)
        }

        return try {
            _syncState.value = SyncState.SYNCING
            val userId = user.uid
            var syncedCount = 0

            // 1. Push all pending local mutations to Firebase
            val pendingList = dao.getPendingSync(userId)
            for (item in pendingList) {
                when (item.syncStatus) {
                    "PENDING_INSERT", "PENDING_UPDATE" -> {
                        val docMap = item.toDomain().toFirestoreMap(updatedAt = item.updatedAt)
                        transactionsCollection.document(item.id).set(docMap).await()
                        dao.markAsSynced(item.id, userId, System.currentTimeMillis())
                        syncedCount++
                    }
                    "PENDING_DELETE" -> {
                        transactionsCollection.document(item.id).delete().await()
                        dao.deleteById(item.id, userId)
                        syncedCount++
                    }
                }
            }

            // 2. Pull remote changes with Conflict Resolution (Last-Write-Wins on updatedAt)
            val remoteSnapshot = transactionsCollection.whereEqualTo("userId", userId).get().await()
            val remoteEntitiesToUpsert = mutableListOf<TransactionEntity>()

            for (doc in remoteSnapshot.documents) {
                val data = doc.data ?: continue
                val remoteUpdatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
                val localItem = dao.getById(doc.id, userId)

                if (localItem == null || localItem.syncStatus == "SYNCED") {
                    data.toTransactionDomain(doc.id).let {
                        remoteEntitiesToUpsert.add(it.toEntity(syncStatus = "SYNCED", updatedAt = remoteUpdatedAt))
                    }
                } else if (remoteUpdatedAt > localItem.updatedAt) {
                    // Conflict: remote is newer
                    data.toTransactionDomain(doc.id).let {
                        remoteEntitiesToUpsert.add(it.toEntity(syncStatus = "SYNCED", updatedAt = remoteUpdatedAt))
                    }
                }
            }

            if (remoteEntitiesToUpsert.isNotEmpty()) {
                dao.upsertAll(remoteEntitiesToUpsert)
            }

            _syncState.value = SyncState.IDLE
            Result.success(syncedCount)
        } catch (e: Exception) {
            Log.e(TAG, "syncAll failed", e)
            _syncState.value = SyncState.ERROR
            Result.failure(e)
        }
    }

    override suspend fun clearLocalCacheForUser(userId: String) {
        synchronized(this) {
            syncListener?.remove()
            syncListener = null
            currentSyncUserId = null
        }
        dao.deleteAllForUser(userId)
        Log.d(TAG, "Local cache cleared and sync listener removed for user $userId")
    }
}
