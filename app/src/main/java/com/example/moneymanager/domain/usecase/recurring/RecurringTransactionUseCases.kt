package com.example.moneymanager.domain.usecase.recurring

import com.example.moneymanager.domain.model.RecurringFrequency
import com.example.moneymanager.domain.model.RecurringTransaction
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.RecurringTransactionRepository
import com.example.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

class GetRecurringTransactionsUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository
) {
    operator fun invoke(userId: String): Flow<List<RecurringTransaction>> {
        return repository.getAllByUser(userId)
    }
}

class SaveRecurringTransactionUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository
) {
    suspend operator fun invoke(userId: String, item: RecurringTransaction): Result<Unit> {
        val itemId = if (item.id.isBlank()) UUID.randomUUID().toString() else item.id
        val nextDate = if (item.nextExecutionDate <= 0) calculateNextDate(item.startDate, item.frequency) else item.nextExecutionDate
        val prepared = item.copy(id = itemId, userId = userId, nextExecutionDate = nextDate)
        return repository.save(userId, prepared)
    }
}

class ToggleRecurringActiveUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository
) {
    suspend operator fun invoke(userId: String, id: String, isActive: Boolean): Result<Unit> {
        return repository.updateActiveStatus(userId, id, isActive)
    }
}

class DeleteRecurringTransactionUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository
) {
    suspend operator fun invoke(userId: String, id: String): Result<Unit> {
        return repository.delete(userId, id)
    }
}

class ProcessDueRecurringTransactionsUseCase @Inject constructor(
    private val recurringRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(): Result<Int> {
        return try {
            val now = System.currentTimeMillis()
            val dueItems = recurringRepository.getDueItems(now)
            var count = 0

            for (item in dueItems) {
                var execDate = item.nextExecutionDate
                var currentItem = item
                var iterations = 0
                val maxIterations = 50 // Guard against runaway loops

                while (execDate <= now && iterations < maxIterations) {
                    val newTx = Transaction(
                        id = UUID.randomUUID().toString(),
                        userId = currentItem.userId,
                        amount = currentItem.amount,
                        type = currentItem.type,
                        category = currentItem.category,
                        description = if (currentItem.description.isNotBlank()) currentItem.description else currentItem.title,
                        date = execDate
                    )
                    val txResult = transactionRepository.addTransaction(newTx)
                    if (txResult.isSuccess) {
                        count++
                        val nextDate = calculateNextDate(execDate, currentItem.frequency)
                        currentItem = currentItem.copy(
                            lastExecutedDate = now,
                            nextExecutionDate = nextDate
                        )
                        recurringRepository.save(currentItem.userId, currentItem)
                        execDate = nextDate
                        iterations++
                    } else {
                        break
                    }
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

fun calculateNextDate(fromDate: Long, frequency: RecurringFrequency): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = fromDate
    }
    when (frequency) {
        RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
        RecurringFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
        RecurringFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
        RecurringFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
    }
    return cal.timeInMillis
}
