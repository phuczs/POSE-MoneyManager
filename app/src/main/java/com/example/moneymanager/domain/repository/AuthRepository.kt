package com.example.moneymanager.domain.repository

import com.example.moneymanager.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for authentication operations.
 *
 * Clean Architecture note:
 * - This interface is completely free of Firebase or Google SDK types.
 * - [signInWithGoogle] accepts a plain [String] idToken; the UI layer
 *   (Activity/ViewModel) is responsible for extracting the token from
 *   the GoogleSignInAccount object before calling this method.
 *
 * Security note:
 * - [currentUser] emits null when unauthenticated — all UI flows must
 *   gate on this before accessing any user data.
 * - [deleteAccount] requires a prior re-authentication for sensitive actions
 *   (enforced in the implementation layer).
 */
interface AuthRepository {
    /** Reactive stream of the currently authenticated user. Emits null when signed out. */
    val currentUser: Flow<User?>

    /** Convenience stream derived from [currentUser]. */
    val isUserAuthenticated: Flow<Boolean>

    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String): Result<User>

    /**
     * Sign in with a Google ID token (extracted from GoogleSignInAccount.idToken).
     * The caller must obtain this token from the Google Sign-In flow in the Activity.
     */
    suspend fun signInWithGoogle(idToken: String): Result<User>

    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun signOut(): Result<Unit>

    // Profile management
    suspend fun updateDisplayName(displayName: String): Result<Unit>
    suspend fun updatePhoneNumber(phoneNumber: String): Result<Unit>
    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun updateProfilePhoto(photoUrl: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}
