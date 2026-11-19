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
 * Persists dismissed proactive coach nudge IDs (day-scoped) and notification anti-spam timestamps.
 */
@Singleton
class CoachPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("proactive_coach_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DISMISSED_NUDGES = "dismissed_nudge_ids"
        private const val KEY_LAST_COACH_PUSH_MS = "last_coach_push_ms"
        private const val KEY_LAST_DIGEST_PUSH_MS = "last_digest_push_ms"
        private const val MAX_STORED_DISMISSED = 30
    }

    private val _dismissedNudgeIds = MutableStateFlow(loadDismissedNudges())
    val dismissedNudgeIds: StateFlow<Set<String>> = _dismissedNudgeIds.asStateFlow()

    private fun loadDismissedNudges(): Set<String> =
        prefs.getStringSet(KEY_DISMISSED_NUDGES, emptySet())?.toSet() ?: emptySet()

    fun dismissNudge(nudgeId: String) {
        if (nudgeId.isBlank()) return
        val updated = (_dismissedNudgeIds.value + nudgeId).toList().takeLast(MAX_STORED_DISMISSED).toSet()
        _dismissedNudgeIds.value = updated
        prefs.edit().putStringSet(KEY_DISMISSED_NUDGES, updated).apply()
    }

    fun getLastCoachPushMillis(): Long = prefs.getLong(KEY_LAST_COACH_PUSH_MS, 0L)

    fun recordCoachPush(nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_COACH_PUSH_MS, nowMillis).apply()
    }

    fun getLastDigestPushMillis(): Long = prefs.getLong(KEY_LAST_DIGEST_PUSH_MS, 0L)

    fun recordDigestPush(nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_DIGEST_PUSH_MS, nowMillis).apply()
    }
}
