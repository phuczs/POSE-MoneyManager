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
}