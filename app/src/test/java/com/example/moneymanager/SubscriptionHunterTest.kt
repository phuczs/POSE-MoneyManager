package com.example.moneymanager

import com.example.moneymanager.domain.model.ChallengeStatus
import com.example.moneymanager.domain.model.DetectedSubscription
import com.example.moneymanager.domain.model.RecurringFrequency
import com.example.moneymanager.domain.model.RecurringTransaction
import com.example.moneymanager.domain.model.SavingsChallenge
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.RecurringTransactionRepository
import com.example.moneymanager.domain.usecase.ai.ConvertSubscriptionToRecurringUseCase
import com.example.moneymanager.domain.usecase.ai.DetectSubscriptionsUseCase
import com.example.moneymanager.domain.usecase.ai.GenerateChallengesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Unit test suite for Phase 5.4: Gamified Savings & Subscription Hunter.
 *
 * Verifies:
 * 1. Subscription Hunter accurately identifies >= 85% of popular subscriptions.
 * 2. Cycle detection identifies periodic subscriptions from repeating timestamps.
 * 3. Already tracked recurring items are correctly flagged.
 * 4. 1-Tap Convert to RecurringTransaction works and preserves metadata.
 * 5. Savings challenge generates contextual challenges based on transaction habits.
 * 6. Real-time streak and saved amount calculation functions correctly.
 */
class SubscriptionHunterTest {

    private lateinit var detectSubscriptionsUseCase: DetectSubscriptionsUseCase
    private lateinit var generateChallengesUseCase: GenerateChallengesUseCase
    private lateinit var convertSubscriptionUseCase: ConvertSubscriptionToRecurringUseCase
    private lateinit var mockRecurringRepository: RecurringTransactionRepository

    @Before
    fun setUp() {
        detectSubscriptionsUseCase = DetectSubscriptionsUseCase()
        generateChallengesUseCase = GenerateChallengesUseCase()
        mockRecurringRepository = mockk(relaxed = true)
        convertSubscriptionUseCase = ConvertSubscriptionToRecurringUseCase(mockRecurringRepository)
    }

    @Test
    fun `detectSubscriptions identifies popular subscription services with at least 85 percent accuracy`() {
        // Danh sách 20 dịch vụ đăng ký phổ biến
        val testServices = listOf(
            "Netflix Goi Cao Cap" to "netflix",
            "Spotify Premium Monthly" to "spotify",
            "YouTube Premium Gia Dinh" to "youtube",
            "APPLE.COM/BILL ICLOUD STORAGE" to "apple_services",
            "Google One 100GB" to "google_services",
            "ChatGPT Plus subscription" to "chatgpt",
            "Canva Pro Annual" to "canva",
            "CapCut Pro Thang" to "capcut",
            "Adobe Creative Cloud Photography" to "adobe",
            "Microsoft 365 Family" to "microsoft",
            "Disney+ Hotstar" to "disney",
            "VieON VIP HBO GO" to "vieon",
            "FPT Play Goi VIP" to "fpt_play",
            "Zing MP3 VIP 1 Nam" to "zingmp3",
            "FPT Telecom Cuoc Internet thang 10" to "internet_telco",
            "Viettel Internet cap quang" to "internet_telco",
            "California Fitness & Yoga hoi vien" to "gym_fitness",
            "Duolingo Super Annual" to "duolingo",
            "Notion Plus Workspace" to "notion",
            "GitHub Copilot Individual" to "github"
        )

        val transactions = testServices.mapIndexed { index, (desc, _) ->
            Transaction(
                id = "tx_$index",
                userId = "test_user",
                amount = 150000.0,
                type = "expense",
                category = "Bills & Utilities",
                description = desc,
                date = System.currentTimeMillis() - (index * 1000L * 60)
            )
        }

        val detected = detectSubscriptionsUseCase(transactions)
        val detectedServiceKeys = detected.map { it.serviceKey }.toSet()

        var recognizedCount = 0
        for ((_, expectedKey) in testServices) {
            if (expectedKey in detectedServiceKeys) {
                recognizedCount++
            }
        }

        val accuracyRate = recognizedCount.toDouble() / testServices.size.toDouble()
        println("Subscription Detection Accuracy: ${accuracyRate * 100}% ($recognizedCount / ${testServices.size})")

        // Tiêu chí: >= 85%
        assertTrue("Accuracy must be >= 85%", accuracyRate >= 0.85)
        assertEquals(testServices.size, recognizedCount) // Đạt 100%
    }

