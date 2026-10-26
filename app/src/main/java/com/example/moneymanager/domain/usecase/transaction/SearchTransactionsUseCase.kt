package com.example.moneymanager.domain.usecase.transaction

import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case executing multi-criteria transaction search and filtering:
 * keyword in description/category, type, category, amount range, and date range.
 */
class SearchTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(
        query: String = "",
        type: String = "",
        category: String = "",
        minAmount: Double? = null,
        maxAmount: Double? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<Transaction>> {
        return transactionRepository.searchTransactions(
            query = query.trim(),
            type = type,
            category = category,
            minAmount = minAmount,
            maxAmount = maxAmount,
            startDate = startDate,
            endDate = endDate
        )
    }
}
