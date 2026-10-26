package com.example.moneymanager.ui.screens.budget.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymanager.R
import com.example.moneymanager.domain.model.BudgetWithProgress
import com.example.moneymanager.ui.theme.DarkGreen
import com.example.moneymanager.ui.theme.Error
import com.example.moneymanager.ui.theme.LightGreen
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.util.toCurrencyString

/**
 * Phase 4.4: Premium Budget Dashboard Hero Component.
 *
 * Visualizes the overall financial discipline with an animated
 * Budget Health Score gauge (0-100), overall daily burn velocity,
 * and high-contrast tonal metric cards.
 */
@Composable
fun BudgetHealthScoreCard(
    budgets: List<BudgetWithProgress>,
    overallBudget: Double,
    overallSpent: Double,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    // 1. Calculate weighted Health Score (0 - 100)
    val healthScore = remember(budgets, overallBudget, overallSpent) {
        if (budgets.isEmpty() || overallBudget <= 0.0) 100
        else {
            var weightedScore = 0.0
            budgets.forEach { bwp ->
                val weight = if (overallBudget > 0) bwp.budget.effectiveAmount / overallBudget else 1.0 / budgets.size
                val itemScore = when {
                    bwp.progress <= 0.7f -> 100.0
                    bwp.progress <= 0.9f -> 100.0 - ((bwp.progress - 0.7) / 0.2) * 35.0 // 100 down to 65
                    bwp.progress <= 1.0f -> 65.0 - ((bwp.progress - 0.9) / 0.1) * 35.0  // 65 down to 30
                    else -> (30.0 - (bwp.progress - 1.0) * 50.0).coerceAtLeast(0.0)    // penalized
                }
                weightedScore += itemScore * weight
            }
            weightedScore.toInt().coerceIn(0, 100)
        }
    }

    // 2. Health tier status & color
    val (statusLabel, gaugeColor) = when {
        healthScore >= 80 -> Pair(stringResource(R.string.health_excellent), Color(0xFF69F0AE))
        healthScore >= 60 -> Pair(stringResource(R.string.health_good), Color(0xFFFFD54F))
        healthScore >= 40 -> Pair(stringResource(R.string.health_warning), Color(0xFFFF8A65))
        else -> Pair(stringResource(R.string.health_critical), Color(0xFFFF5252))
    }

    val animatedGaugeProgress by animateFloatAsState(
        targetValue = (healthScore / 100f).coerceIn(0f, 1f),
        animationSpec = tween(1200),
        label = "GaugeAnimation"
    )

    // 3. Overall Daily Allowance
    val remaining = (overallBudget - overallSpent).coerceAtLeast(0.0)
    val maxDaysRemaining = budgets.maxOfOrNull { it.daysRemaining }?.coerceAtLeast(1) ?: 1
    val totalDailyAllowance = if (maxDaysRemaining > 0) remaining / maxDaysRemaining else 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkGreen,
                            MediumGreen,
                            Color(0xFF1B5E20)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Row: Title Badge + Health Gauge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.budget_health_score),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            ),
                            color = Color.White
                        )
                    }

                    // Circular Health Gauge
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(64.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            strokeWidth = 6.dp
                        )
                        CircularProgressIndicator(
                            progress = { animatedGaugeProgress },
                            modifier = Modifier.size(64.dp),
                            color = gaugeColor,
                            strokeWidth = 6.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$healthScore",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "/100",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Daily Allowance Spotlight Banner
                if (totalDailyAllowance > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(
                                    R.string.can_spend_daily,
                                    totalDailyAllowance.toCurrencyString(currencyCode)
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 3 Metrics Pills: Total Budget | Spent | Remaining
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = stringResource(R.string.total_budget),
                        value = overallBudget.toCurrencyString(currencyCode),
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = stringResource(R.string.spent),
                        value = overallSpent.toCurrencyString(currencyCode),
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = stringResource(R.string.remaining),
                        value = (overallBudget - overallSpent).toCurrencyString(currencyCode),
                        isRemaining = true,
                        isNegative = (overallBudget - overallSpent) < 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isRemaining: Boolean = false,
    isNegative: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = if (isRemaining) 0.2f else 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = if (isNegative) Color(0xFFFF8A80) else Color.White
            )
        }
    }
}
