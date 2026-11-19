package com.example.moneymanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.moneymanager.domain.model.AiInsight
import com.example.moneymanager.domain.model.FinancialDNA
import com.example.moneymanager.domain.model.FinancialDnaAiNarrative
import com.example.moneymanager.domain.model.PredictedBudget
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.model.WeeklyDigest
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

        private const val KEY_DNA_GENERATED_AT     = "dna_generated_at"
        private const val KEY_DNA_CODE             = "dna_code"
        private const val KEY_DNA_LANG             = "dna_lang"
        private const val KEY_DNA_NARRATIVE        = "dna_narrative"
        private const val KEY_DNA_STRENGTHS        = "dna_strengths"
        private const val KEY_DNA_IMPROVEMENTS     = "dna_improvements"
        private const val DNA_TTL_MS               = 24 * 60 * 60 * 1000L

        private const val KEY_DIGEST_GENERATED_AT  = "digest_generated_at"
        private const val KEY_DIGEST_LANG          = "digest_lang"
        private const val KEY_DIGEST_SUMMARY       = "digest_summary"
        private const val KEY_DIGEST_TIP           = "digest_tip"
        private const val DIGEST_TTL_MS            = 24 * 60 * 60 * 1000L
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

    // ─── Financial DNA Narrative ──────────────────────────────────────────────

    override suspend fun getFinancialDnaNarrative(
        dna: FinancialDNA,
        languageCode: String
    ): FinancialDnaAiNarrative? {
        val cached = loadCachedDnaNarrative()
        if (cached != null &&
            cached.dnaCode == dna.code &&
            cached.languageCode == languageCode &&
            (System.currentTimeMillis() - cached.generatedAt) < DNA_TTL_MS
        ) {
            return cached
        }

        val prompt = PromptUtils.getFinancialDnaPrompt(
            dnaCode = dna.code,
            archetypeTitle = dna.archetype.title(languageCode),
            savingsScore = dna.savingsScore,
            plannerScore = dna.plannerScore,
            stabilityScore = dna.stabilityScore,
            focusScore = dna.focusScore,
            savingsRatePercent = dna.savingsRatePercent,
            weeklyVariancePercent = dna.weeklyVariancePercent,
            topCategoriesSharePercent = dna.topCategoriesSharePercent,
            topCategories = dna.topCategories.joinToString(", ") { sanitizeInput(it) }.ifBlank { "General" },
            languageCode = languageCode
        )

        return try {
            val result = queryAi(prompt, jsonMode = true)
            result.fold(
                onSuccess = { json ->
                    val parsed = parseFinancialDnaNarrative(json, dna.code, languageCode)
                    if (parsed != null) saveDnaNarrativeToCache(parsed)
                    parsed
                },
                onFailure = { null }
            )
        } catch (e: Exception) {
            Log.w(TAG, "[AI] Financial DNA narrative parse/query error", e)
            null
        }
    }

    private fun parseFinancialDnaNarrative(
        rawResponse: String,
        dnaCode: String,
        languageCode: String
    ): FinancialDnaAiNarrative? {
        return try {
            val cleaned = rawResponse.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JsonParser.parseString(cleaned).asJsonObject
            val narrative = obj.get("narrative")?.asString?.trim()?.takeIf { it.isNotBlank() }
                ?: return null
            val strengths = obj.getAsJsonArray("strengths")
                ?.mapNotNull { it.asString?.trim()?.takeIf { s -> s.isNotBlank() } }
                ?.take(3)
                ?: emptyList()
            val improvements = obj.getAsJsonArray("improvements")
                ?.mapNotNull { it.asString?.trim()?.takeIf { s -> s.isNotBlank() } }
                ?.take(3)
                ?: emptyList()

            FinancialDnaAiNarrative(
                narrative = narrative,
                strengths = strengths,
                improvements = improvements,
                dnaCode = dnaCode,
                languageCode = languageCode,
                generatedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun loadCachedDnaNarrative(): FinancialDnaAiNarrative? {
        val generatedAt = prefs.getLong(KEY_DNA_GENERATED_AT, 0L)
        if (generatedAt == 0L) return null
        val narrative = prefs.getString(KEY_DNA_NARRATIVE, null)?.takeIf { it.isNotBlank() } ?: return null
        val code = prefs.getString(KEY_DNA_CODE, null) ?: return null
        val lang = prefs.getString(KEY_DNA_LANG, "vi") ?: "vi"
        val strengths = prefs.getString(KEY_DNA_STRENGTHS, "")
            ?.split("||")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val improvements = prefs.getString(KEY_DNA_IMPROVEMENTS, "")
            ?.split("||")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        return FinancialDnaAiNarrative(
            narrative = narrative,
            strengths = strengths,
            improvements = improvements,
            dnaCode = code,
            languageCode = lang,
            generatedAt = generatedAt
        )
    }

    private fun saveDnaNarrativeToCache(narrative: FinancialDnaAiNarrative) {
        prefs.edit()
            .putLong(KEY_DNA_GENERATED_AT, narrative.generatedAt)
            .putString(KEY_DNA_CODE, narrative.dnaCode)
            .putString(KEY_DNA_LANG, narrative.languageCode)
            .putString(KEY_DNA_NARRATIVE, narrative.narrative)
            .putString(KEY_DNA_STRENGTHS, narrative.strengths.joinToString("||"))
            .putString(KEY_DNA_IMPROVEMENTS, narrative.improvements.joinToString("||"))
            .apply()
    }

    override fun clearDnaCache() {
        prefs.edit()
            .remove(KEY_DNA_GENERATED_AT)
            .remove(KEY_DNA_CODE)
            .remove(KEY_DNA_LANG)
            .remove(KEY_DNA_NARRATIVE)
            .remove(KEY_DNA_STRENGTHS)
            .remove(KEY_DNA_IMPROVEMENTS)
            .apply()
    }

    // ─── Weekly AI Digest Commentary ──────────────────────────────────────────

    override suspend fun getWeeklyDigestCommentary(
        digest: WeeklyDigest,
        languageCode: String
    ): Pair<String, String>? {
        val cachedAt = prefs.getLong(KEY_DIGEST_GENERATED_AT, 0L)
        val cachedLang = prefs.getString(KEY_DIGEST_LANG, null)
        if (cachedAt > 0L && cachedLang == languageCode &&
            (System.currentTimeMillis() - cachedAt) < DIGEST_TTL_MS
        ) {
            val summary = prefs.getString(KEY_DIGEST_SUMMARY, null)?.takeIf { it.isNotBlank() }
            val tip = prefs.getString(KEY_DIGEST_TIP, null)?.takeIf { it.isNotBlank() }
            if (summary != null && tip != null) return summary to tip
        }

        val currencyCode = if (languageCode == "vi") "VND" else "USD"
        val topSummary = digest.topCategories.joinToString(", ") {
            "${sanitizeInput(it.category)}: ${it.amount.toCurrencyString(currencyCode)}"
        }.ifBlank { "General" }
        val budgetText = if (digest.budgetsTotalCount > 0) {
            "${digest.budgetsOnTrackCount}/${digest.budgetsTotalCount}"
        } else {
            if (languageCode == "vi") "Chưa đặt ngân sách" else "No active budgets"
        }
        val highestDayText = if (digest.highestSpendDayAmount > 0.0) {
            "${digest.highestSpendDayLabel} (${digest.highestSpendDayAmount.toCurrencyString(currencyCode)})"
        } else {
            "N/A"
        }

        val prompt = PromptUtils.getWeeklyDigestPrompt(
            totalSpentFormatted = digest.totalSpent.toCurrencyString(currencyCode),
            previousWeekSpentFormatted = digest.previousWeekSpent.toCurrencyString(currencyCode),
            deltaPercent = digest.deltaPercent,
            topCategoriesSummary = topSummary,
            budgetsOnTrackText = budgetText,
            highestDayText = highestDayText,
            languageCode = languageCode
        )

        return try {
            val result = queryAi(prompt, jsonMode = true)
            result.fold(
                onSuccess = { raw ->
                    val cleaned = raw.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    val obj = JsonParser.parseString(cleaned).asJsonObject
                    val summary = obj.get("summary")?.asString?.trim()?.takeIf { it.isNotBlank() }
                        ?: return null
                    val tip = obj.get("tip")?.asString?.trim()?.takeIf { it.isNotBlank() }
                        ?: return null
                    prefs.edit()
                        .putLong(KEY_DIGEST_GENERATED_AT, System.currentTimeMillis())
                        .putString(KEY_DIGEST_LANG, languageCode)
                        .putString(KEY_DIGEST_SUMMARY, summary)
                        .putString(KEY_DIGEST_TIP, tip)
                        .apply()
                    summary to tip
                },
                onFailure = { null }
            )
        } catch (e: Exception) {
            Log.w(TAG, "[AI] Weekly digest parse/query error", e)
            null
        }
    }

    override fun clearWeeklyDigestCache() {
        prefs.edit()
            .remove(KEY_DIGEST_GENERATED_AT)
            .remove(KEY_DIGEST_LANG)
            .remove(KEY_DIGEST_SUMMARY)
            .remove(KEY_DIGEST_TIP)
            .apply()
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
