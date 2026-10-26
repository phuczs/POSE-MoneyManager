package com.example.moneymanager.data.mapper

import com.example.moneymanager.domain.model.Category

/**
 * Bidirectional mapper between Firestore document fields and [Category] (domain).
 */

/**
 * Converts a Firestore document field map to a domain [Category].
 * The Firestore document ID is passed separately since it is not part of the field map.
 */
fun Map<String, Any?>.toCategoryDomain(docId: String): Category = Category(
    id = docId,
    userId = this["userId"] as? String ?: "",
    name = this["name"] as? String ?: "",
    type = this["type"] as? String ?: "",
    parentId = this["parentId"] as? String,
    iconName = this["iconName"] as? String,
    colorHex = this["colorHex"] as? String
)

/**
 * Converts a domain [Category] to a Firestore-compatible map.
 */
fun Category.toFirestoreMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "name" to name,
    "type" to type,
    "parentId" to parentId,
    "iconName" to iconName,
    "colorHex" to colorHex
)
