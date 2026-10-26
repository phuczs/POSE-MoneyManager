package com.example.moneymanager.util

import java.util.Date

/**
 * Extension property to convert an epoch-millisecond [Long] to a [Date].
 *
 * This bridges the gap between the new domain model (which uses Long for dates)
 * and UI screens that still call `.date.toDate()` — a pattern from the old
 * Firebase Timestamp API.
 *
 * Migration note:
 * - UI screens should eventually call `Date(transaction.date)` directly,
 *   at which point this extension can be removed.
 */
val Long.toDate: Date
    get() = Date(this)

/**
 * Extension function variant for screens that call `.toDate()` with parentheses.
 */
fun Long.toDate(): Date = Date(this)
