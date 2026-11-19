package com.example.moneymanager.domain.usecase.ai

import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.model.WeeklyCategorySpend
import com.example.moneymanager.domain.model.WeeklyDigest
import com.example.moneymanager.domain.repository.AiServiceRepository
import com.example.moneymanager.util.toCurrencyString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

/**
 * Generates a personal 7-day [WeeklyDigest] comparing the past week vs. the prior week.
 *
 * - Statistical aggregation runs 100% locally on [Dispatchers.Default].
 * - Enriches commentary & weekly tip via [AiServiceRepository] with offline deterministic fallback.
 */
class GenerateWeeklyDigestUseCase @Inject constructor(
    private val aiServiceRepository: AiServiceRepository
) {
    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val WEEK_MS = 7L * DAY_MS
        private const val AI_TIMEOUT_MS = 15_000L
    }

    suspend operator fun invoke(
        allTransactions: List<Transaction>,
        activeBudgets: List<Budget>,
        languageCode: String = "vi",
        forceRefresh: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): WeeklyDigest? {
        val localDigest = withContext(Dispatchers.Default) {
            computeLocalDigest(
                allTransactions = allTransactions,
                activeBudgets = activeBudgets,
                languageCode = languageCode,
                nowMillis = nowMillis
            )
        } ?: return null

        if (forceRefresh) {
            aiServiceRepository.clearWeeklyDigestCache()
        }

        val aiCommentary = withTimeoutOrNull(AI_TIMEOUT_MS) {
            aiServiceRepository.getWeeklyDigestCommentary(localDigest, languageCode)
        }

        return if (aiCommentary != null) {
            localDigest.copy(
                aiSummary = aiCommentary.first,
                actionableTip = aiCommentary.second,
                isAiEnhanced = true
            )
        } else {
            localDigest
        }
    }

    fun computeLocalDigest(
        allTransactions: List<Transaction>,
        activeBudgets: List<Budget>,
        languageCode: String = "vi",
        nowMillis: Long = System.currentTimeMillis()
    ): WeeklyDigest? {
        val weekStart = nowMillis - WEEK_MS
        val prevWeekStart = nowMillis - 2L * WEEK_MS

        val thisWeekTxs = allTransactions.filter { it.date in weekStart..nowMillis }
        val prevWeekTxs = allTransactions.filter { it.date in prevWeekStart until weekStart }

        if (thisWeekTxs.isEmpty() && prevWeekTxs.isEmpty()) return null

        val thisWeekExpenses = thisWeekTxs.filter { it.type.equals("expense", ignoreCase = true) && it.amount > 0.0 }
        val totalSpent = thisWeekExpenses.sumOf { it.amount }
        val totalIncome = thisWeekTxs.filter { it.type.equals("income", ignoreCase = true) && it.amount > 0.0 }.sumOf { it.amount }
        val prevWeekSpent = prevWeekTxs.filter { it.type.equals("expense", ignoreCase = true) && it.amount > 0.0 }.sumOf { it.amount }

        val deltaPercent = when {
            prevWeekSpent > 0.0 -> ((totalSpent - prevWeekSpent) / prevWeekSpent) * 100.0
            totalSpent > 0.0 -> 100.0
            else -> 0.0
        }

        val topCategories = if (totalSpent > 0.0) {
            thisWeekExpenses
                .groupBy { it.category.ifBlank { "General" } }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .entries
                .sortedByDescending { it.value }
                .take(3)
                .map { (cat, amt) ->
                    WeeklyCategorySpend(
                        category = cat,
                        amount = amt,
                        sharePercent = (amt / totalSpent * 100.0).toFloat()
                    )
                }
        } else {
            emptyList()
        }

        val dayFormatter = SimpleDateFormat(
            if (languageCode == "vi") "EEEE (dd/MM)" else "EEE (MMM dd)",
            if (languageCode == "vi") Locale.forLanguageTag("vi") else Locale.US
        )
        val peakDay = thisWeekExpenses
            .groupBy { dayFormatter.format(Date(it.date)) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .maxByOrNull { it.value }

        val activeCycleBudgets = activeBudgets.filter { nowMillis in it.startDate..it.endDate && it.effectiveAmount > 0.0 }
        val onTrackCount = activeCycleBudgets.count { budget ->
            val spent = allTransactions.filter {
                it.type.equals("expense", ignoreCase = true) &&
                    it.category.equals(budget.category, ignoreCase = true) &&
                    it.date in budget.startDate..budget.endDate
            }.sumOf { it.amount }
            spent <= budget.effectiveAmount * 0.85
        }

        val (fallbackSummary, fallbackTip) = buildFallbackCopy(
            totalSpent = totalSpent,
            prevWeekSpent = prevWeekSpent,
            deltaPercent = deltaPercent,
            topCategory = topCategories.firstOrNull(),
            languageCode = languageCode
        )

        return WeeklyDigest(
            weekStartMillis = weekStart,
            weekEndMillis = nowMillis,
            totalSpent = totalSpent,
            previousWeekSpent = prevWeekSpent,
            deltaPercent = deltaPercent,
            totalIncome = totalIncome,
            transactionCount = thisWeekTxs.size,
            topCategories = topCategories,
            highestSpendDayLabel = peakDay?.key ?: "",
            highestSpendDayAmount = peakDay?.value ?: 0.0,
            budgetsOnTrackCount = onTrackCount,
            budgetsTotalCount = activeCycleBudgets.size,
            aiSummary = fallbackSummary,
            actionableTip = fallbackTip,
            isAiEnhanced = false,
            generatedAt = nowMillis,
            languageCode = languageCode
        )
    }

    private fun buildFallbackCopy(
        totalSpent: Double,
        prevWeekSpent: Double,
        deltaPercent: Double,
        topCategory: WeeklyCategorySpend?,
        languageCode: String
    ): Pair<String, String> {
        val isVi = languageCode == "vi"
        val currency = if (isVi) "VND" else "USD"
        val topCatName = topCategory?.category ?: (if (isVi) "chi tiêu hàng ngày" else "daily spending")
        val absDelta = abs(deltaPercent).toInt()

        val summary = if (isVi) {
            when {
                prevWeekSpent <= 0.0 ->
                    "7 ngày qua bạn đã chi ${totalSpent.toCurrencyString(currency)}, dẫn đầu là danh mục $topCatName."
                deltaPercent <= -5.0 ->
                    "Tuyệt vời! Bạn đã giảm chi tiêu $absDelta% so với tuần trước (còn ${totalSpent.toCurrencyString(currency)}). Danh mục chiếm tỷ trọng cao nhất là $topCatName."
                deltaPercent >= 10.0 ->
                    "Tuần này tổng chi tăng $absDelta% lên ${totalSpent.toCurrencyString(currency)}, chủ yếu đến từ danh mục $topCatName."
                else ->
                    "Nhịp chi tiêu tuần qua khá ổn định ở mức ${totalSpent.toCurrencyString(currency)}, trọng tâm nằm ở $topCatName."
            }
        } else {
            when {
                prevWeekSpent <= 0.0 ->
                    "Over the past 7 days you spent ${totalSpent.toCurrencyString(currency)}, led by $topCatName."
                deltaPercent <= -5.0 ->
                    "Nice work! You trimmed spending by $absDelta% vs last week (${totalSpent.toCurrencyString(currency)}), with $topCatName as your top category."
                deltaPercent >= 10.0 ->
                    "Spending rose $absDelta% this week to ${totalSpent.toCurrencyString(currency)}, mainly driven by $topCatName."
                else ->
                    "Your spending stayed steady at ${totalSpent.toCurrencyString(currency)} this week, centered around $topCatName."
            }
        }

        val tip = if (isVi) {
            if (deltaPercent >= 10.0 && topCategory != null) {
                "Thử đặt hạn mức tuần thấp hơn 15% cho \"$topCatName\" trong 7 ngày tới để kéo dòng tiền về mức cân bằng."
            } else {
                "Duy trì đà này bằng cách kiểm tra hạn mức chi tiêu an toàn mỗi ngày trước khi thanh toán cho \"$topCatName\"."
            }
        } else {
            if (deltaPercent >= 10.0 && topCategory != null) {
                "Try setting a 15% tighter weekly cap on \"$topCatName\" over the next 7 days to rebalance cashflow."
            } else {
                "Keep your momentum going by glancing at your daily safe allowance before spending on \"$topCatName\"."
            }
        }

        return summary to tip
    }
}
