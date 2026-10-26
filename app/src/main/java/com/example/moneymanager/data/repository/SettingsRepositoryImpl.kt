package com.example.moneymanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.moneymanager.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed implementation of [SettingsRepository].
 *
 * Clean Architecture note:
 * - The [SharedPreferences] dependency stays in this data-layer class.
 *   The domain [SettingsRepository] interface knows nothing about Android storage.
 * - [_alertedBudgets] is now persisted in SharedPreferences to ensure that alerts
 *   are not lost or repeatedly triggered across app restarts.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // ─── Budget Alert Tracking ───────────────────────────────────────────────
    private val _alertedBudgets: MutableSet<String> =
        prefs.getStringSet(KEY_ALERTED_BUDGETS, emptySet())?.toMutableSet() ?: mutableSetOf()

    override fun hasAlerted(budgetId: String): Boolean = _alertedBudgets.contains(budgetId)
    
    override fun setAlerted(budgetId: String) { 
        _alertedBudgets.add(budgetId)
        prefs.edit().putStringSet(KEY_ALERTED_BUDGETS, _alertedBudgets.toSet()).apply()
    }

    // ─── Notifications ───────────────────────────────────────────────────────
    private val _isNotificationsEnabled =
        MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS, true))
    override val isNotificationsEnabled: StateFlow<Boolean> = _isNotificationsEnabled.asStateFlow()

    override fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _isNotificationsEnabled.value = enabled
    }

    // ─── Language ─────────────────────────────────────────────────────────────
    private val _currentLanguage =
        MutableStateFlow(prefs.getString(KEY_LANGUAGE, "vi") ?: "vi")
    override val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    override fun setLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
        _currentLanguage.value = languageCode
    }

    // ─── Accent Color ─────────────────────────────────────────────────────────
    private val _accentColorHex =
        MutableStateFlow(prefs.getString(KEY_ACCENT_COLOR, "") ?: "")
    override val accentColorHex: StateFlow<String> = _accentColorHex.asStateFlow()

    override fun setAccentColorHex(hexCode: String) {
        prefs.edit().putString(KEY_ACCENT_COLOR, hexCode).apply()
        _accentColorHex.value = hexCode
    }

    // ─── Dashboard Widget Order ───────────────────────────────────────────────
    private val defaultWidgets = "AiInsight,Overview,RecentTransactions"
    private val _dashboardWidgetsOrder = MutableStateFlow(
        (prefs.getString(KEY_WIDGET_ORDER, defaultWidgets) ?: defaultWidgets)
            .split(",").filter { it.isNotBlank() }
    )
    override val dashboardWidgetsOrder: StateFlow<List<String>> =
        _dashboardWidgetsOrder.asStateFlow()

    override fun setDashboardWidgetsOrder(order: List<String>) {
        prefs.edit().putString(KEY_WIDGET_ORDER, order.joinToString(",")).apply()
        _dashboardWidgetsOrder.value = order
    }

    companion object {
        private const val KEY_ALERTED_BUDGETS = "alerted_budgets"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_LANGUAGE      = "app_language"
        private const val KEY_ACCENT_COLOR  = "app_accent_color"
        private const val KEY_WIDGET_ORDER  = "dashboard_widget_order"
    }
}
