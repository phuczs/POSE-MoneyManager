package com.example.moneymanager.domain.model

/**
 * Xếp hạng sức khỏe tài chính dựa trên Tỷ lệ tiết kiệm (Savings Rate).
 */
enum class FinancialHealthRating {
    EXCELLENT, // Tỷ lệ tiết kiệm > 30%
    GOOD,      // Tỷ lệ tiết kiệm 10% - 30%
    WARNING,   // Tỷ lệ tiết kiệm 0% - 10%
    DEFICIT    // Chi tiêu vượt thu nhập (Dòng tiền âm)
}

/**
 * Biến động chi tiêu theo từng danh mục giữa hai chu kỳ.
 */
data class CategoryDelta(
    val categoryName: String,
    val currentAmount: Double,
    val previousAmount: Double,
    val deltaAmount: Double,
    val deltaPercent: Double // e.g. +35.2% hoặc -15.0%
)

/**
 * Thống kê tổng hợp so sánh giữa kỳ hiện tại và kỳ trước (MoM hoặc YoY).
 */
data class PeriodComparisonSummary(
    val currentIncome: Double,
    val currentExpense: Double,
    val previousIncome: Double,
    val previousExpense: Double,
    val incomeDeltaPercent: Double,
    val expenseDeltaPercent: Double,
    val netSavings: Double,
    val savingsRate: Float, // % (e.g. 25.5f)
    val healthRating: FinancialHealthRating,
    val topSurgeCategory: CategoryDelta?,
    val topDecreasedCategory: CategoryDelta?
)

/**
 * Điểm dữ liệu trên biểu đồ xu hướng dòng tiền (Income vs Expense).
 */
data class CashflowTrendPoint(
    val label: String,
    val income: Double,
    val expense: Double,
    val netSavings: Double,
    val timestamp: Long = 0L
)

/**
 * Dự báo tốc độ chi tiêu và số dư cuối tháng dựa trên số ngày thực tế.
 */
data class MonthEndProjection(
    val daysElapsed: Int,
    val totalDaysInMonth: Int,
    val averageDailyBurn: Double,
    val currentSpent: Double,
    val projectedMonthEndExpense: Double,
    val currentIncome: Double,
    val projectedRemainingBalance: Double
)

/**
 * Báo cáo dòng tiền toàn diện phục vụ hiển thị UI.
 */
data class CashflowAnalyticsReport(
    val comparison: PeriodComparisonSummary,
    val trendPoints: List<CashflowTrendPoint>,
    val projection: MonthEndProjection
)
