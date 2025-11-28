package com.example.moneymanager.data.remote

import com.example.moneymanager.data.model.OllamaRequest
import com.example.moneymanager.data.model.OllamaResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OllamaService {
    @POST("api/generate")
    suspend fun generate(@Body request: OllamaRequest): OllamaResponse
}