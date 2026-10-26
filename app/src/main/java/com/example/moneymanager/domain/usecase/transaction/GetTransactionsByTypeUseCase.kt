package com.example.moneymanager.domain.usecase.transaction

import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns a reactive stream of transactions filtered by type ("income" or "expense")
 * directly from the data layer/database.
 */
class GetTransactionsByTypeUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(type: String): Flow<List<Transaction>> =
        transactionRepository.getTransactionsByType(type)
}
