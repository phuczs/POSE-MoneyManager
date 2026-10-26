package com.example.moneymanager.data.repository

import android.util.Log
import com.example.moneymanager.domain.model.User
import com.example.moneymanager.domain.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of [AuthRepository].
 *
 * Clean Architecture note:
 * - This class handles GoogleSignInAccount in [signInWithGoogle] via the
 *   caller-provided [idToken] — no Google SDK types leak into the domain interface.
 * - [currentUser] is a cold Flow backed by [FirebaseAuth.AuthStateListener].
 *
 * Security note:
 * - [signIn] catches specific Firebase exceptions and maps them to generic messages
 *   to avoid leaking internal auth infrastructure details to callers.
 * - [updatePassword] requires re-authentication before the password change.
 * - [deleteAccount] requires the user to be currently authenticated.
 */
@Singleton
class FirebaseAuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toDomain())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val isUserAuthenticated: Flow<Boolean> = currentUser.map { it != null }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.toDomain()?.let { Result.success(it) }
                ?: Result.failure(Exception("Authentication failed"))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("No account found with this email"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Invalid email or password"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.toDomain()?.let { Result.success(it) }
                ?: Result.failure(Exception("Registration failed"))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("An account with this email already exists"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.toDomain()?.let { Result.success(it) }
                ?: Result.failure(Exception("Google sign-in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("No account found with this email"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            user.updateProfile(userProfileChangeRequest { this.displayName = displayName }).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePhoneNumber(phoneNumber: String): Result<Unit> {
        // Phone number update requires a full phone verification flow (OTP).
        // Returning a descriptive failure rather than silently no-op-ing.
        return Result.failure(Exception("Phone number update requires phone verification flow"))
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val email = user.email ?: return Result.failure(Exception("Email not available"))
            // Security: Re-authenticate before sensitive operation
            user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfilePhoto(photoUrl: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            user.updateProfile(userProfileChangeRequest {
                this.photoUri = android.net.Uri.parse(photoUrl)
            }).await()
            user.reload().await()
            Log.d(TAG, "Profile photo updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Maps a [com.google.firebase.auth.FirebaseUser] to a domain [User]. */
    private fun com.google.firebase.auth.FirebaseUser.toDomain() = User(
        id = uid,
        email = email ?: "",
        displayName = displayName ?: "",
        photoUrl = photoUrl?.toString(),
        phoneNumber = phoneNumber,
        createdAt = metadata?.creationTimestamp ?: System.currentTimeMillis(),
        lastLoginAt = metadata?.lastSignInTimestamp ?: System.currentTimeMillis()
    )

    companion object {
        private const val TAG = "FirebaseAuthRepoImpl"
    }
}
