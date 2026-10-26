package com.example.moneymanager.domain.repository

import com.example.moneymanager.domain.model.Budget
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for budget data access.
 *
 * Clean Architecture note:
 * - [date] parameter uses epoch milliseconds (Long) — no java.util.Date dependency.
 * - Implementations scope all queries to the authenticated user's UID.
 */
interface BudgetRepository {

    /**
     * Retrieves active budgets for a given point-in-time [date] (epoch ms).
     * Returns budgets whose [Budget.startDate] <= [date] <= [Budget.endDate].
     */
    fun getBudgets(date: Long): Flow<List<Budget>>

    /**
     * Retrieves all budgets for the authenticated user regardless of active dates.
     */
    fun getAllBudgets(): Flow<List<Budget>>

    /**
     * Retrieves budget history for a specific category, sorted by [Budget.startDate] desc.
     */
    fun getBudgetHistory(category: String, limit: Int = 6): Flow<List<Budget>>

    /** Persists a new budget. The [Budget.userId] is set by the implementation. */
    suspend fun saveBudget(budget: Budget): Result<Unit>

    /** Updates an existing budget by its [Budget.id]. */
    suspend fun updateBudget(budget: Budget): Result<Unit>

    /** Permanently removes a budget by [budgetId]. */
    suspend fun deleteBudget(budgetId: String): Result<Unit>
}
