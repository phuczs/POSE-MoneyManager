package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.model.User
import com.example.moneymanager.data.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInClient: GoogleSignInClient
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser = authRepository.currentUser
    val isAuthenticated = authRepository.isUserAuthenticated

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signIn(email, password)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Authentication failed") }
            )
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signUp(email, password)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Registration failed") }
            )
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.resetPassword(email)
            _authState.value = result.fold(
                onSuccess = { AuthState.PasswordResetSent },
                onFailure = { AuthState.Error(it.message ?: "Password reset failed") }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            // Sign out from Google as well
            googleSignInClient.signOut()
            val result = authRepository.signOut()
            _authState.value = result.fold(
                onSuccess = { AuthState.SignedOut },
                onFailure = { AuthState.Error(it.message ?: "Sign out failed") }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signInWithGoogle(idToken)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Google sign-in failed") }
            )
        }
    }

    fun signInWithGoogleAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signInWithGoogleAccount(account)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Google sign-in failed") }
            )
        }
    }

    fun getGoogleSignInClient() = googleSignInClient

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    // Profile management methods
    fun updateDisplayName(displayName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.updateDisplayName(displayName)
            _authState.value = result.fold(
                onSuccess = { AuthState.ProfileUpdated },
                onFailure = { AuthState.Error(it.message ?: "Failed to update display name") }
            )
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.updatePassword(currentPassword, newPassword)
            _authState.value = result.fold(
                onSuccess = { AuthState.ProfileUpdated },
                onFailure = { AuthState.Error(it.message ?: "Failed to update password") }
            )
        }
    }

    fun updateProfilePhoto(photoUrl: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.updateProfilePhoto(photoUrl)
            _authState.value = result.fold(
                onSuccess = { AuthState.ProfileUpdated },
                onFailure = { AuthState.Error(it.message ?: "Failed to update profile photo") }
            )
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.deleteAccount()
            _authState.value = result.fold(
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