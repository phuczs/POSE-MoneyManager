package com.example.moneymanager.ui.screens.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import com.example.moneymanager.R
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymanager.ui.viewmodel.AuthViewModel // Import
import com.example.moneymanager.util.toCurrencyString
import com.example.moneymanager.util.toDate
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.domain.usecase.analytics.CalculateCashflowAnalyticsUseCase
import com.example.moneymanager.ui.screens.statistics.components.CashflowSummaryCard
import com.example.moneymanager.ui.screens.statistics.components.DualCashflowTrendChart
import com.example.moneymanager.ui.screens.statistics.components.MonthEndProjectionCard
import com.example.moneymanager.ui.screens.statistics.components.PeriodComparisonCard
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.moneymanager.ui.theme.BackgroundGray
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.ui.viewmodel.CategoryViewModel
import com.example.moneymanager.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val transactionsState by transactionViewModel.transactionsState.collectAsState()
    val categoriesState by categoryViewModel.categoriesState.collectAsState()

    // State for filters & tabs
    var activeTab by remember { mutableStateOf(0) } // 0: Breakdown, 1: Cashflow & Trends
    var selectedPeriod by remember { mutableStateOf("Month") } // Week, Month, Year
    var selectedType by remember { mutableStateOf("expense") } // income, expense
    val currentLanguage by authViewModel.currentLanguage.collectAsState(initial = "vi")
    val currencyCode = if (currentLanguage == "vi") "VND" else "USD"

    val cashflowAnalyticsUseCase = remember { CalculateCashflowAnalyticsUseCase() }

    LaunchedEffect(Unit) {
        transactionViewModel.loadAllTransactions()
        categoryViewModel.loadAllCategories()
    }

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.statistics), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundGray
                )
            )
        }
    ) { padding ->
        if (transactionsState is TransactionViewModel.TransactionsState.Loading ||
            categoriesState is CategoryViewModel.CategoriesState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MediumGreen)
            }
        } else if (transactionsState is TransactionViewModel.TransactionsState.Success &&
            categoriesState is CategoryViewModel.CategoriesState.Success) {

            val allTransactions = (transactionsState as TransactionViewModel.TransactionsState.Success).transactions
            val filteredTransactions = filterTransactions(allTransactions, selectedPeriod, selectedType)
            val cashflowReport = remember(allTransactions, selectedPeriod) {
                cashflowAnalyticsUseCase(allTransactions, selectedPeriod)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Top Tab Selector (Breakdown vs Cashflow)
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TabRow(
                            selectedTabIndex = activeTab,
                            containerColor = Color.Transparent,
                            contentColor = MediumGreen,
                            indicator = { tabPositions ->
                                SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                    color = MediumGreen
                                )
                            },
                            divider = {}
                        ) {
                            Tab(
                                selected = activeTab == 0,
                                onClick = { activeTab = 0 },
                                text = {
                                    Text(
                                        stringResource(R.string.tab_breakdown),
                                        fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                            Tab(
                                selected = activeTab == 1,
                                onClick = { activeTab = 1 },
                                text = {
                                    Text(
                                        stringResource(R.string.tab_cashflow),
                                        fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (activeTab == 0) {
                    // --- TAB 0: Phân bổ danh mục & Donut chart ---
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            // Income/Expense Toggle
                            TypeSelector(selectedType) { selectedType = it }
                            Spacer(modifier = Modifier.height(16.dp))
                            // Time Period Toggle
                            PeriodSelector(selectedPeriod) { selectedPeriod = it }
                        }
                    }

                    // Main Donut Chart Section with Legend
                    item {
                        if (filteredTransactions.isNotEmpty()) {
                            DonutChartSection(filteredTransactions, selectedType, currencyCode)
                        } else {
                            EmptyChartState()
                        }
                    }

                    // Bar Chart Trend Section
                    item {
                        if (filteredTransactions.isNotEmpty()) {
                            TrendChartSection(allTransactions, selectedPeriod, selectedType)
                        }
                    }

                    // Category Breakdown List
                    item {
                        if (filteredTransactions.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.details),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                            )
                        }
                    }

                    if (filteredTransactions.isNotEmpty()) {
                        val categoryTotals = filteredTransactions
                            .groupBy { it.category }
                            .mapValues { it.value.sumOf { tx -> tx.amount } }
                            .toList()
                            .sortedByDescending { it.second }

                        items(categoryTotals) { (catName, amount) ->
                            val total = filteredTransactions.sumOf { it.amount }
                            val percentage = (amount / total).toFloat()
                            CategoryBreakdownItem(catName, amount, percentage, selectedType, currencyCode)
                        }
                    }
                } else {
                    // --- TAB 1: Dòng tiền & Thống kê nâng cao ---
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Time Period Selector
                            PeriodSelector(selectedPeriod) { selectedPeriod = it }

                            // 1. Hero Card: Savings Rate & Net Cashflow
                            CashflowSummaryCard(
                                summary = cashflowReport.comparison,
                                currencyCode = currencyCode
                            )

                            // 2. Period Comparison Card (MoM / YoY / WoW)
                            val periodLabel = when (selectedPeriod) {
                                "Week" -> if (currentLanguage == "vi") "tuần này" else "this week"
                                "Year" -> if (currentLanguage == "vi") "năm nay" else "this year"
                                else -> if (currentLanguage == "vi") "tháng này" else "this month"
                            }
                            PeriodComparisonCard(
                                summary = cashflowReport.comparison,
                                periodLabel = periodLabel,
                                currencyCode = currencyCode
                            )

                            // 3. Dual Curve Trend Chart (Thu vs Chi)
                            DualCashflowTrendChart(
                                trendPoints = cashflowReport.trendPoints,
                                currencyCode = currencyCode
                            )

                            // 4. Month-end Burn-rate Projection (hiển thị khi chọn Month)
                            if (selectedPeriod == "Month") {
                                MonthEndProjectionCard(
                                    projection = cashflowReport.projection,
                                    currencyCode = currencyCode
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- UI Components ---

@Composable
fun TypeSelector(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val types = listOf(
            "expense" to stringResource(R.string.expense), 
            "income" to stringResource(R.string.income)
        )
        types.forEach { (key, label) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected == key)
                        if(key == "income") Color(0xFF4CAF50) else Color(0xFFF44336)
                    else Color.Transparent)
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    color = if (selected == key) Color.White else TextGray
                )
            }
        }
    }
}

@Composable
fun PeriodSelector(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val periods = listOf(
            "Week" to stringResource(R.string.week),
            "Month" to stringResource(R.string.month),
            "Year" to stringResource(R.string.year)
        )
        periods.forEach { (period, label) ->
            val isSelected = selected == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) MediumGreen else Color.Transparent)
                    .clickable { onSelect(period) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else TextGray
                )
            }
        }
    }
}

