package com.example.moneymanager.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["isActive", "nextExecutionDate"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val amount: Double,
    val type: String, // "income" or "expense"
    val category: String,
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val startDate: Long,
    val lastExecutedDate: Long? = null,
    val nextExecutionDate: Long,
    val isActive: Boolean = true,
    val description: String = ""
)
