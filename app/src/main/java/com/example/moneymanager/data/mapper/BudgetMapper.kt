package com.example.moneymanager.data.mapper

import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.BudgetPeriod

/**
 * Bidirectional mapper between Firestore document fields and [Budget] (domain).
 *
 * Clean Architecture note:
 * - [Budget.startDate] and [Budget.endDate] are epoch milliseconds in the domain.
 * - Firestore stores them as Timestamps; this mapper handles the conversion.
 *
 * Phase 4.1 note:
 * - New fields (period, isRecurring, rolloverEnabled, etc.) use safe defaults
 *   so existing Firestore documents without these fields remain fully compatible.
 */

/**
 * Converts a Firestore document field map to a domain [Budget].
 */
fun Map<String, Any?>.toBudgetDomain(docId: String): Budget {
    fun anyToEpochMs(value: Any?): Long = when (value) {
        is com.google.firebase.Timestamp -> value.seconds * 1000L + value.nanoseconds / 1_000_000L
        is Long -> value
        else -> System.currentTimeMillis()
    }

    return Budget(
        id = docId,
        userId = this["userId"] as? String ?: "",
        category = this["category"] as? String ?: "",
        allocatedAmount = (this["allocatedAmount"] as? Number)?.toDouble() ?: 0.0,
        startDate = anyToEpochMs(this["startDate"]),
        endDate = anyToEpochMs(this["endDate"]),
        // --- Phase 4.1: Flexible Periods & Auto-Recurring (backward-compatible defaults) ---
        period = BudgetPeriod.fromString(this["period"] as? String),
        isRecurring = this["isRecurring"] as? Boolean ?: false,
        rolloverEnabled = this["rolloverEnabled"] as? Boolean ?: false,
        rolloverAmount = (this["rolloverAmount"] as? Number)?.toDouble() ?: 0.0,
        alertThresholdWarning = (this["alertThresholdWarning"] as? Number)?.toFloat() ?: 0.5f,
        alertThresholdCritical = (this["alertThresholdCritical"] as? Number)?.toFloat() ?: 0.9f,
        notes = this["notes"] as? String ?: ""
    )
}

/**
 * Converts a domain [Budget] to a Firestore-compatible map.
 */
fun Budget.toFirestoreMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "category" to category,
    "allocatedAmount" to allocatedAmount,
    "startDate" to com.google.firebase.Timestamp(startDate / 1000L, ((startDate % 1000L) * 1_000_000L).toInt()),
    "endDate" to com.google.firebase.Timestamp(endDate / 1000L, ((endDate % 1000L) * 1_000_000L).toInt()),
    // --- Phase 4.1: New fields ---
    "period" to period.name,
    "isRecurring" to isRecurring,
    "rolloverEnabled" to rolloverEnabled,
    "rolloverAmount" to rolloverAmount,
    "alertThresholdWarning" to alertThresholdWarning,
    "alertThresholdCritical" to alertThresholdCritical,
    "notes" to notes
)

