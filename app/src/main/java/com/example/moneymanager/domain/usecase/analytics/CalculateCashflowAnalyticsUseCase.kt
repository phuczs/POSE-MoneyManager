package com.example.moneymanager.domain.usecase.analytics

import com.example.moneymanager.domain.model.CashflowAnalyticsReport
import com.example.moneymanager.domain.model.CashflowTrendPoint
import com.example.moneymanager.domain.model.CategoryDelta
import com.example.moneymanager.domain.model.FinancialHealthRating
import com.example.moneymanager.domain.model.MonthEndProjection
import com.example.moneymanager.domain.model.PeriodComparisonSummary
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.util.toDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class CalculateCashflowAnalyticsUseCase @Inject constructor() {

    operator fun invoke(
        allTransactions: List<Transaction>,
        period: String = "Month",
        referenceCalendar: Calendar = Calendar.getInstance()
    ): CashflowAnalyticsReport {
        val (currentTxs, previousTxs) = partitionTransactions(allTransactions, period, referenceCalendar)
        val comparison = calculatePeriodComparison(currentTxs, previousTxs)
        val trendPoints = generateTrendPoints(allTransactions, period, referenceCalendar)
        val projection = calculateMonthEndProjection(allTransactions, referenceCalendar)

        return CashflowAnalyticsReport(
            comparison = comparison,
            trendPoints = trendPoints,
            projection = projection
        )
    }

    /**
     * Phân tách danh sách giao dịch thành kỳ hiện tại và kỳ liền trước.
     */
    fun partitionTransactions(
        transactions: List<Transaction>,
        period: String,
        refCal: Calendar
    ): Pair<List<Transaction>, List<Transaction>> {
        val now = refCal.clone() as Calendar

        return when (period) {
            "Week" -> {
                // Kỳ hiện tại: 7 ngày gần nhất (hôm nay - 6 ngày -> hôm nay)
                val currentCal = now.clone() as Calendar
                currentCal.set(Calendar.HOUR_OF_DAY, 23)
                currentCal.set(Calendar.MINUTE, 59)
                currentCal.set(Calendar.SECOND, 59)
                currentCal.set(Calendar.MILLISECOND, 999)
                val currentEnd = currentCal.timeInMillis

                val currentStartCal = now.clone() as Calendar
                currentStartCal.add(Calendar.DAY_OF_YEAR, -6)
                currentStartCal.set(Calendar.HOUR_OF_DAY, 0)
                currentStartCal.set(Calendar.MINUTE, 0)
                currentStartCal.set(Calendar.SECOND, 0)
                currentStartCal.set(Calendar.MILLISECOND, 0)
                val currentStart = currentStartCal.timeInMillis

                // Kỳ trước: 7 ngày trước đó (hôm nay - 13 ngày -> hôm nay - 7 ngày)
                val prevEndCal = now.clone() as Calendar
                prevEndCal.add(Calendar.DAY_OF_YEAR, -7)
                prevEndCal.set(Calendar.HOUR_OF_DAY, 23)
                prevEndCal.set(Calendar.MINUTE, 59)
                prevEndCal.set(Calendar.SECOND, 59)
                prevEndCal.set(Calendar.MILLISECOND, 999)
                val prevEnd = prevEndCal.timeInMillis

                val prevStartCal = now.clone() as Calendar
                prevStartCal.add(Calendar.DAY_OF_YEAR, -13)
                prevStartCal.set(Calendar.HOUR_OF_DAY, 0)
                prevStartCal.set(Calendar.MINUTE, 0)
                prevStartCal.set(Calendar.SECOND, 0)
                prevStartCal.set(Calendar.MILLISECOND, 0)
                val prevStart = prevStartCal.timeInMillis

                val cur = transactions.filter { it.date in currentStart..currentEnd }
                val prev = transactions.filter { it.date in prevStart..prevEnd }
                cur to prev
            }
            "Year" -> {
                val currentYear = now.get(Calendar.YEAR)
                val prevYear = currentYear - 1

                val cur = transactions.filter {
                    val y = if (it.year != 0) it.year else {
                        val c = Calendar.getInstance().apply { timeInMillis = it.date }
                        c.get(Calendar.YEAR)
                    }
                    y == currentYear
                }
                val prev = transactions.filter {
                    val y = if (it.year != 0) it.year else {
                        val c = Calendar.getInstance().apply { timeInMillis = it.date }
                        c.get(Calendar.YEAR)
                    }
                    y == prevYear
                }
                cur to prev
            }
            else -> { // "Month"
                val curYear = now.get(Calendar.YEAR)
                val curMonth = now.get(Calendar.MONTH) + 1

                val prevCal = now.clone() as Calendar
                prevCal.add(Calendar.MONTH, -1)
                val prevYear = prevCal.get(Calendar.YEAR)
                val prevMonth = prevCal.get(Calendar.MONTH) + 1

                val cur = transactions.filter {
                    val (y, m) = getTransactionYearMonth(it)
                    y == curYear && m == curMonth
                }
                val prev = transactions.filter {
                    val (y, m) = getTransactionYearMonth(it)
                    y == prevYear && m == prevMonth
                }
                cur to prev
            }
        }
    }

    private fun getTransactionYearMonth(transaction: Transaction): Pair<Int, Int> {
        val y = if (transaction.year != 0) transaction.year else {
            val c = Calendar.getInstance().apply { timeInMillis = transaction.date }
            c.get(Calendar.YEAR)
        }
        val m = if (transaction.month != 0) transaction.month else {
            val c = Calendar.getInstance().apply { timeInMillis = transaction.date }
            c.get(Calendar.MONTH) + 1
        }
        return y to m
    }

    /**
     * Tính toán tổng quan so sánh giữa kỳ hiện tại và kỳ trước.
     */
    fun calculatePeriodComparison(
        currentTxs: List<Transaction>,
        previousTxs: List<Transaction>
    ): PeriodComparisonSummary {
        val curIncome = currentTxs.filter { it.type.equals("income", ignoreCase = true) }.sumOf { it.amount }
        val curExpense = currentTxs.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { it.amount }

        val prevIncome = previousTxs.filter { it.type.equals("income", ignoreCase = true) }.sumOf { it.amount }
        val prevExpense = previousTxs.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { it.amount }

        val incomeDeltaPercent = calculateDeltaPercent(curIncome, prevIncome)
        val expenseDeltaPercent = calculateDeltaPercent(curExpense, prevExpense)

        val netSavings = curIncome - curExpense
        val (savingsRate, rating) = calculateSavingsRate(curIncome, curExpense)

        // Phân tích chi tiết biến động theo danh mục chi tiêu
        val curExpensesByCategory = currentTxs
            .filter { it.type.equals("expense", ignoreCase = true) }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val prevExpensesByCategory = previousTxs
            .filter { it.type.equals("expense", ignoreCase = true) }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val allCategories = (curExpensesByCategory.keys + prevExpensesByCategory.keys).toSet()

        val categoryDeltas = allCategories.map { cat ->
            val cur = curExpensesByCategory[cat] ?: 0.0
            val prev = prevExpensesByCategory[cat] ?: 0.0
            val delta = cur - prev
            val percent = calculateDeltaPercent(cur, prev)
            CategoryDelta(
                categoryName = cat,
                currentAmount = cur,
                previousAmount = prev,
                deltaAmount = delta,
                deltaPercent = percent
            )
        }

        val topSurge = categoryDeltas.filter { it.deltaAmount > 0 }.maxByOrNull { it.deltaAmount }
        val topDecreased = categoryDeltas.filter { it.deltaAmount < 0 }.minByOrNull { it.deltaAmount }

        return PeriodComparisonSummary(
            currentIncome = curIncome,
            currentExpense = curExpense,
            previousIncome = prevIncome,
            previousExpense = prevExpense,
            incomeDeltaPercent = incomeDeltaPercent,
            expenseDeltaPercent = expenseDeltaPercent,
            netSavings = netSavings,
            savingsRate = savingsRate,
            healthRating = rating,
            topSurgeCategory = topSurge,
            topDecreasedCategory = topDecreased
        )
    }

    /**
     * Tính % thay đổi an toàn, tránh lỗi chia cho 0.
     */
    fun calculateDeltaPercent(current: Double, previous: Double): Double {
        if (previous == 0.0) {
            return if (current == 0.0) 0.0 else 100.0
        }
        val delta = ((current - previous) / previous) * 100.0
        return (delta * 10.0).roundToInt() / 10.0 // làm tròn 1 chữ số thập phân
    }

    /**
     * Tính Tỷ lệ tiết kiệm (Savings Rate) và xếp loại sức khỏe tài chính.
     */
    fun calculateSavingsRate(income: Double, expense: Double): Pair<Float, FinancialHealthRating> {
        if (income <= 0.0) {
            return if (expense > 0.0) {
                -100f to FinancialHealthRating.DEFICIT
            } else {
                0f to FinancialHealthRating.WARNING
            }
        }

        val net = income - expense
        val rate = ((net / income) * 100.0).toFloat()
        val roundedRate = (rate * 10).roundToInt() / 10f

        val rating = when {
            roundedRate >= 30f -> FinancialHealthRating.EXCELLENT
            roundedRate >= 10f -> FinancialHealthRating.GOOD
            roundedRate >= 0f -> FinancialHealthRating.WARNING
            else -> FinancialHealthRating.DEFICIT
        }

        return roundedRate to rating
    }

    /**
     * Tính toán dự báo chi tiêu cuối tháng dựa trên số ngày trôi qua.
     */
    fun calculateMonthEndProjection(
        allTransactions: List<Transaction>,
        refCal: Calendar = Calendar.getInstance()
    ): MonthEndProjection {
        val now = refCal.clone() as Calendar
        val curYear = now.get(Calendar.YEAR)
        val curMonth = now.get(Calendar.MONTH) + 1
        val daysElapsed = now.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val totalDaysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthTxs = allTransactions.filter {
            val (y, m) = getTransactionYearMonth(it)
            y == curYear && m == curMonth
        }

        val currentSpent = monthTxs.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { it.amount }
        val currentIncome = monthTxs.filter { it.type.equals("income", ignoreCase = true) }.sumOf { it.amount }

        val averageDailyBurn = currentSpent / daysElapsed
        val projectedMonthEndExpense = averageDailyBurn * totalDaysInMonth
        val projectedRemainingBalance = currentIncome - projectedMonthEndExpense

        return MonthEndProjection(
            daysElapsed = daysElapsed,
            totalDaysInMonth = totalDaysInMonth,
            averageDailyBurn = (averageDailyBurn * 100).roundToInt() / 100.0,
            currentSpent = currentSpent,
            projectedMonthEndExpense = (projectedMonthEndExpense * 100).roundToInt() / 100.0,
            currentIncome = currentIncome,
            projectedRemainingBalance = (projectedRemainingBalance * 100).roundToInt() / 100.0
        )
    }

    /**
     * Tạo dữ liệu chuỗi thời gian hai đường (Income vs Expense) cho biểu đồ Canvas.
     */
    fun generateTrendPoints(
        transactions: List<Transaction>,
        period: String,
        refCal: Calendar = Calendar.getInstance()
    ): List<CashflowTrendPoint> {
        val calendar = refCal.clone() as Calendar

        return when (period) {
            "Week" -> {
                (6 downTo 0).map { offset ->
                    val cal = calendar.clone() as Calendar
                    cal.add(Calendar.DAY_OF_YEAR, -offset)
                    val label = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)

                    val targetYear = cal.get(Calendar.YEAR)
                    val targetDay = cal.get(Calendar.DAY_OF_YEAR)

                    val dayTxs = transactions.filter {
                        val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
                        txCal.get(Calendar.YEAR) == targetYear && txCal.get(Calendar.DAY_OF_YEAR) == targetDay
                    }

                    val inc = dayTxs.filter { it.type.equals("income", ignoreCase = true) }.sumOf { it.amount }
                    val exp = dayTxs.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { it.amount }
                    CashflowTrendPoint(
                        label = label,
                        income = inc,
                        expense = exp,
                        netSavings = inc - exp,
                        timestamp = cal.timeInMillis
                    )
                }
            }
            "Year" -> {
                val currentYear = calendar.get(Calendar.YEAR)
                (0..11).map { monthIndex ->
                    val cal = calendar.clone() as Calendar
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.MONTH, monthIndex)
                    val label = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)

                    val monthTxs = transactions.filter {
                        val (y, m) = getTransactionYearMonth(it)
                        y == currentYear && m == (monthIndex + 1)
                    }

                    val inc = monthTxs.filter { it.type.equals("income", ignoreCase = true) }.sumOf { it.amount }
                    val exp = monthTxs.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { it.amount }
                    CashflowTrendPoint(
                        label = label,
                        income = inc,
                        expense = exp,
                        netSavings = inc - exp,
                        timestamp = cal.timeInMillis
                    )
                }
            }
            else -> { // "Month"
                val curYear = calendar.get(Calendar.YEAR)
                val curMonth = calendar.get(Calendar.MONTH) + 1

                (1..4).map { weekIndex ->
                    val weekTxs = transactions.filter {
                        val (y, m) = getTransactionYearMonth(it)
                        if (y == curYear && m == curMonth) {
                            val c = Calendar.getInstance().apply { timeInMillis = it.date }
                            val day = c.get(Calendar.DAY_OF_MONTH)
                            val w = ((day - 1) / 7 + 1).coerceAtMost(4)
                            w == weekIndex
                        } else false
                    }

                    val inc = weekTxs.filter { it.type.equals("income", ignoreCase = true) }.sumOf { it.amount }
                    val exp = weekTxs.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { it.amount }
                    CashflowTrendPoint(
                        label = "T$weekIndex", // Tuần 1 .. Tuần 4
                        income = inc,
                        expense = exp,
                        netSavings = inc - exp
                    )
                }
            }
        }
    }
}
