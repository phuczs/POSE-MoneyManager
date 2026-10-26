package com.example.moneymanager.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["userId", "timestamp"]),
        Index(value = ["userId"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val isError: Boolean = false
)
