package com.example.moneymanager.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    var photoUrl: String? = null,
    val phoneNumber: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)