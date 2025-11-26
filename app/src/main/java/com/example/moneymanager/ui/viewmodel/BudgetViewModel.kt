package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.model.Budget
import com.example.moneymanager.data.model.BudgetStatus
import com.example.moneymanager.data.repository.BudgetRepository
import com.example.moneymanager.data.repository.SettingsRepository
import com.example.moneymanager.data.repository.TransactionRepository
import com.example.moneymanager.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationHelper: NotificationHelper, // Injected
    private val settingsRepository: SettingsRepository // Injected
) : ViewModel() {

    private val _uiState = MutableStateFlow<BudgetsUiState>(BudgetsUiState.Loading)
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    // Track which budgets we've already alerted in this session to avoid spam

    init {
        loadBudgets()
    }

    fun loadBudgets() {
        viewModelScope.launch {
            val today = Date()
            val budgetsFlow = budgetRepository.getBudgets(today)
            val transactionsFlow = transactionRepository.getAllTransactions()

            combine(budgetsFlow, transactionsFlow) { budgets, transactions ->
                val updatedBudgets = budgets.map { budget ->
                    val spent = transactions
                        .filter { it.category == budget.category && it.date.toDate() in budget.startDate..budget.endDate && it.type == "expense" }
                        .sumOf { it.amount }
                    budget.copy(spentAmount = spent)
                }

                val overallBudget = updatedBudgets.sumOf { it.allocatedAmount }
                val overallSpent = updatedBudgets.sumOf { it.spentAmount }

                // Trigger alerts if needed
                checkBudgetAlerts(updatedBudgets)

                BudgetsUiState.Success(
                    budgets = updatedBudgets.sortedByDescending { it.progress },
                    topThreeBudgets = updatedBudgets.sortedByDescending { it.progress }.take(3),
                    overallBudget = overallBudget,
                    overallSpent = overallSpent
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private suspend fun checkBudgetAlerts(budgets: List<Budget>) {
        // Check if user has disabled notifications globally
        if (!settingsRepository.isNotificationsEnabled.first()) return

        budgets.forEach { budget ->
            // Alert if status is Warning or Over
            if (budget.status == BudgetStatus.Warning || budget.status == BudgetStatus.Over) {

                // --- FIX: Check the singleton repository instead of local state ---
                if (!settingsRepository.hasAlerted(budget.id)) {
                    val percent = (budget.progress * 100).toInt()
                    notificationHelper.showBudgetAlert(budget.id, budget.category, percent)

                    // Mark as alerted in the singleton repository
                    settingsRepository.setAlerted(budget.id)
                }
            }
        }
    }

    fun saveBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.saveBudget(budget)
        }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.updateBudget(budget)
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budgetId)
        }
    }

    fun getProjectedDate(budget: Budget): Date? {
        if (budget.spentAmount == 0.0) return null

        val now = System.currentTimeMillis()
        val daysSinceStart = TimeUnit.MILLISECONDS.toDays(now - budget.startDate.time).coerceAtLeast(1)
        val dailySpending = budget.spentAmount / daysSinceStart

        if (dailySpending == 0.0) return null

        val remainingAmount = budget.allocatedAmount - budget.spentAmount
        if (remainingAmount <= 0) return null

        val daysLeft = (remainingAmount / dailySpending).toLong()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.add(Calendar.DAY_OF_YEAR, daysLeft.toInt())
        return calendar.time
    }
}

sealed interface BudgetsUiState {
    object Loading : BudgetsUiState
    data class Success(
        val budgets: List<Budget>,
        val topThreeBudgets: List<Budget>,
        val overallBudget: Double,
        val overallSpent: Double,
    ) : BudgetsUiState
    data class Error(val message: String) : BudgetsUiState
}