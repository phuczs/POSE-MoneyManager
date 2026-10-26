package com.example.moneymanager.domain.usecase.transaction

import com.example.moneymanager.domain.repository.TransactionRepository
import javax.inject.Inject

/** Deletes a transaction by its ID. Validates that the ID is not blank. */
class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transactionId: String): Result<Unit> {
        if (transactionId.isBlank()) {
            return Result.failure(IllegalArgumentException("Transaction ID cannot be empty"))
        }
        return transactionRepository.deleteTransaction(transactionId)
    }
}
