package com.example.moneymanager.domain.usecase.ai

import com.example.moneymanager.domain.model.ChallengeStatus
import com.example.moneymanager.domain.model.SavingsChallenge
import com.example.moneymanager.domain.model.Transaction
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max

/**
 * UseCase for AI Gamified Savings Challenges (Thử thách Tiết kiệm Game hóa).
 *
 * 1. Analyzes spending habits to propose context-relevant challenges (e.g. coffee cutoff, no-buy week, home cooking).
 * 2. Real-time evaluation of streak, daily limit compliance, and actual monetary savings retained.
 */
class GenerateChallengesUseCase @Inject constructor() {

    /**
     * Proposes tailored challenges based on user transaction history over the past 30-60 days.
     */
    fun generateSuggestions(transactions: List<Transaction>): List<SavingsChallenge> {
        val expenses = transactions.filter { it.type.equals("expense", ignoreCase = true) && it.amount > 0 }
        val now = System.currentTimeMillis()
        val suggestions = mutableListOf<SavingsChallenge>()

        // 1. Phân tích chi tiêu Cafe / Trà sữa / Nước uống hàng quán
        val cafeRegex = "(?i)\\b(cafe|cà phê|coffee|trà sữa|tra sua|highlands|starbucks|phúc long|the coffee house|toco|katinat)\\b".toRegex()
        val cafeTxs = expenses.filter { cafeRegex.containsMatchIn("${it.description} ${it.category}") }
        val totalCafeSpend = cafeTxs.sumOf { it.amount }
        val cafeBaselineDaily = if (cafeTxs.isNotEmpty()) (totalCafeSpend / 30.0).coerceAtLeast(30000.0) else 45000.0

        suggestions.add(
            SavingsChallenge(
                id = "challenge_cafe_fasting",
                title = "1 Tuần Không Trà Sữa & Cà Phê Hàng Quán",
                description = "Tự pha cà phê hoặc uống nước lọc trong 7 ngày. Giữ lại khoản chi nhỏ hàng ngày để thấy số dư tăng vọt!",
                category = "Food & Beverage",
                keywordFilter = "(?i)\\b(cafe|cà phê|coffee|trà sữa|tra sua|highlands|starbucks|phúc long|the coffee house|toco|katinat)\\b",
                targetDays = 7,
                currentStreak = 0,
                dailyBudgetLimit = 0.0,
                baselineDailyAverage = cafeBaselineDaily,
                targetSavings = cafeBaselineDaily * 7,
                startDate = now,
                endDate = now + (7L * 24 * 60 * 60 * 1000),
                status = ChallengeStatus.SUGGESTED,
                badgeName = "Bậc Thầy Tự Pha Chế",
                badgeIcon = "☕"
            )
        )

        // 2. Phân tích chi tiêu Mua sắm (Shopping / No-Buy Challenge)
        val shoppingRegex = "(?i)\\b(shopee|lazada|tiki|tiktok shop|mua sắm|shopping|quần áo|thời trang|zara|uniqlo)\\b".toRegex()
        val shoppingTxs = expenses.filter { shoppingRegex.containsMatchIn("${it.description} ${it.category}") || it.category.equals("Shopping", ignoreCase = true) }
        val totalShopping = shoppingTxs.sumOf { it.amount }
        val shoppingBaselineDaily = if (shoppingTxs.isNotEmpty()) (totalShopping / 30.0).coerceAtLeast(50000.0) else 75000.0

        suggestions.add(
            SavingsChallenge(
                id = "challenge_no_buy_week",
                title = "7 Ngày No-Buy (Cắt Mua Sắm Bốc Đồng)",
                description = "Nói không với các đơn hàng thương mại điện tử và mua sắm không thiết yếu trong 1 tuần.",
                category = "Shopping",
                keywordFilter = "(?i)\\b(shopee|lazada|tiki|tiktok shop|mua sắm|shopping|quần áo|thời trang|zara|uniqlo)\\b",
                targetDays = 7,
                currentStreak = 0,
                dailyBudgetLimit = 0.0,
                baselineDailyAverage = shoppingBaselineDaily,
                targetSavings = shoppingBaselineDaily * 7,
                startDate = now,
                endDate = now + (7L * 24 * 60 * 60 * 1000),
                status = ChallengeStatus.SUGGESTED,
                badgeName = "Thợ Săn Kỷ Luật",
                badgeIcon = "🛍️"
            )
        )

        // 3. Phân tích Ăn ngoài vs Nấu ăn tại nhà (Home Cooking Challenge)
        val diningRegex = "(?i)\\b(nhà hàng|restaurant|ăn ngoài|buffet|lẩu|nướng|bbq|quán nhậu|grabfood|shopeefood|baemin)\\b".toRegex()
        val diningTxs = expenses.filter { diningRegex.containsMatchIn("${it.description} ${it.category}") }
        val diningBaselineDaily = if (diningTxs.isNotEmpty()) (diningTxs.sumOf { it.amount } / 30.0).coerceAtLeast(60000.0) else 80000.0

        suggestions.add(
            SavingsChallenge(
                id = "challenge_home_cooking_5days",
                title = "5 Ngày Nấu Ăn & Chuẩn Bị Cơm Nhà",
                description = "Tự chuẩn bị bữa ăn thay vì đặt đồ ăn ngoài hoặc ăn hàng quán liên tục.",
                category = "Food & Beverage",
                keywordFilter = "(?i)\\b(nhà hàng|restaurant|ăn ngoài|buffet|lẩu|nướng|bbq|quán nhậu|grabfood|shopeefood|baemin)\\b",
                targetDays = 5,
                currentStreak = 0,
                dailyBudgetLimit = 0.0,
                baselineDailyAverage = diningBaselineDaily,
                targetSavings = diningBaselineDaily * 5,
                startDate = now,
                endDate = now + (5L * 24 * 60 * 60 * 1000),
                status = ChallengeStatus.SUGGESTED,
                badgeName = "Đầu Bếp Gia Đình",
                badgeIcon = "🍳"
            )
        )

        // 4. Thử thách Vi chi tiêu: "Chi tiêu tối giản 50k/ngày"
        val totalDailyAvg = if (expenses.isNotEmpty()) (expenses.sumOf { it.amount } / 30.0).coerceAtLeast(100000.0) else 150000.0
        val targetLimit = 50000.0
        val expectedDailySaving = (totalDailyAvg - targetLimit).coerceAtLeast(30000.0)

        suggestions.add(
            SavingsChallenge(
                id = "challenge_micro_spending_50k",
                title = "Thử Thách Tiết Kiệm: Chi Tiêu Dưới 50k/Ngày",
                description = "Giới hạn toàn bộ chi tiêu phát sinh trong ngày dưới 50.000đ trong 5 ngày liên tiếp.",
                category = "",
                keywordFilter = "",
                targetDays = 5,
                currentStreak = 0,
                dailyBudgetLimit = targetLimit,
                baselineDailyAverage = totalDailyAvg,
                targetSavings = expectedDailySaving * 5,
                startDate = now,
                endDate = now + (5L * 24 * 60 * 60 * 1000),
                status = ChallengeStatus.SUGGESTED,
                badgeName = "Chiến Binh Tối Giản",
                badgeIcon = "🛡️"
            )
        )

        return suggestions
    }

