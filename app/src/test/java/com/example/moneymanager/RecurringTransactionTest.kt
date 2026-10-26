package com.example.moneymanager

import androidx.paging.PagingData
import com.example.moneymanager.domain.model.RecurringFrequency
import com.example.moneymanager.domain.model.RecurringTransaction
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.RecurringTransactionRepository
import com.example.moneymanager.domain.repository.TransactionRepository
import com.example.moneymanager.domain.usecase.recurring.DeleteRecurringTransactionUseCase
import com.example.moneymanager.domain.usecase.recurring.GetRecurringTransactionsUseCase
import com.example.moneymanager.domain.usecase.recurring.ProcessDueRecurringTransactionsUseCase
import com.example.moneymanager.domain.usecase.recurring.SaveRecurringTransactionUseCase
import com.example.moneymanager.domain.usecase.recurring.ToggleRecurringActiveUseCase
import com.example.moneymanager.domain.usecase.recurring.calculateNextDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.YearMonth
import java.util.Calendar

class RecurringTransactionTest {

    // ─── Fake Repositories ──────────────────────────────────────────────────

    private class FakeRecurringTransactionRepository : RecurringTransactionRepository {
        val items = mutableListOf<RecurringTransaction>()

        override fun getAllByUser(userId: String): Flow<List<RecurringTransaction>> =
            flowOf(items.filter { it.userId == userId })

        override suspend fun getDueItems(currentTime: Long): List<RecurringTransaction> =
            items.filter { it.isActive && it.nextExecutionDate <= currentTime }

        override suspend fun save(userId: String, item: RecurringTransaction): Result<Unit> {
            items.removeAll { it.id == item.id }
            items.add(item)
            return Result.success(Unit)
        }

        override suspend fun updateActiveStatus(userId: String, id: String, isActive: Boolean): Result<Unit> {
            val idx = items.indexOfFirst { it.id == id }
            if (idx != -1) {
                items[idx] = items[idx].copy(isActive = isActive)
            }
            return Result.success(Unit)
        }

        override suspend fun delete(userId: String, id: String): Result<Unit> {
            items.removeAll { it.id == id }
            return Result.success(Unit)
        }
    }

    private class FakeTransactionRepository : TransactionRepository {
        val addedTransactions = mutableListOf<Transaction>()

        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(addedTransactions)
        override fun getTransactionsByType(type: String): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByMonth(month: Int, year: Int): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByTypeAndMonth(type: String, yearMonth: YearMonth): Flow<List<Transaction>> = flowOf(emptyList())
        override fun searchTransactions(query: String, type: String, category: String, minAmount: Double?, maxAmount: Double?, startDate: Long?, endDate: Long?): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getPagedTransactions(type: String?, month: Int?, year: Int?): Flow<PagingData<Transaction>> = flowOf(PagingData.empty())

        override suspend fun addTransaction(transaction: Transaction): Result<Transaction> {
            addedTransactions.add(transaction)
            return Result.success(transaction)
        }

        override suspend fun updateTransaction(transaction: Transaction): Result<Transaction> {
            val idx = addedTransactions.indexOfFirst { it.id == transaction.id }
            if (idx != -1) addedTransactions[idx] = transaction
            return Result.success(transaction)
        }

        override suspend fun deleteTransaction(transactionId: String): Result<Unit> {
            addedTransactions.removeAll { it.id == transactionId }
            return Result.success(Unit)
        }

        override suspend fun getTransactionById(transactionId: String): Result<Transaction> {
            val tx = addedTransactions.firstOrNull { it.id == transactionId }
            return if (tx != null) Result.success(tx) else Result.failure(NoSuchElementException())
        }

        override suspend fun clearLocalCacheForUser(userId: String) {
            addedTransactions.removeAll { it.userId == userId }
        }
    }

    private lateinit var recurringRepo: FakeRecurringTransactionRepository
    private lateinit var transactionRepo: FakeTransactionRepository
    private lateinit var saveUseCase: SaveRecurringTransactionUseCase
    private lateinit var processDueUseCase: ProcessDueRecurringTransactionsUseCase
    private lateinit var toggleActiveUseCase: ToggleRecurringActiveUseCase
    private lateinit var deleteUseCase: DeleteRecurringTransactionUseCase
    private lateinit var getUseCase: GetRecurringTransactionsUseCase

    @Before
    fun setup() {
        recurringRepo = FakeRecurringTransactionRepository()
        transactionRepo = FakeTransactionRepository()
        saveUseCase = SaveRecurringTransactionUseCase(recurringRepo)
        processDueUseCase = ProcessDueRecurringTransactionsUseCase(recurringRepo, transactionRepo)
        toggleActiveUseCase = ToggleRecurringActiveUseCase(recurringRepo)
        deleteUseCase = DeleteRecurringTransactionUseCase(recurringRepo)
        getUseCase = GetRecurringTransactionsUseCase(recurringRepo)
    }

