package com.example.moneymanager

import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.util.HabitAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Date

/**
 * Unit tests for [HabitAnalyzer].
 *
 * All tests are pure JVM tests: no Android SDK, no MockK, no Robolectric needed.
 * Run with: ./gradlew test
 */
class HabitAnalyzerTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun makeTimestamp(daysAgo: Int = 0, hourOfDay: Int = 8): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun makeCoffeeExpense(daysAgo: Int = 0, hourOfDay: Int = 8): Transaction = Transaction(
        id = "tx_${daysAgo}_$hourOfDay",
        userId = "user1",
        amount = 5.0,
        type = "expense",
        category = "Food & Drink",
        description = "coffee",
        date = makeTimestamp(daysAgo, hourOfDay)
    )

    private fun makeIncomeTransaction(): Transaction = Transaction(
        id = "income_1",
        userId = "user1",
        amount = 1000.0,
        type = "income",
        category = "Salary",
        description = "monthly salary",
        date = makeTimestamp(daysAgo = 0, hourOfDay = 8)
    )

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `analyze returns null for empty list`() {
        val result = HabitAnalyzer.analyze(emptyList(), currentHour = 8)
        assertNull(result)
    }

    @Test
    fun `analyze returns null when only income transactions exist`() {
        val transactions = List(5) { makeIncomeTransaction() }
        val result = HabitAnalyzer.analyze(transactions, currentHour = 8)
        assertNull(result)
    }

    @Test
    fun `analyze returns suggestion when threshold is met with recent transactions`() {
        val transactions = listOf(
            makeCoffeeExpense(daysAgo = 1, hourOfDay = 8),
            makeCoffeeExpense(daysAgo = 2, hourOfDay = 8),
            makeCoffeeExpense(daysAgo = 3, hourOfDay = 8)
        )

        val result = HabitAnalyzer.analyze(transactions, currentHour = 8)

        assertNotNull(result)
        assertEquals("Food & Drink", result!!.category)
        assertEquals("Coffee", result.description) // Verify title-case conversion
        assertEquals(5.0, result.amount, 0.001)
    }

    @Test
    fun `analyze returns null when count is below threshold`() {
        val transactions = listOf(
            makeCoffeeExpense(daysAgo = 1, hourOfDay = 8),
            makeCoffeeExpense(daysAgo = 2, hourOfDay = 8)
            // Only 2 — below default minOccurrences of 3
        )

        val result = HabitAnalyzer.analyze(transactions, currentHour = 8)
        assertNull(result)
    }

    @Test
    fun `analyze returns null when all transactions are stale (older than staleDays)`() {
        val staleDays = 30
        val transactions = listOf(
            makeCoffeeExpense(daysAgo = staleDays + 1, hourOfDay = 8),
            makeCoffeeExpense(daysAgo = staleDays + 2, hourOfDay = 8),
            makeCoffeeExpense(daysAgo = staleDays + 3, hourOfDay = 8)
        )

        val result = HabitAnalyzer.analyze(transactions, currentHour = 8, staleDays = staleDays)
        assertNull(result)
    }

    @Test
    fun `analyze returns null when transactions are outside the time window`() {
        // currentHour = 8, timeWindowHours = 2: valid range is 6–10
        // These transactions all happened at 14:00 (outside the window)
        val transactions = listOf(
            makeCoffeeExpense(daysAgo = 1, hourOfDay = 14),
            makeCoffeeExpense(daysAgo = 2, hourOfDay = 14),
            makeCoffeeExpense(daysAgo = 3, hourOfDay = 14)
        )

        val result = HabitAnalyzer.analyze(transactions, currentHour = 8, timeWindowHours = 2)
        assertNull(result)
    }

    @Test
    fun `analyze returns suggestion when mixed transactions include qualifying pattern`() {
        val transactions = listOf(
            // 3 qualifying coffee expenses
            makeCoffeeExpense(daysAgo = 1, hourOfDay = 8),
            makeCoffeeExpense(daysAgo = 2, hourOfDay = 8),
            makeCoffeeExpense(daysAgo = 3, hourOfDay = 8),
            // 1 income transaction — should be ignored
            makeIncomeTransaction(),
            // 1 different expense — should not win
            Transaction(
                id = "other_1",
                userId = "user1",
                amount = 20.0,
                type = "expense",
                category = "Transport",
                description = "taxi",
                date = makeTimestamp(daysAgo = 1, hourOfDay = 8)
            )
        )

        val result = HabitAnalyzer.analyze(transactions, currentHour = 8)

        assertNotNull(result)
        assertEquals("Food & Drink", result!!.category)
    }

    @Test
    fun `analyze is case-insensitive when grouping descriptions`() {
        // "Coffee" and "coffee" and " Coffee " should all count as the same habit
        val makeTx = { desc: String -> Transaction(
            id = "tx_$desc",
            userId = "user1",
            amount = 5.0,
            type = "expense",
            category = "Food & Drink",
            description = desc,
            date = makeTimestamp(daysAgo = 1, hourOfDay = 8)
        )}

        val transactions = listOf(
            makeTx("Coffee"),
            makeTx("coffee"),
            makeTx(" Coffee ")
        )

        val result = HabitAnalyzer.analyze(transactions, currentHour = 8)
        assertNotNull("Should detect habit from case/whitespace variants", result)
    }
}
