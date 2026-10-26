package com.example.moneymanager.di

import com.example.moneymanager.BuildConfig
import com.example.moneymanager.data.repository.GeminiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for AI provider dependencies.
 *
 * Separated from FirebaseModule to keep concerns isolated.
 * The Gemini API key is injected from BuildConfig (which reads from local.properties at
 * build time — the key never appears in source code).
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    /**
     * Provides the [GeminiRepository] singleton, passing the API key from BuildConfig.
     *
     * Security: BuildConfig.GEMINI_API_KEY is populated at compile time from local.properties,
     * which is git-ignored. The key is never committed to source control.
     *
     * If the key is blank (dev machine without key, or CI), GeminiRepository.isAvailable
     * returns false and the system transparently falls back to Ollama.
     */
    @Provides
    @Singleton
    fun provideGeminiRepository(): GeminiRepository =
        GeminiRepository(apiKey = BuildConfig.GEMINI_API_KEY)
}
