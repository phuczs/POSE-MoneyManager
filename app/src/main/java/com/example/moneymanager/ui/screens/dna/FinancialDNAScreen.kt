package com.example.moneymanager.ui.screens.dna

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneymanager.R
import com.example.moneymanager.domain.model.DnaAxis
import com.example.moneymanager.domain.model.FinancialArchetype
import com.example.moneymanager.domain.model.FinancialDNA
import com.example.moneymanager.ui.screens.dna.components.FinancialDnaRadarChart
import com.example.moneymanager.ui.theme.BackgroundGray
import com.example.moneymanager.ui.theme.DarkGreen
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.ui.viewmodel.FinancialDNAViewModel
import com.example.moneymanager.ui.viewmodel.FinancialDnaUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDNAScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    viewModel: FinancialDNAViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedAxis by viewModel.selectedAxis.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        containerColor = Color(0xFFF4F6F5),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.financial_dna_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (uiState is FinancialDnaUiState.Success) {
                        IconButton(
                            onClick = { viewModel.refreshFinancialDna() },
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MediumGreen
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.dna_refresh),
                                    tint = MediumGreen
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF4F6F5))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is FinancialDnaUiState.Loading -> DnaLoadingState()
                is FinancialDnaUiState.InsufficientData -> DnaInsufficientDataState(
                    currentCount = state.currentTransactions,
                    requiredCount = state.requiredTransactions,
                    onAddTransactionClick = onNavigateToAddTransaction
                )
                is FinancialDnaUiState.Error -> DnaErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadFinancialDna() }
                )
                is FinancialDnaUiState.Success -> {
                    val dna = state.dna
                    val shareMessage = buildShareMessage(context, dna, currentLanguage)
                    FinancialDnaContent(
                        dna = dna,
                        languageCode = currentLanguage,
                        selectedAxis = selectedAxis,
                        isRefreshing = isRefreshing,
                        onAxisSelected = { viewModel.selectAxis(it) },
                        onShareClick = { launchShareIntent(context, shareMessage) },
                        onRefreshClick = { viewModel.refreshFinancialDna() }
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialDnaContent(
    dna: FinancialDNA,
    languageCode: String,
    selectedAxis: DnaAxis,
    isRefreshing: Boolean,
    onAxisSelected: (DnaAxis) -> Unit,
    onShareClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(dna.archetype.accentColorHex)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Focal Point: Collector's Financial Identity Passport Card
        item(key = "passport_hero") {
            DnaPassportHeroCard(
                dna = dna,
                languageCode = languageCode,
                accentColor = accentColor,
                isRefreshing = isRefreshing,
                onShareClick = onShareClick,
                onRefreshClick = onRefreshClick
            )
        }

        // 2. Asymmetric Quantitative Metric Strip
        item(key = "metrics_strip") {
            AsymmetricMetricsStrip(
                dna = dna,
                accentColor = accentColor
            )
        }

        // 3. Interactive 4-Axis Radar & Axis Polarity Inspector
        item(key = "radar_section") {
            RadarAndSpectrumSection(
                dna = dna,
                accentColor = accentColor,
                selectedAxis = selectedAxis,
                onAxisSelected = onAxisSelected
            )
        }

        // 4. Editorial Money Story & Dominant Categories
        item(key = "editorial_story") {
            EditorialStorySection(
                dna = dna,
                accentColor = accentColor
            )
        }

        // 5. Contrasting Superpowers vs Growth Opportunities
        item(key = "coaching_split") {
            CoachingSplitSection(
                strengths = dna.strengths,
                improvements = dna.improvements
            )
        }

        // 6. Quiet Data Provenance Footer
        item(key = "footer") {
            Text(
                text = stringResource(
                    R.string.dna_analyzed_footer,
                    dna.analyzedTransactionsCount,
                    dna.analyzedDays
                ),
                style = MaterialTheme.typography.labelSmall,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun DnaPassportHeroCard(
    dna: FinancialDNA,
    languageCode: String,
    accentColor: Color,
    isRefreshing: Boolean,
    onShareClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val obsidian = Color(0xFF0B1C1A)
    val deepTint = Color(
        red = (obsidian.red * 0.65f + accentColor.red * 0.35f),
        green = (obsidian.green * 0.65f + accentColor.green * 0.35f),
        blue = (obsidian.blue * 0.65f + accentColor.blue * 0.35f),
        alpha = 1f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(obsidian, deepTint, Color(0xFF102421))
                )
            )
    ) {
        // Atmospheric geometric orbital rings in background
        Canvas(modifier = Modifier.matchParentSize()) {
            val topRight = Offset(size.width * 0.88f, size.height * 0.16f)
            listOf(75f, 130f, 195f).forEachIndexed { i, r ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f - i * 0.014f),
                    radius = r,
                    center = topRight,
                    style = Stroke(width = 1.5f)
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.30f), Color.Transparent),
                    center = topRight,
                    radius = 220f
                ),
                radius = 220f,
                center = topRight
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Top micro-label + AI badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dna_passport_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    ),
                    color = Color.White.copy(alpha = 0.65f)
                )

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = if (dna.isAiEnhanced) accentColor.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (dna.isAiEnhanced) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.16f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFE082),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                if (dna.isAiEnhanced) R.string.dna_ai_enhanced_badge
                                else R.string.dna_local_analysis_badge
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4-letter DNA code tiles + Archetype emblem
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dna.code.forEach { letter ->
                        Box(
                            modifier = Modifier
                                .size(width = 42.dp, height = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.11f))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.24f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dna.archetype.emoji,
                        fontSize = 28.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Archetype Editorial Title
            Text(
                text = dna.archetype.title(languageCode),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    lineHeight = 30.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "\"${dna.archetype.tagline(languageCode)}\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 20.sp
                ),
                color = Color.White.copy(alpha = 0.78f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tactile Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onShareClick,
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = obsidian,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dna_share_card),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = obsidian
                        )
                    }
                }

                Surface(
                    onClick = onRefreshClick,
                    enabled = !isRefreshing,
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.dna_refresh),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AsymmetricMetricsStrip(
    dna: FinancialDNA,
    accentColor: Color
) {
    val savingsFormatted = String.format(Locale.US, "%.1f%%", dna.savingsRatePercent)
    val swingFormatted = String.format(Locale.US, "±%.0f%%", dna.weeklyVariancePercent)
    val focusFormatted = String.format(Locale.US, "%.0f%%", dna.topCategoriesSharePercent)
    val isPositiveSavings = dna.savingsRatePercent >= 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left focal stat tile (Savings Rate)
        Surface(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE6ECEA))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.dna_metric_savings_rate),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray
                )
                Text(
                    text = savingsFormatted,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFeatureSettings = "tnum",
                        letterSpacing = (-0.5).sp
                    ),
                    color = if (isPositiveSavings) accentColor else Color(0xFFD32F2F)
                )
                LinearProgressIndicator(
                    progress = { ((dna.savingsRatePercent.coerceIn(0.0, 50.0)) / 50.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = if (isPositiveSavings) accentColor else Color(0xFFD32F2F),
                    trackColor = Color(0xFFEAEFEF),
                    strokeCap = StrokeCap.Round
                )
            }
        }

        // Right stacked compact metrics
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactMetricTile(
                label = stringResource(R.string.dna_metric_weekly_swing),
                value = swingFormatted,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            CompactMetricTile(
                label = stringResource(R.string.dna_metric_top_focus),
                value = focusFormatted,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun CompactMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE6ECEA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum"
                ),
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun RadarAndSpectrumSection(
    dna: FinancialDNA,
    accentColor: Color,
    selectedAxis: DnaAxis,
    onAxisSelected: (DnaAxis) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE6ECEA))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.dna_radar_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.dna_radar_hint),
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            FinancialDnaRadarChart(
                savingsScore = dna.savingsScore,
                plannerScore = dna.plannerScore,
                stabilityScore = dna.stabilityScore,
                focusScore = dna.focusScore,
                accentColor = accentColor,
                selectedAxis = selectedAxis,
                onAxisSelected = onAxisSelected
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4 interactive spectrum bars — tapping any bar focuses that axis on the radar
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AxisSpectrumRow(
                    leftLabel = stringResource(R.string.dna_axis_saver),
                    rightLabel = stringResource(R.string.dna_axis_indulger),
                    leftPercent = dna.savingsScore,
                    isSelected = selectedAxis == DnaAxis.SAVINGS_VS_INDULGENCE,
                    accentColor = accentColor,
                    onClick = { onAxisSelected(DnaAxis.SAVINGS_VS_INDULGENCE) }
                )
                AxisSpectrumRow(
                    leftLabel = stringResource(R.string.dna_axis_planner),
                    rightLabel = stringResource(R.string.dna_axis_spontaneous),
                    leftPercent = dna.plannerScore,
                    isSelected = selectedAxis == DnaAxis.PLANNER_VS_SPONTANEOUS,
                    accentColor = accentColor,
                    onClick = { onAxisSelected(DnaAxis.PLANNER_VS_SPONTANEOUS) }
                )
                AxisSpectrumRow(
                    leftLabel = stringResource(R.string.dna_axis_consistent),
                    rightLabel = stringResource(R.string.dna_axis_volatile),
                    leftPercent = dna.stabilityScore,
                    isSelected = selectedAxis == DnaAxis.CONSISTENT_VS_VOLATILE,
                    accentColor = accentColor,
                    onClick = { onAxisSelected(DnaAxis.CONSISTENT_VS_VOLATILE) }
                )
                AxisSpectrumRow(
                    leftLabel = stringResource(R.string.dna_axis_targeted),
                    rightLabel = stringResource(R.string.dna_axis_diverse),
                    leftPercent = dna.focusScore,
                    isSelected = selectedAxis == DnaAxis.TARGETED_VS_DIVERSE,
                    accentColor = accentColor,
                    onClick = { onAxisSelected(DnaAxis.TARGETED_VS_DIVERSE) }
                )
            }
        }
    }
}

