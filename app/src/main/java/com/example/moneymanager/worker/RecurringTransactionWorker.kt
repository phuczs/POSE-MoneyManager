package com.example.moneymanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.moneymanager.domain.usecase.recurring.ProcessDueRecurringTransactionsUseCase
import com.example.moneymanager.util.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class RecurringTransactionWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun processDueRecurringTransactionsUseCase(): ProcessDueRecurringTransactionsUseCase
        fun notificationHelper(): NotificationHelper
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                WorkerEntryPoint::class.java
            )
            val useCase = entryPoint.processDueRecurringTransactionsUseCase()
            val notificationHelper = entryPoint.notificationHelper()

            val result = useCase()
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                if (count > 0) {
                    notificationHelper.showRecurringTransactionNotification(count)
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "recurring_transactions_worker"

        fun enqueuePeriodicWork(context: Context) {
            try {
                val workRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
                    12, TimeUnit.HOURS
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
