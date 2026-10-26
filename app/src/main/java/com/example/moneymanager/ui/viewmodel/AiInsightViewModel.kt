package com.example.moneymanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.domain.model.AiInsight
import com.example.moneymanager.domain.repository.AiServiceRepository
import com.example.moneymanager.domain.repository.SettingsRepository
import com.example.moneymanager.domain.usecase.ai.GetMonthlyInsightUseCase
import com.example.moneymanager.domain.usecase.transaction.GetAllTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class AiInsightViewModel @Inject constructor(
    private val getMonthlyInsightUseCase: GetMonthlyInsightUseCase,
    private val getAllTransactionsUseCase: GetAllTransactionsUseCase,
    private val aiServiceRepository: AiServiceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _insightState = MutableStateFlow<InsightUiState>(InsightUiState.Loading)
    val insightState: StateFlow<InsightUiState> = _insightState.asStateFlow()

    init {
        loadInsight()
    }

    fun loadInsight() {
        viewModelScope.launch {
            _insightState.value = InsightUiState.Loading
            try {
                val languageCode = settingsRepository.currentLanguage.value

                val allTransactions = withTimeoutOrNull(5_000) {
                    getAllTransactionsUseCase().first()
                } ?: run {
                    _insightState.value = InsightUiState.Unavailable
                    return@launch
                }

                if (allTransactions.isEmpty()) {
                    _insightState.value = InsightUiState.InsufficientData
                    return@launch
                }

                // Business logic (month filtering) is now in GetMonthlyInsightUseCase
                val insight = getMonthlyInsightUseCase(
                    allTransactions = allTransactions,
                    languageCode = languageCode
                )

                _insightState.value = if (insight != null) {
                    InsightUiState.Success(insight)
                } else {
                    InsightUiState.InsufficientData
                }

            } catch (e: Exception) {
                Log.e("AiInsightViewModel", "Error loading insight", e)
                _insightState.value = InsightUiState.Unavailable
            }
        }
    }

    /** Force-refreshes by invalidating the 12-hour cache then re-generating. */
    fun refreshInsight() {
        aiServiceRepository.clearInsightCache()
        loadInsight()
    }
}

sealed interface InsightUiState {
    data object Loading : InsightUiState
    data class Success(val insight: AiInsight) : InsightUiState
    data object Unavailable : InsightUiState      // AI unreachable — hide card silently
    data object InsufficientData : InsightUiState // Not enough transactions yet
}
