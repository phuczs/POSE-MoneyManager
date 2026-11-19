package com.example.moneymanager.domain.repository

import com.example.moneymanager.domain.model.AiInsight
import com.example.moneymanager.domain.model.FinancialDNA
import com.example.moneymanager.domain.model.FinancialDnaAiNarrative
import com.example.moneymanager.domain.model.PredictedBudget
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.model.WeeklyDigest

/**
 * Domain contract for AI-powered financial analysis services.
 *
 * Clean Architecture note:
 * - Extracted from the concrete [AiServiceRepositoryImpl] which previously
 *   mixed service orchestration and caching in a single class.
 * - The interface exposes only domain-oriented capabilities; all prompt
 *   engineering, JSON parsing, and caching details stay in the implementation.
 *
 * Security note:
 * - [rawInput] fields in implementations must be sanitized before being
 *   embedded into AI prompts to prevent prompt injection attacks.
 * - Financial data sent to external AI APIs (Gemini) is limited to
 *   aggregated summaries, never raw transaction lists with PII.
 */
interface AiServiceRepository {

    /**
     * Routes a prompt to the best available AI provider (Gemini → Ollama fallback).
     * @param jsonMode If true, the AI is instructed to return strict JSON.
     */
    suspend fun queryAi(prompt: String, jsonMode: Boolean = false): Result<String>

    /**
     * Predicts the most likely spending category for a transaction description.
     * Uses the user's recent transaction history for personalized prediction.
     *
     * @return A category name from [availableCategories], or "General" as fallback.
     */
    suspend fun getCategoryPrediction(
        rawDescription: String,
        amount: Double,
        recentTransactions: List<Transaction>,
        availableCategories: List<String>
    ): String

    /**
     * Generates a monthly financial insight comparing current vs. previous month.
     * Results are cached with a 12-hour TTL to avoid redundant AI calls.
     *
     * @return null if there is insufficient data or the AI call fails.
     */
    suspend fun getMonthlyInsight(
        currentMonthTransactions: List<Transaction>,
        previousMonthTransactions: List<Transaction>,
        languageCode: String = "vi"
    ): AiInsight?

    /**
     * Generates AI-suggested budgets for the top spending categories,
     * based on 3 months of expense history.
     *
     * @return Empty list if data is insufficient or the AI call fails.
     */
    suspend fun getPredictiveBudgets(
        threeMonthTransactions: List<Transaction>,
        availableCategories: List<String>,
        languageCode: String = "vi"
    ): List<PredictedBudget>

    /** Invalidates the cached monthly insight, forcing a fresh generation on next call. */
    fun clearInsightCache()

    /**
     * Generates a personalized AI narrative, strengths, and growth tips for a computed [FinancialDNA].
     * Cached for 24 hours per DNA code & language to minimize latency and API usage.
     *
     * @return null if AI providers are unavailable (caller falls back to local deterministic narrative).
     */
    suspend fun getFinancialDnaNarrative(
        dna: FinancialDNA,
        languageCode: String = "vi"
    ): FinancialDnaAiNarrative?

    /** Invalidates the cached Financial DNA narrative. */
    fun clearDnaCache()

    /**
     * Generates AI commentary and 1 actionable tip for a [WeeklyDigest].
     * Cached for 24 hours per language; returns null if AI providers are unreachable.
     */
    suspend fun getWeeklyDigestCommentary(
        digest: WeeklyDigest,
        languageCode: String = "vi"
    ): Pair<String, String>?

    /** Invalidates the cached weekly digest AI commentary. */
    fun clearWeeklyDigestCache()
}
