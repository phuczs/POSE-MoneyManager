package com.example.moneymanager.domain.repository

import com.example.moneymanager.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatHistoryRepository {
    fun getMessages(userId: String): Flow<List<ChatMessage>>
    suspend fun saveMessage(userId: String, message: ChatMessage): Result<Unit>
    suspend fun clearHistory(userId: String): Result<Unit>
}
