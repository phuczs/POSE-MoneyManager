package com.example.moneymanager.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists transaction IDs that the user has reviewed and marked as legitimate
 * so Anomaly Detective does not repeatedly flag dismissed transactions.
 */
@Singleton
class AnomalyPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("anomaly_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DISMISSED_IDS = "dismissed_anomaly_ids"
    }

    private val _dismissedIds = MutableStateFlow(loadDismissedIds())
    val dismissedIds: StateFlow<Set<String>> = _dismissedIds.asStateFlow()

    private fun loadDismissedIds(): Set<String> {
        return prefs.getStringSet(KEY_DISMISSED_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun dismiss(transactionId: String) {
        if (transactionId.isBlank()) return
        val updated = _dismissedIds.value + transactionId
        _dismissedIds.value = updated
        prefs.edit().putStringSet(KEY_DISMISSED_IDS, updated).apply()
    }

    fun clearAll() {
        _dismissedIds.value = emptySet()
        prefs.edit().remove(KEY_DISMISSED_IDS).apply()
    }
}
