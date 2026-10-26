package com.example.moneymanager.domain.usecase.auth

import com.example.moneymanager.domain.model.User
import com.example.moneymanager.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Registers a new user with email/password.
 *
 * Security note:
 * - Password length validation (minimum 6 chars) enforced here in domain,
 *   in addition to any Firebase-level restrictions.
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Email cannot be blank"))
        if (password.length < 6) return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        return authRepository.signUp(email.trim(), password)
    }
}