@Composable
fun DonutChartSection(transactions: List<Transaction>, type: String, currencyCode: String) {
    val totalAmount = transactions.sumOf { it.amount }
    val categoryData = transactions.groupBy { it.category }
        .map { it.key to it.value.sumOf { tx -> tx.amount } }
        .sortedByDescending { it.second }
        .take(5) // Take top 5 for chart/legend

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Title centered
            Text(
                text = stringResource(R.string.total_type_format, if (type == "income") stringResource(R.string.income) else stringResource(R.string.expense)),
                style = MaterialTheme.typography.labelMedium,
                color = TextGray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chart on the Left
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedDonutChart(
                        data = categoryData,
                        total = totalAmount,
                        modifier = Modifier.size(130.dp)
                    )
                    // Inner Text with custom compact formatter
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = totalAmount.toCurrencyString(currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Legend on the Right
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    categoryData.forEachIndexed { index, (category, amount) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(getChartColor(index))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${(amount / totalAmount * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedDonutChart(
    data: List<Pair<String, Double>>,
    total: Double,
    modifier: Modifier
) {
    val animation = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animation.snapTo(0f)
        animation.animateTo(1f, animationSpec = tween(durationMillis = 1000, easing = LinearEasing))
    }

    Canvas(modifier = modifier) {
        var startAngle = -90f
        val strokeWidth = 35f
        val radius = size.minDimension / 2 - strokeWidth / 2
        val center = Offset(size.width / 2, size.height / 2)

        data.forEachIndexed { index, (_, amount) ->
            val sweepAngle = ((amount / total) * 360f).toFloat() * animation.value
            val color = getChartColor(index)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun TrendChartSection(allTransactions: List<Transaction>, period: String, type: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.trends_format, when(period) {
                    "Week" -> stringResource(R.string.week)
                    "Month" -> stringResource(R.string.month)
                    else -> stringResource(R.string.year)
                }),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            val chartData = prepareTrendData(allTransactions, period, type)

            if (chartData.isNotEmpty() && chartData.any { it.second > 0 }) {
                CustomBarChart(
                    data = chartData,
                    color = if(type == "income") Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_data_trends), color = TextGray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CustomBarChart(
    data: List<Pair<String, Double>>,
    color: Color,
    modifier: Modifier
) {
    val maxValue = data.maxOfOrNull { it.second } ?: 1.0

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 1.5f)
        val space = (size.width - (barWidth * data.size)) / (data.size + 1)
        val height = size.height

        data.forEachIndexed { index, (label, value) ->
            val barHeight = (value / maxValue * (height - 40f)).toFloat().coerceAtLeast(4f)
            val x = space + (index * (barWidth + space))
            val y = height - barHeight - 40f // Leave space for text

            // Draw Bar
            drawRoundRect(
                color = if(value > 0) color else Color.LightGray.copy(alpha = 0.3f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Draw Label
            val textPaint = android.graphics.Paint().apply {
                setColor(android.graphics.Color.GRAY)
                textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
            }

            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barWidth / 2,
                height - 10f,
                textPaint
            )
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
fun CategoryBreakdownItem(
    name: String,
    amount: Double,
    percentage: Float,
    type: String,
    currencyCode: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Percentage Bubble
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if(type == "income") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if(type == "income") Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                // Visual Progress Bar
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if(type == "income") Color(0xFF4CAF50) else Color(0xFFF44336),
                    trackColor = BackgroundGray,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = amount.toCurrencyString(currencyCode),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun EmptyChartState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextGray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.no_data_period), color = TextGray)
        }
    }
}

// --- Helpers (Delegated to StatisticsCalculator for clean architecture separation) ---

fun formatCompactNumber(number: Double): String =
    com.example.moneymanager.util.StatisticsCalculator.formatCompactNumber(number)

fun filterTransactions(transactions: List<Transaction>, period: String, type: String): List<Transaction> =
    com.example.moneymanager.util.StatisticsCalculator.filterTransactions(transactions, period, type)

fun prepareTrendData(transactions: List<Transaction>, period: String, type: String): List<Pair<String, Double>> =
    com.example.moneymanager.util.StatisticsCalculator.prepareTrendData(transactions, period, type)

fun getChartColor(index: Int): Color =
    com.example.moneymanager.util.StatisticsCalculator.getChartColor(index)