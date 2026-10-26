package com.example.moneymanager.util

import androidx.compose.ui.graphics.Color
import com.example.moneymanager.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

/**
 * Utility containing calculation and aggregation algorithms for financial statistics.
 * Extracted from StatisticsScreen to separate domain/calculation logic from Jetpack Compose UI.
 */
object StatisticsCalculator {

    /**
     * Formats a large number into compact notation (e.g. 1.5K, 2.3M).
     */
    fun formatCompactNumber(number: Double): String {
        if (number < 1000) return String.format(Locale.getDefault(), "%.0f", number)
        val exp = (ln(number) / ln(1000.0)).toInt()
        val char = "KMGTPE"[exp - 1]
        return String.format(Locale.getDefault(), "%.1f%c", number / 1000.0.pow(exp.toDouble()), char)
    }

    /**
     * Filters transactions by time period ("Week", "Month", "Year") and type ("income" or "expense").
     */
    fun filterTransactions(transactions: List<Transaction>, period: String, type: String): List<Transaction> {
        val now = Calendar.getInstance()
        val filteredByType = transactions.filter { it.type == type }

        return when (period) {
            "Week" -> {
                now.set(Calendar.DAY_OF_WEEK, now.firstDayOfWeek)
                val startOfWeek = now.time
                filteredByType.filter { it.date.toDate() >= startOfWeek }
            }
            "Month" -> {
                val currentMonth = now.get(Calendar.MONTH) + 1
                val currentYear = now.get(Calendar.YEAR)
                filteredByType.filter {
                    val txYear = if (it.year == 0) {
                        val c = Calendar.getInstance().apply { time = it.date.toDate() }
                        c.get(Calendar.YEAR)
                    } else it.year

                    val txMonth = if (it.month == 0) {
                        val c = Calendar.getInstance().apply { time = it.date.toDate() }
                        c.get(Calendar.MONTH) + 1
                    } else it.month

                    txMonth == currentMonth && txYear == currentYear
                }
            }
            "Year" -> {
                val currentYear = now.get(Calendar.YEAR)
                filteredByType.filter {
                    val txYear = if (it.year == 0) {
                        val c = Calendar.getInstance().apply { time = it.date.toDate() }
                        c.get(Calendar.YEAR)
                    } else it.year
                    txYear == currentYear
                }
            }
            else -> filteredByType
        }
    }

    /**
     * Groups and sums transactions into chart-ready trend buckets:
     * - "Week": 7 days (e.g. Mon..Sun)
     * - "Month": 4 weeks (W1..W4)
     * - "Year": 12 months (Jan..Dec)
     */
    fun prepareTrendData(transactions: List<Transaction>, period: String, type: String): List<Pair<String, Double>> {
        val filtered = transactions.filter { it.type == type }
        val calendar = Calendar.getInstance()

        return when (period) {
            "Week" -> {
                (6 downTo 0).map { offset ->
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -offset)
                    val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)

                    val sum = filtered.filter {
                        val txCal = Calendar.getInstance().apply { time = it.date.toDate() }
                        txCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                                txCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                    }.sumOf { it.amount }
                    dayOfWeek to sum
                }
            }
            "Month" -> {
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)

                (1..4).map { week ->
                    val sum = filtered.filter {
                        val c = Calendar.getInstance().apply { time = it.date.toDate() }
                        val txYear = if (it.year == 0) c.get(Calendar.YEAR) else it.year
                        val txMonth = if (it.month == 0) c.get(Calendar.MONTH) + 1 else it.month

                        txYear == currentYear && txMonth == currentMonth &&
                                (c.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1 == week
                    }.sumOf { it.amount }
                    "W$week" to sum
                }
            }
            "Year" -> {
                val currentYear = calendar.get(Calendar.YEAR)
                (0..11).map { monthIndex ->
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_MONTH, 1) // Avoid rollover bug
                    cal.set(Calendar.MONTH, monthIndex)
                    val monthName = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)

                    val sum = filtered.filter {
                        val c = Calendar.getInstance().apply { time = it.date.toDate() }
                        val txYear = if (it.year == 0) c.get(Calendar.YEAR) else it.year
                        val txMonth = if (it.month == 0) c.get(Calendar.MONTH) + 1 else it.month

                        txYear == currentYear && txMonth == (monthIndex + 1)
                    }.sumOf { it.amount }
                    monthName to sum
                }
            }
            else -> emptyList()
        }
    }

    /**
     * Color palette for chart categories.
     */
    fun getChartColor(index: Int): Color {
        val colors = listOf(
            Color(0xFF00796B), // MediumGreen
            Color(0xFF00A79B), // LightGreen
            Color(0xFFFFC107), // Amber
            Color(0xFFEF5350), // Red
            Color(0xFF42A5F5), // Blue
            Color(0xFF9C27B0), // Purple
            Color(0xFF795548)  // Brown
        )
        return colors[index % colors.size]
    }
}
