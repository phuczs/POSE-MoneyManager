package com.example.moneymanager.ui.screens.statistics.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymanager.R
import com.example.moneymanager.domain.model.CashflowTrendPoint
import com.example.moneymanager.ui.theme.*
import com.example.moneymanager.util.StatisticsCalculator
import com.example.moneymanager.util.toCurrencyString

@Composable
fun DualCashflowTrendChart(
    trendPoints: List<CashflowTrendPoint>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(trendPoints) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(700))
    }

    val maxAmount = remember(trendPoints) {
        val maxVal = trendPoints.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0
        if (maxVal > 0) maxVal else 1.0
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
            // Header + Legend
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
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = MediumGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.cashflow_trend_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MediumGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.income),
                        style = MaterialTheme.typography.labelSmall.copy(color = TextGray)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Error)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.expense),
                        style = MaterialTheme.typography.labelSmall.copy(color = TextGray)
                    )
                }
            }

            // Selected point details banner (tooltip)
            selectedIndex?.let { idx ->
                if (idx in trendPoints.indices) {
                    val pt = trendPoints[idx]
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF9FAFB),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mốc ${pt.label}:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Thu: +${pt.income.toCurrencyString(currencyCode)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MediumGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Chi: -${pt.expense.toCurrencyString(currencyCode)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Canvas Chart
            if (trendPoints.isEmpty() || maxAmount <= 1.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_data_trends),
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextGray)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(trendPoints) {
                                detectTapGestures { offset ->
                                    val count = trendPoints.size
                                    if (count > 0) {
                                        val slotWidth = size.width / count
                                        val clickedIdx = (offset.x / slotWidth).toInt().coerceIn(0, count - 1)
                                        selectedIndex = if (selectedIndex == clickedIdx) null else clickedIdx
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height - 30.dp.toPx()
                        val count = trendPoints.size
                        val slotWidth = canvasWidth / count
                        val barWidth = (slotWidth * 0.28f).coerceAtMost(16.dp.toPx())
                        val progress = animationProgress.value

                        // Baseline
                        drawLine(
                            color = Color(0xFFE0E0E0),
                            start = Offset(0f, canvasHeight),
                            end = Offset(canvasWidth, canvasHeight),
                            strokeWidth = 1.dp.toPx()
                        )

                        trendPoints.forEachIndexed { index, point ->
                            val slotCenterX = index * slotWidth + (slotWidth / 2f)

                            // Background highlight for selected column
                            if (selectedIndex == index) {
                                drawRoundRect(
                                    color = MediumGreen.copy(alpha = 0.08f),
                                    topLeft = Offset(index * slotWidth + 2.dp.toPx(), 0f),
                                    size = Size(slotWidth - 4.dp.toPx(), canvasHeight),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                            }

                            // Income bar (left)
                            val incomeHeight = ((point.income / maxAmount) * canvasHeight * progress).toFloat()
                            val incomeLeft = slotCenterX - barWidth - 2.dp.toPx()
                            val incomeTop = canvasHeight - incomeHeight
                            drawRoundRect(
                                color = MediumGreen,
                                topLeft = Offset(incomeLeft, incomeTop),
                                size = Size(barWidth, incomeHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            // Expense bar (right)
                            val expenseHeight = ((point.expense / maxAmount) * canvasHeight * progress).toFloat()
                            val expenseLeft = slotCenterX + 2.dp.toPx()
                            val expenseTop = canvasHeight - expenseHeight
                            drawRoundRect(
                                color = Error,
                                topLeft = Offset(expenseLeft, expenseTop),
                                size = Size(barWidth, expenseHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            // Label on X axis
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    color = if (selectedIndex == index) android.graphics.Color.DKGRAY else android.graphics.Color.GRAY
                                    textSize = 10.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = selectedIndex == index
                                }
                                drawText(
                                    point.label,
                                    slotCenterX,
                                    canvasHeight + 20.dp.toPx(),
                                    paint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