    /**
     * Evaluates challenge streak and real-time saved amount based on actual user transactions.
     */
    fun evaluateProgress(
        challenge: SavingsChallenge,
        transactions: List<Transaction>,
        now: Long = System.currentTimeMillis()
    ): SavingsChallenge {
        if (challenge.status == ChallengeStatus.SUGGESTED) return challenge

        val regex = if (challenge.keywordFilter.isNotBlank()) challenge.keywordFilter.toRegex() else null
        val expenses = transactions.filter { it.type.equals("expense", ignoreCase = true) && it.amount > 0 }

        // Tính số ngày từ startDate đến ngày hiện tại
        val startCal = Calendar.getInstance().apply {
            timeInMillis = challenge.startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = challenge.endDate.coerceAtMost(now)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        var streak = 0
        var totalSaved = 0.0
        var daysEvaluated = 0

        val currentDayCal = startCal.clone() as Calendar
        val todayCal = Calendar.getInstance().apply { timeInMillis = now }

        while (!currentDayCal.after(endCal) && !currentDayCal.after(todayCal)) {
            val dayStart = currentDayCal.timeInMillis
            val dayEnd = (currentDayCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // Lọc các giao dịch trong ngày này vi phạm hoặc liên quan đến challenge
            val dayTxs = expenses.filter { it.date in dayStart..dayEnd }
            val matchingDayTxs = dayTxs.filter { tx ->
                val matchCategory = challenge.category.isBlank() || tx.category.equals(challenge.category, ignoreCase = true)
                val matchRegex = regex?.containsMatchIn("${tx.description} ${tx.category}") ?: true
                matchCategory && matchRegex
            }

            val actualDaySpend = matchingDayTxs.sumOf { it.amount }
            val isSuccess = actualDaySpend <= challenge.dailyBudgetLimit

            if (isSuccess) {
                streak++
                val daySaved = max(0.0, challenge.baselineDailyAverage - actualDaySpend)
                totalSaved += daySaved
            } else {
                // Vi phạm ngày -> streak bị ngắt (hoặc tính lại từ đầu)
                streak = 0
            }

            daysEvaluated++
            currentDayCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val isCompleted = streak >= challenge.targetDays
        val isExpired = now > challenge.endDate && !isCompleted

        val newStatus = when {
            isCompleted -> ChallengeStatus.COMPLETED
            isExpired -> ChallengeStatus.FAILED
            else -> ChallengeStatus.ACTIVE
        }

        return challenge.copy(
            currentStreak = streak,
            daysCompleted = daysEvaluated,
            savedAmount = totalSaved,
            status = newStatus
        )
    }
}
