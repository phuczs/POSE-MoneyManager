package com.example.moneymanager

import com.example.moneymanager.domain.model.FinancialHealthRating
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.usecase.analytics.CalculateCashflowAnalyticsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class CashflowAnalyticsTest {

    private lateinit var useCase: CalculateCashflowAnalyticsUseCase

    @Before
    fun setUp() {
        useCase = CalculateCashflowAnalyticsUseCase()
    }

    @Test
    fun testCalculateSavingsRate_variousTiers() {
        // Tier 1: > 30% -> EXCELLENT
        val (rate1, rating1) = useCase.calculateSavingsRate(10_000_000.0, 6_000_000.0)
        assertEquals(40.0f, rate1, 0.01f)
        assertEquals(FinancialHealthRating.EXCELLENT, rating1)

        // Tier 2: 10% - 30% -> GOOD
        val (rate2, rating2) = useCase.calculateSavingsRate(10_000_000.0, 8_000_000.0)
        assertEquals(20.0f, rate2, 0.01f)
        assertEquals(FinancialHealthRating.GOOD, rating2)

        // Tier 3: 0% - 10% -> WARNING
        val (rate3, rating3) = useCase.calculateSavingsRate(10_000_000.0, 9_500_000.0)
        assertEquals(5.0f, rate3, 0.01f)
        assertEquals(FinancialHealthRating.WARNING, rating3)

        // Tier 4: < 0% -> DEFICIT (Chi tiêu vượt thu nhập)
        val (rate4, rating4) = useCase.calculateSavingsRate(10_000_000.0, 12_500_000.0)
        assertEquals(-25.0f, rate4, 0.01f)
        assertEquals(FinancialHealthRating.DEFICIT, rating4)

        // Edge case: Thu nhập = 0, có chi tiêu
        val (rate5, rating5) = useCase.calculateSavingsRate(0.0, 500_000.0)
        assertEquals(-100.0f, rate5, 0.01f)
        assertEquals(FinancialHealthRating.DEFICIT, rating5)

        // Edge case: Không có thu nhập và không chi tiêu
        val (rate6, rating6) = useCase.calculateSavingsRate(0.0, 0.0)
        assertEquals(0.0f, rate6, 0.01f)
        assertEquals(FinancialHealthRating.WARNING, rating6)
    }

    @Test
    fun testDeltaPercentCalculation_handlesEdgeCases() {
        // Tăng từ 0 lên 500
        val delta1 = useCase.calculateDeltaPercent(500.0, 0.0)
        assertEquals(100.0, delta1, 0.01)

        // Cả 2 kỳ đều 0
        val delta2 = useCase.calculateDeltaPercent(0.0, 0.0)
        assertEquals(0.0, delta2, 0.01)

        // Tăng trưởng bình thường +25%
        val delta3 = useCase.calculateDeltaPercent(125.0, 100.0)
        assertEquals(25.0, delta3, 0.01)

        // Giảm chi tiêu -30%
        val delta4 = useCase.calculateDeltaPercent(70.0, 100.0)
        assertEquals(-30.0, delta4, 0.01)
    }

    @Test
    fun testPeriodComparison_detectsTopSurgeAndDecreasedCategory() {
        val prevTransactions = listOf(
            createTransaction(id = "1", type = "expense", category = "Ăn uống", amount = 2_000_000.0),
            createTransaction(id = "2", type = "expense", category = "Mua sắm", amount = 1_500_000.0),
            createTransaction(id = "3", type = "income", category = "Lương", amount = 15_000_000.0)
        )

        val curTransactions = listOf(
            createTransaction(id = "4", type = "expense", category = "Ăn uống", amount = 3_500_000.0), // +1.5M (Surge)
            createTransaction(id = "5", type = "expense", category = "Mua sắm", amount = 500_000.0),   // -1.0M (Decreased)
            createTransaction(id = "6", type = "income", category = "Lương", amount = 15_000_000.0)
        )

        val comparison = useCase.calculatePeriodComparison(curTransactions, prevTransactions)

        assertEquals(15_000_000.0, comparison.currentIncome, 0.01)
        assertEquals(4_000_000.0, comparison.currentExpense, 0.01)
        assertEquals(11_000_000.0, comparison.netSavings, 0.01)
        assertEquals(73.3f, comparison.savingsRate, 0.1f)
        assertEquals(FinancialHealthRating.EXCELLENT, comparison.healthRating)

        // Top surge: Ăn uống
        assertNotNull(comparison.topSurgeCategory)
        assertEquals("Ăn uống", comparison.topSurgeCategory?.categoryName)
        assertEquals(1_500_000.0, comparison.topSurgeCategory?.deltaAmount ?: 0.0, 0.01)
        assertEquals(75.0, comparison.topSurgeCategory?.deltaPercent ?: 0.0, 0.1)

        // Top decreased: Mua sắm
        assertNotNull(comparison.topDecreasedCategory)
        assertEquals("Mua sắm", comparison.topDecreasedCategory?.categoryName)
        assertEquals(-1_000_000.0, comparison.topDecreasedCategory?.deltaAmount ?: 0.0, 0.01)
        assertEquals(-66.7, comparison.topDecreasedCategory?.deltaPercent ?: 0.0, 0.1)
    }

    @Test
    fun testMonthEndProjection_burnRateCalculation() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER) // Tháng 10 có 31 ngày
            set(Calendar.DAY_OF_MONTH, 10)
        }

        val txs = listOf(
            createTransaction(id = "1", type = "expense", category = "Ăn uống", amount = 3_000_000.0, year = 2026, month = 10, day = 5),
            createTransaction(id = "2", type = "expense", category = "Xăng xe", amount = 2_000_000.0, year = 2026, month = 10, day = 8),
            createTransaction(id = "3", type = "income", category = "Lương", amount = 20_000_000.0, year = 2026, month = 10, day = 1)
        )

        val projection = useCase.calculateMonthEndProjection(txs, cal)

        assertEquals(10, projection.daysElapsed)
        assertEquals(31, projection.totalDaysInMonth)
        assertEquals(5_000_000.0, projection.currentSpent, 0.01)
        assertEquals(500_000.0, projection.averageDailyBurn, 0.01) // 5,000,000 / 10
        assertEquals(15_500_000.0, projection.projectedMonthEndExpense, 0.01) // 500,000 * 31
        assertEquals(20_000_000.0, projection.currentIncome, 0.01)
        assertEquals(4_500_000.0, projection.projectedRemainingBalance, 0.01) // 20M - 15.5M
    }

    @Test
    fun testPartitionTransactions_acrossYearBoundary() {
        // Kiểm tra chuyển giao năm: Kỳ hiện tại là tháng 1/2026, kỳ trước phải là tháng 12/2025
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JANUARY) // Tháng 1
            set(Calendar.DAY_OF_MONTH, 15)
        }

        val txJan2026 = createTransaction(id = "1", type = "expense", amount = 1_000_000.0, year = 2026, month = 1)
        val txDec2025 = createTransaction(id = "2", type = "expense", amount = 2_000_000.0, year = 2025, month = 12)
        val txNov2025 = createTransaction(id = "3", type = "expense", amount = 3_000_000.0, year = 2025, month = 11)

        val (cur, prev) = useCase.partitionTransactions(listOf(txJan2026, txDec2025, txNov2025), "Month", cal)

        assertEquals(1, cur.size)
        assertEquals("1", cur[0].id)
        assertEquals(1, prev.size)
        assertEquals("2", prev[0].id)
    }

    @Test
    fun testGenerateTrendPoints_monthGeneratesFourWeeks() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 20)
        }

        val tx1 = createTransaction(id = "1", type = "expense", amount = 100_000.0, year = 2026, month = 10, day = 3) // Week 1
        val tx2 = createTransaction(id = "2", type = "income", amount = 500_000.0, year = 2026, month = 10, day = 10) // Week 2

        val points = useCase.generateTrendPoints(listOf(tx1, tx2), "Month", cal)

        assertEquals(4, points.size)
        assertEquals("T1", points[0].label)
        assertEquals(100_000.0, points[0].expense, 0.01)
        assertEquals("T2", points[1].label)
        assertEquals(500_000.0, points[1].income, 0.01)
    }

    private fun createTransaction(
        id: String,
        type: String,
        amount: Double,
        category: String = "Chung",
        year: Int = 2026,
        month: Int = 10,
        day: Int = 1
    ): Transaction {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Transaction(
            id = id,
            userId = "test_user",
            amount = amount,
            category = category,
            type = type,
            date = cal.timeInMillis,
            year = year,
            month = month,
            description = "Test transaction $id"
        )
    }
}
