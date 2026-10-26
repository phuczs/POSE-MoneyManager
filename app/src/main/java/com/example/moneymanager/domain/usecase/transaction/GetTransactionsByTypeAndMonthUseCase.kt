package com.example.moneymanager.domain.usecase.transaction

import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import javax.inject.Inject

/** Returns transactions filtered by type and month. */
class GetTransactionsByTypeAndMonthUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(type: String, yearMonth: YearMonth): Flow<List<Transaction>> =
        transactionRepository.getTransactionsByTypeAndMonth(type, yearMonth)
}
