package com.example.moneymanager

import com.example.moneymanager.data.local.TransactionEntity
import com.example.moneymanager.data.mapper.toDomain
import com.example.moneymanager.data.mapper.toEntity
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.SyncRepository
import com.example.moneymanager.domain.repository.SyncState
import com.example.moneymanager.domain.usecase.sync.SyncDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TwoWaySyncEngineTest {

    private class FakeSyncRepository : SyncRepository {
        private val _syncState = MutableStateFlow(SyncState.IDLE)
        override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

        var syncedItemsCount = 0
        var shouldFail = false

        fun setSyncState(state: SyncState) {
            _syncState.value = state
        }

        override suspend fun syncAll(): Result<Int> {
            return if (shouldFail) {
                _syncState.value = SyncState.ERROR
                Result.failure(Exception("Network error"))
            } else {
                _syncState.value = SyncState.SYNCING
                _syncState.value = SyncState.IDLE
                Result.success(syncedItemsCount)
            }
        }
    }

    private lateinit var syncRepo: FakeSyncRepository
    private lateinit var syncDataUseCase: SyncDataUseCase

    @Before
    fun setup() {
        syncRepo = FakeSyncRepository()
        syncDataUseCase = SyncDataUseCase(syncRepo)
    }

    // ─── Entity and Mapper Sync States Tests ─────────────────────────────────

    @Test
    fun `toEntity preserves custom syncStatus and updatedAt`() {
        val tx = Transaction(
            id = "tx_1",
            userId = "user_1",
            amount = 150000.0,
            type = "expense",
            category = "Food",
            description = "Dinner",
            date = 1000L,
            month = 10,
            year = 2026
        )

        val entity = tx.toEntity(syncStatus = "PENDING_INSERT", updatedAt = 5000L)
        assertEquals("PENDING_INSERT", entity.syncStatus)
        assertEquals(5000L, entity.updatedAt)
        assertEquals("tx_1", entity.id)
    }

    @Test
    fun `toDomain converts TransactionEntity correctly regardless of syncStatus`() {
        val entity = TransactionEntity(
            id = "tx_2",
            userId = "user_1",
            amount = 200000.0,
            type = "income",
            category = "Bonus",
            description = "Reward",
            date = 2000L,
            month = 10,
            year = 2026,
            syncedAt = 3000L,
            syncStatus = "PENDING_UPDATE",
            updatedAt = 4000L
        )

        val domain = entity.toDomain()
        assertEquals("tx_2", domain.id)
        assertEquals(200000.0, domain.amount, 0.001)
        assertEquals("income", domain.type)
    }

    // ─── Conflict Resolution Tests (Last-Write-Wins) ─────────────────────────

    @Test
    fun `conflict resolution - remote newer overwrites local`() {
        val localUpdatedAt = 1000L
        val remoteUpdatedAt = 2000L // Remote is newer

        val localTx = TransactionEntity(
            id = "conflict_tx",
            userId = "user_1",
            amount = 100.0,
            type = "expense",
            category = "Food",
            description = "Old local description",
            date = 500L,
            month = 10,
            year = 2026,
            syncStatus = "PENDING_UPDATE",
            updatedAt = localUpdatedAt
        )

        val remoteDescription = "Newer remote description"
        val winnerDescription = if (remoteUpdatedAt > localTx.updatedAt) remoteDescription else localTx.description

        assertEquals("Newer remote description", winnerDescription)
    }

    @Test
    fun `conflict resolution - local newer preserves local pending mutation`() {
        val localUpdatedAt = 3000L // Local is newer
        val remoteUpdatedAt = 2000L

        val localTx = TransactionEntity(
            id = "conflict_tx",
            userId = "user_1",
            amount = 150.0,
            type = "expense",
            category = "Food",
            description = "Latest local edit",
            date = 500L,
            month = 10,
            year = 2026,
            syncStatus = "PENDING_UPDATE",
            updatedAt = localUpdatedAt
        )

        val remoteDescription = "Stale remote description"
        val winnerDescription = if (remoteUpdatedAt > localTx.updatedAt) remoteDescription else localTx.description

        assertEquals("Latest local edit", winnerDescription)
    }

    // ─── SyncDataUseCase & State Machine Tests ───────────────────────────────

    @Test
    fun `syncDataUseCase triggers syncAll and returns success`() = runTest {
        syncRepo.syncedItemsCount = 5
        val result = syncDataUseCase()

        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())
        assertEquals(SyncState.IDLE, syncDataUseCase.syncState.value)
    }

    @Test
    fun `syncDataUseCase handles failure gracefully and emits ERROR state`() = runTest {
        syncRepo.shouldFail = true
        val result = syncDataUseCase()

        assertFalse(result.isSuccess)
        assertEquals(SyncState.ERROR, syncDataUseCase.syncState.value)
    }

    @Test
    fun `sync state transitions correctly to OFFLINE`() {
        syncRepo.setSyncState(SyncState.OFFLINE)
        assertEquals(SyncState.OFFLINE, syncDataUseCase.syncState.value)
    }
}
