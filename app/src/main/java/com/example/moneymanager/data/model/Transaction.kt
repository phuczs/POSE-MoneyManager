@file:Suppress("NOTHING_TO_INLINE")
package com.example.moneymanager.data.model

/**
 * Backward-compatibility alias.
 *
 * The canonical Transaction type has moved to [com.example.moneymanager.domain.model.Transaction].
 * This alias ensures existing UI screens compile without changes during migration.
 *
 * TODO: Update all UI screen imports to use [com.example.moneymanager.domain.model.Transaction]
 *       and then delete this file.
 */
typealias Transaction = com.example.moneymanager.domain.model.Transaction