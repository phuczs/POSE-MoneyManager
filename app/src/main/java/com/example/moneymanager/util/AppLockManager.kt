package com.example.moneymanager.util

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {

    companion object {
        private const val PREFS_NAME = "security_settings"
        private const val KEY_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_LOCK_TIMEOUT = "lock_timeout_minutes"
        private const val KEY_FLAG_SECURE = "flag_secure_enabled"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_LOCK_ENABLED, false))
    val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    private val _lockTimeoutMinutes = MutableStateFlow(prefs.getInt(KEY_LOCK_TIMEOUT, 0))
    val lockTimeoutMinutes: StateFlow<Int> = _lockTimeoutMinutes.asStateFlow()

    private val _isFlagSecureEnabled = MutableStateFlow(prefs.getBoolean(KEY_FLAG_SECURE, false))
    val isFlagSecureEnabled: StateFlow<Boolean> = _isFlagSecureEnabled.asStateFlow()

    private val _isLocked = MutableStateFlow(prefs.getBoolean(KEY_LOCK_ENABLED, false))
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L

    init {
        // Observe app-level process lifecycle
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (_isLockEnabled.value) {
            lastBackgroundTimestamp = System.currentTimeMillis()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (_isLockEnabled.value && lastBackgroundTimestamp > 0) {
            val elapsedMillis = System.currentTimeMillis() - lastBackgroundTimestamp
            val timeoutMillis = _lockTimeoutMinutes.value * 60 * 1000L
            if (elapsedMillis >= timeoutMillis) {
                _isLocked.value = true
            }
        }
    }

    fun unlock() {
        _isLocked.value = false
        lastBackgroundTimestamp = 0L
    }

    fun lockNow() {
        if (_isLockEnabled.value) {
            _isLocked.value = true
        }
    }

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
        _isLockEnabled.value = enabled
        if (!enabled) {
            _isLocked.value = false
        }
    }

    fun setLockTimeoutMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_LOCK_TIMEOUT, minutes).apply()
        _lockTimeoutMinutes.value = minutes
    }

    fun setFlagSecureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FLAG_SECURE, enabled).apply()
        _isFlagSecureEnabled.value = enabled
    }
}
