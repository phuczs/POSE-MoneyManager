package com.example.moneymanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.usecase.ai.PredictCategoryUseCase
import com.example.moneymanager.domain.usecase.transaction.AddTransactionUseCase
import com.example.moneymanager.domain.usecase.transaction.ClearTransactionCacheUseCase
import com.example.moneymanager.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.moneymanager.domain.usecase.transaction.GetAllTransactionsUseCase
import com.example.moneymanager.domain.usecase.transaction.GetPagedTransactionsUseCase
import com.example.moneymanager.domain.usecase.transaction.GetTransactionsByMonthUseCase
import com.example.moneymanager.domain.usecase.transaction.GetTransactionsByTypeAndMonthUseCase
import com.example.moneymanager.domain.usecase.transaction.GetTransactionsByTypeUseCase
import com.example.moneymanager.domain.usecase.transaction.SearchTransactionsUseCase
import com.example.moneymanager.domain.usecase.transaction.GetTransactionByIdUseCase
import com.example.moneymanager.domain.usecase.transaction.UpdateTransactionUseCase
import com.example.moneymanager.domain.usecase.category.GetAllCategoriesUseCase
import com.example.moneymanager.domain.repository.AiServiceRepository
import com.example.moneymanager.util.HabitAnalyzer
import com.example.moneymanager.util.PromptUtils
import com.example.moneymanager.util.toCurrencyString
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HabitualSuggestion(
    val category: String,
    val description: String,
    val amount: Double
)

