package com.example.moneymanager.domain.model

import androidx.compose.runtime.Immutable

enum class AnomalyType {
    DUPLICATE_SUSPECT,
    AMOUNT_SPIKE,
    FREQUENCY_SPIKE,
    FIRST_TIME_CATEGORY_LARGE,
    UNUSUAL_HOUR
}

enum class AnomalySeverity(val rank: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3)
}

/**
 * Represents a detected behavioral or statistical anomaly on a user transaction.
 */
@Immutable
data class TransactionAnomaly(
    val transactionId: String,
    val type: AnomalyType,
    val severity: AnomalySeverity,
    val titleVi: String,
    val titleEn: String,
    val explanationVi: String,
    val explanationEn: String,
    val zScore: Double? = null,
    val relatedTransactionId: String? = null
) {
    fun title(languageCode: String): String = if (languageCode == "vi") titleVi else titleEn
    fun explanation(languageCode: String): String = if (languageCode == "vi") explanationVi else explanationEn
}
