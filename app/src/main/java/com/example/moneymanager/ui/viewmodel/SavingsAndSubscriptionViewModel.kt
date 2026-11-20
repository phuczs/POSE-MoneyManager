package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.SavingsChallengePreferences
import com.example.moneymanager.domain.model.ChallengeStatus
import com.example.moneymanager.domain.model.DetectedSubscription
import com.example.moneymanager.domain.model.SavingsChallenge
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.RecurringTransactionRepository
import com.example.moneymanager.domain.repository.TransactionRepository
import com.example.moneymanager.domain.usecase.ai.ConvertSubscriptionToRecurringUseCase
import com.example.moneymanager.domain.usecase.ai.DetectSubscriptionsUseCase
import com.example.moneymanager.domain.usecase.ai.GenerateChallengesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavingsAndSubscriptionViewModel @Inject constructor(
    private val detectSubscriptionsUseCase: DetectSubscriptionsUseCase,
    private val generateChallengesUseCase: GenerateChallengesUseCase,
    private val convertSubscriptionUseCase: ConvertSubscriptionToRecurringUseCase,
    private val challengePreferences: SavingsChallengePreferences,
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringTransactionRepository
) : ViewModel() {

    private val _activeChallenge = MutableStateFlow<SavingsChallenge?>(null)
    val activeChallenge: StateFlow<SavingsChallenge?> = _activeChallenge.asStateFlow()

    private val _suggestedChallenges = MutableStateFlow<List<SavingsChallenge>>(emptyList())
    val suggestedChallenges: StateFlow<List<SavingsChallenge>> = _suggestedChallenges.asStateFlow()

    private val _detectedSubscriptions = MutableStateFlow<List<DetectedSubscription>>(emptyList())
    val detectedSubscriptions: StateFlow<List<DetectedSubscription>> = _detectedSubscriptions.asStateFlow()

    private val _totalMonthlySubscription = MutableStateFlow(0.0)
    val totalMonthlySubscription: StateFlow<Double> = _totalMonthlySubscription.asStateFlow()

    private val _totalAnnualSubscription = MutableStateFlow(0.0)
    val totalAnnualSubscription: StateFlow<Double> = _totalAnnualSubscription.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun loadData(userId: String) {
        viewModelScope.launch {
            val transactions = transactionRepository.getAllTransactions().firstOrNull() ?: emptyList()
            val recurring = recurringRepository.getAllByUser(userId).firstOrNull() ?: emptyList()

            // 1. Detect Subscriptions
            val subs = detectSubscriptionsUseCase(transactions, recurring)
            _detectedSubscriptions.value = subs
            _totalMonthlySubscription.value = subs.filter { !it.isAlreadyTracked }.sumOf { it.amount }
            _totalAnnualSubscription.value = subs.filter { !it.isAlreadyTracked }.sumOf { it.annualCost }

            // 2. Load & Evaluate Challenges
            val savedChallenges = challengePreferences.challenges.value
            val currentActive = savedChallenges.firstOrNull { it.status == ChallengeStatus.ACTIVE }

            if (currentActive != null) {
                // Real-time evaluation against fresh transactions
                val updated = generateChallengesUseCase.evaluateProgress(currentActive, transactions)
                challengePreferences.saveChallenge(updated)
                _activeChallenge.value = updated
            } else {
                _activeChallenge.value = null
            }

            // Generate contextual suggestions if no active or to show options
            val suggestions = generateChallengesUseCase.generateSuggestions(transactions)
            _suggestedChallenges.value = suggestions
        }
    }

    fun acceptChallenge(challenge: SavingsChallenge) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val active = challenge.copy(
                startDate = now,
                endDate = now + (challenge.targetDays.toLong() * 24 * 60 * 60 * 1000),
                status = ChallengeStatus.ACTIVE,
                currentStreak = 0,
                savedAmount = 0.0,
                daysCompleted = 0
            )
            challengePreferences.saveChallenge(active)
            _activeChallenge.value = active
            _actionMessage.value = "Đã nhận thử thách: ${challenge.title}! Chúc bạn thành công!"
        }
    }

    fun abandonChallenge(challengeId: String) {
        viewModelScope.launch {
            challengePreferences.removeChallenge(challengeId)
            _activeChallenge.value = null
            _actionMessage.value = "Đã dừng thử thách."
        }
    }

    fun convertSubscriptionToRecurring(
        subscription: DetectedSubscription,
        userId: String
    ) {
        viewModelScope.launch {
            val result = convertSubscriptionUseCase(userId, subscription)
            if (result.isSuccess) {
                _detectedSubscriptions.value = _detectedSubscriptions.value.map {
                    if (it.id == subscription.id) it.copy(isAlreadyTracked = true) else it
                }
                _actionMessage.value = "Đã chuyển ${subscription.name} sang theo dõi định kỳ!"
            } else {
                _actionMessage.value = "Không thể lưu gói định kỳ: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
