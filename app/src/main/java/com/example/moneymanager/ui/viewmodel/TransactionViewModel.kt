package com.example.moneymanager.ui.viewmodel

import android.os.Build
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.data.repository.CategoryRepository
import com.example.moneymanager.data.repository.OllamaRepository
import com.google.gson.Gson
import com.example.moneymanager.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.YearMonth
import com.example.moneymanager.util.toCurrencyString
import com.example.moneymanager.data.model.AiTransactionData
import com.example.moneymanager.util.PromptUtils
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val ollamaRepository: OllamaRepository, // Inject thêm
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _transactionsState = MutableStateFlow<TransactionsState>(TransactionsState.Loading)
    val transactionsState: StateFlow<TransactionsState> = _transactionsState.asStateFlow()

    private val _currentTransaction = MutableStateFlow<Transaction?>(null)
    val currentTransaction: StateFlow<Transaction?> = _currentTransaction.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    private val _selectedTransactionIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedTransactionIds: StateFlow<Set<String>> = _selectedTransactionIds.asStateFlow()
    private val _quickAddState = MutableStateFlow<QuickAddState>(QuickAddState.Idle)
    val quickAddState: StateFlow<QuickAddState> = _quickAddState.asStateFlow()

    private val gson = Gson()

    // --- Search Logic ---
    private var _cachedTransactions: List<Transaction> = emptyList() // Stores raw data from repo
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    fun processQuickAdd(input: String) {
        if (input.isBlank()) return

        viewModelScope.launch {
            _quickAddState.value = QuickAddState.Loading

            try {
                // 1. Lấy danh mục để AI map đúng
                val categories = withTimeoutOrNull(2000) {
                    categoryRepository.getAllCategories().first()
                } ?: emptyList<com.example.moneymanager.data.model.Category>()
                val categoryNames = categories.joinToString(", ") { it.name }

                // 2. Prompt CHUYÊN BIỆT cho việc trích xuất dữ liệu (Extraction Prompt)
                // Prompt này ngắn gọn, chỉ tập trung vào việc lấy số tiền và loại
               val prompt = PromptUtils.getQuickAddPrompt(input, categoryNames)

                val result = ollamaRepository.sendMessage(prompt, jsonMode = true)

                result.fold(
                    onSuccess = { responseText ->
                        val transaction = parseAndSaveTransaction(responseText, input, categories)
                        if (transaction != null) {
                            _quickAddState.value = QuickAddState.Success("Đã thêm: ${transaction.description} (${transaction.amount.toCurrencyString()})")
                            // Reset state sau 3s để ẩn thông báo
                            kotlinx.coroutines.delay(3000)
                            _quickAddState.value = QuickAddState.Idle
                        } else {
                            _quickAddState.value = QuickAddState.Error("Không hiểu câu lệnh. Hãy thử: 'Ăn trưa 30k'")
                        }
                    },
                    onFailure = {
                        _quickAddState.value = QuickAddState.Error("Lỗi kết nối AI: ${it.message}")
                    }
                )

            } catch (e: Exception) {
                _quickAddState.value = QuickAddState.Error("Lỗi: ${e.message}")
            }
        }
    }
    private suspend fun parseAndSaveTransaction(
        jsonResponse: String,
        originalInput: String,
        availableCategories: List<com.example.moneymanager.data.model.Category>
    ): Transaction? {
        return try {
            val aiData = gson.fromJson(jsonResponse, AiTransactionData::class.java)

            if (aiData.amount != null && aiData.amount > 0) {
                val matchedCategory = availableCategories.find {
                    it.name.equals(aiData.category, ignoreCase = true)
                } ?: availableCategories.firstOrNull()

                val transaction = Transaction(
                    amount = aiData.amount,
                    type = aiData.type ?: "expense",
                    category = matchedCategory?.name ?: "General",
                    description = aiData.description ?: originalInput,
                    date = Timestamp.now(),
                    month = LocalDate.now().monthValue,
                    year = LocalDate.now().year
                )

                addTransaction(transaction) // Gọi hàm add có sẵn
                transaction
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("QuickAdd", "Parse Error", e)
            null
        }
    }
    sealed interface QuickAddState {
        data object Idle : QuickAddState
        data object Loading : QuickAddState
        data class Success(val message: String) : QuickAddState
        data class Error(val message: String) : QuickAddState
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applySearchFilter()
    }

    private fun applySearchFilter() {
        val query = _searchQuery.value
        if (query.isBlank()) {
            _transactionsState.value = TransactionsState.Success(_cachedTransactions)
        } else {
            val filtered = _cachedTransactions.filter { transaction ->
                transaction.category.contains(query, ignoreCase = true) ||
                        transaction.description.contains(query, ignoreCase = true) ||
                        transaction.amount.toString().contains(query)
            }
            _transactionsState.value = TransactionsState.Success(filtered)
        }
    }

    // --- Selection Mode ---

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun toggleTransactionSelection(transactionId: String) {
        _selectedTransactionIds.value = if (_selectedTransactionIds.value.contains(transactionId)) {
            _selectedTransactionIds.value - transactionId
        } else {
            _selectedTransactionIds.value + transactionId
        }
    }

    fun selectAllTransaction(transactions: List<Transaction>) {
        _selectedTransactionIds.value = transactions.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedTransactionIds.value = emptySet()
    }

    fun deleteSelectedTransactions() {
        viewModelScope.launch {
            val idsToDelete = _selectedTransactionIds.value.toList()
            var successCount = 0
            var failCount = 0

            idsToDelete.forEach { id ->
                transactionRepository.deleteTransaction(id).fold(
                    onSuccess = { successCount++ },
                    onFailure = { failCount++ }
                )
            }
            _isSelectionMode.value = false
            _selectedTransactionIds.value = emptySet()
            if (failCount > 0 ){
                Log.e("TransactionViewModel", "Failed to delete $failCount transactions")
            }
        }
    }

    init {
        loadAllTransactions()
    }

    // --- Data Loading (Updated to use cache) ---

    fun loadAllTransactions() {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            transactionRepository.getAllTransactions()
                .catch { e ->
                    _transactionsState.value = TransactionsState.Error(e.message ?: "Failed to load transactions")
                }
                .collectLatest { transactions ->
                    _cachedTransactions = transactions
                    applySearchFilter()
                }
        }
    }

    fun loadTransactionsByType(type: String) {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            transactionRepository.getTransactionsByType(type)
                .catch { e ->
                    Log.e("TransactionViewModel", "Error loading by type: ${e.message}", e)
                    _transactionsState.value = TransactionsState.Error(e.message ?: "Failed to load transactions by type")
                }
                .collectLatest { transactions ->
                    _cachedTransactions = transactions
                    applySearchFilter()
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadTransactionsByMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            transactionRepository.getTransactionsByMonth(yearMonth.monthValue, yearMonth.year)
                .catch { e ->
                    Log.e("TransactionViewModel", "Error loading by month: ${e.message}", e)
                    _transactionsState.value = TransactionsState.Error(e.message ?: "Failed to load transactions by month")
                }
                .collectLatest { transactions ->
                    _cachedTransactions = transactions
                    applySearchFilter()
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadTransactionsByTypeAndMonth(type: String, yearMonth: YearMonth) {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            transactionRepository.getTransactionsByTypeAndMonth(type, yearMonth)
                .catch { e ->
                    Log.e("TransactionViewModel", "Error loading by type and month: ${e.message}", e)
                    _transactionsState.value = TransactionsState.Error(e.message ?: "Failed to load filtered transactions")
                }
                .collectLatest { transactions ->
                    _cachedTransactions = transactions
                    applySearchFilter()
                }
        }
    }

    fun loadRecentTransactions(limit: Int = 5) {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            transactionRepository.getRecentTransactions(limit)
                .catch { e ->
                    _transactionsState.value = TransactionsState.Error(e.message ?: "Failed to load transactions")
                }
                .collectLatest { transactions ->
                    _cachedTransactions = transactions
                    applySearchFilter()
                }
        }
    }

    // --- CRUD Operations ---

    fun getTransactionById(id: String) {
        viewModelScope.launch {
            transactionRepository.getTransactionById(id).fold(
                onSuccess = { transaction -> _currentTransaction.value = transaction },
                onFailure = { /* Handle error */ }
            )
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            transactionRepository.addTransaction(transaction)
                .fold(
                    onSuccess = { loadAllTransactions() },
                    onFailure = { error ->
                        _transactionsState.value = TransactionsState.Error(error.message ?: "Failed to add transaction")
                    }
                )
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
                .fold(
                    onSuccess = { loadAllTransactions() },
                    onFailure = { /* Handle error */ }
                )
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transactionId)
                .fold(
                    onSuccess = { loadAllTransactions() },
                    onFailure = { /* Handle error */ }
                )
        }
    }

    sealed class TransactionsState {
        object Loading : TransactionsState()
        data class Success(val transactions: List<Transaction>) : TransactionsState()
        data class Error(val message: String) : TransactionsState()
    }
}