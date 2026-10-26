package com.example.moneymanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.PredictedBudget
import com.example.moneymanager.domain.repository.SettingsRepository
import com.example.moneymanager.domain.usecase.ai.GetPredictiveBudgetsUseCase
import com.example.moneymanager.domain.usecase.budget.SaveBudgetUseCase
import com.example.moneymanager.domain.usecase.category.GetAllCategoriesUseCase
import com.example.moneymanager.domain.usecase.transaction.GetAllTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PredictiveBudgetViewModel @Inject constructor(
    private val getPredictiveBudgetsUseCase: GetPredictiveBudgetsUseCase,
    private val getAllTransactionsUseCase: GetAllTransactionsUseCase,
    private val saveBudgetUseCase: SaveBudgetUseCase,
    private val getAllCategoriesUseCase: GetAllCategoriesUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PredictiveBudgetUiState>(PredictiveBudgetUiState.Idle)
    val uiState: StateFlow<PredictiveBudgetUiState> = _uiState.asStateFlow()

    private val _confirmedBudgetMessage = MutableStateFlow<String?>(null)
    val confirmedBudgetMessage: StateFlow<String?> = _confirmedBudgetMessage.asStateFlow()

    /**
     * Loads AI-predicted budgets.
     * Filtering and data-sufficiency checks are handled by [GetPredictiveBudgetsUseCase].
     */
    fun loadPredictions() {
        viewModelScope.launch {
            _uiState.value = PredictiveBudgetUiState.Loading
            try {
                val languageCode = settingsRepository.currentLanguage.value

                val allTransactions = withTimeoutOrNull(6_000) {
                    getAllTransactionsUseCase().first()
                } ?: run {
                    _uiState.value = PredictiveBudgetUiState.AiUnavailable
                    return@launch
                }

                val categories = withTimeoutOrNull(3_000) {
                    getAllCategoriesUseCase().first()
                } ?: emptyList()

                val predictions = getPredictiveBudgetsUseCase(
                    allTransactions = allTransactions,
                    availableCategories = categories.map { it.name },
                    languageCode = languageCode
                )

                _uiState.value = if (predictions.isEmpty()) {
                    PredictiveBudgetUiState.InsufficientData
                } else {
                    PredictiveBudgetUiState.Success(predictions)
                }

            } catch (e: Exception) {
                Log.e("PredictiveBudgetVM", "Error loading predictions", e)
                _uiState.value = PredictiveBudgetUiState.AiUnavailable
            }
        }
    }

    /** User confirmed an AI suggestion — save it as an actual Budget. */
    fun confirmBudget(prediction: PredictedBudget, languageCode: String = "vi") {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val startDate = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val endDate = calendar.timeInMillis

                val budget = Budget(
                    category = prediction.category,
                    allocatedAmount = prediction.suggestedAmount,
                    startDate = startDate,
                    endDate = endDate
                )

                saveBudgetUseCase(budget)

                _confirmedBudgetMessage.value = if (languageCode == "vi") {
                    "Đã thêm ngân sách cho \"${prediction.category}\" ✓"
                } else {
                    "Budget added for \"${prediction.category}\" ✓"
                }
            } catch (e: Exception) {
                Log.e("PredictiveBudgetVM", "Failed to save budget", e)
                _confirmedBudgetMessage.value = if (languageCode == "vi") {
                    "Lỗi khi lưu ngân sách"
                } else {
                    "Failed to save budget"
                }
            }
        }
    }

    fun clearConfirmationMessage() { _confirmedBudgetMessage.value = null }
}

sealed interface PredictiveBudgetUiState {
    data object Idle : PredictiveBudgetUiState
    data object Loading : PredictiveBudgetUiState
    data class Success(val predictions: List<PredictedBudget>) : PredictiveBudgetUiState
    data object InsufficientData : PredictiveBudgetUiState
    data object AiUnavailable : PredictiveBudgetUiState
}
