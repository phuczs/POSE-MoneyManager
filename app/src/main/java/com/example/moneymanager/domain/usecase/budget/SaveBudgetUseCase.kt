package com.example.moneymanager.domain.usecase.budget

import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.repository.BudgetRepository
import javax.inject.Inject

/** Saves a new budget after validating it has a positive allocated amount. */
class SaveBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget): Result<Unit> {
        if (budget.allocatedAmount <= 0) {
            return Result.failure(IllegalArgumentException("Budget allocated amount must be positive"))
        }
        if (budget.startDate > budget.endDate) {
            return Result.failure(IllegalArgumentException("Budget start date must be before end date"))
        }
        return budgetRepository.saveBudget(budget)
    }
}