    @Test
    fun `detectSubscriptions detects generic periodic cycles for unbranded expenses`() {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000

        // 3 giao dịch cùng mô tả "Tien thue phong tro" cách nhau 30 ngày
        val cycleTxs = listOf(
            Transaction(id = "c1", amount = 3500000.0, type = "expense", description = "Tien thue phong tro", date = now - (60 * dayMillis)),
            Transaction(id = "c2", amount = 3500000.0, type = "expense", description = "Tien thue phong tro", date = now - (30 * dayMillis)),
            Transaction(id = "c3", amount = 3500000.0, type = "expense", description = "Tien thue phong tro", date = now)
        )

        val detected = detectSubscriptionsUseCase(cycleTxs)
        val cycleSub = detected.firstOrNull { it.name.contains("Tien thue phong tro", ignoreCase = true) }

        assertNotNull(cycleSub)
        assertEquals(RecurringFrequency.MONTHLY, cycleSub?.frequency)
        assertEquals(3, cycleSub?.detectedCycleCount)
        assertEquals(3500000.0, cycleSub?.amount ?: 0.0, 0.01)
    }

    @Test
    fun `detectSubscriptions flags already tracked recurring transactions`() {
        val transactions = listOf(
            Transaction(id = "tx1", amount = 260000.0, type = "expense", description = "Netflix Premium", date = System.currentTimeMillis()),
            Transaction(id = "tx2", amount = 59000.0, type = "expense", description = "Spotify Individual", date = System.currentTimeMillis())
        )

        val existingRecurring = listOf(
            RecurringTransaction(
                id = "rec1",
                userId = "user_1",
                title = "Netflix",
                amount = 260000.0,
                frequency = RecurringFrequency.MONTHLY
            )
        )

        val detected = detectSubscriptionsUseCase(transactions, existingRecurring)
        val netflix = detected.firstOrNull { it.serviceKey == "netflix" }
        val spotify = detected.firstOrNull { it.serviceKey == "spotify" }

        assertNotNull(netflix)
        assertTrue("Netflix should be marked as already tracked", netflix!!.isAlreadyTracked)

        assertNotNull(spotify)
        assertFalse("Spotify should not be marked as tracked", spotify!!.isAlreadyTracked)
    }

    @Test
    fun `convertSubscriptionToRecurring preserves metadata and saves via repository`() = runTest {
        val captured = slot<RecurringTransaction>()
        coEvery { mockRecurringRepository.save(any(), capture(captured)) } returns Result.success(Unit)

        val subscription = DetectedSubscription(
            id = "sub_test_1",
            name = "Spotify Premium",
            serviceKey = "spotify",
            category = "Entertainment",
            amount = 59000.0,
            frequency = RecurringFrequency.MONTHLY,
            lastPaymentDate = 1000000L,
            nextExpectedDate = 2000000L
        )

        val result = convertSubscriptionUseCase("user_123", subscription)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { mockRecurringRepository.save("user_123", any()) }
        assertEquals("Spotify Premium", captured.captured.title)
        assertEquals(59000.0, captured.captured.amount, 0.01)
        assertEquals("Entertainment", captured.captured.category)
        assertEquals(RecurringFrequency.MONTHLY, captured.captured.frequency)
        assertEquals(2000000L, captured.captured.nextExecutionDate)
    }

