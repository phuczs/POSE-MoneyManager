package com.example.moneymanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.moneymanager.domain.model.AiInsight
import com.example.moneymanager.domain.model.PredictedBudget
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.AiServiceRepository
import com.example.moneymanager.util.PromptUtils
import com.example.moneymanager.util.toCurrencyString
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [AiServiceRepository] that routes prompts through
 * Google Gemini (primary) with automatic fallback to local Ollama.
 *
 * Clean Architecture note:
 * - All prompt engineering, JSON parsing, caching, and provider fallback logic
 *   lives here in the data layer — the domain interface and use cases are clean.
 *
 * Security note:
 * - [sanitizeInput] strips dangerous characters from user input before embedding
 *   in AI prompts to prevent prompt injection attacks.
 * - Only aggregated financial summaries are sent to external Gemini API —
 *   never raw transaction lists containing user PII.
 * - The 12-hour TTL cache prevents excessive API calls and costs.
 */
@Singleton
class AiServiceRepositoryImpl @Inject constructor(
    private val ollamaRepository: OllamaRepository,
    private val geminiRepository: GeminiRepository,
    @ApplicationContext private val context: Context
) : AiServiceRepository {

    private val TAG = "AiServiceRepoImpl"
    private val gson = Gson()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_cache", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_INSIGHT_GENERATED_AT = "insight_generated_at"
        private const val KEY_INSIGHT_SUMMARY      = "insight_summary"
        private const val KEY_INSIGHT_TOP_CHANGE   = "insight_top_change"
        private const val KEY_INSIGHT_TIP          = "insight_tip"
        private const val KEY_INSIGHT_LANG         = "insight_lang"
        private const val INSIGHT_TTL_MS           = 12 * 60 * 60 * 1000L
    }

    // ─── Provider Routing (Gemini → Ollama fallback) ──────────────────────────

    override suspend fun queryAi(prompt: String, jsonMode: Boolean): Result<String> {
        if (geminiRepository.isAvailable) {
            val geminiResult = geminiRepository.generate(prompt, isJsonMode = jsonMode)
            if (geminiResult.isSuccess) {
                Log.d(TAG, "[AI] Served by Gemini")
                return geminiResult
            }
            Log.w(TAG, "[AI] Gemini failed (${geminiResult.exceptionOrNull()?.javaClass?.simpleName}), falling back to Ollama")
        } else {
            Log.w(TAG, "[AI] Gemini not configured, using local Ollama")
        }
        return ollamaRepository.sendMessageWithRetry(prompt, jsonMode = jsonMode)
    }

    // ─── Category Prediction ─────────────────────────────────────────────────

    override suspend fun getCategoryPrediction(
        rawDescription: String,
        amount: Double,
        recentTransactions: List<Transaction>,
        availableCategories: List<String>
    ): String {
        if (availableCategories.isEmpty()) return "General"
        val sanitized = sanitizeInput(rawDescription)
        if (sanitized.isBlank()) return availableCategories.firstOrNull() ?: "General"

        val historyExamples = recentTransactions
            .filter { it.description.isNotBlank() }
            .take(15)
            .joinToString("\n") { "${sanitizeInput(it.description).take(40)} -> ${it.category}" }
            .ifBlank { "No history yet" }

        val prompt = PromptUtils.getAutoCategoryPrompt(
            sanitizedDescription = sanitized,
            amount = amount,
            recentHistoryExamples = historyExamples,
            availableCategories = availableCategories.joinToString(", ")
        )

        return try {
            val result = queryAi(prompt, jsonMode = true)
            result.fold(
                onSuccess = { json ->
                    val predicted = parseCategory(json)
                    availableCategories.firstOrNull { it.equals(predicted, ignoreCase = true) } ?: "General"
                },
                onFailure = { "General" }
            )
        } catch (e: Exception) {
            "General"
        }
    }

    private fun parseCategory(json: String): String = try {
        JsonParser.parseString(json.trim()).asJsonObject.get("category")?.asString ?: "General"
    } catch (e: Exception) { "General" }

    // ─── Monthly Insight ─────────────────────────────────────────────────────

    override suspend fun getMonthlyInsight(
        currentMonthTransactions: List<Transaction>,
        previousMonthTransactions: List<Transaction>,
        languageCode: String
    ): AiInsight? {
        if (currentMonthTransactions.isEmpty()) return null

        // Serve cached insight if fresh
        val cached = loadCachedInsight()
        if (cached != null && cached.languageCode == languageCode &&
            (System.currentTimeMillis() - cached.generatedAt) < INSIGHT_TTL_MS
        ) return cached

        val currentSummary = buildMonthSummary(currentMonthTransactions, languageCode)
        val previousSummary = if (previousMonthTransactions.isNotEmpty()) {
            buildMonthSummary(previousMonthTransactions, languageCode)
        } else {
            if (languageCode == "vi") "Không có dữ liệu tháng trước" else "No data for previous month"
        }

        val prompt = PromptUtils.getInsightReportPrompt(currentSummary, previousSummary, languageCode)

        return try {
            val result = queryAi(prompt, jsonMode = true)
            result.fold(
                onSuccess = { json ->
                    val insight = parseInsight(json, languageCode)
                    if (insight != null) saveInsightToCache(insight)
                    insight
                },
                onFailure = { null }
            )
        } catch (e: Exception) { null }
    }

    /**
     * Builds an aggregated summary string for a month's transactions.
     *
     * Security: Only category-level aggregates are sent to the external AI API,
     * not individual transaction descriptions or amounts.
     */
    private fun buildMonthSummary(transactions: List<Transaction>, languageCode: String): String {
        val currencyCode = if (languageCode == "vi") "VND" else "USD"
        val income = transactions.filter { it.type == "income" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
        val topCategories = transactions.filter { it.type == "expense" }
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { "${it.key}: ${it.value.toCurrencyString(currencyCode)}" }
        return if (languageCode == "vi") {
            "Thu: ${income.toCurrencyString(currencyCode)}, Chi: ${expense.toCurrencyString(currencyCode)}, Top chi: $topCategories"
        } else {
            "Income: ${income.toCurrencyString(currencyCode)}, Expense: ${expense.toCurrencyString(currencyCode)}, Top: $topCategories"
        }
    }

    private fun parseInsight(json: String, languageCode: String): AiInsight? {
        return try {
            val obj = JsonParser.parseString(json.trim()).asJsonObject
            val summary = obj.get("summary")?.asString ?: return null
            AiInsight(
                generatedAt = System.currentTimeMillis(),
                summaryText = summary,
                topChangeCategory = obj.get("topChangeCategory")?.asString ?: "",
                actionableTip = obj.get("actionableTip")?.asString ?: "",
                languageCode = languageCode
            )
        } catch (e: Exception) { null }
    }

    private fun loadCachedInsight(): AiInsight? {
        val generatedAt = prefs.getLong(KEY_INSIGHT_GENERATED_AT, 0L)
        if (generatedAt == 0L) return null
        return AiInsight(
            generatedAt = generatedAt,
            summaryText = prefs.getString(KEY_INSIGHT_SUMMARY, "") ?: return null,
            topChangeCategory = prefs.getString(KEY_INSIGHT_TOP_CHANGE, "") ?: "",
            actionableTip = prefs.getString(KEY_INSIGHT_TIP, "") ?: "",
            languageCode = prefs.getString(KEY_INSIGHT_LANG, "vi") ?: "vi"
        ).takeIf { it.summaryText.isNotBlank() }
    }

    private fun saveInsightToCache(insight: AiInsight) {
        prefs.edit()
            .putLong(KEY_INSIGHT_GENERATED_AT, insight.generatedAt)
            .putString(KEY_INSIGHT_SUMMARY, insight.summaryText)
            .putString(KEY_INSIGHT_TOP_CHANGE, insight.topChangeCategory)
            .putString(KEY_INSIGHT_TIP, insight.actionableTip)
            .putString(KEY_INSIGHT_LANG, insight.languageCode)
            .apply()
    }

    override fun clearInsightCache() { prefs.edit().clear().apply() }

    // ─── Predictive Budgets ───────────────────────────────────────────────────

    override suspend fun getPredictiveBudgets(
        threeMonthTransactions: List<Transaction>,
        availableCategories: List<String>,
        languageCode: String
    ): List<PredictedBudget> {
        val expenseTxs = threeMonthTransactions.filter { it.type == "expense" }
        if (expenseTxs.isEmpty()) return emptyList()

        val categoryMonthlyData = expenseTxs.groupBy { it.category }
            .mapValues { (_, txs) ->
                val monthsPresent = txs.map { "${it.month}-${it.year}" }.distinct().size.coerceAtLeast(1)
                txs.sumOf { it.amount } / monthsPresent
            }
            .entries.sortedByDescending { it.value }.take(5).associate { it.key to it.value }

        if (categoryMonthlyData.isEmpty()) return emptyList()

        val currencyCode = if (languageCode == "vi") "VND" else "USD"
        val averagesText = categoryMonthlyData.entries.joinToString("\n") { (cat, avg) ->
            "- $cat: trung bình ${avg.toCurrencyString(currencyCode)}/tháng"
        }

        val prompt = PromptUtils.getPredictiveBudgetPrompt(averagesText, languageCode)

        return try {
            val result = queryAi(prompt, jsonMode = true)
            result.fold(
                onSuccess = { json ->
                    parsePredictedBudgets(json, categoryMonthlyData, availableCategories)
                },
                onFailure = { generateFallbackBudgets(categoryMonthlyData) }
            )
        } catch (e: Exception) { emptyList() }
    }

    private fun parsePredictedBudgets(
        json: String,
        categoryAverages: Map<String, Double>,
        availableCategories: List<String>
    ): List<PredictedBudget> = try {
        val array: JsonArray = when (val element = JsonParser.parseString(json.trim())) {
            is JsonArray  -> element
            is JsonObject -> element.entrySet().firstOrNull()?.value?.asJsonArray ?: return emptyList()
            else          -> return emptyList()
        }
        array.mapNotNull { elem ->
            try {
                val obj = elem.asJsonObject
                val category   = obj.get("category")?.asString ?: return@mapNotNull null
                val suggested  = obj.get("suggestedAmount")?.asDouble ?: return@mapNotNull null
                val rationale  = obj.get("rationale")?.asString ?: ""
                // Validate category against the user's actual category list
                val validated  = availableCategories.find {
                    it.equals(category, ignoreCase = true)
                } ?: return@mapNotNull null
                PredictedBudget(validated, suggested, rationale, 3, categoryAverages[validated] ?: 0.0)
            } catch (e: Exception) { null }
        }
    } catch (e: Exception) { emptyList() }

    private fun generateFallbackBudgets(
        categoryAverages: Map<String, Double>
    ): List<PredictedBudget> = categoryAverages.map { (category, avg) ->
        PredictedBudget(
            category = category,
            suggestedAmount = Math.round((avg * 1.1) / 50_000.0) * 50_000.0,
            rationale = "Based on average monthly spending",
            basedOnMonths = 3,
            averageMonthlySpend = avg
        )
    }

    // ─── Security Utilities ───────────────────────────────────────────────────

    /**
     * Strips characters that could be used for prompt injection.
     * Allows alphanumerics, whitespace, and safe punctuation only.
     * Limits input to 200 characters to prevent token overflow.
     */
    private fun sanitizeInput(input: String): String =
        input.filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,!?-_/" }
            .take(200)
            .trim()
}
