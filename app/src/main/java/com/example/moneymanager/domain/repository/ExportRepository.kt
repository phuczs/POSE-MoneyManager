package com.example.moneymanager.domain.repository

import com.example.moneymanager.domain.model.Transaction
import java.io.File

interface ExportRepository {
    suspend fun exportToCsv(transactions: List<Transaction>, fileName: String): Result<File>
    suspend fun exportToPdf(transactions: List<Transaction>, title: String, fileName: String): Result<File>
}
