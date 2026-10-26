package com.example.moneymanager.data.repository

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    private val apiKey: String
) {
    companion object {
        private const val TAG = "GeminiRepository"
        // gemini-1.5-flash was deprecated by Google in April 2025.
        // Use gemini-2.5-flash-preview-04-17 (stable, free tier, same API)
        private const val MODEL_NAME = "gemini-2.5-flash-preview-04-17"
        private const val REQUEST_TIMEOUT_MS = 20_000L
        private const val MAX_INPUT_CHARS = 8_000
    }

    // 2. CẬP NHẬT: Tạo 2 model riêng biệt (1 cho text thường, 1 cho JSON)
    private val textModel: GenerativeModel? by lazy { createModel("text/plain") }
    private val jsonModel: GenerativeModel? by lazy { createModel("application/json") }

    private fun createModel(mimeType: String): GenerativeModel? {
        if (apiKey.isBlank()) {
            Log.w(TAG, "GEMINI_API_KEY is empty. Add 'geminiApiKey=AIza...' to local.properties.")
            return null
        }
        return GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.2f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 1024
                responseMimeType = mimeType // Quan trọng: Ép Gemini trả về đúng format
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.LOW_AND_ABOVE),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.LOW_AND_ABOVE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.LOW_AND_ABOVE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.LOW_AND_ABOVE)
            )
        )
    }

    val isAvailable: Boolean get() = textModel != null

    /**
     * @param isJsonMode Nếu true, model sẽ bị ép trả về JSON thuần túy (không có markdown)
     */
    suspend fun generate(prompt: String, isJsonMode: Boolean = false): Result<String> {
        val activeModel = if (isJsonMode) jsonModel else textModel

        if (activeModel == null) {
            return Result.failure(IllegalStateException("Gemini API key not configured."))
        }

        val boundedPrompt = if (prompt.length > MAX_INPUT_CHARS) {
            Log.w(TAG, "Prompt truncated from ${prompt.length} to $MAX_INPUT_CHARS chars")
            prompt.take(MAX_INPUT_CHARS)
        } else {
            prompt
        }

        return try {
            val startMs = System.currentTimeMillis()

            val response = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                activeModel.generateContent(
                    content { text(boundedPrompt) }
                )
            } ?: return Result.failure(Exception("Gemini request timed out after ${REQUEST_TIMEOUT_MS}ms"))

            val elapsed = System.currentTimeMillis() - startMs
            Log.d(TAG, "Gemini response received in ${elapsed}ms")

            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(Exception("Gemini returned empty response"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini generate failed: ${e.javaClass.simpleName} — ${e.message}")
            Result.failure(e)
        }
    }
}