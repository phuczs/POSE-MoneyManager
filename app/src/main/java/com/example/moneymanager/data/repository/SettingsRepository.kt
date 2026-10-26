package com.example.moneymanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val NOTIFICATIONS_KEY = "notifications_enabled"

    private val LANGUAGE_KEY = "app_language"
    private val ACCENT_COLOR_KEY = "app_accent_color"
    private val WIDGET_ORDER_KEY = "dashboard_widget_order"
    private val _isNotificationsEnabled = MutableStateFlow(prefs.getBoolean(NOTIFICATIONS_KEY, true))

    private val _alertedBudgets = mutableSetOf<String>()

    fun hasAlerted(budgetId: String): Boolean {
        return _alertedBudgets.contains(budgetId)
    }

    fun setAlerted(budgetId: String) {
        _alertedBudgets.add(budgetId)
    }
    val isNotificationsEnabled: StateFlow<Boolean> = _isNotificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(NOTIFICATIONS_KEY, enabled).apply()
        _isNotificationsEnabled.value = enabled
    }
    private val _currentLanguage = MutableStateFlow(prefs.getString(LANGUAGE_KEY, "vi") ?: "vi")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setLanguage(languageCode: String) {
        prefs.edit().putString(LANGUAGE_KEY, languageCode).apply()
        _currentLanguage.value = languageCode
    }

    // --- Customization Settings ---
    private val _accentColorHex = MutableStateFlow(prefs.getString(ACCENT_COLOR_KEY, "") ?: "")
    val accentColorHex: StateFlow<String> = _accentColorHex.asStateFlow()

    fun setAccentColorHex(hexCode: String) {
        prefs.edit().putString(ACCENT_COLOR_KEY, hexCode).apply()
        _accentColorHex.value = hexCode
    }

    // Format: "widgets,separated,by,comma". Default order
    private val defaultWidgets = "AiInsight,Overview,RecentTransactions"
    private val _dashboardWidgetsOrder = MutableStateFlow(
        (prefs.getString(WIDGET_ORDER_KEY, defaultWidgets) ?: defaultWidgets).split(",").filter { it.isNotBlank() }
    )
    val dashboardWidgetsOrder: StateFlow<List<String>> = _dashboardWidgetsOrder.asStateFlow()

    fun setDashboardWidgetsOrder(order: List<String>) {
        val joined = order.joinToString(",")
        prefs.edit().putString(WIDGET_ORDER_KEY, joined).apply()
        _dashboardWidgetsOrder.value = order
    }
}