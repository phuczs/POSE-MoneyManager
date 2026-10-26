package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.moneymanager.util.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val appLockManager: AppLockManager
) : ViewModel() {

    val isLockEnabled: StateFlow<Boolean> = appLockManager.isLockEnabled
    val lockTimeoutMinutes: StateFlow<Int> = appLockManager.lockTimeoutMinutes
    val isFlagSecureEnabled: StateFlow<Boolean> = appLockManager.isFlagSecureEnabled
    val isLocked: StateFlow<Boolean> = appLockManager.isLocked

    fun unlock() {
        appLockManager.unlock()
    }

    fun lockNow() {
        appLockManager.lockNow()
    }

    fun setLockEnabled(enabled: Boolean) {
        appLockManager.setLockEnabled(enabled)
    }

    fun setLockTimeoutMinutes(minutes: Int) {
        appLockManager.setLockTimeoutMinutes(minutes)
    }

    fun setFlagSecureEnabled(enabled: Boolean) {
        appLockManager.setFlagSecureEnabled(enabled)
    }
}
