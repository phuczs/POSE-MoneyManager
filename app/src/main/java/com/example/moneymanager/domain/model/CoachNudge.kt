package com.example.moneymanager.domain.model

import androidx.compose.runtime.Immutable

enum class NudgeType {
    DAILY_SPIKE,
    MID_MONTH_BUDGET_BURN,
    MONTH_END_OVERSPEND_RISK,
    WEEKEND_SAVINGS_WIN,
    INACTIVITY_REMINDER
}

enum class NudgeTone {
    URGENT,
    WARNING,
    CELEBRATION,
    GENTLE
}

enum class NudgeActionTarget {
    OPEN_CHAT,
    OPEN_BUDGETS,
    ADD_TRANSACTION,
    OPEN_WEEKLY_DIGEST
}

/**
 * A contextual, proactive financial coaching nudge generated from real-time spending signals.
 */
@Immutable
data class CoachNudge(
    val id: String,
    val type: NudgeType,
    val tone: NudgeTone,
    val emoji: String,
    val title: String,
    val message: String,
    val actionLabel: String,
    val actionTarget: NudgeActionTarget,
    val priority: Int
)
