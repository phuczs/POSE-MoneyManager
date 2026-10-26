package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.domain.model.RecurringFrequency
import com.example.moneymanager.domain.model.RecurringTransaction
import com.example.moneymanager.domain.repository.AuthRepository
import com.example.moneymanager.domain.usecase.recurring.DeleteRecurringTransactionUseCase
import com.example.moneymanager.domain.usecase.recurring.GetRecurringTransactionsUseCase
import com.example.moneymanager.domain.usecase.recurring.ProcessDueRecurringTransactionsUseCase
import com.example.moneymanager.domain.usecase.recurring.SaveRecurringTransactionUseCase
import com.example.moneymanager.domain.usecase.recurring.ToggleRecurringActiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecurringUiState {
    data object Loading : RecurringUiState
    data class Success(val items: List<RecurringTransaction>) : RecurringUiState
    data class Error(val message: String) : RecurringUiState
}

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    private val saveRecurringTransactionUseCase: SaveRecurringTransactionUseCase,
    private val toggleRecurringActiveUseCase: ToggleRecurringActiveUseCase,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val processDueRecurringTransactionsUseCase: ProcessDueRecurringTransactionsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecurringUiState>(RecurringUiState.Loading)
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    private var currentUserId: String = "local_user"

    init {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                currentUserId = user?.id ?: "local_user"
                loadRecurringTransactions(currentUserId)
            }
        }
        processDueItems()
    }

    private fun loadRecurringTransactions(userId: String) {
        viewModelScope.launch {
            _uiState.value = RecurringUiState.Loading
            getRecurringTransactionsUseCase(userId).collectLatest { list ->
                _uiState.value = RecurringUiState.Success(list)
            }
        }
    }

    fun processDueItems() {
        viewModelScope.launch {
            processDueRecurringTransactionsUseCase()
        }
    }

    fun saveRecurringTransaction(
        id: String = "",
        title: String,
        amount: Double,
        type: String,
        category: String,
        frequency: RecurringFrequency,
        startDate: Long,
        nextExecutionDate: Long? = null,
        isActive: Boolean = true,
        description: String = ""
    ) {
        viewModelScope.launch {
            val item = RecurringTransaction(
                id = id,
                userId = currentUserId,
                title = title,
                amount = amount,
                type = type,
                category = category,
                frequency = frequency,
                startDate = startDate,
                nextExecutionDate = nextExecutionDate ?: startDate,
                isActive = isActive,
                description = description
            )
            saveRecurringTransactionUseCase(currentUserId, item)
        }
    }

    fun toggleActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            toggleRecurringActiveUseCase(currentUserId, id, isActive)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            deleteRecurringTransactionUseCase(currentUserId, id)
        }
    }
}
