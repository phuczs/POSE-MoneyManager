package com.example.moneymanager.domain.usecase.export

import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.ExportRepository
import java.io.File
import javax.inject.Inject

class ExportTransactionsUseCase @Inject constructor(
    private val exportRepository: ExportRepository
) {
    suspend fun toCsv(transactions: List<Transaction>, fileName: String): Result<File> {
        return exportRepository.exportToCsv(transactions, fileName)
    }

    suspend fun toPdf(transactions: List<Transaction>, title: String, fileName: String): Result<File> {
        return exportRepository.exportToPdf(transactions, title, fileName)
    }
}
