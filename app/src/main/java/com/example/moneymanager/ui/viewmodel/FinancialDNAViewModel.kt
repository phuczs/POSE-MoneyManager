package com.example.moneymanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.domain.model.DnaAxis
import com.example.moneymanager.domain.model.FinancialDNA
import com.example.moneymanager.domain.repository.SettingsRepository
import com.example.moneymanager.domain.usecase.ai.AnalyzeFinancialDNAUseCase
import com.example.moneymanager.domain.usecase.budget.GetAllBudgetsUseCase
import com.example.moneymanager.domain.usecase.transaction.GetAllTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

sealed interface FinancialDnaUiState {
    data object Loading : FinancialDnaUiState
    data class Success(val dna: FinancialDNA) : FinancialDnaUiState
    data class InsufficientData(
        val currentTransactions: Int,
        val requiredTransactions: Int = AnalyzeFinancialDNAUseCase.MIN_TRANSACTIONS_REQUIRED
    ) : FinancialDnaUiState
    data class Error(val message: String) : FinancialDnaUiState
}

@HiltViewModel
class FinancialDNAViewModel @Inject constructor(
    private val analyzeFinancialDNAUseCase: AnalyzeFinancialDNAUseCase,
    private val getAllTransactionsUseCase: GetAllTransactionsUseCase,
    private val getAllBudgetsUseCase: GetAllBudgetsUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FinancialDnaUiState>(FinancialDnaUiState.Loading)
    val uiState: StateFlow<FinancialDnaUiState> = _uiState.asStateFlow()

    private val _selectedAxis = MutableStateFlow(DnaAxis.SAVINGS_VS_INDULGENCE)
    val selectedAxis: StateFlow<DnaAxis> = _selectedAxis.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val currentLanguage: StateFlow<String> = settingsRepository.currentLanguage

    init {
        loadFinancialDna()
    }

    fun selectAxis(axis: DnaAxis) {
        _selectedAxis.value = axis
    }

    fun refreshFinancialDna() {
        loadFinancialDna(forceRefresh = true)
    }

    fun loadFinancialDna(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val hadData = _uiState.value is FinancialDnaUiState.Success
            if (forceRefresh && hadData) {
                _isRefreshing.value = true
            } else {
                _uiState.value = FinancialDnaUiState.Loading
            }

            try {
                val languageCode = settingsRepository.currentLanguage.value
                val transactions = withTimeoutOrNull(6_000L) {
                    getAllTransactionsUseCase().first()
                } ?: emptyList()

                val budgets = withTimeoutOrNull(5_000L) {
                    getAllBudgetsUseCase().first()
                } ?: emptyList()

                val expenseCount = transactions.count {
                    it.type.equals("expense", ignoreCase = true) && it.amount > 0.0
                }
                if (transactions.size < AnalyzeFinancialDNAUseCase.MIN_TRANSACTIONS_REQUIRED ||
                    expenseCount < AnalyzeFinancialDNAUseCase.MIN_EXPENSES_REQUIRED
                ) {
                    _uiState.value = FinancialDnaUiState.InsufficientData(
                        currentTransactions = transactions.size,
                        requiredTransactions = AnalyzeFinancialDNAUseCase.MIN_TRANSACTIONS_REQUIRED
                    )
                    return@launch
                }

                val dna = analyzeFinancialDNAUseCase(
                    transactions = transactions,
                    budgets = budgets,
                    languageCode = languageCode,
                    forceRefresh = forceRefresh
                )

                _uiState.value = if (dna != null) {
                    FinancialDnaUiState.Success(dna)
                } else {
                    FinancialDnaUiState.InsufficientData(
                        currentTransactions = transactions.size,
                        requiredTransactions = AnalyzeFinancialDNAUseCase.MIN_TRANSACTIONS_REQUIRED
                    )
                }
            } catch (e: Exception) {
                Log.e("FinancialDNAViewModel", "Failed to analyze Financial DNA", e)
                if (!hadData) {
                    _uiState.value = FinancialDnaUiState.Error(e.localizedMessage ?: "Unknown error")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
