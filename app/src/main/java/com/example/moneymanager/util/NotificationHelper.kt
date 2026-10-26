package com.example.moneymanager.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.moneymanager.R // Ensure this matches your package

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "budget_alerts"
        const val CHANNEL_NAME = "Budget Alerts"
        const val RECURRING_CHANNEL_ID = "recurring_transactions"
        const val RECURRING_CHANNEL_NAME = "Recurring Transactions"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val budgetChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for budget limits"
            }
            manager.createNotificationChannel(budgetChannel)

            val recurringChannel = NotificationChannel(
                RECURRING_CHANNEL_ID,
                RECURRING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for automated recurring transactions"
            }
            manager.createNotificationChannel(recurringChannel)
        }
    }

    fun showBudgetAlert(budgetId: String, budgetName: String, percent: Int) {
        try {
            val icon = android.R.drawable.stat_sys_warning

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle("Budget Alert: $budgetName")
                .setContentText("You have reached $percent% of your $budgetName budget.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(budgetId.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showRecurringTransactionNotification(count: Int) {
        try {
            val icon = android.R.drawable.stat_notify_sync

            val builder = NotificationCompat.Builder(context, RECURRING_CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle("Recurring Transactions Processed")
                .setContentText("Processed $count automated recurring transaction(s).")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}