    // ─── calculateNextDate Tests ─────────────────────────────────────────────

    @Test
    fun `calculateNextDate advances daily frequency by 1 day`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 15, 10, 0, 0)
        }
        val start = cal.timeInMillis

        val next = calculateNextDate(start, RecurringFrequency.DAILY)

        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2026, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, nextCal.get(Calendar.MONTH))
        assertEquals(16, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculateNextDate advances daily frequency over month boundary`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 31, 10, 0, 0)
        }
        val next = calculateNextDate(cal.timeInMillis, RecurringFrequency.DAILY)
        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(Calendar.FEBRUARY, nextCal.get(Calendar.MONTH))
        assertEquals(1, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculateNextDate advances daily frequency over year boundary`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 31, 10, 0, 0)
        }
        val next = calculateNextDate(cal.timeInMillis, RecurringFrequency.DAILY)
        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2027, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, nextCal.get(Calendar.MONTH))
        assertEquals(1, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculateNextDate advances weekly frequency by 7 days`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 15, 10, 0, 0)
        }
        val next = calculateNextDate(cal.timeInMillis, RecurringFrequency.WEEKLY)
        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(22, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculateNextDate advances monthly frequency by 1 month`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 15, 10, 0, 0)
        }
        val next = calculateNextDate(cal.timeInMillis, RecurringFrequency.MONTHLY)
        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(Calendar.FEBRUARY, nextCal.get(Calendar.MONTH))
        assertEquals(15, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculateNextDate handles month-end rollover gracefully (Jan 31 to Feb 28)`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 31, 10, 0, 0)
        }
        val next = calculateNextDate(cal.timeInMillis, RecurringFrequency.MONTHLY)
        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(Calendar.FEBRUARY, nextCal.get(Calendar.MONTH))
        assertEquals(28, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculateNextDate advances yearly frequency by 1 year`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 15, 10, 0, 0)
        }
        val next = calculateNextDate(cal.timeInMillis, RecurringFrequency.YEARLY)
        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2027, nextCal.get(Calendar.YEAR))
    }

    // ─── SaveRecurringTransactionUseCase Tests ───────────────────────────────

    @Test
    fun `saveUseCase generates UUID if id is blank and computes nextDate if zero`() = runTest {
        val item = RecurringTransaction(
            id = "",
            userId = "user_1",
            title = "Gym Membership",
            amount = 500000.0,
            type = "expense",
            category = "Health",
            frequency = RecurringFrequency.MONTHLY,
            startDate = 1000000L,
            nextExecutionDate = 0L,
            isActive = true
        )

        val result = saveUseCase("user_1", item)
        assertTrue(result.isSuccess)
        assertEquals(1, recurringRepo.items.size)

        val saved = recurringRepo.items.first()
        assertTrue(saved.id.isNotBlank())
        assertEquals("Gym Membership", saved.title)
        assertTrue(saved.nextExecutionDate > item.startDate)
    }

    @Test
    fun `saveUseCase preserves explicit nextExecutionDate when provided`() = runTest {
        val explicitNext = 2000000L
        val item = RecurringTransaction(
            id = "existing_id",
            userId = "user_1",
            title = "Netflix",
            amount = 260000.0,
            type = "expense",
            category = "Entertainment",
            frequency = RecurringFrequency.MONTHLY,
            startDate = 1000000L,
            nextExecutionDate = explicitNext,
            isActive = true
        )

        val result = saveUseCase("user_1", item)
        assertTrue(result.isSuccess)

        val saved = recurringRepo.items.first()
        assertEquals("existing_id", saved.id)
        assertEquals(explicitNext, saved.nextExecutionDate)
    }

    // ─── ProcessDueRecurringTransactionsUseCase Tests ────────────────────────

    @Test
    fun `processDueUseCase does not process future items`() = runTest {
        val futureTime = System.currentTimeMillis() + 1000000L
        recurringRepo.items.add(
            RecurringTransaction(
                id = "item_future",
                userId = "user_1",
                title = "Future Bill",
                amount = 100.0,
                type = "expense",
                category = "Bills",
                frequency = RecurringFrequency.MONTHLY,
                startDate = futureTime,
                nextExecutionDate = futureTime,
                isActive = true
            )
        )

        val result = processDueUseCase()
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        assertEquals(0, transactionRepo.addedTransactions.size)
    }

    @Test
    fun `processDueUseCase processes due item, adds transaction, and advances nextExecutionDate`() = runTest {
        val now = System.currentTimeMillis()
        val pastDate = now - 1000L // 1 second ago
        val recurringItem = RecurringTransaction(
            id = "item_1",
            userId = "user_1",
            title = "Spotify",
            amount = 59000.0,
            type = "expense",
            category = "Entertainment",
            frequency = RecurringFrequency.MONTHLY,
            startDate = pastDate,
            nextExecutionDate = pastDate,
            isActive = true,
            description = "Family plan"
        )
        recurringRepo.items.add(recurringItem)

        val result = processDueUseCase()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())

        // 1. Transaction should have been added
        assertEquals(1, transactionRepo.addedTransactions.size)
        val addedTx = transactionRepo.addedTransactions.first()
        assertEquals("user_1", addedTx.userId)
        assertEquals(59000.0, addedTx.amount, 0.001)
        assertEquals("expense", addedTx.type)
        assertEquals("Entertainment", addedTx.category)
        assertEquals("Family plan", addedTx.description)
        assertEquals(pastDate, addedTx.date)

        // 2. Next execution date should be advanced into the future
        val updatedRecurring = recurringRepo.items.first { it.id == "item_1" }
        assertTrue(updatedRecurring.nextExecutionDate > now)
    }

    @Test
    fun `processDueUseCase ignores inactive items`() = runTest {
        val pastDate = System.currentTimeMillis() - 10000L
        recurringRepo.items.add(
            RecurringTransaction(
                id = "item_inactive",
                userId = "user_1",
                title = "Cancelled Subscription",
                amount = 100.0,
                type = "expense",
                category = "Bills",
                frequency = RecurringFrequency.DAILY,
                startDate = pastDate,
                nextExecutionDate = pastDate,
                isActive = false
            )
        )

        val result = processDueUseCase()
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        assertEquals(0, transactionRepo.addedTransactions.size)
    }

    @Test
    fun `processDueUseCase catches up missed cycles when item was due multiple days ago`() = runTest {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -3) // 3 days ago
        }
        val threeDaysAgo = cal.timeInMillis

        recurringRepo.items.add(
            RecurringTransaction(
                id = "daily_item",
                userId = "user_1",
                title = "Daily Coffee",
                amount = 30000.0,
                type = "expense",
                category = "Food",
                frequency = RecurringFrequency.DAILY,
                startDate = threeDaysAgo,
                nextExecutionDate = threeDaysAgo,
                isActive = true
            )
        )

        val result = processDueUseCase()
        assertTrue(result.isSuccess)
        // Should have caught up for 3 days ago, 2 days ago, 1 day ago, and today
        val processedCount = result.getOrNull() ?: 0
        assertTrue("Should process at least 3 missed daily occurrences", processedCount >= 3)
        assertEquals(processedCount, transactionRepo.addedTransactions.size)

        // After catch-up, next execution date should be in the future
        val updated = recurringRepo.items.first { it.id == "daily_item" }
        assertTrue(updated.nextExecutionDate > System.currentTimeMillis())
    }

    // ─── Toggle and Delete Tests ─────────────────────────────────────────────

    @Test
    fun `toggleActiveUseCase updates isActive correctly`() = runTest {
        recurringRepo.items.add(
            RecurringTransaction(
                id = "item_toggle",
                userId = "user_1",
                title = "Rent",
                amount = 5000000.0,
                type = "expense",
                category = "Housing",
                frequency = RecurringFrequency.MONTHLY,
                startDate = 1000L,
                nextExecutionDate = 2000L,
                isActive = true
            )
        )

        toggleActiveUseCase("user_1", "item_toggle", false)
        assertFalse(recurringRepo.items.first().isActive)

        toggleActiveUseCase("user_1", "item_toggle", true)
        assertTrue(recurringRepo.items.first().isActive)
    }

    @Test
    fun `deleteUseCase removes item from repository`() = runTest {
        recurringRepo.items.add(
            RecurringTransaction(
                id = "item_to_delete",
                userId = "user_1",
                title = "To Delete",
                amount = 10.0,
                type = "expense",
                category = "Other",
                frequency = RecurringFrequency.DAILY,
                startDate = 1000L,
                nextExecutionDate = 2000L,
                isActive = true
            )
        )

        deleteUseCase("user_1", "item_to_delete")
        assertTrue(recurringRepo.items.isEmpty())
    }

    @Test
    fun `getRecurringTransactionsUseCase returns user items`() = runTest {
        recurringRepo.items.add(
            RecurringTransaction(
                id = "item_u1",
                userId = "user_1",
                title = "User 1 Item",
                amount = 10.0,
                type = "expense",
                category = "Other",
                frequency = RecurringFrequency.DAILY,
                startDate = 1000L,
                nextExecutionDate = 2000L,
                isActive = true
            )
        )
        recurringRepo.items.add(
            RecurringTransaction(
                id = "item_u2",
                userId = "user_2",
                title = "User 2 Item",
                amount = 20.0,
                type = "expense",
                category = "Other",
                frequency = RecurringFrequency.DAILY,
                startDate = 1000L,
                nextExecutionDate = 2000L,
                isActive = true
            )
        )

        val u1Items = getUseCase("user_1").first()
        assertEquals(1, u1Items.size)
        assertEquals("User 1 Item", u1Items.first().title)
    }
}
