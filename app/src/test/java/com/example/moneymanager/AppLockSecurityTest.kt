package com.example.moneymanager

import android.content.Context
import android.content.SharedPreferences
import com.example.moneymanager.ui.viewmodel.SecurityViewModel
import com.example.moneymanager.util.AppLockManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockSecurityTest {

    private class FakeEditor(private val storage: MutableMap<String, Any>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null && value != null) temp[key] = value
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) temp.remove(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            temp.clear()
            return this
        }
        override fun commit(): Boolean {
            storage.putAll(temp)
            return true
        }
        override fun apply() {
            storage.putAll(temp)
        }
    }

    private class FakeSharedPreferences : SharedPreferences {
        val storage = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = storage
        override fun getString(key: String?, defValue: String?): String? = storage[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = null
        override fun getInt(key: String?, defValue: Int): Int = storage[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = storage[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = storage[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = storage[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = storage.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(storage)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var mockContext: Context
    private lateinit var lockManager: AppLockManager
    private lateinit var securityViewModel: SecurityViewModel

    @Before
    fun setup() {
        fakePrefs = FakeSharedPreferences()
        mockContext = mockk(relaxed = true)
        every { mockContext.getSharedPreferences(any(), any()) } returns fakePrefs

        lockManager = AppLockManager(mockContext)
        securityViewModel = SecurityViewModel(lockManager)
    }

    @Test
    fun `initial state defaults to unlocked and disabled`() {
        assertFalse(lockManager.isLockEnabled.value)
        assertFalse(lockManager.isLocked.value)
        assertEquals(0, lockManager.lockTimeoutMinutes.value)
        assertFalse(lockManager.isFlagSecureEnabled.value)
    }

    @Test
    fun `enable lock updates state and preferences`() {
        securityViewModel.setLockEnabled(true)
        assertTrue(lockManager.isLockEnabled.value)
        assertTrue(fakePrefs.getBoolean("app_lock_enabled", false))

        securityViewModel.setLockEnabled(false)
        assertFalse(lockManager.isLockEnabled.value)
        assertFalse(lockManager.isLocked.value)
    }

    @Test
    fun `lockNow locks application when lock is enabled`() {
        securityViewModel.setLockEnabled(true)
        securityViewModel.lockNow()
        assertTrue(securityViewModel.isLocked.value)

        securityViewModel.unlock()
        assertFalse(securityViewModel.isLocked.value)
    }

    @Test
    fun `lockNow does nothing when lock is disabled`() {
        securityViewModel.setLockEnabled(false)
        securityViewModel.lockNow()
        assertFalse(securityViewModel.isLocked.value)
    }

    @Test
    fun `setTimeout updates state and preferences`() {
        securityViewModel.setLockTimeoutMinutes(5)
        assertEquals(5, lockManager.lockTimeoutMinutes.value)
        assertEquals(5, fakePrefs.getInt("lock_timeout_minutes", 0))
    }

    @Test
    fun `setFlagSecure updates state and preferences`() {
        securityViewModel.setFlagSecureEnabled(true)
        assertTrue(lockManager.isFlagSecureEnabled.value)
        assertTrue(fakePrefs.getBoolean("flag_secure_enabled", false))

        securityViewModel.setFlagSecureEnabled(false)
        assertFalse(lockManager.isFlagSecureEnabled.value)
    }
}