@Composable
private fun AxisSpectrumRow(
    leftLabel: String,
    rightLabel: String,
    leftPercent: Int,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val rightPercent = (100 - leftPercent).coerceIn(0, 100)
    val animatedFraction by animateFloatAsState(
        targetValue = (leftPercent / 100f).coerceIn(0.05f, 0.95f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "axis_bar"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$leftLabel  $leftPercent%",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (leftPercent >= 50) FontWeight.Bold else FontWeight.Medium,
                    fontFeatureSettings = "tnum"
                ),
                color = if (leftPercent >= 50) accentColor else TextGray
            )
            Text(
                text = "$rightPercent%  $rightLabel",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (leftPercent < 50) FontWeight.Bold else FontWeight.Medium,
                    fontFeatureSettings = "tnum"
                ),
                color = if (leftPercent < 50) accentColor else TextGray
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color(0xFFE7ECEB))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(accentColor)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorialStorySection(
    dna: FinancialDNA,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE6ECEA))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.dna_story_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Editorial pull-quote bar
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(68.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                AnimatedContent(
                    targetState = dna.aiNarrative,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "narrative_swap"
                ) { story ->
                    Text(
                        text = story,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = TextPrimary
                    )
                }
            }

            if (dna.topCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.dna_top_categories_label),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dna.topCategories.forEachIndexed { idx, category ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (idx == 0) accentColor.copy(alpha = 0.12f) else BackgroundGray
                        ) {
                            Text(
                                text = "#${idx + 1} $category",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (idx == 0) accentColor else TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachingSplitSection(
    strengths: List<String>,
    improvements: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Strengths block — cool emerald tint
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFEDF8F4),
            border = BorderStroke(1.dp, Color(0xFFC8EADF))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = DarkGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dna_strengths_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DarkGreen
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                strengths.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = MediumGreen,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = Color(0xFF14352E)
                        )
                    }
                }
            }
        }

        // Growth opportunities block — warm amber parchment tint
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFFF8EE),
            border = BorderStroke(1.dp, Color(0xFFF5DFB8))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dna_improvements_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF92400E)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                improvements.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "→",
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = Color(0xFF4A2E07)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DnaLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MediumGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MediumGreen,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.dna_loading_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.dna_loading_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = TextGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DnaInsufficientDataState(
    currentCount: Int,
    requiredCount: Int,
    onAddTransactionClick: () -> Unit
) {
    val progress = (currentCount.toFloat() / requiredCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF0B1C1A)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🧬", fontSize = 40.sp)
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.dna_insufficient_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.dna_insufficient_desc, requiredCount),
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = MediumGreen,
            trackColor = Color(0xFFDCE5E3),
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.dna_insufficient_progress, currentCount, requiredCount),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            ),
            color = MediumGreen
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onAddTransactionClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MediumGreen),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.dna_add_transaction_cta),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DnaErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MediumGreen)
        ) {
            Text(text = stringResource(R.string.dna_refresh), fontWeight = FontWeight.Bold)
        }
    }
}

