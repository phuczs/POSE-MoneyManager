package com.example.moneymanager

import com.example.moneymanager.domain.model.AiInsight
import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.FinancialArchetype
import com.example.moneymanager.domain.model.FinancialDNA
import com.example.moneymanager.domain.model.FinancialDnaAiNarrative
import com.example.moneymanager.domain.model.PredictedBudget
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.model.WeeklyDigest
import com.example.moneymanager.domain.repository.AiServiceRepository
import com.example.moneymanager.domain.usecase.ai.AnalyzeFinancialDNAUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AnalyzeFinancialDNATest {

    private class FakeAiServiceRepository(
        var narrativeToReturn: FinancialDnaAiNarrative? = null
    ) : AiServiceRepository {
        var clearDnaCacheCalled = false

        override suspend fun queryAi(prompt: String, jsonMode: Boolean): Result<String> =
            Result.failure(UnsupportedOperationException())

        override suspend fun getCategoryPrediction(
            rawDescription: String,
            amount: Double,
            recentTransactions: List<Transaction>,
            availableCategories: List<String>
        ): String = "General"

        override suspend fun getMonthlyInsight(
            currentMonthTransactions: List<Transaction>,
            previousMonthTransactions: List<Transaction>,
            languageCode: String
        ): AiInsight? = null

        override suspend fun getPredictiveBudgets(
            threeMonthTransactions: List<Transaction>,
            availableCategories: List<String>,
            languageCode: String
        ): List<PredictedBudget> = emptyList()

        override fun clearInsightCache() {}

        override suspend fun getFinancialDnaNarrative(
            dna: FinancialDNA,
            languageCode: String
        ): FinancialDnaAiNarrative? = narrativeToReturn

        override fun clearDnaCache() {
            clearDnaCacheCalled = true
        }

        override suspend fun getWeeklyDigestCommentary(
            digest: WeeklyDigest,
            languageCode: String
        ): Pair<String, String>? = null

        override fun clearWeeklyDigestCache() {}
    }

    private fun epochForDaysAgo(daysAgo: Int, hourOfDay: Int = 12): Long {
        return Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, hourOfDay, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }.timeInMillis
    }

    private val refNow: Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 15, 18, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun insufficientTransactions_returnsNull() {
        val useCase = AnalyzeFinancialDNAUseCase(FakeAiServiceRepository())
        val txs = listOf(
            Transaction(id = "1", amount = 50_000.0, type = "expense", category = "Food", date = epochForDaysAgo(1)),
            Transaction(id = "2", amount = 100_000.0, type = "expense", category = "Food", date = epochForDaysAgo(2))
        )
        assertNull(useCase.computeLocalDna(txs, emptyList(), refNow, "vi"))
    }

    @Test
    fun highSavingsSteadyPlannerTargeted_classifiesAsSPCT() {
        val useCase = AnalyzeFinancialDNAUseCase(FakeAiServiceRepository())
        val startRange = epochForDaysAgo(60)
        val endRange = refNow

        val txs = listOf(
            // Strong income
            Transaction(id = "inc1", amount = 20_000_000.0, type = "income", category = "Salary", date = epochForDaysAgo(30)),
            Transaction(id = "inc2", amount = 20_000_000.0, type = "income", category = "Salary", date = epochForDaysAgo(1)),
            // Very steady weekly expenses concentrated in Food & Transport
            Transaction(id = "e1", amount = 1_500_000.0, type = "expense", category = "Food", date = epochForDaysAgo(28, 12)),
            Transaction(id = "e2", amount = 500_000.0, type = "expense", category = "Transport", date = epochForDaysAgo(27, 9)),
            Transaction(id = "e3", amount = 1_500_000.0, type = "expense", category = "Food", date = epochForDaysAgo(21, 12)),
            Transaction(id = "e4", amount = 500_000.0, type = "expense", category = "Transport", date = epochForDaysAgo(20, 9)),
            Transaction(id = "e5", amount = 1_500_000.0, type = "expense", category = "Food", date = epochForDaysAgo(14, 12)),
            Transaction(id = "e6", amount = 500_000.0, type = "expense", category = "Transport", date = epochForDaysAgo(13, 9)),
            Transaction(id = "e7", amount = 1_500_000.0, type = "expense", category = "Food", date = epochForDaysAgo(7, 12)),
            Transaction(id = "e8", amount = 500_000.0, type = "expense", category = "Transport", date = epochForDaysAgo(6, 9))
        )
        val budgets = listOf(
            Budget(id = "b1", category = "Food", allocatedAmount = 8_000_000.0, startDate = startRange, endDate = endRange, isRecurring = true),
            Budget(id = "b2", category = "Transport", allocatedAmount = 3_000_000.0, startDate = startRange, endDate = endRange, isRecurring = true)
        )

        val dna = useCase.computeLocalDna(txs, budgets, refNow, "vi")
        assertNotNull(dna)
        assertEquals("SPCT", dna!!.code)
        assertEquals(FinancialArchetype.SPCT, dna.archetype)
        assertTrue("Expected high savingsScore, got ${dna.savingsScore}", dna.savingsScore >= 80)
        assertTrue("Expected high plannerScore, got ${dna.plannerScore}", dna.plannerScore >= 70)
        assertTrue("Expected high stabilityScore, got ${dna.stabilityScore}", dna.stabilityScore >= 80)
        assertTrue("Expected high focusScore, got ${dna.focusScore}", dna.focusScore >= 70)
        assertEquals(80.0, dna.savingsRatePercent, 0.5)
    }

    @Test
    fun deficitVolatileSpontaneousDiverse_classifiesAsIFVD() {
        val useCase = AnalyzeFinancialDNAUseCase(FakeAiServiceRepository())

        val txs = listOf(
            // Small income, huge deficit spending
            Transaction(id = "inc1", amount = 5_000_000.0, type = "income", category = "Salary", date = epochForDaysAgo(25)),
            // Wildly varying amounts across 8 distinct categories, late-night shopping
            Transaction(id = "e1", amount = 50_000.0, type = "expense", category = "Food", date = epochForDaysAgo(28, 23)),
            Transaction(id = "e2", amount = 80_000.0, type = "expense", category = "Coffee", date = epochForDaysAgo(21, 23)),
            Transaction(id = "e3", amount = 3_500_000.0, type = "expense", category = "Shopping", date = epochForDaysAgo(14, 1)),
            Transaction(id = "e4", amount = 2_800_000.0, type = "expense", category = "Travel", date = epochForDaysAgo(14, 2)),
            Transaction(id = "e5", amount = 2_200_000.0, type = "expense", category = "Tech", date = epochForDaysAgo(13, 23)),
            Transaction(id = "e6", amount = 1_900_000.0, type = "expense", category = "Fashion", date = epochForDaysAgo(13, 0)),
            Transaction(id = "e7", amount = 1_600_000.0, type = "expense", category = "Gaming", date = epochForDaysAgo(12, 2)),
            Transaction(id = "e8", amount = 1_400_000.0, type = "expense", category = "Gifts", date = epochForDaysAgo(12, 23)),
            Transaction(id = "e9", amount = 1_200_000.0, type = "expense", category = "Entertainment", date = epochForDaysAgo(12, 1)),
            Transaction(id = "e10", amount = 40_000.0, type = "expense", category = "Transport", date = epochForDaysAgo(3, 23))
        )

        val dna = useCase.computeLocalDna(txs, emptyList(), refNow, "en")
        assertNotNull(dna)
        assertEquals("IFVD", dna!!.code)
        assertEquals(FinancialArchetype.IFVD, dna.archetype)
        assertTrue("Expected low savingsScore, got ${dna.savingsScore}", dna.savingsScore < 50)
        assertTrue("Expected low plannerScore, got ${dna.plannerScore}", dna.plannerScore < 50)
        assertTrue("Expected low stabilityScore, got ${dna.stabilityScore}", dna.stabilityScore < 50)
        assertTrue("Expected low focusScore, got ${dna.focusScore}", dna.focusScore < 50)
    }

    @Test
    fun allSixteenArchetypes_haveValidBilingualMetadata() {
        assertEquals(16, FinancialArchetype.entries.size)
        val codes = FinancialArchetype.entries.map { it.code }.toSet()
        assertEquals(16, codes.size)

        FinancialArchetype.entries.forEach { archetype ->
            assertEquals(archetype, FinancialArchetype.fromCode(archetype.code))
            assertTrue(archetype.title("vi").isNotBlank())
            assertTrue(archetype.title("en").isNotBlank())
            assertTrue(archetype.tagline("vi").isNotBlank())
            assertTrue(archetype.tagline("en").isNotBlank())
            assertTrue(archetype.defaultStrengths("vi").isNotEmpty())
            assertTrue(archetype.defaultStrengths("en").isNotEmpty())
            assertTrue(archetype.defaultImprovements("vi").isNotEmpty())
            assertTrue(archetype.defaultImprovements("en").isNotEmpty())
        }
    }

    @Test
    fun invoke_mergesAiNarrativeAndSupportsForceRefresh() = runBlocking {
        val fakeRepo = FakeAiServiceRepository(
            narrativeToReturn = FinancialDnaAiNarrative(
                narrative = "AI custom story tailored to your exact spending.",
                strengths = listOf("AI Strength 1", "AI Strength 2"),
                improvements = listOf("AI Tip 1"),
                dnaCode = "SPCT",
                languageCode = "en"
            )
        )
        val useCase = AnalyzeFinancialDNAUseCase(fakeRepo)

        val txs = listOf(
            Transaction(id = "i1", amount = 10_000_000.0, type = "income", category = "Salary", date = epochForDaysAgo(10)),
            Transaction(id = "e1", amount = 500_000.0, type = "expense", category = "Food", date = epochForDaysAgo(9)),
            Transaction(id = "e2", amount = 500_000.0, type = "expense", category = "Food", date = epochForDaysAgo(7)),
            Transaction(id = "e3", amount = 500_000.0, type = "expense", category = "Food", date = epochForDaysAgo(5)),
            Transaction(id = "e4", amount = 500_000.0, type = "expense", category = "Food", date = epochForDaysAgo(2))
        )

        val dna = useCase(txs, emptyList(), languageCode = "en", forceRefresh = true, nowMillis = refNow)
        assertNotNull(dna)
        assertTrue(fakeRepo.clearDnaCacheCalled)
        assertTrue(dna!!.isAiEnhanced)
        assertEquals("AI custom story tailored to your exact spending.", dna.aiNarrative)
        assertEquals(listOf("AI Strength 1", "AI Strength 2"), dna.strengths)
        assertEquals(listOf("AI Tip 1"), dna.improvements)
    }

    @Test
    fun invoke_offlineFallback_returnsDeterministicLocalDna() = runBlocking {
        val fakeRepo = FakeAiServiceRepository(narrativeToReturn = null)
        val useCase = AnalyzeFinancialDNAUseCase(fakeRepo)

        val txs = listOf(
            Transaction(id = "i1", amount = 10_000_000.0, type = "income", category = "Salary", date = epochForDaysAgo(10)),
            Transaction(id = "e1", amount = 500_000.0, type = "expense", category = "Food", date = epochForDaysAgo(9)),
            Transaction(id = "e2", amount = 500_000.0, type = "expense", category = "Food", date = epochForDaysAgo(7)),
            Transaction(id = "e3", amount = 500_000.0, type = "expense", category = "Food", date = epochForDaysAgo(5)),
            Transaction(id = "e4", amount = 500_000.0, type = "expense", category = "Food", date = epochForDaysAgo(2))
        )

        val dna = useCase(txs, emptyList(), languageCode = "vi", nowMillis = refNow)
        assertNotNull(dna)
        assertFalse(dna!!.isAiEnhanced)
        assertTrue(dna.aiNarrative.isNotBlank())
        assertTrue(dna.strengths.isNotEmpty())
        assertTrue(dna.improvements.isNotEmpty())
    }

    @Test
    fun zeroIncomeWithBudgets_producesBoundedScoresWithoutNaN() {
        val useCase = AnalyzeFinancialDNAUseCase(FakeAiServiceRepository())
        val txs = listOf(
            Transaction(id = "e1", amount = 200_000.0, type = "expense", category = "Food", date = epochForDaysAgo(10)),
            Transaction(id = "e2", amount = 300_000.0, type = "expense", category = "Food", date = epochForDaysAgo(8)),
            Transaction(id = "e3", amount = 250_000.0, type = "expense", category = "Transport", date = epochForDaysAgo(6)),
            Transaction(id = "e4", amount = 150_000.0, type = "expense", category = "Bills", date = epochForDaysAgo(4)),
            Transaction(id = "e5", amount = 100_000.0, type = "expense", category = "Food", date = epochForDaysAgo(2))
        )
        val budgets = listOf(
            Budget(id = "b1", category = "Food", allocatedAmount = 2_000_000.0, startDate = epochForDaysAgo(30), endDate = refNow)
        )

        val dna = useCase.computeLocalDna(txs, budgets, refNow, "vi")
        assertNotNull(dna)
        assertTrue(dna!!.savingsScore in 0..100)
        assertTrue(dna.plannerScore in 0..100)
        assertTrue(dna.stabilityScore in 0..100)
        assertTrue(dna.focusScore in 0..100)
        assertFalse(dna.savingsRatePercent.isNaN())
        assertFalse(dna.weeklyVariancePercent.isNaN())
        assertFalse(dna.topCategoriesSharePercent.isNaN())
    }
}
