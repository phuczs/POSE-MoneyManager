package com.example.moneymanager.data.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false, // Để hiển thị loading indicator cho tin nhắn đang chờ
    val isError: Boolean = false // Để đánh dấu tin nhắn lỗi
)