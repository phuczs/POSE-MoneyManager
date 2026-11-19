package com.example.moneymanager.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.moneymanager.MainActivity
import com.example.moneymanager.widget.MoneyManagerWidgetProvider
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
        const val AI_COACH_CHANNEL_ID = "ai_coach"
        const val AI_COACH_CHANNEL_NAME = "AI Coach & Weekly Digest"
        const val NAV_WEEKLY_DIGEST = "weekly_digest"
        const val NAV_BUDGETS = "budgets"
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

            val aiCoachChannel = NotificationChannel(
                AI_COACH_CHANNEL_ID,
                AI_COACH_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Proactive AI Coach nudges and Monday Weekly Financial Digest"
            }
            manager.createNotificationChannel(aiCoachChannel)
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

    fun showWeeklyDigestNotification(title: String, summary: String) {
        showDeepLinkedCoachNotification(
            notificationId = 5301,
            title = title,
            body = summary,
            navTarget = NAV_WEEKLY_DIGEST
        )
    }

    fun showCoachNudgeNotification(
        nudgeId: String,
        title: String,
        body: String,
        navTarget: String
    ) {
        showDeepLinkedCoachNotification(
            notificationId = 5302 + (nudgeId.hashCode() and 0xFF),
            title = title,
            body = body,
            navTarget = navTarget
        )
    }

    private fun showDeepLinkedCoachNotification(
        notificationId: Int,
        title: String,
        body: String,
        navTarget: String
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MoneyManagerWidgetProvider.EXTRA_NAVIGATE_TO, navTarget)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, AI_COACH_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}