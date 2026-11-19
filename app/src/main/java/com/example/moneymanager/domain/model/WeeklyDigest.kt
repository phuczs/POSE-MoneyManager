package com.example.moneymanager.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class WeeklyCategorySpend(
    val category: String,
    val amount: Double,
    val sharePercent: Float
)

/**
 * Aggregated 7-day personal financial digest comparing this week vs. the prior week,
 * enriched with AI commentary and one concrete action tip.
 */
@Immutable
data class WeeklyDigest(
    val weekStartMillis: Long,
    val weekEndMillis: Long,
    val totalSpent: Double,
    val previousWeekSpent: Double,
    val deltaPercent: Double, // Negative means saved compared to previous week
    val totalIncome: Double,
    val transactionCount: Int,
    val topCategories: List<WeeklyCategorySpend>,
    val highestSpendDayLabel: String,
    val highestSpendDayAmount: Double,
    val budgetsOnTrackCount: Int,
    val budgetsTotalCount: Int,
    val aiSummary: String,
    val actionableTip: String,
    val isAiEnhanced: Boolean = false,
    val generatedAt: Long = System.currentTimeMillis(),
    val languageCode: String = "vi"
)
