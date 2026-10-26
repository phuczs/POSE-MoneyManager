package com.example.moneymanager.di

import com.example.moneymanager.data.repository.AiServiceRepositoryImpl
import com.example.moneymanager.data.repository.FirebaseAuthRepositoryImpl
import com.example.moneymanager.data.repository.FirebaseBudgetRepositoryImpl
import com.example.moneymanager.data.repository.FirebaseCategoryRepositoryImpl
import com.example.moneymanager.data.repository.OfflineFirstTransactionRepositoryImpl
import com.example.moneymanager.data.repository.SettingsRepositoryImpl
import com.example.moneymanager.domain.repository.AiServiceRepository
import com.example.moneymanager.domain.repository.AuthRepository
import com.example.moneymanager.domain.repository.BudgetRepository
import com.example.moneymanager.domain.repository.CategoryRepository
import com.example.moneymanager.domain.repository.SettingsRepository
import com.example.moneymanager.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds domain repository interfaces to their data layer implementations.
 *
 * Clean Architecture note:
 * - This module is the ONLY place where concrete [*Impl] classes are mentioned.
 *   All other code (use cases, ViewModels) depends only on the [domain.repository.*] interfaces.
 * - Using [Binds] instead of [Provides] is more efficient (no extra class instantiation).
 *
 * Security note:
 * - [SettingsRepository] is scoped to [SingletonComponent] to ensure settings state is
 *   consistent across all ViewModels within the same app session.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: OfflineFirstTransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: OfflineFirstTransactionRepositoryImpl): com.example.moneymanager.domain.repository.SyncRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: FirebaseCategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: FirebaseBudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindAiServiceRepository(impl: AiServiceRepositoryImpl): AiServiceRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindChatHistoryRepository(impl: com.example.moneymanager.data.repository.RoomChatHistoryRepositoryImpl): com.example.moneymanager.domain.repository.ChatHistoryRepository

    @Binds
    @Singleton
    abstract fun bindRecurringTransactionRepository(impl: com.example.moneymanager.data.repository.RoomRecurringTransactionRepositoryImpl): com.example.moneymanager.domain.repository.RecurringTransactionRepository

    @Binds
    @Singleton
    abstract fun bindExportRepository(impl: com.example.moneymanager.data.repository.AndroidExportRepositoryImpl): com.example.moneymanager.domain.repository.ExportRepository
}
