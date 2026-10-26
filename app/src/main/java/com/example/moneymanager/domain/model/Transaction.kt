package com.example.moneymanager.domain.model

/**
 * Pure domain model for a financial transaction.
 *
 * Security & Clean Architecture notes:
 * - Zero dependency on Firebase, Room, or any Android SDK.
 * - [date] is stored as epoch milliseconds (Long) — framework-agnostic.
 *   The data layer mappers handle conversion to/from Firestore Timestamp and Room Long.
 * - [userId] is always enforced by repository implementations; never trust
 *   caller-provided userId — implementations must overwrite from FirebaseAuth.
 */
data class Transaction(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val type: String = "",          // "income" or "expense"
    val category: String = "",
    val description: String = "",
    val date: Long = System.currentTimeMillis(), // epoch milliseconds
    val month: Int = 0,             // 1 = January, 12 = December
    val year: Int = 0
)
