package com.example.moneymanager.domain.usecase.transaction

import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.TransactionRepository
import javax.inject.Inject

/** Updates an existing transaction. Validates that the transaction ID is not blank. */
class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Transaction> {
        if (transaction.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Transaction ID cannot be empty"))
        }
        return transactionRepository.updateTransaction(transaction)
    }
}
