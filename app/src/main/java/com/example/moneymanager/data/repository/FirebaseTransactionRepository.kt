package com.example.moneymanager.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.moneymanager.data.mapper.toFirestoreMap
import androidx.paging.PagingData
import com.example.moneymanager.data.mapper.toTransactionDomain
import com.example.moneymanager.data.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy Firebase-only transaction repository.
 *
 * @deprecated Superseded by [OfflineFirstTransactionRepository].
 * This class is kept only to satisfy the [TransactionRepository] interface while
 * [OfflineFirstTransactionRepository] is wired as the primary binding in [RepositoryModule].
 *
 * All document deserialization uses [toTransactionDomain] instead of Firestore's `toObjects()`,
 * because the domain [Transaction.date] is epoch-ms [Long] — not a Firebase [Timestamp].
 */
@Singleton
class FirebaseTransactionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : TransactionRepository {

    private val transactionsCollection = firestore.collection("transactions")

    private fun getCurrentUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    override fun getAllTransactions(): Flow<List<Transaction>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = transactionsCollection
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        doc.data?.toTransactionDomain(doc.id)
                    }
                    trySend(transactions)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override fun getTransactionsByType(type: String): Flow<List<Transaction>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = transactionsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", type)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        doc.data?.toTransactionDomain(doc.id)
                    }
                    trySend(transactions)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override fun getTransactionsByMonth(month: Int, year: Int): Flow<List<Transaction>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = transactionsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        doc.data?.toTransactionDomain(doc.id)
                    }
                    trySend(transactions)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getTransactionsByTypeAndMonth(
        type: String,
        yearMonth: YearMonth
    ): Flow<List<Transaction>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = transactionsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", type)
            .whereEqualTo("month", yearMonth.monthValue)
            .whereEqualTo("year", yearMonth.year)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        doc.data?.toTransactionDomain(doc.id)
                    }
                    trySend(transactions)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = transactionsCollection
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        doc.data?.toTransactionDomain(doc.id)
                    }
                    trySend(transactions)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun addTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            val userId = getCurrentUserId()
            val newTransaction = transaction.copy(userId = userId)
            val documentRef = transactionsCollection.document()
            val transactionWithId = newTransaction.copy(id = documentRef.id)
            documentRef.set(transactionWithId.toFirestoreMap()).await()
            Result.success(transactionWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            val userId = getCurrentUserId()
            if (transaction.id.isEmpty()) {
                return Result.failure(IllegalArgumentException("Transaction ID cannot be empty"))
            }
            val doc = transactionsCollection.document(transaction.id).get().await()
            if (!doc.exists() || doc.getString("userId") != userId) {
                return Result.failure(Exception("Transaction not found or unauthorized"))
            }
            val updated = transaction.copy(userId = userId)
            transactionsCollection.document(transaction.id)
                .set(updated.toFirestoreMap()).await()
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            val doc = transactionsCollection.document(transactionId).get().await()
            if (!doc.exists() || doc.getString("userId") != userId) {
                return Result.failure(Exception("Transaction not found or unauthorized"))
            }
            transactionsCollection.document(transactionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTransactionById(transactionId: String): Result<Transaction> {
        return try {
            val userId = getCurrentUserId()
            val doc = transactionsCollection.document(transactionId).get().await()
            val data = doc.data
            if (doc.exists() && data != null && doc.getString("userId") == userId) {
                Result.success(data.toTransactionDomain(doc.id))
            } else {
                Result.failure(Exception("Transaction not found or unauthorized"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Deprecated("Use OfflineFirstTransactionRepository instead", level = DeprecationLevel.ERROR)
    override fun getPagedTransactions(type: String?, month: Int?, year: Int?): Flow<PagingData<Transaction>> =
        emptyFlow()

    @Deprecated("Use OfflineFirstTransactionRepository instead", level = DeprecationLevel.ERROR)
    override suspend fun clearLocalCacheForUser(userId: String) {
        // No-op: FirebaseTransactionRepository has no local cache
    }
}