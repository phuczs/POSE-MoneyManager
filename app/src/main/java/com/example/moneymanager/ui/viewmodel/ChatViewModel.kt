package com.example.moneymanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.domain.model.ChatMessage
import com.example.moneymanager.domain.repository.AiServiceRepository
import com.example.moneymanager.domain.repository.AuthRepository
import com.example.moneymanager.domain.repository.ChatHistoryRepository
import com.example.moneymanager.domain.repository.SettingsRepository
import com.example.moneymanager.domain.usecase.budget.GetBudgetsUseCase
import com.example.moneymanager.domain.usecase.transaction.GetAllTransactionsUseCase
import com.example.moneymanager.util.PromptUtils
import com.example.moneymanager.util.toCurrencyString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiServiceRepository: AiServiceRepository,
    private val getAllTransactionsUseCase: GetAllTransactionsUseCase,
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val settingsRepository: SettingsRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private var financialContextPrompt: String = ""
    private var currentUserId: String = "local_user"

    init {
        observeUserAndHistory()
    }

    private fun observeUserAndHistory() {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                currentUserId = user?.id ?: "local_user"
                loadChatHistory(currentUserId)
                initializeFinancialContext()
            }
        }
    }

    private fun loadChatHistory(userId: String) {
        viewModelScope.launch {
            chatHistoryRepository.getMessages(userId).collectLatest { persistedMessages ->
                if (persistedMessages.isEmpty()) {
                    val currentLanguage = settingsRepository.currentLanguage.value
                    val welcomeText = if (currentLanguage == "vi")
                        "Xin chào! Tôi là trợ lý tài chính của bạn. Dựa trên dữ liệu tháng này, tôi có thể giúp gì cho bạn?"
                    else
                        "Hello! I am your financial assistant. Based on this month's data, how can I help you?"
                    val initialMessage = ChatMessage(content = welcomeText, isFromUser = false)
                    chatHistoryRepository.saveMessage(userId, initialMessage)
                } else {
                    _messages.value = persistedMessages
                }
            }
        }
    }

    private fun initializeFinancialContext() {
        viewModelScope.launch {
            _chatState.value = ChatUiState.Loading

            try {
                val allTransactions = withTimeoutOrNull(3000) {
                    getAllTransactionsUseCase().first()
                } ?: emptyList()
                val allBudgets = withTimeoutOrNull(3000) {
                    getBudgetsUseCase(System.currentTimeMillis()).first()
                } ?: emptyList()

                val today = LocalDate.now()
                val thisMonthTransactions = allTransactions.filter {
                    val txDate = java.time.Instant.ofEpochMilli(it.date)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    txDate.month == today.month && txDate.year == today.year
                }

                val totalIncome = thisMonthTransactions.filter { it.type == "income" }.sumOf { it.amount }
                val totalExpense = thisMonthTransactions.filter { it.type == "expense" }.sumOf { it.amount }
                val balance = totalIncome - totalExpense

                val expenseByCategory = thisMonthTransactions.filter { it.type == "expense" }
                    .groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
                    .toList().sortedByDescending { it.second }.take(3)
                    .joinToString(", ") { "${it.first}: ${it.second.toCurrencyString()}" }

                val budgetAnalysis = if (allBudgets.isNotEmpty()) {
                    val details = allBudgets.joinToString("\n") { budget ->
                        val spentForBudget = allTransactions.filter { tx ->
                            tx.category == budget.category &&
                                    tx.type == "expense" &&
                                    tx.date >= budget.startDate &&
                                    tx.date <= budget.endDate
                        }.sumOf { it.amount }
                        val percent = if (budget.allocatedAmount > 0) (spentForBudget / budget.allocatedAmount * 100).toInt() else 0
                        val status = when {
                            percent >= 100 -> "OVER BUDGET"
                            percent >= 80  -> "Warning"
                            else           -> "Safe"
                        }
                        "- ${budget.category}: ${spentForBudget.toCurrencyString()}/${budget.allocatedAmount.toCurrencyString()} ($percent% - $status)"
                    }
                    "\nBUDGET STATUS:\n$details"
                } else {
                    "\nBUDGET STATUS: No budgets configured."
                }

                val currentLanguage = settingsRepository.currentLanguage.value
                financialContextPrompt = PromptUtils.getFinancialAdvisorPrompt(
                    totalIncome, totalExpense, balance, expenseByCategory, budgetAnalysis,
                    languageCode = currentLanguage
                )

                _chatState.value = ChatUiState.Success

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Context init error: ${e.message}", e)
                _chatState.value = ChatUiState.Success
            }
        }
    }

    fun sendMessage(messageContent: String) {
        if (messageContent.isBlank()) return

        val userMessage = ChatMessage(content = messageContent, isFromUser = true)
        viewModelScope.launch {
            chatHistoryRepository.saveMessage(currentUserId, userMessage)
        }

        val loadingMessageId = UUID.randomUUID().toString()
        val tempMessages = _messages.value + ChatMessage(id = loadingMessageId, content = "", isFromUser = false, isLoading = true)
        _messages.value = tempMessages

        viewModelScope.launch {
            try {
                val currentLanguage = settingsRepository.currentLanguage.value
                val prompt = PromptUtils.getChatAdvisorPrompt(
                    financialContextPrompt, messageContent, languageCode = currentLanguage
                )

                val result = aiServiceRepository.queryAi(prompt, jsonMode = false)

                removeLoadingMessage(loadingMessageId)
                result.fold(
                    onSuccess = { responseText ->
                        val aiMessage = ChatMessage(content = responseText.trim(), isFromUser = false)
                        chatHistoryRepository.saveMessage(currentUserId, aiMessage)
                    },
                    onFailure = { error ->
                        val errMessage = ChatMessage(content = "Connection error: ${error.message}", isFromUser = false, isError = true)
                        chatHistoryRepository.saveMessage(currentUserId, errMessage)
                    }
                )
            } catch (e: Exception) {
                removeLoadingMessage(loadingMessageId)
                val errMessage = ChatMessage(content = "Error: ${e.message}", isFromUser = false, isError = true)
                chatHistoryRepository.saveMessage(currentUserId, errMessage)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatHistoryRepository.clearHistory(currentUserId)
            val currentLanguage = settingsRepository.currentLanguage.value
            val welcomeText = if (currentLanguage == "vi")
                "Đã xóa lịch sử trò chuyện. Tôi có thể giúp gì cho bạn?"
            else
                "Chat history cleared. How can I help you?"
            val initialMessage = ChatMessage(content = welcomeText, isFromUser = false)
            chatHistoryRepository.saveMessage(currentUserId, initialMessage)
        }
    }

    private fun removeLoadingMessage(messageId: String) {
        _messages.value = _messages.value.filter { it.id != messageId }
    }
}

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data object Success : ChatUiState
    data class Error(val message: String) : ChatUiState
}