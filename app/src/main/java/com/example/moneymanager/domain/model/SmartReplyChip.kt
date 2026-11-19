package com.example.moneymanager.domain.model

import androidx.compose.runtime.Immutable

enum class SmartReplyCategory {
    ALERT,
    ANOMALY,
    BUDGET,
    INSIGHT
}

/**
 * A context-aware quick question chip surfaced in the AI Financial Advisor chat.
 */
@Immutable
data class SmartReplyChip(
    val id: String,
    val label: String,
    val promptText: String,
    val category: SmartReplyCategory
)
