package com.example.moneymanager.util

import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.ui.viewmodel.HabitualSuggestion
import java.util.Calendar
import java.util.Locale

/**
 * Pure, stateless utility for detecting habitual spending patterns.
 *
 * Kept completely separate from Android framework classes (no Context, no ViewModel)
 * so it can be unit-tested without Robolectric or Android test runners.
 *
 * Clean Architecture note:
 * - Uses the domain [Transaction] model (epoch-ms Long for dates).
 *
 * Algorithm:
 * 1. Filter to expense transactions only.
 * 2. Retain only those that occurred within a [timeWindowHours] radius of [currentHour].
 * 3. Group by (category, normalized description, amount).
 * 4. Surface the most frequent group if it meets [minOccurrences] and happened within [staleDays].
 */
object HabitAnalyzer {

    private const val DEFAULT_TIME_WINDOW_HOURS = 2
    private const val DEFAULT_MIN_OCCURRENCES = 3
    private const val DEFAULT_STALE_DAYS = 30

    /**
     * @param transactions Full list of the user's cached transactions.
     * @param currentHour  Hour-of-day (0–23) to check for patterns. Defaults to now.
     * @param timeWindowHours  How far either side of [currentHour] to look (±hours).
     * @param minOccurrences   Minimum repetitions before a habit is surfaced.
     * @param staleDays        Ignore habits whose last occurrence is older than this.
     * @return A [HabitualSuggestion] if a habit is detected, or null otherwise.
     */
    fun analyze(
        transactions: List<Transaction>,
        currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        timeWindowHours: Int = DEFAULT_TIME_WINDOW_HOURS,
        minOccurrences: Int = DEFAULT_MIN_OCCURRENCES,
        staleDays: Int = DEFAULT_STALE_DAYS
    ): HabitualSuggestion? {
        if (transactions.isEmpty()) return null

        val expenses = transactions.filter { it.type == "expense" }
        if (expenses.isEmpty()) return null

        val timeWindowTxs = expenses.filter { tx ->
            val txHour = Calendar.getInstance()
                .also { it.timeInMillis = tx.date } // epoch ms — no Timestamp conversion needed
                .get(Calendar.HOUR_OF_DAY)
            val diff = Math.abs(txHour - currentHour)
            val minDiff = Math.min(diff, 24 - diff) // Handle midnight wrap-around
            minDiff <= timeWindowHours
        }

        if (timeWindowTxs.isEmpty()) return null

        val groups = timeWindowTxs.groupBy { tx ->
            Triple(tx.category, tx.description.trim().lowercase(Locale.getDefault()), tx.amount)
        }

        val mostFrequent = groups.maxByOrNull { it.value.size } ?: return null

        if (mostFrequent.value.size < minOccurrences) return null

        val (category, description, amount) = mostFrequent.key
        if (description.isBlank()) return null

        // Staleness guard: ignore habits not seen recently
        val cutoffMs = Calendar.getInstance()
            .also { it.add(Calendar.DAY_OF_YEAR, -staleDays) }
            .timeInMillis
        val happenedRecently = mostFrequent.value.any { it.date > cutoffMs }
        if (!happenedRecently) return null

        val displayDesc = description.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        return HabitualSuggestion(
            category = category,
            description = displayDesc,
            amount = amount
        )
    }
}
