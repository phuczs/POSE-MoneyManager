package com.example.moneymanager.domain.usecase.ai

import com.example.moneymanager.domain.model.AiInsight
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.AiServiceRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Generates a monthly financial insight by comparing current vs. previous month transactions.
 *
 * Business logic moved here from [AiInsightViewModel]:
 * - Month/year filtering of transactions.
 * - Delegation to [AiServiceRepository] with language code from settings.
 *
 * Security note:
 * - Only aggregated financial summaries are sent to external AI — not raw transaction details.
 *   This is enforced in [AiServiceRepositoryImpl.buildMonthSummary].
 */
class GetMonthlyInsightUseCase @Inject constructor(
    private val aiServiceRepository: AiServiceRepository
) {
    suspend operator fun invoke(
        allTransactions: List<Transaction>,
        languageCode: String = "vi"
    ): AiInsight? {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        val currentMonthTxs = allTransactions.filter {
            val d = java.time.Instant.ofEpochMilli(it.date).atZone(zoneId).toLocalDate()
            d.month == today.month && d.year == today.year
        }

        val prevMonth = today.minusMonths(1)
        val prevMonthTxs = allTransactions.filter {
            val d = java.time.Instant.ofEpochMilli(it.date).atZone(zoneId).toLocalDate()
            d.month == prevMonth.month && d.year == prevMonth.year
        }

        if (currentMonthTxs.isEmpty()) return null

        return aiServiceRepository.getMonthlyInsight(
            currentMonthTransactions = currentMonthTxs,
            previousMonthTransactions = prevMonthTxs,
            languageCode = languageCode
        )
    }
}
