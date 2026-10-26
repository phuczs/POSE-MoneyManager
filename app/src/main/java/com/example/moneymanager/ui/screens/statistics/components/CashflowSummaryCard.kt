package com.example.moneymanager.ui.screens.statistics.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymanager.R
import com.example.moneymanager.domain.model.FinancialHealthRating
import com.example.moneymanager.domain.model.PeriodComparisonSummary
import com.example.moneymanager.ui.theme.*
import com.example.moneymanager.util.toCurrencyString

@Composable
fun CashflowSummaryCard(
    summary: PeriodComparisonSummary,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(summary.savingsRate) {
        val target = (summary.savingsRate / 100f).coerceIn(-1f, 1f)
        animatedProgress.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val (ratingTextRes, ratingBgColor, ratingTextColor) = when (summary.healthRating) {
        FinancialHealthRating.EXCELLENT -> Triple(R.string.savings_rating_excellent, Color(0xFFE8F5E9), Color(0xFF2E7D32))
        FinancialHealthRating.GOOD -> Triple(R.string.savings_rating_good, Color(0xFFE0F2F1), Color(0xFF00796B))
        FinancialHealthRating.WARNING -> Triple(R.string.savings_rating_warning, Color(0xFFFFF8E1), Color(0xFFF57F17))
        FinancialHealthRating.DEFICIT -> Triple(R.string.savings_rating_deficit, Color(0xFFFFEBEE), Color(0xFFC62828))
    }

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
            // Header: Icon + Title + Health Rating Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MediumGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MediumGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.savings_rate),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ratingBgColor
                ) {
                    Text(
                        text = stringResource(ratingTextRes),
                        color = ratingTextColor,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Savings Rate Display & Net Cashflow
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${String.format("%.1f", summary.savingsRate)}%",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ratingTextColor,
                            fontSize = 36.sp
                        )
                    )
                    Text(
                        text = stringResource(R.string.net_savings),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextGray
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val netColor = if (summary.netSavings >= 0) MediumGreen else Error
                    Text(
                        text = (if (summary.netSavings > 0) "+" else "") + summary.netSavings.toCurrencyString(currencyCode),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = netColor
                        )
                    )
                    Text(
                        text = if (summary.netSavings >= 0) "Thặng dư" else "Thâm hụt",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextGray)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Linear gauge bar for savings rate
            val normalizedProgress = ((summary.savingsRate / 100f).coerceIn(0f, 1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFEEEEEE))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (animatedProgress.value).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(LightGreen, MediumGreen)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Income & Expense Breakdown Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income Pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F8E9)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Success.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Success,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.income),
                                style = MaterialTheme.typography.labelSmall.copy(color = TextGray)
                            )
                            Text(
                                text = summary.currentIncome.toCurrencyString(currencyCode),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }
                    }
                }

                // Expense Pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFEBEE)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Error.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.expense),
                                style = MaterialTheme.typography.labelSmall.copy(color = TextGray)
                            )
                            Text(
                                text = summary.currentExpense.toCurrencyString(currencyCode),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
