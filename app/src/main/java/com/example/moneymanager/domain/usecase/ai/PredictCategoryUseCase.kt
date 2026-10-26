package com.example.moneymanager.domain.usecase.ai

import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.AiServiceRepository
import javax.inject.Inject

/**
 * Predicts the most likely spending category for a transaction description.
 *
 * Security note:
 * - Input is passed to [AiServiceRepositoryImpl] which sanitizes it before
 *   embedding in AI prompts to prevent prompt injection attacks.
 */
class PredictCategoryUseCase @Inject constructor(
    private val aiServiceRepository: AiServiceRepository
) {
    suspend operator fun invoke(
        rawDescription: String,
        amount: Double,
        recentTransactions: List<Transaction>,
        availableCategories: List<String>
    ): String {
        if (rawDescription.length < 3) return ""
        if (availableCategories.isEmpty()) return "General"
        return aiServiceRepository.getCategoryPrediction(
            rawDescription = rawDescription,
            amount = amount,
            recentTransactions = recentTransactions,
            availableCategories = availableCategories
        )
    }
}
