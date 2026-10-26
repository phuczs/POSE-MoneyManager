package com.example.moneymanager.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Domain contract for persistent application settings.
 *
 * Clean Architecture note:
 * - Previously [SettingsRepository] was a concrete class injected directly
 *   into ViewModels. Extracting this interface allows the domain layer to
 *   depend on the contract, not the SharedPreferences implementation.
 * - All settings are exposed as [StateFlow] for reactive UI binding.
 */
interface SettingsRepository {

    // ─── Notifications ───────────────────────────────────────────────────────
    val isNotificationsEnabled: StateFlow<Boolean>
    fun setNotificationsEnabled(enabled: Boolean)

    /** Session-scoped budget alert tracking (prevents notification spam). */
    fun hasAlerted(budgetId: String): Boolean
    fun setAlerted(budgetId: String)

    // ─── Language ─────────────────────────────────────────────────────────────
    /** BCP-47 language code, e.g. "vi" or "en". */
    val currentLanguage: StateFlow<String>
    fun setLanguage(languageCode: String)

    // ─── Customization ────────────────────────────────────────────────────────
    /** Accent color as a hex string, e.g. "#FF6B35". Empty means use theme default. */
    val accentColorHex: StateFlow<String>
    fun setAccentColorHex(hexCode: String)

    /** Ordered list of widget identifiers for the dashboard. */
    val dashboardWidgetsOrder: StateFlow<List<String>>
    fun setDashboardWidgetsOrder(order: List<String>)
}
