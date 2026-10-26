package com.example.moneymanager.domain.usecase.transaction

import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Retrieves a single transaction by its ID.
 * Tries Room cache first (fast path), falls back to Firebase if not found.
 */
class GetTransactionByIdUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transactionId: String): Result<Transaction> =
        repository.getTransactionById(transactionId)
}
