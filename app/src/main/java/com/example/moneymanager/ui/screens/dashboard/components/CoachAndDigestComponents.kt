package com.example.moneymanager.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymanager.R
import com.example.moneymanager.domain.model.CoachNudge
import com.example.moneymanager.domain.model.NudgeTone
import com.example.moneymanager.domain.model.WeeklyDigest
import com.example.moneymanager.ui.theme.DarkGreen
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.ui.viewmodel.WeeklyDigestUiState
import com.example.moneymanager.util.toCurrencyString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private data class CoachPalette(
    val accent: Color,
    val container: Color,
    val border: Color,
    val titleColor: Color
)

private fun paletteForTone(tone: NudgeTone): CoachPalette = when (tone) {
    NudgeTone.URGENT -> CoachPalette(
        accent = Color(0xFFC62828),
        container = Color(0xFFFFF3F3),
        border = Color(0xFFFFCDD2),
        titleColor = Color(0xFF8E1B1B)
    )
    NudgeTone.WARNING -> CoachPalette(
        accent = Color(0xFFD97706),
        container = Color(0xFFFFF9EE),
        border = Color(0xFFFBE0AE),
        titleColor = Color(0xFF92400E)
    )
    NudgeTone.CELEBRATION -> CoachPalette(
        accent = MediumGreen,
        container = Color(0xFFEDF8F4),
        border = Color(0xFFC2E8DB),
        titleColor = DarkGreen
    )
    NudgeTone.GENTLE -> CoachPalette(
        accent = Color(0xFF1565C0),
        container = Color(0xFFF1F6FD),
        border = Color(0xFFCBDCF6),
        titleColor = Color(0xFF0D47A1)
    )
}

/**
 * Dismissable, non-intrusive Proactive AI Coach nudge banner on DashboardScreen.
 */
@Composable
fun ProactiveCoachBanner(
    nudge: CoachNudge,
    onActionClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = paletteForTone(nudge.tone)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = palette.container,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = nudge.emoji, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.coach_badge_label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        ),
                        color = palette.accent
                    )
                }

                IconButton(
                    onClick = onDismissClick,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = nudge.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = palette.titleColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = nudge.message,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                color = TextPrimary.copy(alpha = 0.86f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp),
                color = palette.accent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = nudge.actionLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact editorial entry strip on DashboardScreen for opening the Weekly AI Digest sheet.
 */
@Composable
fun WeeklyDigestEntryCard(
    digestState: WeeklyDigestUiState,
    currencyCode: String,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val digest = (digestState as? WeeklyDigestUiState.Success)?.digest ?: return

    val isSaved = digest.deltaPercent <= 0.0
    val deltaColor = if (isSaved) Color(0xFF69F0AE) else Color(0xFFFFAB91)
    val deltaPrefix = if (digest.deltaPercent > 0.0) "+" else ""
    val deltaText = "$deltaPrefix${digest.deltaPercent.toInt()}%"

    Surface(
        onClick = onOpenClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0E2320),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0D221F), Color(0xFF133B34))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color(0xFFFFE082),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.weekly_digest_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (digest.previousWeekSpent > 0.0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = deltaColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = deltaText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFeatureSettings = "tnum",
                                    fontSize = 10.sp
                                ),
                                color = deltaColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${digest.totalSpent.toCurrencyString(currencyCode)} · ${digest.aiSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.weekly_digest_open),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Full Weekly AI Financial Digest modal sheet with 7-day breakdown, AI commentary, and action tip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyDigestBottomSheet(
    digestState: WeeklyDigestUiState,
    isRefreshing: Boolean,
    currencyCode: String,
    onDismiss: () -> Unit,
    onRefreshClick: () -> Unit,
    onAskCoachClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF6F8F7),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        when (digestState) {
            is WeeklyDigestUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MediumGreen)
                }
            }
            is WeeklyDigestUiState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "📅", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.weekly_digest_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }
            is WeeklyDigestUiState.Success -> {
                WeeklyDigestSheetBody(
                    digest = digestState.digest,
                    isRefreshing = isRefreshing,
                    currencyCode = currencyCode,
                    onRefreshClick = onRefreshClick,
                    onAskCoachClick = onAskCoachClick
                )
            }
        }
    }
}

@Composable
private fun WeeklyDigestSheetBody(
    digest: WeeklyDigest,
    isRefreshing: Boolean,
    currencyCode: String,
    onRefreshClick: () -> Unit,
    onAskCoachClick: () -> Unit
) {
    val dateFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
    val rangeLabel = "${dateFmt.format(Date(digest.weekStartMillis))} – ${dateFmt.format(Date(digest.weekEndMillis))}"
    val isSaved = digest.deltaPercent <= 0.0
    val absDelta = abs(digest.deltaPercent).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.weekly_digest_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = TextPrimary
                )
                Text(
                    text = rangeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray
                )
            }

            IconButton(onClick = onRefreshClick, enabled = !isRefreshing) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MediumGreen)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MediumGreen)
                }
            }
        }

        // Hero Spend + Delta Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0B1C1A)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.weekly_digest_total_spent),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = digest.totalSpent.toCurrencyString(currencyCode),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFeatureSettings = "tnum"
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (digest.previousWeekSpent > 0.0) {
                        val badgeColor = if (isSaved) Color(0xFF69F0AE) else Color(0xFFFF8A80)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = badgeColor.copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.AutoMirrored.Filled.TrendingDown
                                    else Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${if (isSaved) "-" else "+"}$absDelta% ${stringResource(R.string.weekly_digest_vs_last_week)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = badgeColor
                                )
                            }
                        }
                    }

                    if (digest.budgetsTotalCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "🎯 ${digest.budgetsOnTrackCount}/${digest.budgetsTotalCount} ${stringResource(R.string.weekly_digest_budgets_health)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top 3 Categories Breakdown
        if (digest.topCategories.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE6ECEA))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.weekly_digest_top_categories),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    val barColors = listOf(MediumGreen, Color(0xFF00838F), Color(0xFFD97706))
                    digest.topCategories.forEachIndexed { idx, cat ->
                        val color = barColors.getOrElse(idx) { MediumGreen }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cat.category,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${cat.amount.toCurrencyString(currencyCode)} (${cat.sharePercent.toInt()}%)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = TextGray
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(Color(0xFFECF0EF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((cat.sharePercent / 100f).coerceIn(0.06f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(color)
                                )
                            }
                        }
                    }

                    if (digest.highestSpendDayLabel.isNotBlank() && digest.highestSpendDayAmount > 0.0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF5F7F6))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.weekly_digest_peak_day),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                            Text(
                                text = "${digest.highestSpendDayLabel} · ${digest.highestSpendDayAmount.toCurrencyString(currencyCode)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // AI Commentary Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE6ECEA))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MediumGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.weekly_digest_ai_commentary),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = digest.aiSummary,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = TextPrimary
                )
            }
        }

        // Actionable Tip Card — warm amber tint
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFFF8EE),
            border = BorderStroke(1.dp, Color(0xFFF5DFB8))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD97706).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.weekly_digest_tip_title),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF92400E)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = digest.actionableTip,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                        color = Color(0xFF4A2E07)
                    )
                }
            }
        }

        // Direct CTA to AI Coach Chat
        Button(
            onClick = onAskCoachClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MediumGreen),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.weekly_digest_ask_coach),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
