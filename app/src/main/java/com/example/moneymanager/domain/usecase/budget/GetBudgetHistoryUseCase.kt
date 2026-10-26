package com.example.moneymanager.domain.usecase.budget

import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Retrieves past and current budgets for a specific category to support period comparisons. */
class GetBudgetHistoryUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(category: String, limit: Int = 6): Flow<List<Budget>> =
        budgetRepository.getBudgetHistory(category, limit)
}
