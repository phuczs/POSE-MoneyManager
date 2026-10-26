package com.example.moneymanager.domain.repository

import androidx.paging.PagingData
import com.example.moneymanager.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/**
 * Domain contract for transaction data access.
 *
 * Clean Architecture note:
 * - This interface lives in the domain layer and knows nothing about Room,
 *   Firestore, or any data source details.
 * - All returned [Transaction] objects use epoch-ms [Long] for dates.
 *
 * Security note:
 * - Implementations must always scope queries to the authenticated user's UID.
 *   The [userId] is never accepted as a parameter to prevent parameter injection attacks.
 */
interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByType(type: String): Flow<List<Transaction>>
    fun getTransactionsByMonth(month: Int, year: Int): Flow<List<Transaction>>
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>
    fun getTransactionsByTypeAndMonth(type: String, yearMonth: YearMonth): Flow<List<Transaction>>

    /** Advanced multi-criteria search and filter for transactions. */
    fun searchTransactions(
        query: String = "",
        type: String = "",
        category: String = "",
        minAmount: Double? = null,
        maxAmount: Double? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<Transaction>>

    /** Paging 3 — returns a stream of pages. Page size is controlled by the caller's Pager config. */
    fun getPagedTransactions(
        type: String? = null,
        month: Int? = null,
        year: Int? = null
    ): Flow<PagingData<Transaction>>

    suspend fun addTransaction(transaction: Transaction): Result<Transaction>
    suspend fun updateTransaction(transaction: Transaction): Result<Transaction>
    suspend fun deleteTransaction(transactionId: String): Result<Unit>
    suspend fun getTransactionById(transactionId: String): Result<Transaction>

    /** Called on logout to wipe user-specific local cache and prevent data leakage. */
    suspend fun clearLocalCacheForUser(userId: String)
}
