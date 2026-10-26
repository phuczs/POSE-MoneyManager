package com.example.moneymanager.ui.screens.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymanager.R
import com.example.moneymanager.domain.model.BudgetHistoryEntry
import com.example.moneymanager.ui.theme.BackgroundGray
import com.example.moneymanager.ui.theme.DarkGreen
import com.example.moneymanager.ui.theme.Error
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.util.toCurrencyString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase 4.2: Budget History & Period Comparison BottomSheet.
 *
 * Visualizes historical budget periods for a specific category,
 * featuring a custom comparison bar chart, trend indicators,
 * and cycle-by-cycle surplus/deficit breakdown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetHistorySheet(
    category: String,
    historyEntries: List<BudgetHistoryEntry>,
    currencyCode: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MediumGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MediumGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = stringResource(R.string.budget_history_title),
                            style = MaterialTheme.typography.labelMedium.copy(color = TextGray)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (historyEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có lịch sử chu kỳ trước cho danh mục này",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Key metrics cards (Average Spent, Total Saved, Latest Trend)
                val totalSpent = historyEntries.sumOf { it.spent }
                val avgSpent = totalSpent / historyEntries.size
                val totalSaved = historyEntries.sumOf { it.savedAmount }

                val trend = if (historyEntries.size >= 2) {
                    val latest = historyEntries[0].spent
                    val previous = historyEntries[1].spent
                    if (previous > 0) ((latest - previous) / previous) * 100.0 else 0.0
                } else null

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Avg spent card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Trung bình chi",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = avgSpent.toCurrencyString(currencyCode),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }

                    // Total saved card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.saved_amount),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = totalSaved.toCurrencyString(currencyCode),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalSaved >= 0) DarkGreen else Error
                                )
                            )
                        }
                    }

                    // Trend card (if 2+ cycles exist)
                    if (trend != null) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (trend <= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Xu hướng",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (trend <= 0) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = if (trend <= 0) DarkGreen else Error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${if (trend > 0) "+" else ""}${String.format(Locale.getDefault(), "%.1f", trend)}%",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (trend <= 0) DarkGreen else Error
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Custom Bar Chart comparing Allocated vs Spent across cycles
                val chartEntries = historyEntries.take(5).reversed()
                Text(
                    text = "Biểu đồ so sánh Hạn mức vs Đã chi",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                HistoryBarChart(
                    entries = chartEntries,
                    currencyCode = currencyCode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Detailed cycles list
                Text(
                    text = "Các chu kỳ trước",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(historyEntries) { entry ->
                        HistoryEntryRow(entry = entry, currencyCode = currencyCode)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryBarChart(
    entries: List<BudgetHistoryEntry>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val maxVal = entries.maxOfOrNull { maxOf(it.allocated + it.rolloverIn, it.spent) }?.coerceAtLeast(1.0) ?: 1.0

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BackgroundGray)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            val count = entries.size
            if (count == 0) return@Canvas

            val groupWidth = size.width / count
            val barWidth = (groupWidth * 0.28f).coerceAtMost(28f)
            val spacing = barWidth * 0.25f

            entries.forEachIndexed { index, entry ->
                val groupCenterX = (index * groupWidth) + (groupWidth / 2f)

                // Allocated bar (Left bar - light teal/green)
                val totalCap = entry.allocated + entry.rolloverIn
                val allocBarHeight = (totalCap / maxVal * (size.height - 10f)).toFloat()
                val allocTop = size.height - allocBarHeight
                drawRoundRect(
                    color = Color(0xFFA5D6A7),
                    topLeft = Offset(groupCenterX - barWidth - (spacing / 2f), allocTop),
                    size = Size(barWidth, allocBarHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Spent bar (Right bar - green or red if over)
                val isOver = entry.spent > totalCap
                val spentBarHeight = (entry.spent / maxVal * (size.height - 10f)).toFloat()
                val spentTop = size.height - spentBarHeight
                drawRoundRect(
                    color = if (isOver) Error else MediumGreen,
                    topLeft = Offset(groupCenterX + (spacing / 2f), spentTop),
                    size = Size(barWidth, spentBarHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        // Legend at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA5D6A7))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Hạn mức", style = MaterialTheme.typography.labelSmall, color = TextGray)

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MediumGreen)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Đã chi", style = MaterialTheme.typography.labelSmall, color = TextGray)
        }
    }
}

@Composable
private fun HistoryEntryRow(
    entry: BudgetHistoryEntry,
    currencyCode: String
) {
    val dateFmt = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    val periodLabel = "${dateFmt.format(Date(entry.periodStart))} - ${dateFmt.format(Date(entry.periodEnd))}"
    val isSaved = entry.savedAmount >= 0

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BackgroundGray.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Text(
                    text = "Chi: ${entry.spent.toCurrencyString(currencyCode)} / Hạn mức: ${entry.allocated.toCurrencyString(currencyCode)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }

            // Saved / Over badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSaved) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ) {
                Text(
                    text = "${if (isSaved) "+" else ""}${entry.savedAmount.toCurrencyString(currencyCode)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSaved) DarkGreen else Error
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
