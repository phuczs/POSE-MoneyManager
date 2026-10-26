package com.example.moneymanager.domain.usecase.transaction

import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Returns transactions for a specific month and year. */
class GetTransactionsByMonthUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(month: Int, year: Int): Flow<List<Transaction>> =
        transactionRepository.getTransactionsByMonth(month, year)
}
