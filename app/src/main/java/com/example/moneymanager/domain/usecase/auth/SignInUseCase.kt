package com.example.moneymanager.domain.usecase.auth

import com.example.moneymanager.domain.model.User
import com.example.moneymanager.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Signs in a user with email/password credentials.
 *
 * Security note:
 * - Input validation here keeps it in the domain layer, not scattered across ViewModels.
 * - The password is never logged.
 */
class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Email cannot be blank"))
        if (password.isBlank()) return Result.failure(IllegalArgumentException("Password cannot be blank"))
        return authRepository.signIn(email.trim(), password)
    }
}
