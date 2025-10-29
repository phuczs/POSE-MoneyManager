package com.example.moneymanager.data.repository


import android.util.Log
import com.example.moneymanager.data.model.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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
import androidx.core.net.toUri

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthRepository {
    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.let { firebaseUser ->
                User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    phoneNumber = firebaseUser.phoneNumber,
                    createdAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                    lastLoginAt = firebaseUser.metadata?.lastSignInTimestamp ?: System.currentTimeMillis()
                )
            })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val isUserAuthenticated: Flow<Boolean> = currentUser.map { it != null }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Result.success(User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    phoneNumber = firebaseUser.phoneNumber,
                    createdAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                    lastLoginAt = firebaseUser.metadata?.lastSignInTimestamp ?: System.currentTimeMillis()
                ))
            } else {
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("User not found"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Invalid credentials"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Result.success(User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    phoneNumber = firebaseUser.phoneNumber,
                    createdAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                    lastLoginAt = firebaseUser.metadata?.lastSignInTimestamp ?: System.currentTimeMillis()
                ))
            } else {
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Email already in use"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Result.success(User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    phoneNumber = firebaseUser.phoneNumber,
                    createdAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                    lastLoginAt = firebaseUser.metadata?.lastSignInTimestamp ?: System.currentTimeMillis()
                ))
            } else {
                Result.failure(Exception("Google sign-in failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogleAccount(account: GoogleSignInAccount): Result<User> {
        return try {
            val idToken = account.idToken
            if (idToken != null) {
                // Use the standard ID token method if available
                signInWithGoogle(idToken)
            } else {
                // Fallback: Create credential with Google account info
                // Note: This is a simplified approach for when ID token is not available
                // In production, you should ensure proper OAuth setup
                Result.failure(Exception("Google Sign-In requires proper OAuth configuration. Please complete Firebase setup."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Google Sign-In error: ${e.message}. Please check your Firebase configuration."))
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Email not found"))
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

    // Profile management methods
    override suspend fun updateDisplayName(displayName: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val profileUpdates = userProfileChangeRequest {
                this.displayName = displayName
            }
            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePhoneNumber(phoneNumber: String): Result<Unit> {
        return try {
            // Note: Phone number update requires phone auth credential
            // This is a placeholder - actual implementation would require phone verification
            Result.failure(Exception("Phone number update requires phone verification flow"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val email = user.email ?: return Result.failure(Exception("Email not available"))
            
            // Re-authenticate user first
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            
            // Update password
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfilePhoto(photoUrl: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val profileUpdates = userProfileChangeRequest {
                this.photoUri = android.net.Uri.parse(photoUrl)
            }
            user.updateProfile(profileUpdates).await()
            user.reload().await()
            Log.d("ProfilePhoto", "Saved photoUrl: ${user.photoUrl}")
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
}

