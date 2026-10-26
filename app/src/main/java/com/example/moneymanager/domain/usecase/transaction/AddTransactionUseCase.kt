package com.example.moneymanager.domain.usecase.transaction

import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.TransactionRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Adds a new transaction, enforcing business rules:
 * - Sets [Transaction.month] and [Transaction.year] from [Transaction.date] if not provided.
 * - The [Transaction.userId] is always overwritten by the repository implementation
 *   using the authenticated user's UID — never trusted from the caller.
 *
 * Security note:
 * - Amount must be positive; type must be "income" or "expense".
 *   These constraints are validated here to keep them in the domain layer.
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Transaction> {
        // Business rule: amount must be positive
        if (transaction.amount <= 0) {
            return Result.failure(IllegalArgumentException("Transaction amount must be positive"))
        }
        // Business rule: type must be valid
        if (transaction.type !in setOf("income", "expense")) {
            return Result.failure(IllegalArgumentException("Transaction type must be 'income' or 'expense'"))
        }

        // Ensure month/year are set from the date if not provided
        val today = LocalDate.now()
        val enriched = transaction.copy(
            month = if (transaction.month == 0) today.monthValue else transaction.month,
            year = if (transaction.year == 0) today.year else transaction.year
        )
        return transactionRepository.addTransaction(enriched)
    }
}
