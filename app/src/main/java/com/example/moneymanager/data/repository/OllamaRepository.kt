package com.example.moneymanager.data.repository

import android.util.Log
import com.example.moneymanager.data.model.OllamaRequest
import com.example.moneymanager.data.remote.OllamaService
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OllamaRepository @Inject constructor(
    private val ollamaService: OllamaService
) {
    companion object {
        private const val TAG = "OllamaRepository"
        private const val DEFAULT_MODEL = "qwen2.5:3b"
    }

    /**
     * Send a single message to Ollama without retry.
     * Used for interactive chat (fast feedback preferred over durability).
     */
    suspend fun sendMessage(
        prompt: String,
        modelName: String = DEFAULT_MODEL,
        jsonMode: Boolean = false
    ): Result<String> {
        return try {
            val request = OllamaRequest(
                model = modelName,
                prompt = prompt,
                format = if (jsonMode) "json" else null
            )
            val response = ollamaService.generate(request)
            Result.success(response.response)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            Result.failure(e)
        }
    }

    /**
     * Send a message with exponential backoff retry for background AI tasks
     * (auto-categorization, insights, predictive budgets).
     * 
     * Security: prompt content is not logged at INFO level to avoid
     * leaking financial context to logcat in production.
     */
    suspend fun sendMessageWithRetry(
        prompt: String,
        modelName: String = DEFAULT_MODEL,
        jsonMode: Boolean = false,
        maxRetries: Int = 2
    ): Result<String> {
        var lastException: Exception? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                val request = OllamaRequest(
                    model = modelName,
                    prompt = prompt,
                    format = if (jsonMode) "json" else null
                )
                val response = ollamaService.generate(request)
                return Result.success(response.response)
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    val delayMs = (500L * (1 shl attempt)) // 500ms, 1000ms
                    Log.w(TAG, "Attempt ${attempt + 1} failed, retrying in ${delayMs}ms: ${e.javaClass.simpleName}")
                    delay(delayMs)
                }
            }
        }
        Log.e(TAG, "All retries exhausted: ${lastException?.javaClass?.simpleName}")
        return Result.failure(lastException ?: Exception("Unknown error after retries"))
    }
}