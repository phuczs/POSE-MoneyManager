package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.BudgetHistoryEntry
import com.example.moneymanager.domain.model.BudgetStatus
import com.example.moneymanager.domain.model.BudgetWithProgress
import com.example.moneymanager.domain.model.DailyVelocityReport
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.SettingsRepository
import com.example.moneymanager.domain.usecase.budget.AutoRenewBudgetsUseCase
import com.example.moneymanager.domain.usecase.budget.DeleteBudgetUseCase
import com.example.moneymanager.domain.usecase.budget.GetAllBudgetsUseCase
import com.example.moneymanager.domain.usecase.budget.GetBudgetHistoryUseCase
import com.example.moneymanager.domain.usecase.budget.GetBudgetsUseCase
import com.example.moneymanager.domain.usecase.budget.SaveBudgetUseCase
import com.example.moneymanager.domain.usecase.budget.UpdateBudgetUseCase
import com.example.moneymanager.domain.usecase.transaction.GetAllTransactionsUseCase
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
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val getAllBudgetsUseCase: GetAllBudgetsUseCase,
    private val getBudgetHistoryUseCase: GetBudgetHistoryUseCase,
    private val autoRenewBudgetsUseCase: AutoRenewBudgetsUseCase,
    private val saveBudgetUseCase: SaveBudgetUseCase,
    private val updateBudgetUseCase: UpdateBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val getAllTransactionsUseCase: GetAllTransactionsUseCase,
    private val notificationHelper: NotificationHelper,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BudgetsUiState>(BudgetsUiState.Loading)
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    private val _dailyVelocityReport = MutableStateFlow<DailyVelocityReport?>(null)
    val dailyVelocityReport: StateFlow<DailyVelocityReport?> = _dailyVelocityReport.asStateFlow()

    private val _budgetHistory = MutableStateFlow<List<BudgetHistoryEntry>>(emptyList())
    val budgetHistory: StateFlow<List<BudgetHistoryEntry>> = _budgetHistory.asStateFlow()

    init {
        loadBudgets()
    }

    fun loadBudgets() {
        viewModelScope.launch {
            combine(
                getAllBudgetsUseCase(),
                getAllTransactionsUseCase()
            ) { allBudgets, transactions ->
                val todayMs = System.currentTimeMillis()

                // Check expired recurring budgets and auto-renew
                val expiredRecurring = allBudgets.filter { it.isRecurring && it.endDate < todayMs }
                if (expiredRecurring.isNotEmpty()) {
                    val expiredWithProgress = expiredRecurring.map { b ->
                        val spent = transactions
                            .filter {
                                it.category == b.category &&
                                        it.date >= b.startDate &&
                                        it.date <= b.endDate &&
                                        it.type == "expense"
                            }
                            .sumOf { it.amount }
                        BudgetWithProgress(b, spent)
                    }
                    autoRenewBudgetsUseCase(expiredWithProgress, allBudgets)
                }

                // Active budgets for current point in time
                val activeBudgets = allBudgets.filter { it.startDate <= todayMs && it.endDate >= todayMs }
                val updatedBudgets = activeBudgets.map { budget ->
                    val spent = transactions
                        .filter {
                            it.category == budget.category &&
                                    it.date >= budget.startDate &&
                                    it.date <= budget.endDate &&
                                    it.type == "expense"
                        }
                        .sumOf { it.amount }
                    BudgetWithProgress(budget, spent)
                }

                val overallBudget = updatedBudgets.sumOf { it.budget.effectiveAmount }
                val overallSpent = updatedBudgets.sumOf { it.spentAmount }

                checkBudgetAlerts(updatedBudgets)

                _dailyVelocityReport.value = calculateDailyVelocity(transactions)

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

    /**
     * Loads historical budget periods for a given category for period comparison (Phase 4.2).
     */
    fun loadBudgetHistory(category: String, limit: Int = 6) {
        viewModelScope.launch {
            combine(
                getBudgetHistoryUseCase(category, limit),
                getAllTransactionsUseCase()
            ) { historyBudgets, transactions ->
                historyBudgets.map { budget ->
                    val spent = transactions
                        .filter {
                            it.category == budget.category &&
                                    it.date >= budget.startDate &&
                                    it.date <= budget.endDate &&
                                    it.type == "expense"
                        }
                        .sumOf { it.amount }
                    BudgetHistoryEntry(
                        budgetId = budget.id,
                        category = budget.category,
                        allocated = budget.allocatedAmount,
                        spent = spent,
                        periodStart = budget.startDate,
                        periodEnd = budget.endDate,
                        period = budget.period,
                        rolloverIn = budget.rolloverAmount
                    )
                }
            }.collect { entries ->
                _budgetHistory.value = entries
            }
        }
    }

    /**
     * Analyzes daily expense trends against monthly daily average.
     * Computes spending velocity and projects end-of-month expenditure.
     */
    private fun calculateDailyVelocity(transactions: List<Transaction>): DailyVelocityReport? {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentDay = now.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Filter expense transactions for this month
        val thisMonthExpenses = transactions.filter { tx ->
            if (tx.type != "expense") return@filter false
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
            val txYear = if (tx.year == 0) txCal.get(Calendar.YEAR) else tx.year
            val txMonth = if (tx.month == 0) txCal.get(Calendar.MONTH) + 1 else tx.month
            txYear == currentYear && txMonth == currentMonth
        }

        if (thisMonthExpenses.isEmpty()) return null

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todaySpent = thisMonthExpenses
            .filter { it.date >= startOfToday }
            .sumOf { it.amount }

        val totalMonthSpent = thisMonthExpenses.sumOf { it.amount }
        val priorSpent = totalMonthSpent - todaySpent

        // Daily average based on elapsed days prior to today
        val daysPrior = (currentDay - 1).coerceAtLeast(1)
        val dailyAverage = if (currentDay > 1) {
            priorSpent / daysPrior
        } else {
            todaySpent
        }

        val percentOverAverage = if (dailyAverage > 0.0) {
            (todaySpent / dailyAverage) * 100.0
        } else 100.0

        val projectedMonthTotal = if (currentDay > 0) {
            (totalMonthSpent / currentDay) * daysInMonth
        } else totalMonthSpent

        // Flag high velocity if today's spending is >= 1.5x daily average AND today's spend is significant (>= 50,000 VND / equivalent)
        val isHighVelocity = currentDay > 1 && todaySpent >= dailyAverage * 1.5 && todaySpent >= 50000.0

        return DailyVelocityReport(
            todaySpent = todaySpent,
            dailyAverage = dailyAverage,
            percentOverAverage = percentOverAverage,
            projectedMonthTotal = projectedMonthTotal,
            isHighVelocity = isHighVelocity
        )
    }

    private suspend fun checkBudgetAlerts(budgets: List<BudgetWithProgress>) {
        if (!settingsRepository.isNotificationsEnabled.first()) return
        budgets.forEach { budgetWP ->
            if (budgetWP.status == BudgetStatus.Warning || budgetWP.status == BudgetStatus.Over) {
                if (!settingsRepository.hasAlerted(budgetWP.budget.id)) {
                    val percent = (budgetWP.progress * 100).toInt()
                    notificationHelper.showBudgetAlert(budgetWP.budget.id, budgetWP.budget.category, percent)
                    settingsRepository.setAlerted(budgetWP.budget.id)
                }
            }
        }
    }

    fun saveBudget(budget: Budget) {
        viewModelScope.launch { saveBudgetUseCase(budget) }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch { updateBudgetUseCase(budget) }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch { deleteBudgetUseCase(budgetId) }
    }

    /** Projects the date when a budget will be fully spent based on daily spending rate. */
    fun getProjectedDate(budgetWP: BudgetWithProgress): Date? {
        if (budgetWP.spentAmount == 0.0) return null
        val now = System.currentTimeMillis()
        val daysSinceStart = TimeUnit.MILLISECONDS.toDays(now - budgetWP.budget.startDate).coerceAtLeast(1)
        val dailySpending = budgetWP.spentAmount / daysSinceStart
        if (dailySpending == 0.0) return null
        val remainingAmount = budgetWP.budget.allocatedAmount - budgetWP.spentAmount
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
        val budgets: List<BudgetWithProgress>,
        val topThreeBudgets: List<BudgetWithProgress>,
        val overallBudget: Double,
        val overallSpent: Double,
    ) : BudgetsUiState
    data class Error(val message: String) : BudgetsUiState
}