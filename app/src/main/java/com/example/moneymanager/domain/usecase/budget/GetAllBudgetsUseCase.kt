package com.example.moneymanager.domain.usecase.budget

import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Retrieves all budgets for the authenticated user. */
class GetAllBudgetsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(): Flow<List<Budget>> = budgetRepository.getAllBudgets()
}
