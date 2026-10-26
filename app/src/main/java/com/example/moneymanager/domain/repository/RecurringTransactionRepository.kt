package com.example.moneymanager.domain.repository

import com.example.moneymanager.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {
    fun getAllByUser(userId: String): Flow<List<RecurringTransaction>>
    suspend fun getDueItems(currentTime: Long): List<RecurringTransaction>
    suspend fun save(userId: String, item: RecurringTransaction): Result<Unit>
    suspend fun updateActiveStatus(userId: String, id: String, isActive: Boolean): Result<Unit>
    suspend fun delete(userId: String, id: String): Result<Unit>
}
