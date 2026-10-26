package com.example.moneymanager

import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.util.StatisticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StatisticsCalculatorTest {

    @Test
    fun `formatCompactNumber formats numbers under 1000 without suffix`() {
        val result = StatisticsCalculator.formatCompactNumber(450.0)
        assertEquals("450", result)
    }

    @Test
    fun `formatCompactNumber formats thousands with K suffix`() {
        val result = StatisticsCalculator.formatCompactNumber(1500.0)
        assertTrue("Expected 1.5K or localized equivalent", result.endsWith("K") && result.contains("1"))
    }

    @Test
    fun `formatCompactNumber formats millions with M suffix`() {
        val result = StatisticsCalculator.formatCompactNumber(2500000.0)
        assertTrue("Expected 2.5M or localized equivalent", result.endsWith("M") && result.contains("2"))
    }

    @Test
    fun `filterTransactions only returns matching type`() {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

        val transactions = listOf(
            Transaction(id = "1", amount = 100.0, type = "income", date = now, month = currentMonth, year = currentYear),
            Transaction(id = "2", amount = 50.0, type = "expense", date = now, month = currentMonth, year = currentYear),
            Transaction(id = "3", amount = 200.0, type = "expense", date = now, month = currentMonth, year = currentYear)
        )

        val expenseResult = StatisticsCalculator.filterTransactions(transactions, period = "Month", type = "expense")
        assertEquals(2, expenseResult.size)
        assertTrue(expenseResult.all { it.type == "expense" })

        val incomeResult = StatisticsCalculator.filterTransactions(transactions, period = "Month", type = "income")
        assertEquals(1, incomeResult.size)
        assertEquals("1", incomeResult[0].id)
    }

    @Test
    fun `prepareTrendData returns 7 entries for Week period`() {
        val trend = StatisticsCalculator.prepareTrendData(emptyList(), period = "Week", type = "expense")
        assertEquals(7, trend.size)
    }

    @Test
    fun `prepareTrendData returns 4 entries for Month period`() {
        val trend = StatisticsCalculator.prepareTrendData(emptyList(), period = "Month", type = "expense")
        assertEquals(4, trend.size)
        assertEquals("W1", trend[0].first)
    }

    @Test
    fun `prepareTrendData returns 12 entries for Year period`() {
        val trend = StatisticsCalculator.prepareTrendData(emptyList(), period = "Year", type = "expense")
        assertEquals(12, trend.size)
    }

    @Test
    fun `getChartColor returns colors and cycles through palette`() {
        val color0 = StatisticsCalculator.getChartColor(0)
        val color7 = StatisticsCalculator.getChartColor(7) // 7 % 7 == 0
        assertNotNull(color0)
        assertEquals(color0, color7)
    }
}
