package com.example.moneymanager.data.repository

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.repository.ExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

import android.os.Environment

class AndroidExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ExportRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private fun getExportDirectory(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!publicDownloads.exists()) {
            publicDownloads.mkdirs()
        }
        return if (publicDownloads.exists()) {
            publicDownloads
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        }
    }

    override suspend fun exportToCsv(
        transactions: List<Transaction>,
        fileName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = getExportDirectory()
            val file = File(exportDir, if (fileName.endsWith(".csv")) fileName else "$fileName.csv")

            val sb = java.lang.StringBuilder()
            // UTF-8 BOM for Excel compatibility
            sb.append('\uFEFF')
            // Header
            sb.append("ID,Date,Type,Category,Amount,Description\n")

            for (tx in transactions) {
                val dateStr = dateFormat.format(Date(tx.date))
                val safeDesc = "\"" + tx.description.replace("\"", "\"\"") + "\""
                val safeCat = "\"" + tx.category.replace("\"", "\"\"") + "\""
                sb.append("${tx.id},$dateStr,${tx.type},$safeCat,${tx.amount},$safeDesc\n")
            }

            file.writeText(sb.toString(), Charsets.UTF_8)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportToPdf(
        transactions: List<Transaction>,
        title: String,
        fileName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = getExportDirectory()
            val file = File(exportDir, if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf")

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait: 595 x 842 pt
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#1B5E20")
                textSize = 20f
                isFakeBoldText = true
            }

            val headerPaint = Paint().apply {
                color = Color.parseColor("#333333")
                textSize = 11f
                isFakeBoldText = true
            }

            val textPaint = Paint().apply {
                color = Color.parseColor("#444444")
                textSize = 10f
            }

            val linePaint = Paint().apply {
                color = Color.parseColor("#CCCCCC")
                strokeWidth = 1f
            }

            var y = 50f

            // Title
            canvas.drawText(title, 40f, y, titlePaint)
            y += 24f

            // Generated date
            canvas.drawText("Generated on: ${dateFormat.format(Date())}", 40f, y, textPaint)
            y += 30f

            // Summary box
            val totalIncome = transactions.filter { it.type.equals("income", ignoreCase = true) }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { it.amount }
            val netBalance = totalIncome - totalExpense

            val summaryPaint = Paint().apply {
                color = Color.parseColor("#F5F5F5")
            }
            canvas.drawRect(40f, y, 555f, y + 50f, summaryPaint)

            canvas.drawText(String.format(Locale.US, "Total Income: $%.2f", totalIncome), 50f, y + 20f, headerPaint)
            canvas.drawText(String.format(Locale.US, "Total Expense: $%.2f", totalExpense), 220f, y + 20f, headerPaint)
            canvas.drawText(String.format(Locale.US, "Net Balance: $%.2f", netBalance), 400f, y + 20f, headerPaint)
            y += 70f

            // Table Header
            canvas.drawText("Date", 40f, y, headerPaint)
            canvas.drawText("Category", 140f, y, headerPaint)
            canvas.drawText("Description", 260f, y, headerPaint)
            canvas.drawText("Type", 440f, y, headerPaint)
            canvas.drawText("Amount", 500f, y, headerPaint)
            y += 10f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f

            var pageNumber = 1

            for (tx in transactions) {
                if (y > 780f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas
                    y = 50f

                    // Table Header on new page
                    canvas.drawText("Date", 40f, y, headerPaint)
                    canvas.drawText("Category", 140f, y, headerPaint)
                    canvas.drawText("Description", 260f, y, headerPaint)
                    canvas.drawText("Type", 440f, y, headerPaint)
                    canvas.drawText("Amount", 500f, y, headerPaint)
                    y += 10f
                    canvas.drawLine(40f, y, 555f, y, linePaint)
                    y += 20f
                }

                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(tx.date))
                val truncatedDesc = if (tx.description.length > 22) tx.description.take(20) + ".." else tx.description
                val truncatedCat = if (tx.category.length > 15) tx.category.take(13) + ".." else tx.category

                canvas.drawText(dateStr, 40f, y, textPaint)
                canvas.drawText(truncatedCat, 140f, y, textPaint)
                canvas.drawText(truncatedDesc, 260f, y, textPaint)
                canvas.drawText(tx.type.capitalize(Locale.getDefault()), 440f, y, textPaint)

                val amountPaint = Paint(textPaint).apply {
                    color = if (tx.type.equals("income", ignoreCase = true)) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
                }
                canvas.drawText(String.format(Locale.US, "$%.2f", tx.amount), 500f, y, amountPaint)

                y += 22f
            }

            pdfDocument.finishPage(page)

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
