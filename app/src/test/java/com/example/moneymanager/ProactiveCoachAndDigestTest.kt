package com.example.moneymanager

import com.example.moneymanager.domain.model.AiInsight
import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.FinancialArchetype
import com.example.moneymanager.domain.model.FinancialDNA
import com.example.moneymanager.domain.model.FinancialDnaAiNarrative
import com.example.moneymanager.domain.model.NudgeTone
import com.example.moneymanager.domain.model.NudgeType
import com.example.moneymanager.domain.model.PredictedBudget
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.model.WeeklyDigest
import com.example.moneymanager.domain.repository.AiServiceRepository
import com.example.moneymanager.domain.usecase.ai.EvaluateSpendingNudgeUseCase
import com.example.moneymanager.domain.usecase.ai.GenerateWeeklyDigestUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ProactiveCoachAndDigestTest {

    private val coach = EvaluateSpendingNudgeUseCase()

    private class FakeDigestAiRepo(
        var commentary: Pair<String, String>? = null
    ) : AiServiceRepository {
        var clearedDigestCache = false
        override suspend fun queryAi(prompt: String, jsonMode: Boolean): Result<String> = Result.failure(UnsupportedOperationException())
        override suspend fun getCategoryPrediction(rawDescription: String, amount: Double, recentTransactions: List<Transaction>, availableCategories: List<String>): String = "General"
        override suspend fun getMonthlyInsight(currentMonthTransactions: List<Transaction>, previousMonthTransactions: List<Transaction>, languageCode: String): AiInsight? = null
        override suspend fun getPredictiveBudgets(threeMonthTransactions: List<Transaction>, availableCategories: List<String>, languageCode: String): List<PredictedBudget> = emptyList()
        override fun clearInsightCache() {}
        override suspend fun getFinancialDnaNarrative(dna: FinancialDNA, languageCode: String): FinancialDnaAiNarrative? = null
        override fun clearDnaCache() {}
        override suspend fun getWeeklyDigestCommentary(digest: WeeklyDigest, languageCode: String): Pair<String, String>? = commentary
        override fun clearWeeklyDigestCache() { clearedDigestCache = true }
    }

    private fun epoch(dayOfMonth: Int, hour: Int = 12): Long {
        return Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, dayOfMonth, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun dailySpikeNudge_triggersWhenTodayExceeds150PercentOfAverage() {
        val now = epoch(20, 18)
        val txs = listOf(
            Transaction(id = "d1", amount = 100_000.0, type = "expense", category = "Food", date = epoch(10)),
            Transaction(id = "d2", amount = 100_000.0, type = "expense", category = "Food", date = epoch(12)),
            Transaction(id = "d3", amount = 100_000.0, type = "expense", category = "Food", date = epoch(15)),
            Transaction(id = "today", amount = 250_000.0, type = "expense", category = "Shopping", date = epoch(20, 14))
        )

        val nudge = coach(txs, emptyList(), languageCode = "vi", nowMillis = now)
        assertNotNull(nudge)
        assertEquals(NudgeType.DAILY_SPIKE, nudge!!.type)
        assertEquals(NudgeTone.URGENT, nudge.tone)
    }

    @Test
    fun midCycleBudgetBurnNudge_triggersWhen80PercentSpentWithDaysLeft() {
        val now = epoch(15, 12)
        val budget = Budget(
            id = "b_food",
            category = "Food",
            allocatedAmount = 1_000_000.0,
            startDate = epoch(1, 0),
            endDate = epoch(31, 23)
        )
        val txs = listOf(
            Transaction(id = "e1", amount = 450_000.0, type = "expense", category = "Food", date = epoch(5)),
            Transaction(id = "e2", amount = 400_000.0, type = "expense", category = "Food", date = epoch(12))
        )

        val nudge = coach(txs, listOf(budget), languageCode = "en", nowMillis = now)
        assertNotNull(nudge)
        assertEquals(NudgeType.MID_MONTH_BUDGET_BURN, nudge!!.type)
        assertEquals(NudgeTone.WARNING, nudge.tone)
        assertTrue(nudge.title.contains("85%"))
    }

    @Test
    fun monthEndOverspendNudge_triggersInLastWeekWhenProjectedExceedsIncome() {
        val now = epoch(26, 12) // 5 days left in October (31 days)
        val txs = listOf(
            Transaction(id = "inc", amount = 10_000_000.0, type = "income", category = "Salary", date = epoch(1)),
            Transaction(id = "e1", amount = 4_500_000.0, type = "expense", category = "Rent", date = epoch(5)),
            Transaction(id = "e2", amount = 4_800_000.0, type = "expense", category = "Living", date = epoch(18))
        )
        // 9.3M in 26 days -> projected ~11.09M > 10M income
        val nudge = coach(txs, emptyList(), languageCode = "vi", nowMillis = now)
        assertNotNull(nudge)
        assertEquals(NudgeType.MONTH_END_OVERSPEND_RISK, nudge!!.type)
    }

    @Test
    fun weeklySavingsWinNudge_celebratesWithDnaArchetypePersonalization() {
        val now = epoch(20, 12)
        val txs = listOf(
            // Prior week (days 6..12): 2,000,000
            Transaction(id = "p1", amount = 1_000_000.0, type = "expense", category = "Food", date = epoch(8)),
            Transaction(id = "p2", amount = 1_000_000.0, type = "expense", category = "Shopping", date = epoch(11)),
            // This week (days 13..20): 1,200,000 (-40% win!)
            Transaction(id = "c1", amount = 600_000.0, type = "expense", category = "Food", date = epoch(15)),
            Transaction(id = "c2", amount = 600_000.0, type = "expense", category = "Food", date = epoch(18))
        )

        val nudge = coach(
            allTransactions = txs,
            activeBudgets = emptyList(),
            dnaArchetype = FinancialArchetype.SPCT,
            languageCode = "vi",
            nowMillis = now
        )
        assertNotNull(nudge)
        assertEquals(NudgeType.WEEKEND_SAVINGS_WIN, nudge!!.type)
        assertEquals(NudgeTone.CELEBRATION, nudge.tone)
        assertTrue(nudge.message.contains(FinancialArchetype.SPCT.titleVi))
    }

    @Test
    fun inactivityNudge_triggersAfterFiveDaysWithoutLogs() {
        val now = epoch(20, 12)
        val txs = listOf(
            Transaction(id = "old", amount = 50_000.0, type = "expense", category = "Food", date = epoch(12))
        )
        val nudge = coach(txs, emptyList(), languageCode = "en", nowMillis = now)
        assertNotNull(nudge)
        assertEquals(NudgeType.INACTIVITY_REMINDER, nudge!!.type)
        assertEquals(NudgeTone.GENTLE, nudge.tone)
    }

    @Test
    fun dismissedNudge_isFilteredOut() {
        val now = epoch(20, 12)
        val txs = listOf(
            Transaction(id = "old", amount = 50_000.0, type = "expense", category = "Food", date = epoch(12))
        )
        val first = coach(txs, emptyList(), languageCode = "en", nowMillis = now)
        assertNotNull(first)
        val afterDismiss = coach(txs, emptyList(), dismissedNudgeIds = setOf(first!!.id), languageCode = "en", nowMillis = now)
        assertEquals(null, afterDismiss)
    }

    @Test
    fun weeklyDigest_computesExactSpendDeltaTopCategoriesAndBudgetsOnTrack() {
        val now = epoch(20, 18)
        val useCase = GenerateWeeklyDigestUseCase(FakeDigestAiRepo())

        val txs = listOf(
            // Prior week (days 6..13): 2,000,000
            Transaction(id = "p1", amount = 2_000_000.0, type = "expense", category = "Food", date = epoch(9)),
            // This week (days 13..20): 1,500,000 (-25%)
            Transaction(id = "c1", amount = 900_000.0, type = "expense", category = "Food", date = epoch(16)),
            Transaction(id = "c2", amount = 400_000.0, type = "expense", category = "Transport", date = epoch(17)),
            Transaction(id = "c3", amount = 200_000.0, type = "expense", category = "Coffee", date = epoch(19))
        )
        val budgets = listOf(
            Budget(id = "b1", category = "Food", allocatedAmount = 5_000_000.0, startDate = epoch(1), endDate = epoch(31)),
            Budget(id = "b2", category = "Transport", allocatedAmount = 300_000.0, startDate = epoch(1), endDate = epoch(31))
        )

        val digest = useCase.computeLocalDigest(txs, budgets, languageCode = "vi", nowMillis = now)
        assertNotNull(digest)
        assertEquals(1_500_000.0, digest!!.totalSpent, 0.01)
        assertEquals(2_000_000.0, digest.previousWeekSpent, 0.01)
        assertEquals(-25.0, digest.deltaPercent, 0.01)
        assertEquals(3, digest.topCategories.size)
        assertEquals("Food", digest.topCategories[0].category)
        assertEquals(60f, digest.topCategories[0].sharePercent, 0.1f)
        assertEquals(1, digest.budgetsOnTrackCount)
        assertEquals(2, digest.budgetsTotalCount)
        assertEquals(900_000.0, digest.highestSpendDayAmount, 0.01)
    }

    @Test
    fun weeklyDigest_mergesAiCommentaryAndFallsBackOffline() = runBlocking {
        val now = epoch(20, 18)
        val txs = listOf(
            Transaction(id = "c1", amount = 500_000.0, type = "expense", category = "Food", date = epoch(18))
        )

        val onlineRepo = FakeDigestAiRepo(commentary = "AI Summary!" to "AI Tip!")
        val onlineUseCase = GenerateWeeklyDigestUseCase(onlineRepo)
        val enriched = onlineUseCase(txs, emptyList(), languageCode = "en", forceRefresh = true, nowMillis = now)
        assertNotNull(enriched)
        assertTrue(onlineRepo.clearedDigestCache)
        assertTrue(enriched!!.isAiEnhanced)
        assertEquals("AI Summary!", enriched.aiSummary)
        assertEquals("AI Tip!", enriched.actionableTip)

        val offlineUseCase = GenerateWeeklyDigestUseCase(FakeDigestAiRepo(commentary = null))
        val fallback = offlineUseCase(txs, emptyList(), languageCode = "vi", nowMillis = now)
        assertNotNull(fallback)
        assertFalse(fallback!!.isAiEnhanced)
        assertTrue(fallback.aiSummary.isNotBlank())
        assertTrue(fallback.actionableTip.isNotBlank())
    }
}
