package com.example.moneymanager.domain.model

/**
 * Pure domain model for detected recurring subscriptions from transaction history.
 *
 * Automatically detected via pattern matching for popular services (Netflix, Spotify, etc.)
 * or regular billing cycle analysis (~30 days, ~7 days).
 */
data class DetectedSubscription(
    val id: String = "",
    val name: String = "",
    val serviceKey: String = "",
    val category: String = "Bills & Utilities",
    val amount: Double = 0.0,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val detectedCycleCount: Int = 1,
    val transactionIds: List<String> = emptyList(),
    val lastPaymentDate: Long = System.currentTimeMillis(),
    val nextExpectedDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val annualCost: Double = if (frequency == RecurringFrequency.YEARLY) amount else (amount * 12),
    val isAlreadyTracked: Boolean = false,
    val suggestedTip: String = "",
    val iconEmoji: String = "💳"
)
