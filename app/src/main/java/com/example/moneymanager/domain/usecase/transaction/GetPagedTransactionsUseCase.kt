package com.example.moneymanager.domain.usecase.transaction

import androidx.paging.PagingData
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Returns a Paging3 stream of transactions with optional type/month/year filters. */
class GetPagedTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(
        type: String? = null,
        month: Int? = null,
        year: Int? = null
    ): Flow<PagingData<Transaction>> =
        transactionRepository.getPagedTransactions(type, month, year)
}
