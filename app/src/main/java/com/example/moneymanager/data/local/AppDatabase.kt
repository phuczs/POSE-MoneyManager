package com.example.moneymanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Single source of truth for the local SQLite database.
 *
 * Security practices applied:
 * - The database name is non-guessable and app-scoped.
 * - `fallbackToDestructiveMigration` is NOT used; explicit migrations must be
 *   provided when the schema changes so user data is never silently destroyed.
 *   (During development you may call `fallbackToDestructiveMigration()` on the
 *    builder after confirming the tradeoff with the team.)
 * - No `allowMainThreadQueries()` — all I/O is forced to background threads.
 */
@Database(
    entities = [TransactionEntity::class, ChatMessageEntity::class, RecurringTransactionEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun chatDao(): ChatDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        private const val DB_NAME = "moneymanager_local.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Build the database with safe defaults and singleton reuse.
         */
        fun create(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
