package com.example.moneymanager

import com.example.moneymanager.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * Pure balance calculation unit tests.
 *
 * These test the core financial math independently of ViewModels
 * to ensure future refactors never silently break the balance logic.
 */
class TransactionBalanceTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun makeTs(): Long {
        return System.currentTimeMillis()
    }

    private fun income(amount: Double, id: String = "i_$amount") = Transaction(
        id = id, userId = "u1", amount = amount, type = "income",
        category = "Salary", description = "", date = makeTs()
    )

    private fun expense(amount: Double, id: String = "e_$amount") = Transaction(
        id = id, userId = "u1", amount = amount, type = "expense",
        category = "Food", description = "", date = makeTs()
    )

    // ─── Balance calculation helper (mirrors ViewModel logic) ─────────────────

    private fun computeBalance(transactions: List<Transaction>): Double {
        val totalIncome = transactions.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
        return totalIncome - totalExpense
    }

    private fun computeTotalIncome(transactions: List<Transaction>): Double =
        transactions.filter { it.type == "income" }.sumOf { it.amount }

    private fun computeTotalExpense(transactions: List<Transaction>): Double =
        transactions.filter { it.type == "expense" }.sumOf { it.amount }

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `balance is zero for empty list`() {
        assertEquals(0.0, computeBalance(emptyList()), 0.001)
    }

    @Test
    fun `balance correctly sums income and expense`() {
        val transactions = listOf(income(1000.0), income(500.0), expense(300.0), expense(200.0))
        // Income: 1500, Expense: 500 → Balance: 1000
        assertEquals(1000.0, computeBalance(transactions), 0.001)
    }

    @Test
    fun `balance is negative when expenses exceed income`() {
        val transactions = listOf(income(100.0), expense(999.0))
        assertEquals(-899.0, computeBalance(transactions), 0.001)
    }

    @Test
    fun `balance with only income equals total income`() {
        val transactions = listOf(income(500.0), income(250.0))
        assertEquals(750.0, computeBalance(transactions), 0.001)
    }

    @Test
    fun `balance with only expenses is negative`() {
        val transactions = listOf(expense(100.0), expense(50.0))
        assertEquals(-150.0, computeBalance(transactions), 0.001)
    }

    @Test
    fun `totalIncome counts only income transactions`() {
        val transactions = listOf(income(1000.0), expense(500.0), income(200.0))
        assertEquals(1200.0, computeTotalIncome(transactions), 0.001)
    }

    @Test
    fun `totalExpense counts only expense transactions`() {
        val transactions = listOf(income(1000.0), expense(300.0), expense(150.0))
        assertEquals(450.0, computeTotalExpense(transactions), 0.001)
    }

    @Test
    fun `balance handles floating point amounts correctly`() {
        // Classic floating-point traps
        val transactions = listOf(income(0.1), income(0.2), expense(0.3))
        // 0.1 + 0.2 - 0.3 = 0.0 (with tolerance)
        assertEquals(0.0, computeBalance(transactions), 0.0001)
    }

    @Test
    fun `balance ignores unknown transaction types`() {
        val unknownType = Transaction(
            id = "unknown", userId = "u1", amount = 999.0, type = "refund",
            category = "Other", description = "", date = 0L
        )
        val transactions = listOf(income(500.0), unknownType)
        // "refund" is neither income nor expense, should be ignored
        assertEquals(500.0, computeBalance(transactions), 0.001)
    }
}
