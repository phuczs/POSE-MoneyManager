package com.example.moneymanager

import com.example.moneymanager.domain.model.AnomalySeverity
import com.example.moneymanager.domain.model.AnomalyType
import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.SmartReplyCategory
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.usecase.ai.DetectTransactionAnomalyUseCase
import com.example.moneymanager.domain.usecase.ai.GenerateSmartRepliesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AnomalyDetectiveTest {

    private val detector = DetectTransactionAnomalyUseCase()
    private val smartRepliesGenerator = GenerateSmartRepliesUseCase()

    private fun epoch(dayOfMonth: Int, hour: Int = 12, minute: Int = 0): Long {
        return Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, dayOfMonth, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private val refNow = epoch(dayOfMonth = 20, hour = 18, minute = 0)

    @Test
    fun duplicateSuspect_flagsSecondTransactionWithin15Minutes() {
        val tx1 = Transaction(id = "t1", amount = 45_000.0, type = "expense", category = "Transport", date = epoch(20, 10, 0))
        val tx2 = Transaction(id = "t2", amount = 45_000.0, type = "expense", category = "Transport", date = epoch(20, 10, 8))

        val anomalies = detector(listOf(tx1, tx2), nowMillis = refNow)

        assertNull("Original transaction should not be flagged", anomalies["t1"])
        val flagged = anomalies["t2"]
        assertNotNull("Duplicate transaction should be flagged", flagged)
        assertEquals(AnomalyType.DUPLICATE_SUSPECT, flagged!!.type)
        assertEquals(AnomalySeverity.HIGH, flagged.severity)
        assertEquals("t1", flagged.relatedTransactionId)
    }

    @Test
    fun duplicateSuspect_ignoresTransactionsBeyond15Minutes() {
        val tx1 = Transaction(id = "t1", amount = 45_000.0, type = "expense", category = "Transport", date = epoch(20, 10, 0))
        val tx2 = Transaction(id = "t2", amount = 45_000.0, type = "expense", category = "Transport", date = epoch(20, 10, 30))

        val anomalies = detector(listOf(tx1, tx2), nowMillis = refNow)
        assertTrue(anomalies.isEmpty())
    }

    @Test
    fun amountSpike_flagsOutlierWithHighZScore() {
        val txs = listOf(
            Transaction(id = "c1", amount = 35_000.0, type = "expense", category = "Coffee", date = epoch(10, 9)),
            Transaction(id = "c2", amount = 40_000.0, type = "expense", category = "Coffee", date = epoch(12, 9)),
            Transaction(id = "c3", amount = 45_000.0, type = "expense", category = "Coffee", date = epoch(14, 9)),
            Transaction(id = "c4", amount = 38_000.0, type = "expense", category = "Coffee", date = epoch(16, 9)),
            Transaction(id = "c5", amount = 500_000.0, type = "expense", category = "Coffee", date = epoch(19, 14))
        )

        val anomalies = detector(txs, nowMillis = refNow)
        val spike = anomalies["c5"]
        assertNotNull("500k coffee spike should be flagged", spike)
        assertEquals(AnomalyType.AMOUNT_SPIKE, spike!!.type)
        assertEquals(AnomalySeverity.HIGH, spike.severity)
        assertNotNull(spike.zScore)
        assertTrue(spike.zScore!! >= 2.0)
    }

    @Test
    fun amountSpike_flagsOutlierWhenBaselineStdDevIsZero() {
        val txs = listOf(
            Transaction(id = "c1", amount = 30_000.0, type = "expense", category = "Coffee", date = epoch(10, 9)),
            Transaction(id = "c2", amount = 30_000.0, type = "expense", category = "Coffee", date = epoch(12, 9)),
            Transaction(id = "c3", amount = 30_000.0, type = "expense", category = "Coffee", date = epoch(14, 9)),
            Transaction(id = "c4", amount = 120_000.0, type = "expense", category = "Coffee", date = epoch(18, 9))
        )

        val anomalies = detector(txs, nowMillis = refNow)
        val spike = anomalies["c4"]
        assertNotNull("4x spike over constant baseline should be flagged", spike)
        assertEquals(AnomalyType.AMOUNT_SPIKE, spike!!.type)
    }

    @Test
    fun frequencySpike_flagsFourSameDayTransactionsInCategory() {
        val txs = listOf(
            // Normal historical pace: 1 per day
            Transaction(id = "h1", amount = 100_000.0, type = "expense", category = "Shopping", date = epoch(5, 11)),
            Transaction(id = "h2", amount = 110_000.0, type = "expense", category = "Shopping", date = epoch(10, 15)),
            // Burst of 4 on day 19 (spaced > 15m apart so not duplicates)
            Transaction(id = "b1", amount = 95_000.0, type = "expense", category = "Shopping", date = epoch(19, 9, 0)),
            Transaction(id = "b2", amount = 105_000.0, type = "expense", category = "Shopping", date = epoch(19, 11, 0)),
            Transaction(id = "b3", amount = 90_000.0, type = "expense", category = "Shopping", date = epoch(19, 14, 0)),
            Transaction(id = "b4", amount = 115_000.0, type = "expense", category = "Shopping", date = epoch(19, 17, 0))
        )

        val anomalies = detector(txs, nowMillis = refNow)
        val burst = anomalies["b4"]
        assertNotNull("4th same-day transaction should trigger frequency spike", burst)
        assertEquals(AnomalyType.FREQUENCY_SPIKE, burst!!.type)
        assertEquals(AnomalySeverity.MEDIUM, burst.severity)
    }

    @Test
    fun firstTimeCategoryLarge_flagsLargeNewCategoryExpense() {
        val normalTxs = (1..8).map { i ->
            Transaction(
                id = "n$i",
                amount = 60_000.0,
                type = "expense",
                category = "Food",
                date = epoch(i, 12)
            )
        }
        val brandNewLarge = Transaction(
            id = "new1",
            amount = 900_000.0,
            type = "expense",
            category = "Luxury",
            date = epoch(19, 15)
        )

        val anomalies = detector(normalTxs + brandNewLarge, nowMillis = refNow)
        val flagged = anomalies["new1"]
        assertNotNull(flagged)
        assertEquals(AnomalyType.FIRST_TIME_CATEGORY_LARGE, flagged!!.type)
    }

    @Test
    fun unusualHour_flagsLateNightMeaningfulExpense() {
        val daytimeTxs = (1..8).map { i ->
            Transaction(
                id = "d$i",
                amount = 80_000.0,
                type = "expense",
                category = "Food",
                date = epoch(i, 13)
            )
        }
        val lateNightTx = Transaction(
            id = "night1",
            amount = 90_000.0,
            type = "expense",
            category = "Food",
            date = epoch(19, 2, 45)
        )

        val anomalies = detector(daytimeTxs + lateNightTx, nowMillis = refNow)
        val flagged = anomalies["night1"]
        assertNotNull("2:45 AM transaction should trigger UNUSUAL_HOUR", flagged)
        assertEquals(AnomalyType.UNUSUAL_HOUR, flagged!!.type)
        assertEquals(AnomalySeverity.LOW, flagged.severity)
    }

    @Test
    fun dismissedIds_areExcludedFromResults() {
        val tx1 = Transaction(id = "t1", amount = 45_000.0, type = "expense", category = "Transport", date = epoch(20, 10, 0))
        val tx2 = Transaction(id = "t2", amount = 45_000.0, type = "expense", category = "Transport", date = epoch(20, 10, 5))

        val anomalies = detector(listOf(tx1, tx2), dismissedIds = setOf("t2"), nowMillis = refNow)
        assertFalse(anomalies.containsKey("t2"))
    }

    @Test
    fun smartReplies_prioritizesAnomalyAndRiskyBudgetAndDeficit() {
        val txs = listOf(
            Transaction(id = "inc1", amount = 5_000_000.0, type = "income", category = "Salary", date = epoch(1, 9)),
            Transaction(id = "e1", amount = 1_500_000.0, type = "expense", category = "Food", date = epoch(5, 12)),
            Transaction(id = "e2", amount = 1_500_000.0, type = "expense", category = "Food", date = epoch(10, 12)),
            Transaction(id = "e3", amount = 1_500_000.0, type = "expense", category = "Food", date = epoch(15, 12)),
            Transaction(id = "e4", amount = 4_500_000.0, type = "expense", category = "Food", date = epoch(19, 12))
        )
        val budgets = listOf(
            Budget(
                id = "b_food",
                category = "Food",
                allocatedAmount = 5_000_000.0,
                startDate = epoch(1, 0),
                endDate = epoch(31, 23)
            )
        )
        val anomalies = detector(txs, nowMillis = refNow)
        val replies = smartRepliesGenerator(
            allTransactions = txs,
            activeBudgets = budgets,
            anomalies = anomalies,
            languageCode = "vi",
            nowMillis = refNow
        )

        assertEquals(4, replies.size)
        assertEquals(SmartReplyCategory.ANOMALY, replies[0].category)
        assertEquals(SmartReplyCategory.BUDGET, replies[1].category)
        assertEquals(SmartReplyCategory.ALERT, replies[2].category)
    }

    @Test
    fun smartReplies_adaptsToEarlyMonthVsMidMonth() {
        val earlyNow = epoch(dayOfMonth = 3, hour = 10)
        val earlyReplies = smartRepliesGenerator(
            allTransactions = emptyList(),
            activeBudgets = emptyList(),
            languageCode = "en",
            nowMillis = earlyNow
        )
        assertTrue(earlyReplies.any { it.id == "early_last_month" })
        assertTrue(earlyReplies.any { it.id == "early_plan_budget" })

        val midNow = epoch(dayOfMonth = 18, hour = 10)
        val midReplies = smartRepliesGenerator(
            allTransactions = emptyList(),
            activeBudgets = emptyList(),
            languageCode = "en",
            nowMillis = midNow
        )
        assertTrue(midReplies.any { it.id == "mid_forecast" })
        assertTrue(midReplies.any { it.id == "mid_cutback" })
    }
}
