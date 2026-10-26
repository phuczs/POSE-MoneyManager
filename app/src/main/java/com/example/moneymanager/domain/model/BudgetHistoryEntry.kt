package com.example.moneymanager.domain.model

/**
 * Domain model representing a historical snapshot of a budget cycle.
 * Used for comparing progress and spending across periods (Phase 4.2).
 */
data class BudgetHistoryEntry(
    val budgetId: String = "",
    val category: String = "",
    val allocated: Double = 0.0,
    val spent: Double = 0.0,
    val periodStart: Long = 0L,
    val periodEnd: Long = 0L,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val rolloverIn: Double = 0.0,
    val rolloverOut: Double = 0.0
) {
    /** Amount saved during this period. Can be negative if over budget. */
    val savedAmount: Double get() = (allocated + rolloverIn) - spent

    /** Budget utilization ratio (e.g. 0.85 = 85% spent). */
    val utilizationRate: Float
        get() {
            val totalCap = allocated + rolloverIn
            return if (totalCap > 0) (spent / totalCap).toFloat() else 0f
        }
}
