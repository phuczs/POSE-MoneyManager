package com.example.moneymanager.domain.usecase.ai

import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.FinancialArchetype
import com.example.moneymanager.domain.model.FinancialDNA
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.AiServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Analyzes a user's transaction & budget history to construct their personalized
 * 4-axis [FinancialDNA] profile (MBTI-style financial personality).
 *
 * Architecture:
 * - Deterministic statistical engine runs 100% locally on [Dispatchers.Default].
 * - Optional LLM enrichment via [AiServiceRepository] crafts a warm personalized
 *   narrative while gracefully falling back to local defaults when offline.
 */
class AnalyzeFinancialDNAUseCase @Inject constructor(
    private val aiServiceRepository: AiServiceRepository
) {
    companion object {
        const val MIN_TRANSACTIONS_REQUIRED = 5
        const val MIN_EXPENSES_REQUIRED = 3
        private const val WINDOW_DAYS = 90L
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val WEEK_MS = 7L * DAY_MS
        private const val AI_TIMEOUT_MS = 18_000L
    }

    suspend operator fun invoke(
        transactions: List<Transaction>,
        budgets: List<Budget>,
        languageCode: String = "vi",
        forceRefresh: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): FinancialDNA? {
        val localDna = withContext(Dispatchers.Default) {
            computeLocalDna(
                transactions = transactions,
                budgets = budgets,
                nowMillis = nowMillis,
                languageCode = languageCode
            )
        } ?: return null

        if (forceRefresh) {
            aiServiceRepository.clearDnaCache()
        }

        val aiNarrative = withTimeoutOrNull(AI_TIMEOUT_MS) {
            aiServiceRepository.getFinancialDnaNarrative(
                dna = localDna,
                languageCode = languageCode
            )
        }

        return if (aiNarrative != null) {
            localDna.copy(
                aiNarrative = aiNarrative.narrative,
                strengths = aiNarrative.strengths.ifEmpty { localDna.strengths },
                improvements = aiNarrative.improvements.ifEmpty { localDna.improvements },
                isAiEnhanced = true,
                generatedAt = aiNarrative.generatedAt
            )
        } else {
            localDna
        }
    }

    /**
     * Pure deterministic calculation of the 4 Financial DNA axes and metrics.
     * Kept public/internal-friendly for fast, deterministic unit testing.
     */
    fun computeLocalDna(
        transactions: List<Transaction>,
        budgets: List<Budget>,
        nowMillis: Long = System.currentTimeMillis(),
        languageCode: String = "vi"
    ): FinancialDNA? {
        if (transactions.size < MIN_TRANSACTIONS_REQUIRED) return null

        val cutoff = nowMillis - WINDOW_DAYS * DAY_MS
        val recentTxs = transactions.filter { it.date >= cutoff }
        val analysisTxs = if (recentTxs.size >= MIN_TRANSACTIONS_REQUIRED) recentTxs else transactions

        val expenseTxs = analysisTxs.filter { it.type.equals("expense", ignoreCase = true) && it.amount > 0.0 }
        if (expenseTxs.size < MIN_EXPENSES_REQUIRED) return null

        val incomeTxs = analysisTxs.filter { it.type.equals("income", ignoreCase = true) && it.amount > 0.0 }

        val totalExpense = expenseTxs.sumOf { it.amount }
        val totalIncome = incomeTxs.sumOf { it.amount }

        val earliestDate = analysisTxs.minOf { it.date }
        val latestDate = analysisTxs.maxOf { it.date }
        val analyzedDays = (((latestDate - earliestDate) / DAY_MS).toInt() + 1).coerceAtLeast(1)

        // 1. Axis 1: Savings vs Indulgence (0..100)
        val (savingsScore, savingsRatePercent) = computeSavingsAxis(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            budgets = budgets,
            expenseTxs = expenseTxs
        )

        // 2. Axis 2: Planner vs Spontaneous (0..100)
        val plannerScore = computePlannerAxis(
            expenseTxs = expenseTxs,
            budgets = budgets
        )

        // 3. Axis 3: Consistent vs Volatile (0..100)
        val (stabilityScore, weeklyVariancePercent) = computeStabilityAxis(
            expenseTxs = expenseTxs,
            analyzedDays = analyzedDays
        )

        // 4. Axis 4: Targeted vs Diverse (0..100)
        val (focusScore, topCategoriesSharePercent, topCategories) = computeFocusAxis(
            expenseTxs = expenseTxs,
            totalExpense = totalExpense
        )

        val code = buildString {
            append(if (savingsScore >= 50) 'S' else 'I')
            append(if (plannerScore >= 50) 'P' else 'F')
            append(if (stabilityScore >= 50) 'C' else 'V')
            append(if (focusScore >= 50) 'T' else 'D')
        }
        val archetype = FinancialArchetype.fromCode(code)

        val localNarrative = buildDeterministicNarrative(
            archetype = archetype,
            savingsRatePercent = savingsRatePercent,
            weeklyVariancePercent = weeklyVariancePercent,
            topCategoriesSharePercent = topCategoriesSharePercent,
            topCategories = topCategories,
            languageCode = languageCode
        )

        return FinancialDNA(
            code = code,
            archetype = archetype,
            savingsScore = savingsScore,
            plannerScore = plannerScore,
            stabilityScore = stabilityScore,
            focusScore = focusScore,
            savingsRatePercent = savingsRatePercent,
            weeklyVariancePercent = weeklyVariancePercent,
            topCategoriesSharePercent = topCategoriesSharePercent,
            topCategories = topCategories,
            analyzedTransactionsCount = analysisTxs.size,
            analyzedDays = analyzedDays,
            aiNarrative = localNarrative,
            strengths = archetype.defaultStrengths(languageCode),
            improvements = archetype.defaultImprovements(languageCode),
            isAiEnhanced = false,
            generatedAt = nowMillis,
            languageCode = languageCode
        )
    }

    private fun computeSavingsAxis(
        totalIncome: Double,
        totalExpense: Double,
        budgets: List<Budget>,
        expenseTxs: List<Transaction>
    ): Pair<Int, Double> {
        if (totalIncome > 0.0) {
            val rate = ((totalIncome - totalExpense) / totalIncome) * 100.0
            val score = if (rate >= 0.0) {
                (45.0 + (rate.coerceAtMost(50.0) / 50.0) * 53.0).roundToInt().coerceIn(45, 98)
            } else {
                (45.0 + (rate.coerceAtLeast(-50.0) / 50.0) * 40.0).roundToInt().coerceIn(5, 44)
            }
            return score to rate.coerceIn(-100.0, 100.0)
        }

        // No income logged: estimate savings discipline from budget headroom if available
        if (budgets.isNotEmpty()) {
            val totalBudgetLimit = budgets.sumOf { it.effectiveAmount }
            if (totalBudgetLimit > 0.0) {
                val budgetedSpend = budgets.sumOf { budget ->
                    expenseTxs.filter {
                        it.category.equals(budget.category, ignoreCase = true) &&
                            it.date in budget.startDate..budget.endDate
                    }.sumOf { it.amount }
                }
                val headroomRate = ((totalBudgetLimit - budgetedSpend) / totalBudgetLimit) * 100.0
                val score = (50.0 + (headroomRate.coerceIn(-50.0, 50.0) * 0.8)).roundToInt().coerceIn(10, 90)
                return score to headroomRate.coerceIn(-100.0, 100.0)
            }
        }

        return 50 to 0.0
    }

    private fun computePlannerAxis(
        expenseTxs: List<Transaction>,
        budgets: List<Budget>
    ): Int {
        // Signal A: Budget presence & adherence (up to 50 points)
        val budgetSignal: Double = if (budgets.isNotEmpty()) {
            val withinLimitCount = budgets.count { budget ->
                val spent = expenseTxs.filter {
                    it.category.equals(budget.category, ignoreCase = true) &&
                        it.date in budget.startDate..budget.endDate
                }.sumOf { it.amount }
                spent <= budget.effectiveAmount
            }
            val adherenceRatio = withinLimitCount.toDouble() / budgets.size.toDouble()
            val recurringBonus = if (budgets.any { it.isRecurring }) 6.0 else 0.0
            (20.0 + adherenceRatio * 24.0 + recurringBonus).coerceAtMost(50.0)
        } else {
            14.0
        }

        // Signal B: Structured / repeating spending ratio (up to 25 points)
        val structuredCount = expenseTxs.count { tx ->
            expenseTxs.any { other ->
                other.id != tx.id &&
                    other.category.equals(tx.category, ignoreCase = true) &&
                    abs(other.amount - tx.amount) <= (tx.amount * 0.20).coerceAtLeast(1.0)
            }
        }
        val structuredRatio = structuredCount.toDouble() / expenseTxs.size.toDouble()
        val structuredSignal = structuredRatio * 25.0

        // Signal C: Daytime vs late-night impulse ratio (up to 25 points)
        val calendar = Calendar.getInstance()
        val lateNightCount = expenseTxs.count { tx ->
            calendar.timeInMillis = tx.date
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hour >= 22 || hour < 5
        }
        val calmTimingRatio = 1.0 - (lateNightCount.toDouble() / expenseTxs.size.toDouble())
        val timingSignal = calmTimingRatio * 25.0

        return (budgetSignal + structuredSignal + timingSignal).roundToInt().coerceIn(10, 98)
    }

    private fun computeStabilityAxis(
        expenseTxs: List<Transaction>,
        analyzedDays: Int
    ): Pair<Int, Double> {
        val bucketSizeMs = if (analyzedDays >= 14) WEEK_MS else DAY_MS
        val minDate = expenseTxs.minOf { it.date }
        val bucketTotals = expenseTxs
            .groupBy { ((it.date - minDate) / bucketSizeMs).toInt() }
            .values
            .map { bucket -> bucket.sumOf { it.amount } }

        if (bucketTotals.size < 2) {
            return 72 to 15.0
        }

        val mean = bucketTotals.average()
        if (mean <= 0.0) return 70 to 0.0

        val variance = bucketTotals.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        val cv = stdDev / mean
        val variancePercent = (cv * 100.0).coerceIn(0.0, 200.0)

        // cv = 0.0 -> 96, cv = 0.5 -> 64, cv = 0.72 -> 50, cv >= 1.35 -> 10
        val score = (96.0 - cv * 64.0).roundToInt().coerceIn(10, 96)
        return score to variancePercent
    }

    private fun computeFocusAxis(
        expenseTxs: List<Transaction>,
        totalExpense: Double
    ): Triple<Int, Double, List<String>> {
        if (totalExpense <= 0.0) return Triple(50, 0.0, emptyList())

        val categoryTotals = expenseTxs
            .groupBy { it.category.ifBlank { "General" } }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }

        val top3 = categoryTotals.take(3)
        val top3Names = top3.map { it.key }
        val top3Share = top3.sumOf { it.value } / totalExpense
        val top3Percent = (top3Share * 100.0).coerceIn(0.0, 100.0)

        // Herfindahl-Hirschman Index measures true concentration even when <= 3 categories exist
        val hhi = categoryTotals.sumOf { (_, amount) ->
            val share = amount / totalExpense
            share * share
        }
        val concentrationIndex = (0.55 * top3Share) + (0.45 * sqrt(hhi))
        // Map roughly [0.35 .. 1.0] -> [15 .. 96]
        val normalized = ((concentrationIndex - 0.30) / 0.70).coerceIn(0.0, 1.0)
        val score = (14.0 + normalized * 82.0).roundToInt().coerceIn(10, 96)

        return Triple(score, top3Percent, top3Names)
    }

    private fun buildDeterministicNarrative(
        archetype: FinancialArchetype,
        savingsRatePercent: Double,
        weeklyVariancePercent: Double,
        topCategoriesSharePercent: Double,
        topCategories: List<String>,
        languageCode: String
    ): String {
        val topCatsText = topCategories.joinToString(", ")
        val topShareFormatted = String.format(Locale.US, "%.0f%%", topCategoriesSharePercent)
        val varianceFormatted = String.format(Locale.US, "±%.0f%%", weeklyVariancePercent)
        val savingsFormatted = String.format(Locale.US, "%.1f%%", savingsRatePercent)

        return if (languageCode == "vi") {
            "Hồ sơ của bạn mang đậm dấu ấn \"${archetype.titleVi}\": $topShareFormatted tổng chi tiêu tập trung vào $topCatsText, với biên độ dao động $varianceFormatted và tỷ lệ tiết kiệm đạt $savingsFormatted. ${archetype.taglineVi}"
        } else {
            "Your money habits embody \"${archetype.titleEn}\": $topShareFormatted of spending flows into $topCatsText, with a $varianceFormatted rhythm and a $savingsFormatted savings rate. ${archetype.taglineEn}"
        }
    }
}
