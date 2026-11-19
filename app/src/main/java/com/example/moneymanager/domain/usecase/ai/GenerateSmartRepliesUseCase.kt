package com.example.moneymanager.domain.usecase.ai

import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.BudgetWithProgress
import com.example.moneymanager.domain.model.SmartReplyCategory
import com.example.moneymanager.domain.model.SmartReplyChip
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.model.TransactionAnomaly
import com.example.moneymanager.util.toCurrencyString
import java.util.Calendar
import javax.inject.Inject

/**
 * Generates up to 4 context-aware quick question chips for the AI Financial Advisor chat,
 * adapted in real time to the user's live wallet state:
 * - Unresolved anomalous/large recent transactions
 * - Budgets approaching or exceeding their cap
 * - Income-vs-expense deficit alerts
 * - Time of month (early-month review vs. mid/late-month forecast)
 *
 * 100% deterministic and on-device (< 5ms, zero AI token overhead).
 */
class GenerateSmartRepliesUseCase @Inject constructor() {

    operator fun invoke(
        allTransactions: List<Transaction>,
        activeBudgets: List<Budget>,
        anomalies: Map<String, TransactionAnomaly> = emptyMap(),
        languageCode: String = "vi",
        nowMillis: Long = System.currentTimeMillis()
    ): List<SmartReplyChip> {
        val isVi = languageCode == "vi"
        val currencyCode = if (isVi) "VND" else "USD"
        val chips = mutableListOf<SmartReplyChip>()

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)

        val thisMonthTxs = allTransactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
            (txCal.get(Calendar.MONTH) + 1) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }
        val monthIncome = thisMonthTxs.filter { it.type.equals("income", ignoreCase = true) }.sumOf { it.amount }
        val monthExpense = thisMonthTxs.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { it.amount }

        // 1. Highest priority: Recent high/medium anomaly or big expense
        val topAnomalyEntry = anomalies.values
            .sortedByDescending { it.severity.rank }
            .firstOrNull()
        val anomalousTx = topAnomalyEntry?.let { a -> allTransactions.firstOrNull { it.id == a.transactionId } }
        if (anomalousTx != null) {
            val formattedAmount = anomalousTx.amount.toCurrencyString(currencyCode)
            chips.add(
                SmartReplyChip(
                    id = "anomaly_${anomalousTx.id}",
                    label = if (isVi) "⚠️ Khoản ${anomalousTx.category} $formattedAmount?"
                            else "⚠️ Impact of $formattedAmount ${anomalousTx.category}?",
                    promptText = if (isVi)
                        "Khoản chi ${anomalousTx.category} $formattedAmount gần đây có ảnh hưởng lớn đến ngân sách tháng này không? Tôi nên điều chỉnh sao?"
                    else
                        "Does my recent $formattedAmount ${anomalousTx.category} expense hurt this month's budget? How should I adjust?",
                    category = SmartReplyCategory.ANOMALY
                )
            )
        }

        // 2. Budget under pressure (>= 75% used)
        val riskyBudget = activeBudgets.map { budget ->
            val spent = allTransactions.filter { tx ->
                tx.type.equals("expense", ignoreCase = true) &&
                    tx.category.equals(budget.category, ignoreCase = true) &&
                    tx.date in budget.startDate..budget.endDate
            }.sumOf { it.amount }
            BudgetWithProgress(budget, spent)
        }.filter { it.progress >= 0.75f }
            .maxByOrNull { it.progress }

        if (riskyBudget != null) {
            val cat = riskyBudget.budget.category
            val pct = (riskyBudget.progress * 100).toInt()
            chips.add(
                SmartReplyChip(
                    id = "budget_${riskyBudget.budget.id}",
                    label = if (isVi) "🔥 Cứu ngân sách $cat ($pct%)"
                            else "🔥 Save my $cat budget ($pct%)",
                    promptText = if (isVi)
                        "Ngân sách $cat của tôi đã dùng $pct% rồi. Hãy gợi ý 3 cách cụ thể để tiết kiệm danh mục này trong phần còn lại của chu kỳ."
                    else
                        "My $cat budget is already at $pct%. Give me 3 concrete ways to cut back for the rest of this cycle.",
                    category = SmartReplyCategory.BUDGET
                )
            )
        }

        // 3. Deficit warning (expense > income when income > 0)
        if (monthIncome > 0.0 && monthExpense > monthIncome) {
            chips.add(
                SmartReplyChip(
                    id = "deficit_alert",
                    label = if (isVi) "🚨 Đang bội chi, cần làm gì?"
                            else "🚨 Spending exceeds income",
                    promptText = if (isVi)
                        "Chi tiêu tháng này đang vượt thu nhập. Tôi nên cắt giảm danh mục nào trước tiên để cân bằng lại dòng tiền?"
                    else
                        "My spending exceeds income this month. Which categories should I cut first to rebalance?",
                    category = SmartReplyCategory.ALERT
                )
            )
        }

        // 4. Time-of-month tailored prompts
        if (dayOfMonth <= 7) {
            chips.add(
                SmartReplyChip(
                    id = "early_last_month",
                    label = if (isVi) "📊 Tổng kết tháng trước" else "📊 Review last month",
                    promptText = if (isVi)
                        "Tháng trước tôi chi tiêu thế nào và có điểm gì cần rút kinh nghiệm cho tháng mới?"
                    else
                        "How did my spending look last month, and what should I improve this new month?",
                    category = SmartReplyCategory.INSIGHT
                )
            )
            chips.add(
                SmartReplyChip(
                    id = "early_plan_budget",
                    label = if (isVi) "🎯 Lập ngân sách tháng này" else "🎯 Plan this month's budget",
                    promptText = if (isVi)
                        "Dựa trên lịch sử chi tiêu của tôi, hãy gợi ý phân bổ ngân sách hợp lý cho tháng này."
                    else
                        "Based on my spending history, suggest an optimal budget split for this month.",
                    category = SmartReplyCategory.BUDGET
                )
            )
        } else {
            chips.add(
                SmartReplyChip(
                    id = "mid_forecast",
                    label = if (isVi) "📈 Dự báo số dư cuối tháng" else "📈 Month-end forecast",
                    promptText = if (isVi)
                        "Với tốc độ chi tiêu hiện tại, dự báo cuối tháng tôi còn dư bao nhiêu tiền?"
                    else
                        "At my current spending pace, how much balance will I have left at month-end?",
                    category = SmartReplyCategory.INSIGHT
                )
            )
            chips.add(
                SmartReplyChip(
                    id = "mid_cutback",
                    label = if (isVi) "✂️ Nên cắt giảm khoản nào?" else "✂️ Where can I cut back?",
                    promptText = if (isVi)
                        "Phân tích top chi tiêu tháng này và chỉ ra 2 danh mục tôi có thể cắt giảm dễ nhất."
                    else
                        "Analyze my top expenses this month and point out the 2 easiest categories to trim.",
                    category = SmartReplyCategory.INSIGHT
                )
            )
        }

        // 5. Evergreen health check fallback
        chips.add(
            SmartReplyChip(
                id = "health_check",
                label = if (isVi) "💡 Đánh giá sức khỏe ví" else "💡 Wallet health check",
                promptText = if (isVi)
                    "Đánh giá ngắn gọn sức khỏe tài chính hiện tại của tôi và cho 1 lời khuyên hành động ngay hôm nay."
                else
                    "Give me a quick wallet health check and 1 actionable tip I can apply today.",
                category = SmartReplyCategory.INSIGHT
            )
        )

        return chips.distinctBy { it.id }.take(4)
    }
}
