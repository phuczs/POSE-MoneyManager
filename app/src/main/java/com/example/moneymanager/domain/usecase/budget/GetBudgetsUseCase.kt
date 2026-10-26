package com.example.moneymanager.domain.usecase.budget

import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Returns active budgets for a given point in time. */
class GetBudgetsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(date: Long = System.currentTimeMillis()): Flow<List<Budget>> =
        budgetRepository.getBudgets(date)
}
