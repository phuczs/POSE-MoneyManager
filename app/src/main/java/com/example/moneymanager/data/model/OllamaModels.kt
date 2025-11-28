package com.example.moneymanager.data.model

data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val format: String? = null
)

data class OllamaResponse(
    val model: String,
    val created_at: String,
    val response: String,
    val done: Boolean
)
data class AiTransactionData(
    val amount: Double? = null,
    val type: String? = null, // "income" hoặc "expense"
    val category: String? = null,
    val description: String? = null,
    val isTransaction: Boolean = false // Cờ đánh dấu xem AI có nhận diện được giao dịch không
)