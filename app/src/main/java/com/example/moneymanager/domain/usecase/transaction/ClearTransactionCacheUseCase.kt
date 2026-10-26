package com.example.moneymanager.domain.usecase.transaction

import com.example.moneymanager.domain.repository.TransactionRepository
import javax.inject.Inject

/** Clears the local Room cache for a specific user — called on logout. */
class ClearTransactionCacheUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(userId: String) =
        transactionRepository.clearLocalCacheForUser(userId)
}
