package com.example.moneymanager.domain.model

import java.util.UUID

/**
 * Represents a single chat message in the AI financial advisor conversation.
 *
 * Clean Architecture note:
 * - UI state flags ([isLoading], [isError]) live here for simplicity since
 *   ChatMessage is UI-driven and never persisted to any data source.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false, // Displays a loading indicator for in-flight AI responses
    val isError: Boolean = false    // Marks error response messages
)
