package com.example.moneymanager.ui.screens.budget.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.moneymanager.domain.model.BudgetWithProgress
import com.example.moneymanager.ui.theme.DarkGreen
import com.example.moneymanager.ui.theme.Error
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.util.toCurrencyString

/**
 * Phase 4.3 & 4.4: Category Intelligence Expansion Drawer.
 *
 * Expands when a [BudgetListItem] is tapped to reveal pacing metrics,
 * burn rate analysis, daily safe allowance, and a shortcut to historical trends.
 */
@Composable
fun CategoryInsightCard(
    budgetWP: BudgetWithProgress,
    currencyCode: String,
    onViewHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budget = budgetWP.budget
    val now = System.currentTimeMillis()
    val totalPeriodMs = (budget.endDate - budget.startDate).coerceAtLeast(1L)
    val elapsedMs = (now - budget.startDate).coerceIn(0L, totalPeriodMs)
    val timeProgress = (elapsedMs.toFloat() / totalPeriodMs.toFloat()).coerceIn(0f, 1f)
    val spendProgress = budgetWP.progress

    val isAheadOfPace = spendProgress > (timeProgress + 0.1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8F9FA))
            .padding(12.dp)
    ) {
        // Pacing Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = if (isAheadOfPace) Color(0xFFE65100) else MediumGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.pace_title),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            Text(
                text = if (isAheadOfPace) stringResource(R.string.pace_ahead) else stringResource(R.string.pace_on_track),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isAheadOfPace) Color(0xFFE65100) else DarkGreen
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Comparative Progress: Time Elapsed vs Budget Spent
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.time_elapsed_format, (timeProgress * 100).toInt()),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            LinearProgressIndicator(
                progress = { timeProgress },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color.LightGray,
                trackColor = Color(0xFFE0E0E0)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.budget_spent_format, (spendProgress * 100).toInt()),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            LinearProgressIndicator(
                progress = { spendProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isAheadOfPace) Color(0xFFFFA000) else MediumGreen,
                trackColor = Color(0xFFE0E0E0)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(10.dp))

        // Daily Allowance + History Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.daily_allowance_total),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
                Text(
                    text = stringResource(
                        R.string.daily_allowance_format,
                        budgetWP.dailyAllowance.toCurrencyString(currencyCode)
                    ),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = DarkGreen
                )
            }

            FilledTonalButton(
                onClick = onViewHistoryClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MediumGreen.copy(alpha = 0.12f),
                    contentColor = MediumGreen
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 6.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.view_history),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
