package com.example.moneymanager.domain.usecase.budget

import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.BudgetPeriod
import com.example.moneymanager.domain.model.BudgetWithProgress
import com.example.moneymanager.domain.repository.BudgetRepository
import java.util.Calendar
import javax.inject.Inject

/**
 * Phase 4.1: Auto-Renew Budgets Use Case
 *
 * When a budget period expires and [Budget.isRecurring] is true, this use case:
 * 1. Computes the next period's start/end dates based on [Budget.period].
 * 2. Calculates rollover amount if [Budget.rolloverEnabled] is true.
 * 3. Creates a new budget for the next period.
 *
 * Idempotent: Will not create a duplicate if a budget for the next period
 * already exists for the same category + user.
 */
class AutoRenewBudgetsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val saveBudgetUseCase: SaveBudgetUseCase
) {
    /**
     * Checks all expired recurring budgets and creates their successors.
     *
     * @param expiredBudgets List of [BudgetWithProgress] whose endDate < now and isRecurring == true.
     * @param existingBudgets All currently active budgets — used for duplicate detection.
     * @return Number of new budgets created.
     */
    suspend operator fun invoke(
        expiredBudgets: List<BudgetWithProgress>,
        existingBudgets: List<Budget>
    ): Int {
        var created = 0

        for (budgetWP in expiredBudgets) {
            val budget = budgetWP.budget
            if (!budget.isRecurring) continue

            val (nextStart, nextEnd) = computeNextPeriod(budget)

            // Idempotent check: skip if a budget for same category in next period already exists
            val alreadyExists = existingBudgets.any { existing ->
                existing.category == budget.category &&
                        existing.startDate == nextStart &&
                        existing.endDate == nextEnd
            }
            if (alreadyExists) continue

            // Calculate rollover
            val rollover = if (budget.rolloverEnabled) {
                (budget.effectiveAmount - budgetWP.spentAmount).coerceAtLeast(0.0)
            } else {
                0.0
            }

            val newBudget = Budget(
                category = budget.category,
                allocatedAmount = budget.allocatedAmount,
                startDate = nextStart,
                endDate = nextEnd,
                period = budget.period,
                isRecurring = budget.isRecurring,
                rolloverEnabled = budget.rolloverEnabled,
                rolloverAmount = rollover,
                alertThresholdWarning = budget.alertThresholdWarning,
                alertThresholdCritical = budget.alertThresholdCritical,
                notes = budget.notes
            )

            val result = saveBudgetUseCase(newBudget)
            if (result.isSuccess) created++
        }

        return created
    }

    companion object {
        /**
         * Computes the next period's start and end timestamps based on the budget's [BudgetPeriod].
         *
         * @return Pair of (nextStartEpochMs, nextEndEpochMs)
         */
        fun computeNextPeriod(budget: Budget): Pair<Long, Long> {
            val cal = Calendar.getInstance().apply { timeInMillis = budget.startDate }

            return when (budget.period) {
                BudgetPeriod.WEEKLY -> {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                    val start = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_YEAR, 6)
                    setEndOfDay(cal)
                    Pair(start, cal.timeInMillis)
                }
                BudgetPeriod.BI_WEEKLY -> {
                    cal.add(Calendar.WEEK_OF_YEAR, 2)
                    val start = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_YEAR, 13)
                    setEndOfDay(cal)
                    Pair(start, cal.timeInMillis)
                }
                BudgetPeriod.MONTHLY -> {
                    cal.add(Calendar.MONTH, 1)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    setStartOfDay(cal)
                    val start = cal.timeInMillis
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    setEndOfDay(cal)
                    Pair(start, cal.timeInMillis)
                }
                BudgetPeriod.QUARTERLY -> {
                    cal.add(Calendar.MONTH, 3)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    setStartOfDay(cal)
                    val start = cal.timeInMillis
                    cal.add(Calendar.MONTH, 2)
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    setEndOfDay(cal)
                    Pair(start, cal.timeInMillis)
                }
                BudgetPeriod.CUSTOM -> {
                    // For custom periods, advance by the same duration
                    val duration = budget.endDate - budget.startDate
                    val nextStart = budget.endDate + 1 // Start the next day
                    val nextEnd = nextStart + duration
                    Pair(nextStart, nextEnd)
                }
            }
        }

        private fun setStartOfDay(cal: Calendar) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        private fun setEndOfDay(cal: Calendar) {
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
        }
    }
}
