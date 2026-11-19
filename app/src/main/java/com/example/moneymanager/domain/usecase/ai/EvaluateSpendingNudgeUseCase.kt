package com.example.moneymanager.domain.usecase.ai

import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.BudgetWithProgress
import com.example.moneymanager.domain.model.CoachNudge
import com.example.moneymanager.domain.model.FinancialArchetype
import com.example.moneymanager.domain.model.NudgeActionTarget
import com.example.moneymanager.domain.model.NudgeTone
import com.example.moneymanager.domain.model.NudgeType
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.util.toCurrencyString
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Proactive AI Coach rule engine.
 *
 * Evaluates real-time behavioral signals (daily spike, rapid mid-cycle budget burn,
 * month-end overspend projection, weekly savings celebration, and tracking inactivity)
 * to surface the single highest-priority contextual [CoachNudge].
 *
 * Runs 100% locally (< 10ms) and adapts messaging tone to the user's [FinancialArchetype].
 */
class EvaluateSpendingNudgeUseCase @Inject constructor() {

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val WEEK_MS = 7L * DAY_MS
        private const val DAILY_SPIKE_MULTIPLIER = 1.5
        private const val BUDGET_BURN_THRESHOLD = 0.80f
        private const val INACTIVITY_DAYS_THRESHOLD = 5
    }

    operator fun invoke(
        allTransactions: List<Transaction>,
        activeBudgets: List<Budget>,
        dismissedNudgeIds: Set<String> = emptySet(),
        dnaArchetype: FinancialArchetype? = null,
        languageCode: String = "vi",
        nowMillis: Long = System.currentTimeMillis()
    ): CoachNudge? {
        val isVi = languageCode == "vi"
        val currencyCode = if (isVi) "VND" else "USD"
        val dayKey = formatDayKey(nowMillis)
        val candidates = mutableListOf<CoachNudge>()

        val expenses = allTransactions.filter {
            it.type.equals("expense", ignoreCase = true) && it.amount > 0.0
        }

        // 1. Mid-cycle rapid budget burn (budget >= 80% used with >= 3 days left)
        evaluateMidCycleBudgetBurn(
            expenses = expenses,
            activeBudgets = activeBudgets,
            dayKey = dayKey,
            isVi = isVi,
            currencyCode = currencyCode,
            nowMillis = nowMillis
        )?.let { candidates.add(it) }

        // 2. Daily spending spike (> 150% of 30-day active-day average)
        evaluateDailySpike(
            expenses = expenses,
            dayKey = dayKey,
            isVi = isVi,
            currencyCode = currencyCode,
            nowMillis = nowMillis
        )?.let { candidates.add(it) }

        // 3. Month-end overspend risk (last 7 days of month and projected expense > income)
        evaluateMonthEndOverspend(
            allTransactions = allTransactions,
            dayKey = dayKey,
            isVi = isVi,
            currencyCode = currencyCode,
            nowMillis = nowMillis
        )?.let { candidates.add(it) }

        // 4. Positive celebration: spent >= 10% less this week vs previous week
        evaluateWeeklySavingsWin(
            expenses = expenses,
            dayKey = dayKey,
            isVi = isVi,
            currencyCode = currencyCode,
            dnaArchetype = dnaArchetype,
            nowMillis = nowMillis
        )?.let { candidates.add(it) }

        // 5. Gentle inactivity reminder (>= 5 days since last logged transaction)
        evaluateInactivity(
            allTransactions = allTransactions,
            dayKey = dayKey,
            isVi = isVi,
            nowMillis = nowMillis
        )?.let { candidates.add(it) }

        return candidates
            .filterNot { it.id in dismissedNudgeIds }
            .maxByOrNull { it.priority }
    }

    private fun evaluateMidCycleBudgetBurn(
        expenses: List<Transaction>,
        activeBudgets: List<Budget>,
        dayKey: String,
        isVi: Boolean,
        currencyCode: String,
        nowMillis: Long
    ): CoachNudge? {
        val risky = activeBudgets
            .filter { nowMillis in it.startDate..it.endDate && it.effectiveAmount > 0.0 }
            .map { budget ->
                val spent = expenses.filter {
                    it.category.equals(budget.category, ignoreCase = true) &&
                        it.date in budget.startDate..budget.endDate
                }.sumOf { it.amount }
                BudgetWithProgress(budget, spent)
            }
            .filter { wp ->
                val daysLeft = ((wp.budget.endDate - nowMillis) / DAY_MS).toInt()
                wp.progress >= BUDGET_BURN_THRESHOLD && daysLeft >= 3
            }
            .maxByOrNull { it.progress } ?: return null

        val cat = risky.budget.category
        val pct = (risky.progress * 100).toInt()
        val daysLeft = ((risky.budget.endDate - nowMillis) / DAY_MS).toInt().coerceAtLeast(1)
        val remaining = (risky.budget.effectiveAmount - risky.spentAmount).coerceAtLeast(0.0)
        val dailySafe = remaining / daysLeft

        val title = if (isVi) "Ngân sách $cat đã chạm $pct%" else "$cat budget hit $pct%"
        val message = if (remaining > 0.0) {
            if (isVi)
                "Còn ${remaining.toCurrencyString(currencyCode)} cho $daysLeft ngày tới. Mỗi ngày chỉ nên chi tối đa ~${dailySafe.toCurrencyString(currencyCode)}."
            else
                "${remaining.toCurrencyString(currencyCode)} left for $daysLeft days — aim for under ${dailySafe.toCurrencyString(currencyCode)}/day."
        } else {
            if (isVi)
                "Bạn đã vượt hạn mức $cat khi vẫn còn $daysLeft ngày trong chu kỳ. Hãy rà soát lại chi tiêu nhé!"
            else
                "You've exceeded $cat with $daysLeft days still left in this cycle. Time for a quick check-in."
        }

        return CoachNudge(
            id = "budget_burn_${risky.budget.id}_$dayKey",
            type = NudgeType.MID_MONTH_BUDGET_BURN,
            tone = if (risky.progress >= 1.0f) NudgeTone.URGENT else NudgeTone.WARNING,
            emoji = "🎯",
            title = title,
            message = message,
            actionLabel = if (isVi) "Xem ngân sách" else "View Budgets",
            actionTarget = NudgeActionTarget.OPEN_BUDGETS,
            priority = if (risky.progress >= 1.0f) 100 else 88
        )
    }

    private fun evaluateDailySpike(
        expenses: List<Transaction>,
        dayKey: String,
        isVi: Boolean,
        currencyCode: String,
        nowMillis: Long
    ): CoachNudge? {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayYear = cal.get(Calendar.YEAR)
        val todayDay = cal.get(Calendar.DAY_OF_YEAR)

        val todaySpent = expenses.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
            txCal.get(Calendar.YEAR) == todayYear && txCal.get(Calendar.DAY_OF_YEAR) == todayDay
        }.sumOf { it.amount }

        if (todaySpent <= 0.0) return null

        val thirtyDaysAgo = nowMillis - 30L * DAY_MS
        val pastDailyTotals = expenses
            .filter { tx ->
                tx.date in thirtyDaysAgo until nowMillis && run {
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    !(txCal.get(Calendar.YEAR) == todayYear && txCal.get(Calendar.DAY_OF_YEAR) == todayDay)
                }
            }
            .groupBy { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                "${txCal.get(Calendar.YEAR)}-${txCal.get(Calendar.DAY_OF_YEAR)}"
            }
            .values
            .map { dayList -> dayList.sumOf { it.amount } }

        if (pastDailyTotals.size < 3) return null
        val avgDaily = pastDailyTotals.average()
        if (avgDaily <= 0.0) return null

        val ratio = todaySpent / avgDaily
        if (ratio < DAILY_SPIKE_MULTIPLIER) return null

        val ratioStr = String.format(Locale.US, "%.1f", ratio)
        return CoachNudge(
            id = "daily_spike_$dayKey",
            type = NudgeType.DAILY_SPIKE,
            tone = if (ratio >= 2.2) NudgeTone.URGENT else NudgeTone.WARNING,
            emoji = "⚡",
            title = if (isVi) "Chi tiêu hôm nay tăng vọt" else "High spending today",
            message = if (isVi)
                "Hôm nay bạn đã chi ${todaySpent.toCurrencyString(currencyCode)}, gấp ${ratioStr}x mức trung bình ngày (${avgDaily.toCurrencyString(currencyCode)}). Hỏi Coach cách cân bằng lại nhé?"
            else
                "You've spent ${todaySpent.toCurrencyString(currencyCode)} today — ${ratioStr}x your usual daily average (${avgDaily.toCurrencyString(currencyCode)}).",
            actionLabel = if (isVi) "Hỏi AI Coach" else "Ask AI Coach",
            actionTarget = NudgeActionTarget.OPEN_CHAT,
            priority = 92
        )
    }

    private fun evaluateMonthEndOverspend(
        allTransactions: List<Transaction>,
        dayKey: String,
        isVi: Boolean,
        currencyCode: String,
        nowMillis: Long
    ): CoachNudge? {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysRemaining = totalDaysInMonth - dayOfMonth
        if (daysRemaining > 7 || dayOfMonth < 15) return null

        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        val thisMonthTxs = allTransactions.filter { tx ->
            val c = Calendar.getInstance().apply { timeInMillis = tx.date }
            (c.get(Calendar.MONTH) + 1) == month && c.get(Calendar.YEAR) == year
        }
        val monthIncome = thisMonthTxs.filter { it.type.equals("income", ignoreCase = true) }.sumOf { it.amount }
        val monthExpense = thisMonthTxs.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { it.amount }

        if (monthIncome <= 0.0 || monthExpense <= 0.0) return null

        val projectedExpense = (monthExpense / dayOfMonth.toDouble()) * totalDaysInMonth.toDouble()
        val projectedOverspend = projectedExpense - monthIncome
        if (projectedOverspend <= 0.0) return null

        return CoachNudge(
            id = "month_end_risk_$dayKey",
            type = NudgeType.MONTH_END_OVERSPEND_RISK,
            tone = NudgeTone.WARNING,
            emoji = "📊",
            title = if (isVi) "Nguy cơ bội chi cuối tháng" else "Month-end overspend risk",
            message = if (isVi)
                "Với nhịp độ hiện tại, cuối tháng bạn có thể chi vượt thu nhập khoảng ${projectedOverspend.toCurrencyString(currencyCode)}. Muốn xem gợi ý cắt giảm nhanh?"
            else
                "At this pace, month-end spending may exceed income by ~${projectedOverspend.toCurrencyString(currencyCode)}. Want quick cutback ideas?",
            actionLabel = if (isVi) "Xem gợi ý AI" else "Get AI Tips",
            actionTarget = NudgeActionTarget.OPEN_CHAT,
            priority = 80
        )
    }

    private fun evaluateWeeklySavingsWin(
        expenses: List<Transaction>,
        dayKey: String,
        isVi: Boolean,
        currencyCode: String,
        dnaArchetype: FinancialArchetype?,
        nowMillis: Long
    ): CoachNudge? {
        val thisWeekStart = nowMillis - WEEK_MS
        val prevWeekStart = nowMillis - 2L * WEEK_MS

        val thisWeekSpent = expenses.filter { it.date in thisWeekStart..nowMillis }.sumOf { it.amount }
        val prevWeekSpent = expenses.filter { it.date in prevWeekStart until thisWeekStart }.sumOf { it.amount }

        if (prevWeekSpent <= 0.0 || thisWeekSpent <= 0.0) return null
        val saved = prevWeekSpent - thisWeekSpent
        val savedPct = (saved / prevWeekSpent) * 100.0
        if (savedPct < 10.0) return null

        val personaSuffix = dnaArchetype?.let { arch ->
            if (isVi) " Đúng phong độ ${arch.titleVi}!" else " True ${arch.titleEn} energy!"
        } ?: ""

        return CoachNudge(
            id = "weekly_win_$dayKey",
            type = NudgeType.WEEKEND_SAVINGS_WIN,
            tone = NudgeTone.CELEBRATION,
            emoji = "✨",
            title = if (isVi) "Tuần này bạn tiết kiệm rất tốt!" else "Great savings week!",
            message = if (isVi)
                "7 ngày qua bạn chi ít hơn tuần trước ${saved.toCurrencyString(currencyCode)} (-${savedPct.toInt()}%).$personaSuffix"
            else
                "You spent ${saved.toCurrencyString(currencyCode)} less than last week (-${savedPct.toInt()}%).$personaSuffix",
            actionLabel = if (isVi) "Xem bản tin tuần" else "Weekly Digest",
            actionTarget = NudgeActionTarget.OPEN_WEEKLY_DIGEST,
            priority = 60
        )
    }

    private fun evaluateInactivity(
        allTransactions: List<Transaction>,
        dayKey: String,
        isVi: Boolean,
        nowMillis: Long
    ): CoachNudge? {
        if (allTransactions.isEmpty()) return null
        val latestDate = allTransactions.maxOf { it.date }
        val daysSilent = ((nowMillis - latestDate) / DAY_MS).toInt()
        if (daysSilent < INACTIVITY_DAYS_THRESHOLD) return null

        return CoachNudge(
            id = "inactivity_$dayKey",
            type = NudgeType.INACTIVITY_REMINDER,
            tone = NudgeTone.GENTLE,
            emoji = "👋",
            title = if (isVi) "Đã $daysSilent ngày chưa ghi chép" else "$daysSilent days since your last log",
            message = if (isVi)
                "Cập nhật nhanh vài khoản chi gần đây để biểu đồ dòng tiền và Financial DNA luôn chính xác nhé!"
            else
                "Log a quick recent expense to keep your cashflow forecast and Financial DNA sharp!",
            actionLabel = if (isVi) "Ghi nhanh" else "Log Now",
            actionTarget = NudgeActionTarget.ADD_TRANSACTION,
            priority = 45
        )
    }

    private fun formatDayKey(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }
}
