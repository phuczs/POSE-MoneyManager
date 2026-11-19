package com.example.moneymanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.CoachPreferences
import com.example.moneymanager.domain.model.CoachNudge
import com.example.moneymanager.domain.model.WeeklyDigest
import com.example.moneymanager.domain.repository.SettingsRepository
import com.example.moneymanager.domain.usecase.ai.AnalyzeFinancialDNAUseCase
import com.example.moneymanager.domain.usecase.ai.EvaluateSpendingNudgeUseCase
import com.example.moneymanager.domain.usecase.ai.GenerateWeeklyDigestUseCase
import com.example.moneymanager.domain.usecase.budget.GetAllBudgetsUseCase
import com.example.moneymanager.domain.usecase.transaction.GetAllTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

sealed interface WeeklyDigestUiState {
    data object Loading : WeeklyDigestUiState
    data class Success(val digest: WeeklyDigest) : WeeklyDigestUiState
    data object Empty : WeeklyDigestUiState
}

@HiltViewModel
class ProactiveCoachViewModel @Inject constructor(
    private val evaluateSpendingNudgeUseCase: EvaluateSpendingNudgeUseCase,
    private val generateWeeklyDigestUseCase: GenerateWeeklyDigestUseCase,
    private val analyzeFinancialDNAUseCase: AnalyzeFinancialDNAUseCase,
    private val getAllTransactionsUseCase: GetAllTransactionsUseCase,
    private val getAllBudgetsUseCase: GetAllBudgetsUseCase,
    private val coachPreferences: CoachPreferences,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /**
     * Reactive top-priority proactive coach nudge evaluated from live wallet state.
     */
    val activeNudge: StateFlow<CoachNudge?> = combine(
        getAllTransactionsUseCase().catch { emit(emptyList()) },
        getAllBudgetsUseCase().catch { emit(emptyList()) },
        coachPreferences.dismissedNudgeIds,
        settingsRepository.currentLanguage
    ) { transactions, budgets, dismissedIds, languageCode ->
        val archetype = analyzeFinancialDNAUseCase.computeLocalDna(
            transactions = transactions,
            budgets = budgets,
            languageCode = languageCode
        )?.archetype

        evaluateSpendingNudgeUseCase(
            allTransactions = transactions,
            activeBudgets = budgets,
            dismissedNudgeIds = dismissedIds,
            dnaArchetype = archetype,
            languageCode = languageCode
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _digestState = MutableStateFlow<WeeklyDigestUiState>(WeeklyDigestUiState.Loading)
    val digestState: StateFlow<WeeklyDigestUiState> = _digestState.asStateFlow()

    private val _isRefreshingDigest = MutableStateFlow(false)
    val isRefreshingDigest: StateFlow<Boolean> = _isRefreshingDigest.asStateFlow()

    init {
        loadWeeklyDigest(forceRefresh = false)
    }

    fun dismissNudge(nudgeId: String) {
        coachPreferences.dismissNudge(nudgeId)
    }

    fun loadWeeklyDigest(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val hadDigest = _digestState.value is WeeklyDigestUiState.Success
            if (forceRefresh && hadDigest) {
                _isRefreshingDigest.value = true
            } else if (!hadDigest) {
                _digestState.value = WeeklyDigestUiState.Loading
            }

            try {
                val languageCode = settingsRepository.currentLanguage.value
                val transactions = withTimeoutOrNull(5_000L) {
                    getAllTransactionsUseCase().first()
                } ?: emptyList()

                val budgets = withTimeoutOrNull(5_000L) {
                    getAllBudgetsUseCase().first()
                } ?: emptyList()

                // Emit instant local digest first so UI never stalls waiting for AI
                val localDigest = generateWeeklyDigestUseCase.computeLocalDigest(
                    allTransactions = transactions,
                    activeBudgets = budgets,
                    languageCode = languageCode
                )
                if (localDigest == null) {
                    _digestState.value = WeeklyDigestUiState.Empty
                    return@launch
                }
                if (!hadDigest) {
                    _digestState.value = WeeklyDigestUiState.Success(localDigest)
                }

                // Upgrade with AI commentary in background
                val enriched = generateWeeklyDigestUseCase(
                    allTransactions = transactions,
                    activeBudgets = budgets,
                    languageCode = languageCode,
                    forceRefresh = forceRefresh
                )
                if (enriched != null) {
                    _digestState.value = WeeklyDigestUiState.Success(enriched)
                }
            } catch (e: Exception) {
                Log.e("ProactiveCoachVM", "Failed to load weekly digest", e)
                if (!hadDigest) {
                    _digestState.value = WeeklyDigestUiState.Empty
                }
            } finally {
                _isRefreshingDigest.value = false
            }
        }
    }
}
