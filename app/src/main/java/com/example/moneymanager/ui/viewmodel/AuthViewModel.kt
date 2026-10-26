package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.domain.model.User
import com.example.moneymanager.domain.repository.SettingsRepository
import com.example.moneymanager.domain.usecase.auth.GetCurrentUserUseCase
import com.example.moneymanager.domain.usecase.auth.SignInUseCase
import com.example.moneymanager.domain.usecase.auth.SignOutUseCase
import com.example.moneymanager.domain.usecase.auth.SignUpUseCase
import com.example.moneymanager.domain.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val authRepository: AuthRepository,
    private val googleSignInClient: GoogleSignInClient,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser = getCurrentUserUseCase()
    val isAuthenticated = getCurrentUserUseCase.isAuthenticated

    // ─── Settings Delegation ──────────────────────────────────────────────────
    val isNotificationsEnabled = settingsRepository.isNotificationsEnabled
    val currentLanguage = settingsRepository.currentLanguage
    val accentColorHex = settingsRepository.accentColorHex
    val dashboardWidgetsOrder = settingsRepository.dashboardWidgetsOrder

    fun toggleNotifications(enabled: Boolean) = settingsRepository.setNotificationsEnabled(enabled)
    fun setAppLanguage(code: String) = settingsRepository.setLanguage(code)
    fun setAccentColorHex(hex: String) = settingsRepository.setAccentColorHex(hex)
    fun setDashboardWidgetsOrder(order: List<String>) = settingsRepository.setDashboardWidgetsOrder(order)

    // ─── Auth Operations ──────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _authState.value = signInUseCase(email, password).fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Authentication failed") }
            )
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _authState.value = signUpUseCase(email, password).fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Registration failed") }
            )
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _authState.value = authRepository.resetPassword(email).fold(
                onSuccess = { AuthState.PasswordResetSent },
                onFailure = { AuthState.Error(it.message ?: "Password reset failed") }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            googleSignInClient.signOut()
            _authState.value = signOutUseCase().fold(
                onSuccess = { AuthState.SignedOut },
                onFailure = { AuthState.Error(it.message ?: "Sign out failed") }
            )
        }
    }

    /**
     * Signs in with a Google ID token.
     * The caller (Activity) is responsible for extracting the token from GoogleSignInAccount.
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _authState.value = authRepository.signInWithGoogle(idToken).fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Google sign-in failed") }
            )
        }
    }

    /**
     * UI-layer bridge: extracts idToken from [GoogleSignInAccount] and delegates to [signInWithGoogle].
     * This keeps [AuthRepository] free of Google SDK types while the UI layer handles the extraction.
     */
    fun signInWithGoogleAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        val idToken = account.idToken
        if (idToken != null) {
            signInWithGoogle(idToken)
        } else {
            _authState.value = AuthState.Error("Google Sign-In failed: no ID token received")
        }
    }

    fun getGoogleSignInClient() = googleSignInClient

    fun resetState() { _authState.value = AuthState.Idle }

    // ─── Profile Operations ───────────────────────────────────────────────────

    fun updateDisplayName(displayName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _authState.value = authRepository.updateDisplayName(displayName).fold(
                onSuccess = { AuthState.ProfileUpdated },
                onFailure = { AuthState.Error(it.message ?: "Failed to update display name") }
            )
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _authState.value = authRepository.updatePassword(currentPassword, newPassword).fold(
                onSuccess = { AuthState.ProfileUpdated },
                onFailure = { AuthState.Error(it.message ?: "Failed to update password") }
            )
        }
    }

    fun updateProfilePhoto(photoUrl: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _authState.value = authRepository.updateProfilePhoto(photoUrl).fold(
                onSuccess = { AuthState.ProfileUpdated },
                onFailure = { AuthState.Error(it.message ?: "Failed to update profile photo") }
            )
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _authState.value = authRepository.deleteAccount().fold(
                onSuccess = { AuthState.AccountDeleted },
                onFailure = { AuthState.Error(it.message ?: "Failed to delete account") }
            )
        }
    }

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val user: User) : AuthState()
        data class Error(val message: String) : AuthState()
        object PasswordResetSent : AuthState()
        object SignedOut : AuthState()
        object ProfileUpdated : AuthState()
        object AccountDeleted : AuthState()
    }
}