    @Test
    fun `generateChallenges proposes coffee and shopping challenges when relevant expenses exist`() {
        val now = System.currentTimeMillis()
        val txs = listOf(
            Transaction(id = "t1", amount = 45000.0, type = "expense", category = "Food & Beverage", description = "Highlands Coffee", date = now),
            Transaction(id = "t2", amount = 55000.0, type = "expense", category = "Food & Beverage", description = "Tra sua Phuc Long", date = now),
            Transaction(id = "t3", amount = 250000.0, type = "expense", category = "Shopping", description = "Shopee don hang quan ao", date = now)
        )

        val suggestions = generateChallengesUseCase.generateSuggestions(txs)

        assertTrue(suggestions.isNotEmpty())
        val cafeChallenge = suggestions.firstOrNull { it.id == "challenge_cafe_fasting" }
        val noBuyChallenge = suggestions.firstOrNull { it.id == "challenge_no_buy_week" }

        assertNotNull(cafeChallenge)
        assertNotNull(noBuyChallenge)
        assertEquals(7, cafeChallenge?.targetDays)
        assertEquals(0.0, cafeChallenge?.dailyBudgetLimit ?: -1.0, 0.01)
        assertTrue((cafeChallenge?.targetSavings ?: 0.0) > 0)
    }

    @Test
    fun `evaluateProgress calculates real-time streak and saved amount correctly`() {
        val dayMillis = 24L * 60 * 60 * 1000
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis
        val startDay = todayStart - (3 * dayMillis) // Bắt đầu từ 3 ngày trước

        val challenge = SavingsChallenge(
            id = "test_cafe_challenge",
            title = "1 Tuần Không Cà Phê Hàng Quán",
            category = "Food & Beverage",
            keywordFilter = "(?i)\\b(cafe|cà phê|coffee)\\b",
            targetDays = 7,
            currentStreak = 0,
            dailyBudgetLimit = 0.0,
            baselineDailyAverage = 50000.0,
            startDate = startDay,
            endDate = startDay + (7 * dayMillis),
            status = ChallengeStatus.ACTIVE
        )

        // Người dùng không chi tiêu cafe trong 3 ngày qua (chỉ có chi tiền mua sách không liên quan)
        val txs = listOf(
            Transaction(id = "tx1", amount = 100000.0, type = "expense", category = "Education", description = "Mua sach", date = startDay + 1000L),
            Transaction(id = "tx2", amount = 50000.0, type = "expense", category = "Transport", description = "Do xang", date = startDay + dayMillis + 1000L)
        )

        val evaluated = generateChallengesUseCase.evaluateProgress(challenge, txs, now = todayStart + (12 * 60 * 60 * 1000))

        // Đã qua 4 ngày (startDay, startDay+1, startDay+2, today) không có chi tiêu cafe nào
        assertEquals(4, evaluated.currentStreak)
        assertEquals(4 * 50000.0, evaluated.savedAmount, 0.01)
        assertEquals(ChallengeStatus.ACTIVE, evaluated.status)
    }

    @Test
    fun `evaluateProgress marks challenge completed when streak reaches target days`() {
        val dayMillis = 24L * 60 * 60 * 1000
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis
        val startDay = todayStart - (5 * dayMillis)

        val challenge = SavingsChallenge(
            id = "test_home_cooking",
            title = "5 Ngày Nấu Ăn Tại Nhà",
            category = "Food & Beverage",
            keywordFilter = "(?i)\\b(nhà hàng|buffet)\\b",
            targetDays = 5,
            currentStreak = 0,
            dailyBudgetLimit = 0.0,
            baselineDailyAverage = 70000.0,
            startDate = startDay,
            endDate = startDay + (5 * dayMillis),
            status = ChallengeStatus.ACTIVE
        )

        // Không có giao dịch vi phạm nào
        val evaluated = generateChallengesUseCase.evaluateProgress(challenge, emptyList(), now = todayStart)

        assertEquals(6, evaluated.currentStreak) // 6 ngày liên tiếp đạt chỉ tiêu
        assertEquals(ChallengeStatus.COMPLETED, evaluated.status)
        assertTrue(evaluated.savedAmount >= (5 * 70000.0))
    }
}
