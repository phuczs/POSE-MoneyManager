package com.example.moneymanager.domain.repository

import kotlinx.coroutines.flow.StateFlow

enum class SyncState {
    IDLE,
    SYNCING,
    OFFLINE,
    ERROR
}

interface SyncRepository {
    val syncState: StateFlow<SyncState>
    suspend fun syncAll(): Result<Int>
}
