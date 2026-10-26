package com.example.moneymanager.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [TransactionEntity].
 *
 * All write operations use @Upsert (INSERT OR REPLACE) to safely handle
 * Firebase sync that may replay existing documents without causing duplicates.
 *
 * All queries are userId-scoped to prevent cross-account data leakage.
 */
@Dao
interface TransactionDao {

    // ─── Write Operations ────────────────────────────────────────────────────

    /** Insert or update a batch of transactions (used during Firebase sync). */
    @Upsert
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    /** Insert or update a single transaction (optimistic local write). */
    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    /**
     * Delete a single transaction permanently by its ID and validate it belongs to the
     * current user before deletion (security: prevents deleting other users' data).
     */
    @Query("DELETE FROM transactions WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: String, userId: String)

    /** Wipe all rows for a user — called on logout to prevent data leakage. */
    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    // ─── Sync Operations ─────────────────────────────────────────────────────

    /** Get all local transactions that need to be synced to Firebase (not yet SYNCED). */
    @Query("SELECT * FROM transactions WHERE userId = :userId AND syncStatus != 'SYNCED'")
    suspend fun getPendingSync(userId: String): List<TransactionEntity>

    /** Mark a transaction as successfully synced to the cloud. */
    @Query("UPDATE transactions SET syncStatus = 'SYNCED', syncedAt = :syncedAt WHERE id = :id AND userId = :userId")
    suspend fun markAsSynced(id: String, userId: String, syncedAt: Long)

    /** Mark a transaction for deletion (soft delete offline until pushed to cloud). */
    @Query("UPDATE transactions SET syncStatus = 'PENDING_DELETE', updatedAt = :updatedAt WHERE id = :id AND userId = :userId")
    suspend fun markForDeletion(id: String, userId: String, updatedAt: Long)

    // ─── Read Operations (Flow — for UI, excluding soft-deleted items) ────────

    @Query("""
        SELECT * FROM transactions 
        WHERE userId = :userId AND syncStatus != 'PENDING_DELETE'
        ORDER BY date DESC 
        LIMIT :limit
        """)
    fun getRecent(userId: String, limit: Int): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE userId = :userId AND syncStatus != 'PENDING_DELETE'
        ORDER BY date DESC
        """)
    fun getAll(userId: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE userId = :userId AND type = :type AND syncStatus != 'PENDING_DELETE'
        ORDER BY date DESC
        """)
    fun getByType(userId: String, type: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE userId = :userId AND month = :month AND year = :year AND syncStatus != 'PENDING_DELETE'
        ORDER BY date DESC
        """)
    fun getByMonth(userId: String, month: Int, year: Int): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE userId = :userId AND type = :type AND month = :month AND year = :year AND syncStatus != 'PENDING_DELETE'
        ORDER BY date DESC
        """)
    fun getByTypeAndMonth(userId: String, type: String, month: Int, year: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: String, userId: String): TransactionEntity?

    @Query("""
        SELECT * FROM transactions
        WHERE userId = :userId
          AND syncStatus != 'PENDING_DELETE'
          AND (:query = '' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
          AND (:type = '' OR type = :type)
          AND (:category = '' OR category = :category)
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY date DESC
        """)
    fun searchTransactions(
        userId: String,
        query: String = "",
        type: String = "",
        category: String = "",
        minAmount: Double? = null,
        maxAmount: Double? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<TransactionEntity>>

    // ─── Paging Source (Paging 3, excluding soft-deleted items) ───────────────

    /** Returns a PagingSource for paginated loading of all transactions. */
    @Query("""
        SELECT * FROM transactions
        WHERE userId = :userId AND syncStatus != 'PENDING_DELETE'
        ORDER BY date DESC
        """)
    fun getPagedAll(userId: String): PagingSource<Int, TransactionEntity>

    /** Returns a PagingSource filtered by type. */
    @Query("""
        SELECT * FROM transactions
        WHERE userId = :userId AND type = :type AND syncStatus != 'PENDING_DELETE'
        ORDER BY date DESC
        """)
    fun getPagedByType(userId: String, type: String): PagingSource<Int, TransactionEntity>

    /** Returns a PagingSource filtered by month/year. */
    @Query("""
        SELECT * FROM transactions
        WHERE userId = :userId AND month = :month AND year = :year AND syncStatus != 'PENDING_DELETE'
        ORDER BY date DESC
        """)
    fun getPagedByMonth(userId: String, month: Int, year: Int): PagingSource<Int, TransactionEntity>

    /** Returns a PagingSource filtered by type AND month/year. */
    @Query("""
        SELECT * FROM transactions
        WHERE userId = :userId AND type = :type AND month = :month AND year = :year AND syncStatus != 'PENDING_DELETE'
        ORDER BY date DESC
        """)
    fun getPagedByTypeAndMonth(userId: String, type: String, month: Int, year: Int): PagingSource<Int, TransactionEntity>
}
