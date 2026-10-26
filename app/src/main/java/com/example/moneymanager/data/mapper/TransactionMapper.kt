package com.example.moneymanager.data.mapper

import com.example.moneymanager.data.local.TransactionEntity
import com.example.moneymanager.domain.model.Transaction

/**
 * Bidirectional mapper between [TransactionEntity] (Room) and [Transaction] (domain).
 *
 * Clean Architecture note:
 * - All mapping responsibility is centralized here, preventing domain models
 *   from importing Room or Firebase types.
 * - [Transaction.date] is epoch milliseconds (Long) in both directions.
 */

/** Converts a Room entity to a domain model. */
fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    userId = userId,
    amount = amount,
    type = type,
    category = category,
    description = description,
    date = date,           // Already epoch ms in TransactionEntity
    month = month,
    year = year
)

/** Converts a domain model to a Room entity for local persistence. */
fun Transaction.toEntity(
    syncStatus: String = "SYNCED",
    updatedAt: Long = System.currentTimeMillis()
): TransactionEntity = TransactionEntity(
    id = id,
    userId = userId,
    amount = amount,
    type = type,
    category = category,
    description = description,
    date = date,           // epoch ms — stored directly in Room
    month = month,
    year = year,
    syncedAt = System.currentTimeMillis(),
    syncStatus = syncStatus,
    updatedAt = updatedAt
)

/**
 * Converts a Firestore document field map to a domain [Transaction].
 *
 * Security note:
 * - The Firestore [Timestamp] is converted to epoch ms here in the data layer,
 *   keeping the domain model free of Firebase types.
 */
fun Map<String, Any?>.toTransactionDomain(docId: String): Transaction {
    val timestamp = this["date"]
    val epochMs: Long = when (timestamp) {
        is com.google.firebase.Timestamp -> timestamp.seconds * 1000L + timestamp.nanoseconds / 1_000_000L
        is Long -> timestamp
        else -> System.currentTimeMillis()
    }
    return Transaction(
        id = docId,
        userId = this["userId"] as? String ?: "",
        amount = (this["amount"] as? Number)?.toDouble() ?: 0.0,
        type = this["type"] as? String ?: "",
        category = this["category"] as? String ?: "",
        description = this["description"] as? String ?: "",
        date = epochMs,
        month = (this["month"] as? Number)?.toInt() ?: 0,
        year = (this["year"] as? Number)?.toInt() ?: 0
    )
}

/**
 * Converts a domain [Transaction] to a Firestore-compatible map.
 * Uses [com.google.firebase.Timestamp] for the date field as Firestore expects it.
 */
fun Transaction.toFirestoreMap(updatedAt: Long = System.currentTimeMillis()): Map<String, Any?> = mapOf(
    "userId" to userId,
    "amount" to amount,
    "type" to type,
    "category" to category,
    "description" to description,
    "date" to com.google.firebase.Timestamp(date / 1000L, ((date % 1000L) * 1_000_000L).toInt()),
    "month" to month,
    "year" to year,
    "updatedAt" to updatedAt
)
