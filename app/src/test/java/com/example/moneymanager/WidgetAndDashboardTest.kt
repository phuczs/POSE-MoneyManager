package com.example.moneymanager

import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.widget.MoneyManagerWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class WidgetAndDashboardTest {

    @Test
    fun testWidgetConstants() {
        assertEquals("add_transaction", MoneyManagerWidgetProvider.NAV_ADD_TRANSACTION)
        assertEquals("chat", MoneyManagerWidgetProvider.NAV_CHAT)
        assertEquals("navigate_to", MoneyManagerWidgetProvider.EXTRA_NAVIGATE_TO)
        assertEquals("com.example.moneymanager.ACTION_REFRESH_WIDGET", MoneyManagerWidgetProvider.ACTION_REFRESH_WIDGET)
    }

    @Test
    fun testMonthlyVsAllTimeBalanceCalculation() {
        val curYear = 2026
        val curMonth = 10

        val txs = listOf(
            // Current month (Tháng 10/2026)
            Transaction(id = "1", type = "income", amount = 10_000_000.0, year = curYear, month = curMonth),
            Transaction(id = "2", type = "expense", amount = 3_000_000.0, year = curYear, month = curMonth),
            // Previous month (Tháng 9/2026)
            Transaction(id = "3", type = "income", amount = 15_000_000.0, year = curYear, month = 9),
            Transaction(id = "4", type = "expense", amount = 8_000_000.0, year = curYear, month = 9)
        )

        // 1. All-time calculations
        val allIncome = txs.filter { it.type == "income" }.sumOf { it.amount }
        val allExpense = txs.filter { it.type == "expense" }.sumOf { it.amount }
        val allBalance = allIncome - allExpense

        assertEquals(25_000_000.0, allIncome, 0.01)
        assertEquals(11_000_000.0, allExpense, 0.01)
        assertEquals(14_000_000.0, allBalance, 0.01)

        // 2. Monthly calculations
        val monthlyTxs = txs.filter { it.year == curYear && it.month == curMonth }
        val monthIncome = monthlyTxs.filter { it.type == "income" }.sumOf { it.amount }
        val monthExpense = monthlyTxs.filter { it.type == "expense" }.sumOf { it.amount }
        val monthBalance = monthIncome - monthExpense

        assertEquals(10_000_000.0, monthIncome, 0.01)
        assertEquals(3_000_000.0, monthExpense, 0.01)
        assertEquals(7_000_000.0, monthBalance, 0.01)
    }
}
