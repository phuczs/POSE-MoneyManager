package com.example.moneymanager

import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.BudgetHistoryEntry
import com.example.moneymanager.domain.model.BudgetPeriod
import com.example.moneymanager.domain.model.BudgetStatus
import com.example.moneymanager.domain.model.BudgetWithProgress
import com.example.moneymanager.domain.repository.BudgetRepository
import com.example.moneymanager.domain.usecase.budget.AutoRenewBudgetsUseCase
import com.example.moneymanager.domain.usecase.budget.SaveBudgetUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BudgetIntelligenceTest {

    @Test
    fun testBudgetPeriod_fromString_defaultsToMonthly() {
        assertEquals(BudgetPeriod.WEEKLY, BudgetPeriod.fromString("WEEKLY"))
        assertEquals(BudgetPeriod.BI_WEEKLY, BudgetPeriod.fromString("bi_weekly"))
        assertEquals(BudgetPeriod.MONTHLY, BudgetPeriod.fromString("MONTHLY"))
        assertEquals(BudgetPeriod.QUARTERLY, BudgetPeriod.fromString("quarterly"))
        assertEquals(BudgetPeriod.CUSTOM, BudgetPeriod.fromString("CUSTOM"))
        // Fallbacks
        assertEquals(BudgetPeriod.MONTHLY, BudgetPeriod.fromString(null))
        assertEquals(BudgetPeriod.MONTHLY, BudgetPeriod.fromString("UNKNOWN_VALUE"))
    }

    @Test
    fun testBudgetEffectiveAmount_includesRollover() {
        val baseBudget = Budget(
            allocatedAmount = 2_000_000.0,
            rolloverAmount = 500_000.0
        )
        assertEquals(2_500_000.0, baseBudget.effectiveAmount, 0.01)

        val noRolloverBudget = Budget(allocatedAmount = 1_000_000.0)
        assertEquals(1_000_000.0, noRolloverBudget.effectiveAmount, 0.01)
    }

    @Test
    fun testBudgetWithProgress_statusAndProgressCalculations() {
        val budget = Budget(
            allocatedAmount = 1_000_000.0,
            rolloverAmount = 0.0,
            alertThresholdWarning = 0.5f,
            alertThresholdCritical = 0.9f
        )

        // 1. Under 50% -> Normal
        val wp1 = BudgetWithProgress(budget, spentAmount = 300_000.0)
        assertEquals(0.3f, wp1.progress, 0.01f)
        assertEquals(BudgetStatus.Normal, wp1.status)

        // 2. Between 50% and 90% -> Warning
        val wp2 = BudgetWithProgress(budget, spentAmount = 750_000.0)
        assertEquals(0.75f, wp2.progress, 0.01f)
        assertEquals(BudgetStatus.Warning, wp2.status)

        // 3. Above 90% -> Over
        val wp3 = BudgetWithProgress(budget, spentAmount = 950_000.0)
        assertEquals(0.95f, wp3.progress, 0.01f)
        assertEquals(BudgetStatus.Over, wp3.status)
    }

    @Test
    fun testBudgetHistoryEntry_savedAmountAndUtilization() {
        // Saved amount when under budget
        val entry1 = BudgetHistoryEntry(
            allocated = 5_000_000.0,
            rolloverIn = 500_000.0,
            spent = 4_000_000.0
        )
        assertEquals(1_500_000.0, entry1.savedAmount, 0.01)
        assertEquals(4_000_000.0 / 5_500_000.0, entry1.utilizationRate.toDouble(), 0.01)

        // Deficit when over budget
        val entry2 = BudgetHistoryEntry(
            allocated = 3_000_000.0,
            rolloverIn = 0.0,
            spent = 3_500_000.0
        )
        assertEquals(-500_000.0, entry2.savedAmount, 0.01)
        assertTrue(entry2.utilizationRate > 1.0f)
    }

    @Test
    fun testAutoRenew_computeNextPeriod_monthly() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        val endMs = cal.timeInMillis

        val budget = Budget(
            startDate = startMs,
            endDate = endMs,
            period = BudgetPeriod.MONTHLY
        )

        val (nextStart, nextEnd) = AutoRenewBudgetsUseCase.computeNextPeriod(budget)

        val nextStartCal = Calendar.getInstance().apply { timeInMillis = nextStart }
        val nextEndCal = Calendar.getInstance().apply { timeInMillis = nextEnd }

        assertEquals(Calendar.NOVEMBER, nextStartCal.get(Calendar.MONTH))
        assertEquals(1, nextStartCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.NOVEMBER, nextEndCal.get(Calendar.MONTH))
        assertEquals(30, nextEndCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testAutoRenew_computeNextPeriod_weekly() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 5, 0, 0, 0) // Monday
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 6) // Sunday
        val endMs = cal.timeInMillis

        val budget = Budget(
            startDate = startMs,
            endDate = endMs,
            period = BudgetPeriod.WEEKLY
        )

        val (nextStart, nextEnd) = AutoRenewBudgetsUseCase.computeNextPeriod(budget)
        val nextStartCal = Calendar.getInstance().apply { timeInMillis = nextStart }

        assertEquals(12, nextStartCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testAutoRenewBudgetsUseCase_rolloverCalculationAndCreation() = runBlocking {
        val createdBudgets = mutableListOf<Budget>()

        val fakeRepo = object : BudgetRepository {
            override fun getBudgets(date: Long) = flowOf(emptyList<Budget>())
            override fun getAllBudgets() = flowOf(emptyList<Budget>())
            override fun getBudgetHistory(category: String, limit: Int) = flowOf(emptyList<Budget>())
            override suspend fun saveBudget(budget: Budget): Result<Unit> {
                createdBudgets.add(budget)
                return Result.success(Unit)
            }
            override suspend fun updateBudget(budget: Budget) = Result.success(Unit)
            override suspend fun deleteBudget(budgetId: String) = Result.success(Unit)
        }

        val saveUseCase = SaveBudgetUseCase(fakeRepo)
        val autoRenewUseCase = AutoRenewBudgetsUseCase(fakeRepo, saveUseCase)

        val now = System.currentTimeMillis()
        val expiredBudget = Budget(
            id = "b1",
            category = "Food",
            allocatedAmount = 3_000_000.0,
            startDate = now - 35 * 86400000L,
            endDate = now - 5 * 86400000L,
            period = BudgetPeriod.MONTHLY,
            isRecurring = true,
            rolloverEnabled = true
        )

        val expiredWP = BudgetWithProgress(expiredBudget, spentAmount = 2_200_000.0)

        val createdCount = autoRenewUseCase(
            expiredBudgets = listOf(expiredWP),
            existingBudgets = emptyList()
        )

        assertEquals(1, createdCount)
        assertEquals(1, createdBudgets.size)
        val renewed = createdBudgets[0]
        assertEquals("Food", renewed.category)
        assertEquals(3_000_000.0, renewed.allocatedAmount, 0.01)
        // Rollover: 3,000,000 - 2,200,000 = 800,000
        assertEquals(800_000.0, renewed.rolloverAmount, 0.01)
        assertEquals(3_800_000.0, renewed.effectiveAmount, 0.01)
        assertTrue(renewed.isRecurring)
        assertTrue(renewed.rolloverEnabled)
    }

    @Test
    fun testAutoRenewBudgetsUseCase_idempotentSkip() = runBlocking {
        val createdBudgets = mutableListOf<Budget>()

        val fakeRepo = object : BudgetRepository {
            override fun getBudgets(date: Long) = flowOf(emptyList<Budget>())
            override fun getAllBudgets() = flowOf(emptyList<Budget>())
            override fun getBudgetHistory(category: String, limit: Int) = flowOf(emptyList<Budget>())
            override suspend fun saveBudget(budget: Budget): Result<Unit> {
                createdBudgets.add(budget)
                return Result.success(Unit)
            }
            override suspend fun updateBudget(budget: Budget) = Result.success(Unit)
            override suspend fun deleteBudget(budgetId: String) = Result.success(Unit)
        }

        val saveUseCase = SaveBudgetUseCase(fakeRepo)
        val autoRenewUseCase = AutoRenewBudgetsUseCase(fakeRepo, saveUseCase)

        val now = System.currentTimeMillis()
        val expiredBudget = Budget(
            id = "b1",
            category = "Shopping",
            allocatedAmount = 1_000_000.0,
            startDate = now - 40 * 86400000L,
            endDate = now - 10 * 86400000L,
            period = BudgetPeriod.MONTHLY,
            isRecurring = true,
            rolloverEnabled = false
        )

        val (nextStart, nextEnd) = AutoRenewBudgetsUseCase.computeNextPeriod(expiredBudget)
        val alreadyExisting = Budget(
            id = "b2",
            category = "Shopping",
            allocatedAmount = 1_000_000.0,
            startDate = nextStart,
            endDate = nextEnd
        )

        val expiredWP = BudgetWithProgress(expiredBudget, spentAmount = 900_000.0)

        val createdCount = autoRenewUseCase(
            expiredBudgets = listOf(expiredWP),
            existingBudgets = listOf(alreadyExisting)
        )

        // Should skip because next cycle already exists
        assertEquals(0, createdCount)
        assertTrue(createdBudgets.isEmpty())
    }
}
