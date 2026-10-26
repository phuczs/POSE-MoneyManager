package com.example.moneymanager.domain.usecase.budget

import com.example.moneymanager.domain.repository.BudgetRepository
import javax.inject.Inject

/** Deletes a budget by its ID. */
class DeleteBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(budgetId: String): Result<Unit> {
        if (budgetId.isBlank()) {
            return Result.failure(IllegalArgumentException("Budget ID cannot be empty"))
        }
        return budgetRepository.deleteBudget(budgetId)
    }
}
