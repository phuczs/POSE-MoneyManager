package com.example.moneymanager.domain.model

/**
 * Represents a cached AI-generated financial insight report.
 *
 * Security note:
 * - This is stored locally (SharedPreferences) and never uploaded to Firestore.
 * - [generatedAt] epoch ms is used for the 12h TTL freshness check in
 *   [AiServiceRepositoryImpl] to avoid re-generating insights on every screen load.
 */
data class AiInsight(
    val generatedAt: Long = 0L,            // epoch ms — used for 12h TTL freshness check
    val summaryText: String = "",           // Full analysis paragraph shown in bottom sheet
    val topChangeCategory: String = "",     // Category with biggest change (for card subtitle)
    val actionableTip: String = "",         // Single actionable recommendation
    val languageCode: String = "vi"         // Language the insight was generated in
)
