package com.example.moneymanager.domain.usecase.ai

import com.example.moneymanager.domain.model.DetectedSubscription
import com.example.moneymanager.domain.model.RecurringTransaction
import com.example.moneymanager.domain.repository.RecurringTransactionRepository
import java.util.UUID
import javax.inject.Inject

/**
 * 1-Tap Convert Detected Subscription into a managed [RecurringTransaction].
 *
 * Automates the transition from passive tracking to active recurring budget enforcement.
 */
class ConvertSubscriptionToRecurringUseCase @Inject constructor(
    private val recurringRepository: RecurringTransactionRepository
) {
    suspend operator fun invoke(
        userId: String,
        subscription: DetectedSubscription
    ): Result<Unit> {
        val item = RecurringTransaction(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = subscription.name,
            amount = subscription.amount,
            type = "expense",
            category = subscription.category.ifBlank { "Bills & Utilities" },
            frequency = subscription.frequency,
            startDate = subscription.lastPaymentDate,
            nextExecutionDate = subscription.nextExpectedDate,
            isActive = true,
            description = "Tự động phát hiện bởi AI Subscription Hunter"
        )
        return recurringRepository.save(userId, item)
    }
}
