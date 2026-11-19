package com.example.moneymanager.domain.usecase.ai

import com.example.moneymanager.domain.model.AnomalySeverity
import com.example.moneymanager.domain.model.AnomalyType
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.model.TransactionAnomaly
import com.example.moneymanager.util.toCurrencyString
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Pure on-device behavioral & statistical anomaly detector for financial transactions.
 *
 * Detects 5 classes of suspicious or accidental transactions:
 * 1. [AnomalyType.DUPLICATE_SUSPECT] — Near-identical transaction within 15 minutes
 * 2. [AnomalyType.AMOUNT_SPIKE] — Leave-one-out Z-score >= 2.0σ above category baseline
 * 3. [AnomalyType.FREQUENCY_SPIKE] — Unusually high same-day burst in a single category
 * 4. [AnomalyType.FIRST_TIME_CATEGORY_LARGE] — Brand-new category with a disproportionately large amount
 * 5. [AnomalyType.UNUSUAL_HOUR] — Meaningful late-night expense (00:00–04:59) deviating from user habit
 *
 * Runs 100% locally (< 50ms, zero network/AI token cost).
 */
class DetectTransactionAnomalyUseCase @Inject constructor() {

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val DUPLICATE_WINDOW_MS = 15L * 60 * 1000
        private const val SCAN_WINDOW_DAYS = 30L
        private const val MIN_CATEGORY_SAMPLES_FOR_ZSCORE = 3
        private const val ZSCORE_THRESHOLD = 2.0
        private const val SAME_DAY_BURST_THRESHOLD = 4
        private const val MIN_EXPENSES_FOR_NEW_CATEGORY = 8
        private const val MIN_EXPENSES_FOR_HOUR_BASELINE = 5
    }

    operator fun invoke(
        allTransactions: List<Transaction>,
        dismissedIds: Set<String> = emptySet(),
        nowMillis: Long = System.currentTimeMillis(),
        currencyCode: String = "VND"
    ): Map<String, TransactionAnomaly> {
        if (allTransactions.size < 2) return emptyMap()

        val cutoff = nowMillis - SCAN_WINDOW_DAYS * DAY_MS
        val candidates = allTransactions.filter {
            it.id.isNotBlank() &&
                it.id !in dismissedIds &&
                it.amount > 0.0 &&
                it.date >= cutoff
        }
        if (candidates.isEmpty()) return emptyMap()

        val allExpenses = allTransactions.filter {
            it.type.equals("expense", ignoreCase = true) && it.amount > 0.0
        }
        val expensesByCategory = allExpenses.groupBy { it.category.lowercase(Locale.ROOT) }
        val overallExpenseMean = if (allExpenses.isNotEmpty()) allExpenses.map { it.amount }.average() else 0.0

        // Late-night baseline ratio
        val calendar = Calendar.getInstance()
        val lateNightRatio = if (allExpenses.size >= MIN_EXPENSES_FOR_HOUR_BASELINE) {
            val lateCount = allExpenses.count { tx ->
                calendar.timeInMillis = tx.date
                calendar.get(Calendar.HOUR_OF_DAY) in 0..4
            }
            lateCount.toDouble() / allExpenses.size.toDouble()
        } else {
            1.0 // Insufficient baseline to flag unusual hour
        }

        // Precompute same-day category counts for frequency spikes
        val expensesByCategoryAndDay = allExpenses.groupBy { tx ->
            calendar.timeInMillis = tx.date
            val dayKey = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
            "${tx.category.lowercase(Locale.ROOT)}|$dayKey"
        }

        val result = LinkedHashMap<String, TransactionAnomaly>()

        for (tx in candidates) {
            val found = mutableListOf<TransactionAnomaly>()

            // 1. Duplicate Suspect (checks both income & expense)
            detectDuplicate(tx, allTransactions)?.let { found.add(it) }

            if (tx.type.equals("expense", ignoreCase = true)) {
                val catKey = tx.category.lowercase(Locale.ROOT)
                val categoryPeers = expensesByCategory[catKey].orEmpty()

                // 2. Amount Spike (Leave-one-out Z-score)
                detectAmountSpike(tx, categoryPeers, currencyCode)?.let { found.add(it) }

                // 3. Same-Day Frequency Spike
                detectFrequencySpike(tx, categoryPeers, expensesByCategoryAndDay, calendar)?.let { found.add(it) }

                // 4. First-Time Large Category
                detectFirstTimeLargeCategory(
                    candidate = tx,
                    categoryCount = categoryPeers.size,
                    totalExpensesCount = allExpenses.size,
                    overallExpenseMean = overallExpenseMean,
                    currencyCode = currencyCode
                )?.let { found.add(it) }

                // 5. Unusual Late-Night Hour
                detectUnusualHour(
                    candidate = tx,
                    lateNightRatio = lateNightRatio,
                    overallExpenseMean = overallExpenseMean,
                    currencyCode = currencyCode,
                    calendar = calendar
                )?.let { found.add(it) }
            }

            if (found.isNotEmpty()) {
                // Keep the highest-severity anomaly for clean UX
                val topAnomaly = found.maxBy { it.severity.rank * 10 + priorityTieBreaker(it.type) }
                result[tx.id] = topAnomaly
            }
        }

        return result
    }

    private fun priorityTieBreaker(type: AnomalyType): Int = when (type) {
        AnomalyType.DUPLICATE_SUSPECT -> 5
        AnomalyType.AMOUNT_SPIKE -> 4
        AnomalyType.FREQUENCY_SPIKE -> 3
        AnomalyType.FIRST_TIME_CATEGORY_LARGE -> 2
        AnomalyType.UNUSUAL_HOUR -> 1
    }

    private fun detectDuplicate(
        candidate: Transaction,
        allTransactions: List<Transaction>
    ): TransactionAnomaly? {
        val tolerance = (candidate.amount * 0.01).coerceAtLeast(0.01)
        val earlierMatch = allTransactions.firstOrNull { other ->
            other.id != candidate.id &&
                other.type.equals(candidate.type, ignoreCase = true) &&
                other.category.equals(candidate.category, ignoreCase = true) &&
                abs(other.amount - candidate.amount) <= tolerance &&
                (candidate.date > other.date || (candidate.date == other.date && candidate.id > other.id)) &&
                (candidate.date - other.date) <= DUPLICATE_WINDOW_MS
        } ?: return null

        val minutesApart = ((candidate.date - earlierMatch.date) / 60_000L).coerceAtLeast(0L)
        val timeLabelVi = if (minutesApart == 0L) "chưa đầy 1 phút" else "$minutesApart phút"
        val timeLabelEn = if (minutesApart == 0L) "less than 1 minute" else "$minutesApart min"

        return TransactionAnomaly(
            transactionId = candidate.id,
            type = AnomalyType.DUPLICATE_SUSPECT,
            severity = AnomalySeverity.HIGH,
            titleVi = "Nghi trùng lặp",
            titleEn = "Possible Duplicate",
            explanationVi = "Trùng số tiền và danh mục \"${candidate.category}\" với một giao dịch cách đó $timeLabelVi. Bạn có bấm nhầm 2 lần không?",
            explanationEn = "Matches another \"${candidate.category}\" transaction from $timeLabelEn earlier with the same amount. Was this entered twice?",
            relatedTransactionId = earlierMatch.id
        )
    }

    private fun detectAmountSpike(
        candidate: Transaction,
        categoryPeers: List<Transaction>,
        currencyCode: String
    ): TransactionAnomaly? {
        val baseline = categoryPeers.filter { it.id != candidate.id }.map { it.amount }
        if (baseline.size < MIN_CATEGORY_SAMPLES_FOR_ZSCORE) return null

        val mean = baseline.average()
        if (mean <= 0.0) return null

        val variance = baseline.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        val ratio = candidate.amount / mean

        val (triggered, zScore) = if (stdDev > 0.01) {
            val z = (candidate.amount - mean) / stdDev
            (z >= ZSCORE_THRESHOLD && ratio >= 1.8) to z
        } else {
            // Baseline transactions are identical (e.g., 30k, 30k, 30k)
            (ratio >= 2.5) to null
        }

        if (!triggered) return null

        val ratioText = String.format(Locale.US, "%.1f", ratio)
        val meanFormatted = mean.toCurrencyString(currencyCode)
        val severity = if (ratio >= 3.0 || (zScore != null && zScore >= 3.0)) {
            AnomalySeverity.HIGH
        } else {
            AnomalySeverity.MEDIUM
        }

        return TransactionAnomaly(
            transactionId = candidate.id,
            type = AnomalyType.AMOUNT_SPIKE,
            severity = severity,
            titleVi = "Cao bất thường",
            titleEn = "Unusual Spike",
            explanationVi = "Khoản chi này cao gấp ${ratioText}x mức trung bình của danh mục \"${candidate.category}\" ($meanFormatted).",
            explanationEn = "This expense is ${ratioText}x higher than your usual \"${candidate.category}\" average ($meanFormatted).",
            zScore = zScore
        )
    }

    private fun detectFrequencySpike(
        candidate: Transaction,
        categoryPeers: List<Transaction>,
        expensesByCategoryAndDay: Map<String, List<Transaction>>,
        calendar: Calendar
    ): TransactionAnomaly? {
        calendar.timeInMillis = candidate.date
        val dayKey = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
        val key = "${candidate.category.lowercase(Locale.ROOT)}|$dayKey"
        val sameDayList = expensesByCategoryAndDay[key].orEmpty()

        if (sameDayList.size < SAME_DAY_BURST_THRESHOLD) return null

        // Only flag the latest transaction in the burst to avoid cluttering every row
        val latestInBurst = sameDayList.maxByOrNull { it.date } ?: return null
        if (latestInBurst.id != candidate.id) return null

        val activeDaysForCategory = categoryPeers.map { tx ->
            calendar.timeInMillis = tx.date
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
        }.distinct().size.coerceAtLeast(1)

        val avgPerActiveDay = categoryPeers.size.toDouble() / activeDaysForCategory.toDouble()
        if (activeDaysForCategory >= 2 && avgPerActiveDay > 2.2) return null

        return TransactionAnomaly(
            transactionId = candidate.id,
            type = AnomalyType.FREQUENCY_SPIKE,
            severity = AnomalySeverity.MEDIUM,
            titleVi = "Tần suất dày",
            titleEn = "Rapid Spending",
            explanationVi = "Bạn có tới ${sameDayList.size} giao dịch \"${candidate.category}\" chỉ trong cùng 1 ngày.",
            explanationEn = "You recorded ${sameDayList.size} \"${candidate.category}\" transactions in a single day."
        )
    }

    private fun detectFirstTimeLargeCategory(
        candidate: Transaction,
        categoryCount: Int,
        totalExpensesCount: Int,
        overallExpenseMean: Double,
        currencyCode: String
    ): TransactionAnomaly? {
        if (totalExpensesCount < MIN_EXPENSES_FOR_NEW_CATEGORY) return null
        if (categoryCount != 1) return null
        if (overallExpenseMean <= 0.0 || candidate.amount < overallExpenseMean * 1.8) return null

        val meanFormatted = overallExpenseMean.toCurrencyString(currencyCode)
        return TransactionAnomaly(
            transactionId = candidate.id,
            type = AnomalyType.FIRST_TIME_CATEGORY_LARGE,
            severity = AnomalySeverity.MEDIUM,
            titleVi = "Danh mục mới",
            titleEn = "New Category",
            explanationVi = "Lần đầu tiên bạn chi vào \"${candidate.category}\" với số tiền lớn hơn đáng kể mức chi bình quân ($meanFormatted).",
            explanationEn = "First-ever expense in \"${candidate.category}\" and noticeably above your average transaction ($meanFormatted)."
        )
    }

    private fun detectUnusualHour(
        candidate: Transaction,
        lateNightRatio: Double,
        overallExpenseMean: Double,
        currencyCode: String,
        calendar: Calendar
    ): TransactionAnomaly? {
        if (lateNightRatio >= 0.15) return null

        calendar.timeInMillis = candidate.date
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (hour !in 0..4) return null

        val minMeaningfulAmount = if (currencyCode == "VND") 50_000.0 else 10.0
        if (candidate.amount < minMeaningfulAmount || candidate.amount < overallExpenseMean * 0.8) {
            return null
        }

        val minute = calendar.get(Calendar.MINUTE)
        val timeText = String.format(Locale.US, "%02d:%02d", hour, minute)
        return TransactionAnomaly(
            transactionId = candidate.id,
            type = AnomalyType.UNUSUAL_HOUR,
            severity = AnomalySeverity.LOW,
            titleVi = "Giờ khuya ($timeText)",
            titleEn = "Late Night ($timeText)",
            explanationVi = "Giao dịch được ghi nhận lúc $timeText — khác với khung giờ chi tiêu thường ngày của bạn.",
            explanationEn = "Recorded at $timeText, outside your usual spending hours."
        )
    }
}
