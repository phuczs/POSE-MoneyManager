package com.example.moneymanager.data.repository

import com.example.moneymanager.data.local.RecurringTransactionDao
import com.example.moneymanager.data.local.RecurringTransactionEntity
import com.example.moneymanager.domain.model.RecurringFrequency
import com.example.moneymanager.domain.model.RecurringTransaction
import com.example.moneymanager.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomRecurringTransactionRepositoryImpl @Inject constructor(
    private val dao: RecurringTransactionDao
) : RecurringTransactionRepository {

    override fun getAllByUser(userId: String): Flow<List<RecurringTransaction>> {
        return dao.getAllByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getDueItems(currentTime: Long): List<RecurringTransaction> {
        return dao.getDueItems(currentTime).map { it.toDomain() }
    }

    override suspend fun save(userId: String, item: RecurringTransaction): Result<Unit> {
        return try {
            dao.insert(item.toEntity(userId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateActiveStatus(userId: String, id: String, isActive: Boolean): Result<Unit> {
        return try {
            dao.updateActiveStatus(userId, id, isActive)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(userId: String, id: String): Result<Unit> {
        return try {
            dao.deleteById(userId, id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun RecurringTransactionEntity.toDomain(): RecurringTransaction {
        val freq = try {
            RecurringFrequency.valueOf(frequency)
        } catch (e: Exception) {
            RecurringFrequency.MONTHLY
        }
        return RecurringTransaction(
            id = id,
            userId = userId,
            title = title,
            amount = amount,
            type = type,
            category = category,
            frequency = freq,
            startDate = startDate,
            lastExecutedDate = lastExecutedDate,
            nextExecutionDate = nextExecutionDate,
            isActive = isActive,
            description = description
        )
    }

    private fun RecurringTransaction.toEntity(userId: String): RecurringTransactionEntity {
        return RecurringTransactionEntity(
            id = id,
            userId = userId,
            title = title,
            amount = amount,
            type = type,
            category = category,
            frequency = frequency.name,
            startDate = startDate,
            lastExecutedDate = lastExecutedDate,
            nextExecutionDate = nextExecutionDate,
            isActive = isActive,
            description = description
        )
    }
}
