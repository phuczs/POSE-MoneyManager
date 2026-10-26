package com.example.moneymanager.domain.model

/**
 * Pure domain model for a transaction category.
 *
 * Clean Architecture note:
 * - No Firebase [DocumentId] annotation — the data layer mapper sets [id]
 *   from the Firestore document ID field.
 */
data class Category(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val type: String = "",          // "income" or "expense"
    val parentId: String? = null,   // null = top-level category
    val iconName: String? = null,   // emoji character or identifier
    val colorHex: String? = null    // e.g. "#FF0000"
)
