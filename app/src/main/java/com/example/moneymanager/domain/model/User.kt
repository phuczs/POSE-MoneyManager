package com.example.moneymanager.domain.model

/**
 * Pure domain model for an authenticated user.
 *
 * Clean Architecture note:
 * - No Firebase User references; the data layer (FirebaseAuthRepositoryImpl)
 *   handles the mapping from FirebaseUser to this domain model.
 * - [photoUrl] is nullable since users may not have a profile photo.
 */
data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
