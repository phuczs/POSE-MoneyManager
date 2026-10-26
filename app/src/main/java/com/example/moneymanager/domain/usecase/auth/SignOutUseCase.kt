package com.example.moneymanager.domain.usecase.auth

import com.example.moneymanager.domain.repository.AuthRepository
import javax.inject.Inject

/** Signs out the current user and clears the auth session. */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.signOut()
}
