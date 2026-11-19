package com.example.moneymanager.ui.screens.dna.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymanager.R
import com.example.moneymanager.domain.model.DnaAxis
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary

/**
 * Custom animated 4-axis diamond radar chart visualizing the user's Financial DNA scores.
 *
 * Vertices:
 * - Top:    Savings vs Indulgence ([savingsScore])
 * - Right:  Planner vs Spontaneous ([plannerScore])
 * - Bottom: Consistent vs Volatile ([stabilityScore])
 * - Left:   Targeted vs Diverse ([focusScore])
 */
@Composable
fun FinancialDnaRadarChart(
    savingsScore: Int,
    plannerScore: Int,
    stabilityScore: Int,
    focusScore: Int,
    accentColor: Color,
    selectedAxis: DnaAxis,
    onAxisSelected: (DnaAxis) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(savingsScore, plannerScore, stabilityScore, focusScore) {
        progress.snapTo(0.15f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val topLabel = stringResource(R.string.dna_axis_saver)
    val rightLabel = stringResource(R.string.dna_axis_planner)
    val bottomLabel = stringResource(R.string.dna_axis_consistent)
    val leftLabel = stringResource(R.string.dna_axis_targeted)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 48.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = (minOf(size.width, size.height) / 2f).coerceAtLeast(1f)
            val center = Offset(cx, cy)

            val ringColor = Color(0xFFDDE5E3)
            val axisLineColor = Color(0xFFC8D5D2)

            // 1. Concentric diamond grid rings (25%, 50%, 75%, 100%)
            listOf(0.25f, 0.50f, 0.75f, 1.0f).forEachIndexed { idx, fraction ->
                val r = radius * fraction
                val ringPath = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r, cy)
                    close()
                }
                drawPath(
                    path = ringPath,
                    color = if (idx == 3) axisLineColor else ringColor,
                    style = Stroke(width = if (idx == 3) 2.5f else 1.5f)
                )
            }

            // 2. Crosshair axis lines
            drawLine(axisLineColor, Offset(cx, cy - radius), Offset(cx, cy + radius), strokeWidth = 1.5f)
            drawLine(axisLineColor, Offset(cx - radius, cy), Offset(cx + radius, cy), strokeWidth = 1.5f)

            // 3. Animated DNA polygon vertices
            val anim = progress.value
            val sFrac = ((savingsScore / 100f).coerceIn(0.08f, 1f)) * anim
            val pFrac = ((plannerScore / 100f).coerceIn(0.08f, 1f)) * anim
            val cFrac = ((stabilityScore / 100f).coerceIn(0.08f, 1f)) * anim
            val tFrac = ((focusScore / 100f).coerceIn(0.08f, 1f)) * anim

            val pTop = Offset(cx, cy - radius * sFrac)
            val pRight = Offset(cx + radius * pFrac, cy)
            val pBottom = Offset(cx, cy + radius * cFrac)
            val pLeft = Offset(cx - radius * tFrac, cy)

            val dataPath = Path().apply {
                moveTo(pTop.x, pTop.y)
                lineTo(pRight.x, pRight.y)
                lineTo(pBottom.x, pBottom.y)
                lineTo(pLeft.x, pLeft.y)
                close()
            }

            // Translucent radial gradient fill
            drawPath(
                path = dataPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.42f),
                        accentColor.copy(alpha = 0.16f)
                    ),
                    center = center,
                    radius = radius
                )
            )

            // Crisp polygon border
            drawPath(
                path = dataPath,
                color = accentColor,
                style = Stroke(
                    width = 5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 4. Vertex nodes with glowing halo on selected axis
            val nodes = listOf(
                DnaAxis.SAVINGS_VS_INDULGENCE to pTop,
                DnaAxis.PLANNER_VS_SPONTANEOUS to pRight,
                DnaAxis.CONSISTENT_VS_VOLATILE to pBottom,
                DnaAxis.TARGETED_VS_DIVERSE to pLeft
            )
            nodes.forEach { (axis, point) ->
                val isSelected = axis == selectedAxis
                if (isSelected) {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.24f),
                        radius = 18f,
                        center = point
                    )
                }
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 10f else 8f,
                    center = point
                )
                drawCircle(
                    color = accentColor,
                    radius = if (isSelected) 6.5f else 5f,
                    center = point
                )
            }
        }

        // Interactive pole labels positioned cleanly at N / E / S / W
        RadarAxisLabel(
            label = topLabel,
            score = savingsScore,
            isSelected = selectedAxis == DnaAxis.SAVINGS_VS_INDULGENCE,
            accentColor = accentColor,
            onClick = { onAxisSelected(DnaAxis.SAVINGS_VS_INDULGENCE) },
            modifier = Modifier.align(Alignment.TopCenter)
        )
        RadarAxisLabel(
            label = rightLabel,
            score = plannerScore,
            isSelected = selectedAxis == DnaAxis.PLANNER_VS_SPONTANEOUS,
            accentColor = accentColor,
            onClick = { onAxisSelected(DnaAxis.PLANNER_VS_SPONTANEOUS) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp)
        )
        RadarAxisLabel(
            label = bottomLabel,
            score = stabilityScore,
            isSelected = selectedAxis == DnaAxis.CONSISTENT_VS_VOLATILE,
            accentColor = accentColor,
            onClick = { onAxisSelected(DnaAxis.CONSISTENT_VS_VOLATILE) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        RadarAxisLabel(
            label = leftLabel,
            score = focusScore,
            isSelected = selectedAxis == DnaAxis.TARGETED_VS_DIVERSE,
            accentColor = accentColor,
            onClick = { onAxisSelected(DnaAxis.TARGETED_VS_DIVERSE) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 2.dp)
        )
    }
}

@Composable
private fun RadarAxisLabel(
    label: String,
    score: Int,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = if (isSelected) accentColor else TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$score%",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                fontFeatureSettings = "tnum"
            ),
            color = if (isSelected) accentColor else TextGray,
            textAlign = TextAlign.Center
        )
    }
}