data class SearchFilterState(
    val query: String = "",
    val type: String = "",
    val category: String = "",
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
) {
    val isActive: Boolean
        get() = query.isNotBlank() || type.isNotBlank() || category.isNotBlank() ||
                minAmount != null || maxAmount != null || startDate != null || endDate != null
}

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val getAllTransactionsUseCase: GetAllTransactionsUseCase,
    private val getTransactionsByTypeUseCase: GetTransactionsByTypeUseCase,
    private val searchTransactionsUseCase: SearchTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getTransactionsByMonthUseCase: GetTransactionsByMonthUseCase,
    private val getTransactionsByTypeAndMonthUseCase: GetTransactionsByTypeAndMonthUseCase,
    private val getPagedTransactionsUseCase: GetPagedTransactionsUseCase,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val predictCategoryUseCase: PredictCategoryUseCase,
    private val getAllCategoriesUseCase: GetAllCategoriesUseCase,
    private val clearTransactionCacheUseCase: ClearTransactionCacheUseCase,
    private val aiServiceRepository: AiServiceRepository,
    private val exportTransactionsUseCase: com.example.moneymanager.domain.usecase.export.ExportTransactionsUseCase,
    private val syncDataUseCase: com.example.moneymanager.domain.usecase.sync.SyncDataUseCase
) : ViewModel() {

    val syncState: StateFlow<com.example.moneymanager.domain.repository.SyncState> = syncDataUseCase.syncState

    fun triggerSync() {
        viewModelScope.launch {
            syncDataUseCase()
        }
    }

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

    // --- Smart Auto-Categorization ---
    private val _aiCategoryPrediction = MutableStateFlow<String?>(null)
    val aiCategoryPrediction: StateFlow<String?> = _aiCategoryPrediction.asStateFlow()
    private var _categoryPredictionJob: Job? = null

    // --- Habitual Suggestions ---
    private val _habitualSuggestion = MutableStateFlow<HabitualSuggestion?>(null)
    val habitualSuggestion: StateFlow<HabitualSuggestion?> = _habitualSuggestion.asStateFlow()

    // --- Paging 3 filter state ---
    private val _pagedFilter = MutableStateFlow(PagedFilter())
    val pagedTransactions: Flow<PagingData<Transaction>> = _pagedFilter
        .flatMapLatest { filter ->
            getPagedTransactionsUseCase(type = filter.type, month = filter.month, year = filter.year)
        }
        .cachedIn(viewModelScope)

    fun setPagedFilter(type: String? = null, month: Int? = null, year: Int? = null) {
        _pagedFilter.value = PagedFilter(type, month, year)
    }

    private val gson = Gson()

    // --- Search Logic ---
    private var _cachedTransactions: List<Transaction> = emptyList()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _searchFilterState = MutableStateFlow(SearchFilterState())
    val searchFilterState: StateFlow<SearchFilterState> = _searchFilterState.asStateFlow()
    private var _searchJob: Job? = null

    init {
        loadAllTransactions()
    }

    /**
     * Triggers AI category prediction with 300ms debounce.
     * Uses the PredictCategoryUseCase which enforces minimum input length.
     */
    fun predictCategory(description: String, amount: Double) {
        _categoryPredictionJob?.cancel()
        if (description.length < 3) {
            _aiCategoryPrediction.value = null
            return
        }
        _categoryPredictionJob = viewModelScope.launch {
            delay(300) // Debounce — don't fire on every keystroke
            try {
                val categories = withTimeoutOrNull(2_000) {
                    getAllCategoriesUseCase().first()
                } ?: return@launch
                val predicted = predictCategoryUseCase(
                    rawDescription = description,
                    amount = amount,
                    recentTransactions = _cachedTransactions.take(30),
                    availableCategories = categories.map { it.name }
                )
                _aiCategoryPrediction.value = predicted.ifBlank { null }
            } catch (e: Exception) {
                Log.w("TransactionViewModel", "Category prediction skipped: ${e.javaClass.simpleName}")
                _aiCategoryPrediction.value = null
            }
        }
    }

    fun clearCategoryPrediction() {
        _categoryPredictionJob?.cancel()
        _aiCategoryPrediction.value = null
    }

    fun processQuickAdd(input: String, languageCode: String = "vi") {
        if (input.isBlank()) return
        val isVi = languageCode == "vi"
        viewModelScope.launch {
            _quickAddState.value = QuickAddState.Loading
            try {
                val categories = withTimeoutOrNull(2000) {
                    getAllCategoriesUseCase().first()
                } ?: emptyList()
                val categoryNames = categories.joinToString(", ") { it.name }
                val prompt = PromptUtils.getQuickAddPrompt(input, categoryNames)

                val result = aiServiceRepository.queryAi(prompt, jsonMode = true)
                result.fold(
                    onSuccess = { responseText ->
                        val transaction = parseAndSaveTransaction(responseText, input, categories)
                        if (transaction != null) {
                            val msg = if (isVi)
                                "Đã thêm: ${transaction.description} (${transaction.amount.toCurrencyString()})"
                            else
                                "Added: ${transaction.description} (${transaction.amount.toCurrencyString()})"
                            _quickAddState.value = QuickAddState.Success(msg)
                            delay(3000)
                            _quickAddState.value = QuickAddState.Idle
                        } else {
                            val errMsg = if (isVi)
                                "Không hiểu câu lệnh. Hãy thử: 'Ăn trưa 30k'"
                            else
                                "Could not understand. Try: 'Lunch 5'"
                            _quickAddState.value = QuickAddState.Error(errMsg)
                        }
                    },
                    onFailure = {
                        val errMsg = if (isVi) "Lỗi kết nối AI: ${it.message}" else "AI connection error: ${it.message}"
                        _quickAddState.value = QuickAddState.Error(errMsg)
                    }
                )
            } catch (e: Exception) {
                val errMsg = if (isVi) "Lỗi: ${e.message}" else "Error: ${e.message}"
                _quickAddState.value = QuickAddState.Error(errMsg)
            }
        }
    }

    fun executeHabit(suggestion: HabitualSuggestion, languageCode: String = "vi") {
        val isVi = languageCode == "vi"
        viewModelScope.launch {
            _quickAddState.value = QuickAddState.Loading
            try {
                val today = LocalDate.now()
                val transaction = Transaction(
                    amount = suggestion.amount,
                    type = "expense",
                    category = suggestion.category,
                    description = suggestion.description,
                    date = System.currentTimeMillis(),
                    month = today.monthValue,
                    year = today.year
                )
                addTransaction(transaction)
                val msg = if (isVi)
                    "Đã thêm nhanh: ${transaction.description} (${transaction.amount.toCurrencyString()})"
                else
                    "Quick added: ${transaction.description} (${transaction.amount.toCurrencyString()})"
                _quickAddState.value = QuickAddState.Success(msg)
                delay(3000)
                _quickAddState.value = QuickAddState.Idle
            } catch (e: Exception) {
                val errMsg = if (isVi) "Lỗi: ${e.message}" else "Error: ${e.message}"
                _quickAddState.value = QuickAddState.Error(errMsg)
            }
        }
    }

    private suspend fun parseAndSaveTransaction(
        jsonResponse: String,
        originalInput: String,
        availableCategories: List<com.example.moneymanager.domain.model.Category>
    ): Transaction? {
        return try {
            val aiData = gson.fromJson(jsonResponse, com.example.moneymanager.data.model.AiTransactionData::class.java)
            if (aiData.amount != null && aiData.amount > 0) {
                val matchedCategory = availableCategories.find {
                    it.name.equals(aiData.category, ignoreCase = true)
                } ?: availableCategories.firstOrNull()

                val today = LocalDate.now()
                val transaction = Transaction(
                    amount = aiData.amount,
                    type = aiData.type ?: "expense",
                    category = matchedCategory?.name ?: "General",
                    description = aiData.description ?: originalInput,
                    date = System.currentTimeMillis(),
                    month = today.monthValue,
                    year = today.year
                )
                addTransaction(transaction)
                transaction
            } else null
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
        _searchFilterState.value = _searchFilterState.value.copy(query = query)
        performSearch()
    }

    fun applyAdvancedFilter(
        type: String = _searchFilterState.value.type,
        category: String = _searchFilterState.value.category,
        minAmount: Double? = _searchFilterState.value.minAmount,
        maxAmount: Double? = _searchFilterState.value.maxAmount,
        startDate: Long? = _searchFilterState.value.startDate,
        endDate: Long? = _searchFilterState.value.endDate
    ) {
        _searchFilterState.value = _searchFilterState.value.copy(
            type = type,
            category = category,
            minAmount = minAmount,
            maxAmount = maxAmount,
            startDate = startDate,
            endDate = endDate
        )
        performSearch()
    }

    fun clearAdvancedFilter() {
        val currentQuery = _searchFilterState.value.query
        _searchFilterState.value = SearchFilterState(query = currentQuery)
        if (currentQuery.isNotBlank()) {
            performSearch()
        } else {
            loadAllTransactions()
        }
    }

    fun performSearch() {
        val filter = _searchFilterState.value
        if (!filter.isActive) {
            _transactionsState.value = TransactionsState.Success(_cachedTransactions)
            return
        }
        _searchJob?.cancel()
        _searchJob = viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            searchTransactionsUseCase(
                query = filter.query,
                type = filter.type,
                category = filter.category,
                minAmount = filter.minAmount,
                maxAmount = filter.maxAmount,
                startDate = filter.startDate,
                endDate = filter.endDate
            )
                .catch { e ->
                    _transactionsState.value = TransactionsState.Error(e.message ?: "Failed to search transactions")
                }
                .collectLatest { transactions ->
                    _transactionsState.value = TransactionsState.Success(transactions)
                }
        }
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
        analyzeHabits(_cachedTransactions)
    }

    private fun analyzeHabits(transactions: List<Transaction>) {
        _habitualSuggestion.value = HabitAnalyzer.analyze(transactions)
    }

    // --- Selection Mode ---

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) _selectedTransactionIds.value = emptySet()
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

    fun clearSelection() { _selectedTransactionIds.value = emptySet() }

    fun deleteSelectedTransactions() {
        viewModelScope.launch {
            val idsToDelete = _selectedTransactionIds.value.toList()
            var failCount = 0
            idsToDelete.forEach { id ->
                deleteTransactionUseCase(id).onFailure { failCount++ }
            }
            _isSelectionMode.value = false
            _selectedTransactionIds.value = emptySet()
            if (failCount > 0) Log.e("TransactionViewModel", "Failed to delete $failCount transactions")
        }
    }

    // --- Data Loading ---

    fun loadAllTransactions() {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            getAllTransactionsUseCase()
                .catch { e ->
                    _transactionsState.value = TransactionsState.Error(e.message ?: "Failed to load transactions")
                }
                .collectLatest { transactions ->
                    _cachedTransactions = transactions
                    applySearchFilter()
                }
        }
    }

    fun loadTransactionsByMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            getTransactionsByMonthUseCase(yearMonth.monthValue, yearMonth.year)
                .catch { e ->
                    _transactionsState.value = TransactionsState.Error(e.message ?: "Failed to load transactions by month")
                }
                .collectLatest { transactions ->
                    _cachedTransactions = transactions
                    applySearchFilter()
                }
        }
    }

    fun loadTransactionsByTypeAndMonth(type: String, yearMonth: YearMonth) {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            getTransactionsByTypeAndMonthUseCase(type, yearMonth)
                .catch { e ->
                    _transactionsState.value = TransactionsState.Error(e.message ?: "Failed to load filtered transactions")
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
            getTransactionsByTypeUseCase(type)
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

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            _transactionsState.value = TransactionsState.Loading
            addTransactionUseCase(transaction).fold(
                onSuccess = { loadAllTransactions() },
                onFailure = { error ->
                    _transactionsState.value = TransactionsState.Error(error.message ?: "Failed to add transaction")
                }
            )
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            updateTransactionUseCase(transaction).fold(
                onSuccess = { loadAllTransactions() },
                onFailure = { error ->
                    _transactionsState.value = TransactionsState.Error(error.message ?: "Failed to update transaction")
                }
            )
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            deleteTransactionUseCase(transactionId).fold(
                onSuccess = { loadAllTransactions() },
                onFailure = { error ->
                    _transactionsState.value = TransactionsState.Error(error.message ?: "Failed to delete transaction")
                }
            )
        }
    }

    /** Loads a single transaction by ID into [currentTransaction]. Used by the Edit screen. */
    fun getTransactionById(transactionId: String) {
        viewModelScope.launch {
            getTransactionByIdUseCase(transactionId).fold(
                onSuccess = { _currentTransaction.value = it },
                onFailure = { Log.e("TransactionViewModel", "Failed to load transaction $transactionId", it) }
            )
        }
    }

    /** Wipes user local cache on logout to prevent cross-account data leakage. */
    fun clearLocalCache(userId: String) {
        viewModelScope.launch {
            try {
                clearTransactionCacheUseCase(userId)
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Failed to clear local cache", e)
            }
        }
    }

    fun exportTransactions(
        context: android.content.Context,
        format: com.example.moneymanager.ui.components.ExportFormat,
        range: com.example.moneymanager.ui.components.ExportRange,
        onFileReady: (android.net.Uri, String) -> Unit
    ) {
        viewModelScope.launch {
            val allList = when (val state = _transactionsState.value) {
                is TransactionsState.Success -> state.transactions
                else -> _cachedTransactions
            }

            val cal = java.util.Calendar.getInstance()
            val now = System.currentTimeMillis()

            val filteredList = when (range) {
                com.example.moneymanager.ui.components.ExportRange.THIS_MONTH -> {
                    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    val startOfMonth = cal.timeInMillis
                    allList.filter { it.date >= startOfMonth }
                }
                com.example.moneymanager.ui.components.ExportRange.LAST_MONTH -> {
                    cal.add(java.util.Calendar.MONTH, -1)
                    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    val startOfLastMonth = cal.timeInMillis

                    cal.add(java.util.Calendar.MONTH, 1)
                    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    val endOfLastMonth = cal.timeInMillis
                    allList.filter { it.date in startOfLastMonth until endOfLastMonth }
                }
                com.example.moneymanager.ui.components.ExportRange.THIS_YEAR -> {
                    cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    val startOfYear = cal.timeInMillis
                    allList.filter { it.date >= startOfYear }
                }
                com.example.moneymanager.ui.components.ExportRange.ALL_TIME -> allList
            }

            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())

            val result = if (format == com.example.moneymanager.ui.components.ExportFormat.CSV) {
                exportTransactionsUseCase.toCsv(filteredList, "MoneyManager_Report_$timeStamp.csv")
            } else {
                exportTransactionsUseCase.toPdf(filteredList, "Financial Report ($range)", "MoneyManager_Report_$timeStamp.pdf")
            }

            result.onSuccess { file ->
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val mimeType = if (format == com.example.moneymanager.ui.components.ExportFormat.CSV) "text/csv" else "application/pdf"
                onFileReady(uri, mimeType)
            }
        }
    }

    sealed class TransactionsState {
        object Loading : TransactionsState()
        data class Success(val transactions: List<Transaction>) : TransactionsState()
        data class Error(val message: String) : TransactionsState()
    }

    data class PagedFilter(
        val type: String? = null,
        val month: Int? = null,
        val year: Int? = null
    )
}