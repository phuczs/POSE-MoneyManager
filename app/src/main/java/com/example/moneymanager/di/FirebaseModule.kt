package com.example.moneymanager.di

import android.content.Context
import com.example.moneymanager.data.local.AppDatabase
import com.example.moneymanager.data.local.TransactionDao
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Firebase infrastructure, Google Sign-In, and Room database.
 *
 * Clean Architecture note:
 * - Repository bindings have been moved to [RepositoryModule], which uses
 *   @Binds instead of @Provides for better efficiency.
 * - This module is now strictly infrastructure — no business logic.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
        val gso = try {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(com.example.moneymanager.R.string.default_web_client_id))
                .requestEmail()
                .build()
        } catch (e: Exception) {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        }
        return GoogleSignIn.getClient(context, gso)
    }

    // ─── Room Database ────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.create(context)

    @Provides
    @Singleton
    fun provideTransactionDao(database: AppDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    @Singleton
    fun provideChatDao(database: AppDatabase): com.example.moneymanager.data.local.ChatDao =
        database.chatDao()

    @Provides
    @Singleton
    fun provideRecurringTransactionDao(database: AppDatabase): com.example.moneymanager.data.local.RecurringTransactionDao =
        database.recurringTransactionDao()
}
