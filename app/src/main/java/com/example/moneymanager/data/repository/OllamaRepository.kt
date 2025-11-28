package com.example.moneymanager.data.repository

import android.util.Log
import com.example.moneymanager.data.model.OllamaRequest
import com.example.moneymanager.data.remote.OllamaService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OllamaRepository @Inject constructor(
    private val ollamaService: OllamaService
) {
    suspend fun sendMessage(prompt: String, modelName: String = "qwen2.5:1.5b", jsonMode: Boolean = false): Result<String> {
        return try {
            val request = OllamaRequest(model = modelName, prompt = prompt,format = if (jsonMode) "json" else null)
            val response = ollamaService.generate(request)
            Result.success(response.response)

        } catch (e: Exception) {
            Log.e("OllamaRepository", "Error generating response", e)
            Result.failure(e)
        }
    }
}