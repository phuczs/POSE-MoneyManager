package com.example.moneymanager.ui.screens.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymanager.R
import com.example.moneymanager.domain.model.PeriodComparisonSummary
import com.example.moneymanager.ui.theme.*
import com.example.moneymanager.util.toCurrencyString

@Composable
fun PeriodComparisonCard(
    summary: PeriodComparisonSummary,
    periodLabel: String,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDE7F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        tint = Color(0xFF673AB7),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.period_comparison_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "So sánh $periodLabel với kỳ liền trước",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextGray)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Comparison Metrics: Expense Delta & Income Delta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Expense Change Card
                val expenseIncreased = summary.expenseDeltaPercent > 0
                val expenseBadgeColor = if (expenseIncreased) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                val expenseTextColor = if (expenseIncreased) Error else Success

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF9FAFB)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.expense),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextGray)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (expenseIncreased) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = expenseTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = (if (expenseIncreased) "+" else "") + "${summary.expenseDeltaPercent}%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = expenseTextColor
                                )
                            )
                        }
                        Text(
                            text = if (expenseIncreased) "Chi tiêu tăng" else "Chi tiêu giảm",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Income Change Card
                val incomeIncreased = summary.incomeDeltaPercent >= 0
                val incomeTextColor = if (incomeIncreased) Success else Error

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF9FAFB)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.income),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextGray)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (incomeIncreased) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = incomeTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = (if (incomeIncreased) "+" else "") + "${summary.incomeDeltaPercent}%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = incomeTextColor
                                )
                            )
                        }
                        Text(
                            text = if (incomeIncreased) "Thu nhập tăng" else "Thu nhập giảm",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Top surge & Top decreased categories (if available)
            if (summary.topSurgeCategory != null || summary.topDecreasedCategory != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Divider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                summary.topSurgeCategory?.let { surge ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.top_increased_category),
                                style = MaterialTheme.typography.labelSmall.copy(color = TextGray)
                            )
                            Text(
                                text = surge.categoryName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                        }
                        Text(
                            text = "+${surge.deltaAmount.toCurrencyString(currencyCode)} (+${surge.deltaPercent}%)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Error
                            )
                        )
                    }
                }

                if (summary.topSurgeCategory != null && summary.topDecreasedCategory != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                summary.topDecreasedCategory?.let { decreased ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.top_decreased_category),
                                style = MaterialTheme.typography.labelSmall.copy(color = TextGray)
                            )
                            Text(
                                text = decreased.categoryName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                        }
                        Text(
                            text = "${decreased.deltaAmount.toCurrencyString(currencyCode)} (${decreased.deltaPercent}%)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Success
                            )
                        )
                    }
                }
            }
        }
    }
}
