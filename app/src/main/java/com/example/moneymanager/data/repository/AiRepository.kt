package com.example.moneymanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.moneymanager.data.model.AiInsight
import com.example.moneymanager.data.model.PredictedBudget
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.util.PromptUtils
import com.example.moneymanager.util.toCurrencyString
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val ollamaRepository: OllamaRepository,
    private val geminiRepository: GeminiRepository,
    @ApplicationContext private val context: Context
) {
    private val TAG = "AiRepository"
    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_cache", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_INSIGHT_GENERATED_AT = "insight_generated_at"
        private const val KEY_INSIGHT_SUMMARY = "insight_summary"
        private const val KEY_INSIGHT_TOP_CHANGE = "insight_top_change"
        private const val KEY_INSIGHT_TIP = "insight_tip"
        private const val KEY_INSIGHT_LANG = "insight_lang"
        private const val INSIGHT_TTL_MS = 12 * 60 * 60 * 1000L
    }

    suspend fun queryAi(prompt: String, jsonMode: Boolean = false): Result<String> {
        // CẬP NHẬT: Truyền jsonMode vào Gemini để báo cho Gemini biết cần trả về JSON
        if (geminiRepository.isAvailable) {
            val geminiResult = geminiRepository.generate(prompt, isJsonMode = jsonMode)
            if (geminiResult.isSuccess) {
                Log.d(TAG, "[AI] Served by Gemini 1.5 Flash")
                return geminiResult
            }
            Log.w(TAG, "[AI] Gemini failed (${geminiResult.exceptionOrNull()?.javaClass?.simpleName}), falling back to Ollama")
        } else {
            Log.w(TAG, "[AI] Gemini not configured (missing Key), using Ollama Local")
        }

        // Fallback: local Ollama
        return ollamaRepository.sendMessageWithRetry(prompt, jsonMode = jsonMode)
    }

    // =====================================================================
    // CÁC HÀM BÊN DƯỚI GIỮ NGUYÊN HOÀN TOÀN NHƯ CŨ CỦA BẠN
    // =====================================================================

    suspend fun getCategoryPrediction(
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
            .joinToString("\n") { tx -> "${sanitizeInput(tx.description).take(40)} -> ${tx.category}" }
            .ifBlank { "No history yet" }

        val categoryList = availableCategories.joinToString(", ")

        val prompt = PromptUtils.getAutoCategoryPrompt(
            sanitizedDescription = sanitized, amount = amount,
            recentHistoryExamples = historyExamples, availableCategories = categoryList
        )

        return try {
            val result = queryAi(prompt, jsonMode = true)
            result.fold(
                onSuccess = { json ->
                    val predicted = parseCategory(json)
                    if (availableCategories.any { it.equals(predicted, ignoreCase = true) }) {
                        availableCategories.first { it.equals(predicted, ignoreCase = true) }
                    } else {
                        "General"
                    }
                },
                onFailure = { e -> "General" }
            )
        } catch (e: Exception) {
            "General"
        }
    }

    private fun parseCategory(json: String): String {
        return try {
            val obj = JsonParser.parseString(json.trim()).asJsonObject
            obj.get("category")?.asString ?: "General"
        } catch (e: Exception) { "General" }
    }

    suspend fun getMonthlyInsight(
        currentMonthTransactions: List<Transaction>,
        previousMonthTransactions: List<Transaction>,
        languageCode: String = "vi"
    ): AiInsight? {
        if (currentMonthTransactions.isEmpty()) return null

        val cachedInsight = loadCachedInsight()
        if (cachedInsight != null && cachedInsight.languageCode == languageCode &&
            (System.currentTimeMillis() - cachedInsight.generatedAt) < INSIGHT_TTL_MS
        ) return cachedInsight

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
                onFailure = { e -> null }
            )
        } catch (e: Exception) { null }
    }

    private fun buildMonthSummary(transactions: List<Transaction>, languageCode: String): String {
        val currencyCode = if (languageCode == "vi") "VND" else "USD"
        val income = transactions.filter { it.type == "income" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
        val topCategories = transactions.filter { it.type == "expense" }.groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }.entries.sortedByDescending { it.value }
            .take(3).joinToString(", ") { "${it.key}: ${it.value.toCurrencyString(currencyCode)}" }
        return if (languageCode == "vi") {
            "Thu: ${income.toCurrencyString(currencyCode)}, Chi: ${expense.toCurrencyString(currencyCode)}, Top chi: $topCategories"
        } else {
            "Income: ${income.toCurrencyString(currencyCode)}, Expense: ${expense.toCurrencyString(currencyCode)}, Top: $topCategories"
        }
    }

    private fun parseInsight(json: String, languageCode: String): AiInsight? {
        return try {
            val obj = JsonParser.parseString(json.trim()).asJsonObject
            AiInsight(
                generatedAt = System.currentTimeMillis(),
                summaryText = obj.get("summary")?.asString ?: return null,
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
        prefs.edit().putLong(KEY_INSIGHT_GENERATED_AT, insight.generatedAt)
            .putString(KEY_INSIGHT_SUMMARY, insight.summaryText).putString(KEY_INSIGHT_TOP_CHANGE, insight.topChangeCategory)
            .putString(KEY_INSIGHT_TIP, insight.actionableTip).putString(KEY_INSIGHT_LANG, insight.languageCode).apply()
    }

    fun clearInsightCache() { prefs.edit().clear().apply() }

    suspend fun getPredictiveBudgets(
        threeMonthTransactions: List<Transaction>,
        availableCategories: List<String>,
        languageCode: String = "vi"
    ): List<PredictedBudget> {
        val expenseTxs = threeMonthTransactions.filter { it.type == "expense" }
        if (expenseTxs.isEmpty()) return emptyList()

        val categoryMonthlyData = expenseTxs.groupBy { it.category }
            .mapValues { (_, txs) ->
                val monthsPresent = txs.map { "${it.month}-${it.year}" }.distinct().size.coerceAtLeast(1)
                txs.sumOf { it.amount } / monthsPresent
            }.entries.sortedByDescending { it.value }.take(5).associate { it.key to it.value }
        if (categoryMonthlyData.isEmpty()) return emptyList()

        val currencyCode = if (languageCode == "vi") "VND" else "USD"
        val averagesText = categoryMonthlyData.entries.joinToString("\n") { (cat, avg) ->
            "- $cat: trung bình ${avg.toCurrencyString(currencyCode)}/tháng"
        }

        val prompt = PromptUtils.getPredictiveBudgetPrompt(averagesText, languageCode)

        return try {
            val result = queryAi(prompt, jsonMode = true)
            result.fold(
                onSuccess = { json -> parsePredictedBudgets(json, categoryMonthlyData, availableCategories) },
                onFailure = { e -> generateFallbackBudgets(categoryMonthlyData, languageCode) }
            )
        } catch (e: Exception) { emptyList() }
    }

    private fun parsePredictedBudgets(json: String, categoryAverages: Map<String, Double>, availableCategories: List<String>): List<PredictedBudget> {
        return try {
            val array: JsonArray = when (val element = JsonParser.parseString(json.trim())) {
                is JsonArray -> element
                is JsonObject -> element.entrySet().firstOrNull()?.value?.asJsonArray ?: return emptyList()
                else -> return emptyList()
            }
            array.mapNotNull { elem ->
                try {
                    val obj = elem.asJsonObject
                    val category = obj.get("category")?.asString ?: return@mapNotNull null
                    val suggested = obj.get("suggestedAmount")?.asDouble ?: return@mapNotNull null
                    val rationale = obj.get("rationale")?.asString ?: ""
                    val validatedCategory = availableCategories.find { it.equals(category, ignoreCase = true) } ?: return@mapNotNull null
                    PredictedBudget(validatedCategory, suggested, rationale, 3, categoryAverages[validatedCategory] ?: 0.0)
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun generateFallbackBudgets(categoryAverages: Map<String, Double>, languageCode: String): List<PredictedBudget> {
        return categoryAverages.map { (category, avg) ->
            PredictedBudget(category, (Math.round((avg * 1.1) / 50_000.0) * 50_000.0), "Dựa trên chi tiêu trung bình", 3, avg)
        }
    }

    private fun sanitizeInput(input: String): String {
        return input.filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,!?-_/" }.take(200).trim()
    }
}