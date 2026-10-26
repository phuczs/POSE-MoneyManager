package com.example.moneymanager.domain.model

/**
 * Represents the time period / cycle length for a budget.
 *
 * Phase 4.1: Flexible Budget Periods — allows users to choose how long a budget cycle lasts.
 */
enum class BudgetPeriod {
    WEEKLY,
    BI_WEEKLY,
    MONTHLY,
    QUARTERLY,
    CUSTOM;

    companion object {
        /** Safe parse with fallback to [MONTHLY] for backward compatibility. */
        fun fromString(value: String?): BudgetPeriod =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
    }
}

/**
 * Pure domain model for a spending budget.
 *
 * Clean Architecture note:
 * - No Firebase or Java Date dependencies.
 * - [startDate] and [endDate] are epoch milliseconds — mappers handle conversion.
 * - This pure model does not contain [spentAmount] to avoid synchronization issues;
 *   spent amounts are dynamically calculated and wrapped in [BudgetWithProgress].
 *
 * Phase 4.1 additions:
 * - [period]: Budget cycle length (Weekly, Bi-weekly, Monthly, Quarterly, Custom).
 * - [isRecurring]: When true, a new budget is auto-created for the next period upon expiry.
 * - [rolloverEnabled]: When true, unspent amount carries over to the next period's budget.
 * - [rolloverAmount]: Amount rolled over from the previous period (added to allocatedAmount).
 * - [alertThresholdWarning] / [alertThresholdCritical]: User-customizable alert thresholds.
 * - [notes]: Optional user memo/note for the budget.
 */
data class Budget(
    val id: String = "",
    val userId: String = "",
    val category: String = "",
    val allocatedAmount: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(), // epoch milliseconds
    val endDate: Long = System.currentTimeMillis(),   // epoch milliseconds
    // --- Phase 4.1: Flexible Periods & Auto-Recurring ---
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val isRecurring: Boolean = false,
    val rolloverEnabled: Boolean = false,
    val rolloverAmount: Double = 0.0,
    val alertThresholdWarning: Float = 0.5f,
    val alertThresholdCritical: Float = 0.9f,
    val notes: String = ""
) {
    /** Total effective budget including rollover from previous period. */
    val effectiveAmount: Double get() = allocatedAmount + rolloverAmount
}

/**
 * UI/Domain wrapper that combines a pure [Budget] with its dynamically calculated
 * [spentAmount], providing progress, status, and smart budget intelligence computations.
 */
data class BudgetWithProgress(
    val budget: Budget,
    val spentAmount: Double
) {
    private companion object {
        const val MS_PER_DAY = 24 * 60 * 60 * 1000L
    }

    /** 0.0 – 1.0+ progress ratio against effective amount. Values above 1.0 indicate over-budget. */
    val progress: Float
        get() = if (budget.effectiveAmount > 0) (spentAmount / budget.effectiveAmount).toFloat() else 0f

    /** Budget status using the budget's customizable thresholds. */
    val status: BudgetStatus
        get() = when {
            progress < budget.alertThresholdWarning  -> BudgetStatus.Normal
            progress <= budget.alertThresholdCritical -> BudgetStatus.Warning
            else                                     -> BudgetStatus.Over
        }

    /** Number of calendar days remaining until budget period ends. */
    val daysRemaining: Int
        get() {
            val now = System.currentTimeMillis()
            return ((budget.endDate - now) / MS_PER_DAY).toInt().coerceAtLeast(0)
        }

    /** How much the user can spend per remaining day to stay within budget. */
    val dailyAllowance: Double
        get() {
            val remaining = budget.effectiveAmount - spentAmount
            return if (daysRemaining > 0) (remaining / daysRemaining).coerceAtLeast(0.0) else 0.0
        }

    /** Projected overspend amount by end of period based on current burn rate. Returns 0.0 if on track. */
    val projectedOverspend: Double
        get() {
            val daysElapsed = ((System.currentTimeMillis() - budget.startDate) / MS_PER_DAY).toInt().coerceAtLeast(1)
            val totalDays = ((budget.endDate - budget.startDate) / MS_PER_DAY).toInt().coerceAtLeast(1)
            val projectedTotal = (spentAmount / daysElapsed) * totalDays
            return (projectedTotal - budget.effectiveAmount).coerceAtLeast(0.0)
        }

    /** Remaining amount (can be negative if over budget). */
    val remainingAmount: Double
        get() = budget.effectiveAmount - spentAmount
}

/** Represents the different states of a budget relative to its allocation. */
enum class BudgetStatus {
    Normal,  // Below warning threshold
    Warning, // Between warning and critical thresholds
    Over     // Above critical threshold
}