private fun buildShareMessage(
    context: Context,
    dna: FinancialDNA,
    languageCode: String
): String {
    val sr = String.format(Locale.US, "%.1f%%", dna.savingsRatePercent)
    val wv = String.format(Locale.US, "±%.0f%%", dna.weeklyVariancePercent)
    val tc = String.format(Locale.US, "%.0f%%", dna.topCategoriesSharePercent)
    return context.getString(
        R.string.dna_share_template,
        dna.archetype.title(languageCode),
        dna.code,
        dna.archetype.tagline(languageCode),
        sr,
        wv,
        tc
    )
}

private fun launchShareIntent(context: Context, text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val chooser = Intent.createChooser(sendIntent, null)
    context.startActivity(chooser)
}

@Preview(showBackground = true)
@Composable
private fun FinancialDnaPreview() {
    val sample = FinancialDNA(
        code = "SPCT",
        archetype = FinancialArchetype.SPCT,
        savingsScore = 82,
        plannerScore = 76,
        stabilityScore = 88,
        focusScore = 74,
        savingsRatePercent = 28.4,
        weeklyVariancePercent = 12.0,
        topCategoriesSharePercent = 78.0,
        topCategories = listOf("Ăn uống", "Di chuyển", "Mua sắm"),
        analyzedTransactionsCount = 42,
        analyzedDays = 60,
        aiNarrative = "Hồ sơ của bạn mang đậm dấu ấn Chiến lược gia Thận trọng: 78% chi tiêu tập trung vào 3 danh mục thiết yếu với nhịp tuần cực kỳ ổn định.",
        strengths = FinancialArchetype.SPCT.defaultStrengthsVi,
        improvements = FinancialArchetype.SPCT.defaultImprovementsVi,
        isAiEnhanced = true,
        languageCode = "vi"
    )
    FinancialDnaContent(
        dna = sample,
        languageCode = "vi",
        selectedAxis = DnaAxis.SAVINGS_VS_INDULGENCE,
        isRefreshing = false,
        onAxisSelected = {},
        onShareClick = {},
        onRefreshClick = {}
    )
}
