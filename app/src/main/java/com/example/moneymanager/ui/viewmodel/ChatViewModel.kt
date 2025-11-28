package com.example.moneymanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.model.ChatMessage
import com.example.moneymanager.data.repository.BudgetRepository
import com.example.moneymanager.data.repository.OllamaRepository
import com.example.moneymanager.data.repository.TransactionRepository
import com.example.moneymanager.util.toCurrencyString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val ollamaRepository: OllamaRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
    // Không cần CategoryRepository nữa vì không xử lý thêm giao dịch ở đây
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    // Biến lưu ngữ cảnh tài chính để AI biết tình hình của bạn
    private var financialContextPrompt: String = ""

    init {
        initializeFinancialContext()
    }

    private fun initializeFinancialContext() {
        viewModelScope.launch {
            _chatState.value = ChatUiState.Loading

            try {
                // 1. Lấy dữ liệu tổng quan (Timeout 3s)
                val allTransactions = withTimeoutOrNull(3000) {
                    transactionRepository.getAllTransactions().first()
                } ?: emptyList()

                // 2. Tính toán số liệu cơ bản
                val today = LocalDate.now()
                val thisMonthTransactions = allTransactions.filter {
                    val txDate = it.date.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    txDate.month == today.month && txDate.year == today.year
                }

                val totalIncome = thisMonthTransactions.filter { it.type == "income" }.sumOf { it.amount }
                val totalExpense = thisMonthTransactions.filter { it.type == "expense" }.sumOf { it.amount }
                val balance = totalIncome - totalExpense

                val expenseByCategory = thisMonthTransactions.filter { it.type == "expense" }
                    .groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(3)
                    .joinToString(", ") { "${it.first}: ${it.second.toCurrencyString()}" }

                // 3. Tạo ngữ cảnh cho AI
                financialContextPrompt = """
                    THÔNG TIN TÀI CHÍNH THÁNG NÀY CỦA NGƯỜI DÙNG:
                    - Tổng thu: ${totalIncome.toCurrencyString()}
                    - Tổng chi: ${totalExpense.toCurrencyString()}
                    - Số dư: ${balance.toCurrencyString()}
                    - Top chi tiêu: ${if (expenseByCategory.isNotBlank()) expenseByCategory else "Chưa có dữ liệu"}
                """.trimIndent()

                _chatState.value = ChatUiState.Success

                // Tin nhắn chào mừng
                val welcomeText = "Xin chào! Tôi là trợ lý tài chính của bạn. Dựa trên dữ liệu tháng này, tôi có thể giúp gì cho bạn?"
                addMessage(ChatMessage(content = welcomeText, isFromUser = false))

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Lỗi khởi tạo: ${e.message}", e)
                _chatState.value = ChatUiState.Success
                addMessage(ChatMessage(content = "Hệ thống đã sẵn sàng. Mời bạn đặt câu hỏi.", isFromUser = false))
            }
        }
    }

    fun sendMessage(messageContent: String) {
        if (messageContent.isBlank()) return

        // 1. Hiện tin nhắn người dùng (isFromUser = true)
        val userMessage = ChatMessage(content = messageContent, isFromUser = true)
        addMessage(userMessage)

        // 2. Hiện loading message
        val loadingMessageId = UUID.randomUUID().toString()
        addMessage(ChatMessage(id = loadingMessageId, content = "", isFromUser = false, isLoading = true))

        viewModelScope.launch {
            try {
                // 3. Tạo Prompt chuyên cho việc TƯ VẤN (Advisor Prompt)
                val prompt = """
                    Bạn là một Chuyên gia Tài chính cá nhân thân thiện và chuyên nghiệp.
                    
                    DỮ LIỆU CỦA NGƯỜI DÙNG:
                    $financialContextPrompt
                    
                    CÂU HỎI: "$messageContent"
                    
                    NHIỆM VỤ:
                    - Trả lời câu hỏi hoặc đưa ra lời khuyên dựa trên số liệu thực tế ở trên.
                    - Phong cách: Lịch sự, ngắn gọn, súc tích, dùng tiếng Việt tự nhiên.
                    - KHÔNG trả về JSON. Hãy trả lời bằng văn bản thường (text).
                """.trimIndent()

                // Gọi API với jsonMode = false (để nhận văn bản thường)
                val result = ollamaRepository.sendMessage(prompt, jsonMode = false)

                removeMessage(loadingMessageId)

                result.fold(
                    onSuccess = { responseText ->
                        // Hiển thị trực tiếp câu trả lời của AI
                        addMessage(ChatMessage(content = responseText.trim(), isFromUser = false))
                    },
                    onFailure = { error ->
                        addMessage(ChatMessage(content = "Lỗi kết nối: ${error.message}", isFromUser = false, isError = true))
                    }
                )
            } catch (e: Exception) {
                removeMessage(loadingMessageId)
                addMessage(ChatMessage(content = "Đã xảy ra lỗi: ${e.message}", isFromUser = false, isError = true))
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    private fun removeMessage(messageId: String) {
        _messages.value = _messages.value.filter { it.id != messageId }
    }
}

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data object Success : ChatUiState
    data class Error(val message: String) : ChatUiState
}