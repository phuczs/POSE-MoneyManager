package com.example.moneymanager.domain.usecase.sync

import com.example.moneymanager.domain.repository.SyncRepository
import com.example.moneymanager.domain.repository.SyncState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class SyncDataUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    val syncState: StateFlow<SyncState> get() = syncRepository.syncState

    suspend operator fun invoke(): Result<Int> {
        return syncRepository.syncAll()
    }
}
