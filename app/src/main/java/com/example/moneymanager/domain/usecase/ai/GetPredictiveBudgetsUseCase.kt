package com.example.moneymanager.domain.usecase.ai

import com.example.moneymanager.domain.model.PredictedBudget
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.AiServiceRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Generates AI-predicted budgets based on the last 3 months of expense data.
 *
 * Business logic moved here from [PredictiveBudgetViewModel]:
 * - 3-month transaction filtering.
 * - Minimum 2-distinct-month data requirement check.
 *
 * Returns empty list if insufficient data or AI call fails.
 */
class GetPredictiveBudgetsUseCase @Inject constructor(
    private val aiServiceRepository: AiServiceRepository
) {
    suspend operator fun invoke(
        allTransactions: List<Transaction>,
        availableCategories: List<String>,
        languageCode: String = "vi"
    ): List<PredictedBudget> {
        val today = LocalDate.now()
        val threeMonthsAgo = today.minusMonths(3)
        val zoneId = ZoneId.systemDefault()

        val threeMonthTxs = allTransactions.filter { tx ->
            val d = java.time.Instant.ofEpochMilli(tx.date).atZone(zoneId).toLocalDate()
            !d.isBefore(threeMonthsAgo) && tx.type == "expense"
        }

        // Require at least 2 distinct months of data for a meaningful prediction
        val distinctMonths = threeMonthTxs.map { "${it.month}-${it.year}" }.distinct().size
        if (distinctMonths < 2 || threeMonthTxs.isEmpty()) return emptyList()

        return aiServiceRepository.getPredictiveBudgets(
            threeMonthTransactions = threeMonthTxs,
            availableCategories = availableCategories,
            languageCode = languageCode
        )
    }
}
