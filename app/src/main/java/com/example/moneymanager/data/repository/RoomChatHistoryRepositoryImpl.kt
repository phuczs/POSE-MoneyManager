package com.example.moneymanager.data.repository

import com.example.moneymanager.data.local.ChatDao
import com.example.moneymanager.data.local.ChatMessageEntity
import com.example.moneymanager.domain.model.ChatMessage
import com.example.moneymanager.domain.repository.ChatHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomChatHistoryRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao
) : ChatHistoryRepository {

    override fun getMessages(userId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessages(userId).map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    content = entity.content,
                    isFromUser = entity.isFromUser,
                    timestamp = entity.timestamp,
                    isError = entity.isError
                )
            }
        }
    }

    override suspend fun saveMessage(userId: String, message: ChatMessage): Result<Unit> {
        return try {
            val entity = ChatMessageEntity(
                id = message.id,
                userId = userId,
                content = message.content,
                isFromUser = message.isFromUser,
                timestamp = message.timestamp,
                isError = message.isError
            )
            chatDao.insert(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearHistory(userId: String): Result<Unit> {
        return try {
            chatDao.clearHistory(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
