package com.example.moneymanager.domain.model

/**
 * Represents an AI-suggested budget for a spending category.
 *
 * Security note:
 * - Only shown as a suggestion — user must explicitly confirm before saving to Firestore.
 * - [averageMonthlySpend] is computed locally for transparency, not trusted from AI output.
 */
data class PredictedBudget(
    val category: String = "",
    val suggestedAmount: Double = 0.0,
    val rationale: String = "",             // Short AI explanation, 1–2 sentences
    val basedOnMonths: Int = 0,             // Number of months of data used for prediction
    val averageMonthlySpend: Double = 0.0   // Computed locally — shown for transparency
)
