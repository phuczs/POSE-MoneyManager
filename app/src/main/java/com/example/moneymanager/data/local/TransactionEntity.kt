package com.example.moneymanager.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for local transaction persistence.
 *
 * Clean Architecture note:
 * - This class has NO reference to the domain [Transaction] model or any Firebase types.
 * - All conversions are performed by [com.example.moneymanager.data.mapper.TransactionMapper].
 *
 * Security:
 * - [userId] is part of every index, enforcing strict per-user data isolation.
 *   Data from one user can never bleed into another user's queries.
 * - [syncedAt] enables staleness detection for cache invalidation.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["userId", "date"]),
        Index(value = ["userId", "type"]),
        Index(value = ["userId", "month", "year"]),
        Index(value = ["userId", "syncStatus"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double,
    val type: String,           // "income" | "expense"
    val category: String,
    val description: String,
    val date: Long,             // epoch milliseconds
    val month: Int,
    val year: Int,
    val syncedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED", // "SYNCED" | "PENDING_INSERT" | "PENDING_UPDATE" | "PENDING_DELETE"
    val updatedAt: Long = System.currentTimeMillis()
)
