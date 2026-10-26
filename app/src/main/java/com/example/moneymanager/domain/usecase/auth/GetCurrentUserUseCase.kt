package com.example.moneymanager.domain.usecase.auth

import com.example.moneymanager.domain.model.User
import com.example.moneymanager.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Returns a reactive stream of the currently authenticated user (null when signed out). */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<User?> = authRepository.currentUser

    val isAuthenticated: Flow<Boolean> = authRepository.isUserAuthenticated
}
