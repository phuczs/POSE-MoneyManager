package com.example.moneymanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.moneymanager.MainActivity
import com.example.moneymanager.R
import com.example.moneymanager.data.local.AppDatabase
import com.example.moneymanager.util.toCurrencyString
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class MoneyManagerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        // 1. Gán ngay RemoteViews mặc định ban đầu một cách đồng bộ
        // Giúp Launcher hệ thống hiển thị ngay lập tức, không bao giờ bị timeout "Can't load widget"
        for (appWidgetId in appWidgetIds) {
            val defaultViews = buildRemoteViews(context)
            appWidgetManager.updateAppWidget(appWidgetId, defaultViews)
        }

        // 2. Tải dữ liệu bất đồng bộ từ Room Database sử dụng goAsync() để giữ Receiver sống
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    val views = buildRemoteViews(context)
                    populateWidgetData(context, views)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, MoneyManagerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.moneymanager.ACTION_REFRESH_WIDGET"
        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val NAV_ADD_TRANSACTION = "add_transaction"
        const val NAV_CHAT = "chat"

        fun updateAllWidgets(context: Context) {
            try {
                val intent = Intent(context, MoneyManagerWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun buildRemoteViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_money_manager)

            // 1. Nhấn toàn bộ thẻ -> Mở MainActivity (Dashboard)
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                100,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // 2. Nhấn nút Refresh -> Broadcast làm mới dữ liệu
            val refreshIntent = Intent(context, MoneyManagerWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            // 3. Nhấn "+ Thêm" -> Mở MainActivity điều hướng đến AddTransactionScreen
            val addIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NAVIGATE_TO, NAV_ADD_TRANSACTION)
            }
            val addPendingIntent = PendingIntent.getActivity(
                context,
                102,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_quick_add, addPendingIntent)

            // 4. Nhấn "Trợ lý AI" -> Mở MainActivity điều hướng đến ChatScreen
            val chatIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NAVIGATE_TO, NAV_CHAT)
            }
            val chatPendingIntent = PendingIntent.getActivity(
                context,
                103,
                chatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_chat, chatPendingIntent)

            return views
        }

        suspend fun populateWidgetData(context: Context, views: RemoteViews) {
            try {
                val userId = try {
                    FirebaseAuth.getInstance().currentUser?.uid
                } catch (e: Exception) {
                    null
                }

                val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                val lang = sharedPrefs.getString("app_language", "vi") ?: "vi"
                val currencyCode = if (lang == "vi") "VND" else "USD"

                if (userId.isNullOrBlank()) {
                    views.setTextViewText(R.id.widget_tv_balance, "--")
                    views.setTextViewText(R.id.widget_tv_expense, if (lang == "vi") "Vui lòng mở ứng dụng" else "Please open app")
                    return
                }

                val db = AppDatabase.create(context)
                val transactions = db.transactionDao().getAll(userId).first()

                val income = transactions.filter { it.type.equals("income", ignoreCase = true) }.sumOf { it.amount }
                val expense = transactions.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { it.amount }
                val balance = income - expense

                // Tính tổng chi tiêu của tháng hiện tại
                val cal = Calendar.getInstance()
                val curMonth = cal.get(Calendar.MONTH) + 1
                val curYear = cal.get(Calendar.YEAR)
                val monthlyExpense = transactions.filter {
                    val y = if (it.year != 0) it.year else {
                        val c = Calendar.getInstance().apply { timeInMillis = it.date }
                        c.get(Calendar.YEAR)
                    }
                    val m = if (it.month != 0) it.month else {
                        val c = Calendar.getInstance().apply { timeInMillis = it.date }
                        c.get(Calendar.MONTH) + 1
                    }
                    y == curYear && m == curMonth && it.type.equals("expense", ignoreCase = true)
                }.sumOf { it.amount }

                views.setTextViewText(R.id.widget_tv_balance, balance.toCurrencyString(currencyCode))
                val expenseStr = context.getString(
                    R.string.widget_monthly_expense,
                    monthlyExpense.toCurrencyString(currencyCode)
                )
                views.setTextViewText(R.id.widget_tv_expense, expenseStr)
            } catch (e: Exception) {
                views.setTextViewText(R.id.widget_tv_balance, "--")
            }
        }
    }
}
