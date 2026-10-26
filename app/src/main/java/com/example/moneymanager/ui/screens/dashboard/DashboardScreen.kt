package com.example.moneymanager.ui.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource // [QUAN TRỌNG]
import com.example.moneymanager.R // [QUAN TRỌNG]
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.domain.model.Transaction
import com.example.moneymanager.ui.theme.BackgroundGray
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.ui.viewmodel.TransactionViewModel
import com.example.moneymanager.ui.viewmodel.BudgetViewModel
import com.example.moneymanager.ui.viewmodel.AiInsightViewModel
import com.example.moneymanager.ui.viewmodel.InsightUiState
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.moneymanager.ui.viewmodel.AuthViewModel
import com.example.moneymanager.util.toCurrencyString
import com.example.moneymanager.util.toDate
import com.valentinilk.shimmer.shimmer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToChat: () -> Unit,
    onTransactionClick: (String) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    aiInsightViewModel: AiInsightViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)
    val transactionsState by transactionViewModel.transactionsState.collectAsState()
    val quickAddState by transactionViewModel.quickAddState.collectAsState()
    val habitualSuggestion by transactionViewModel.habitualSuggestion.collectAsState()
    val insightState by aiInsightViewModel.insightState.collectAsState()
    var quickAddText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val currentLanguage by authViewModel.currentLanguage.collectAsState()
    val dashboardWidgetsOrder by authViewModel.dashboardWidgetsOrder.collectAsState()
    val dailyVelocityReport by budgetViewModel.dailyVelocityReport.collectAsState()
    val currencyCode = if (currentLanguage == "vi") "VND" else "USD"

    LaunchedEffect(quickAddState) {
        when (val state = quickAddState) {
            is TransactionViewModel.QuickAddState.Success -> {
                quickAddText = "" // Xóa ô nhập sau khi thành công
                snackbarHostState.showSnackbar(state.message)
            }
            is TransactionViewModel.QuickAddState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        transactionViewModel.loadAllTransactions()
        budgetViewModel.loadBudgets()
    }

    Scaffold(
        containerColor = BackgroundGray ,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Emerald Header with Balance (Đã bỏ nút Chat ở đây)
            item {
                DashboardHeader(
                    user = currentUser,
                    transactionsState = transactionsState,
                    onProfileClick = onNavigateToProfile,
                    currencyCode = currencyCode
                )
            }
            item {
                dailyVelocityReport?.let { report ->
                    if (report.isHighVelocity) {
                        SpendingVelocityCard(
                            report = report,
                            currencyCode = currencyCode
                        )
                    }
                }
            }
            item {
                QuickAddCard(
                    text = quickAddText,
                    onTextChanged = { quickAddText = it },
                    isLoading = quickAddState is TransactionViewModel.QuickAddState.Loading,
                    onSendClick = {
                        if (quickAddText.isNotBlank()) {
                            transactionViewModel.processQuickAdd(quickAddText, currentLanguage)
                        }
                    },
                    habitualSuggestion = habitualSuggestion,
                    onHabitClick = { suggestion ->
                        transactionViewModel.executeHabit(suggestion, currentLanguage)
                    },
                    currencyCode = currencyCode
                )
            }

            // 2. Quick Actions
            item {
                QuickActionsSection(
                    onNavigateToStatistics = onNavigateToStatistics,
                    onNavigateToCategories = onNavigateToCategories,
                    onNavigateToChat = onNavigateToChat
                )
            }

            // Settings-driven Dynamic Widgets
            dashboardWidgetsOrder.forEach { widgetKey ->
                when (widgetKey) {
                    "AiInsight" -> {
                        item { AiInsightSection(insightState = insightState) }
                    }
                    "Overview" -> {
                        item {
                            SpendingOverviewSection(
                                transactionsState = transactionsState,
                                onNavigateToStatistics = onNavigateToStatistics,
                                currencyCode = currencyCode
                            )
                        }
                    }
                    "RecentTransactions" -> {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.recent_transactions),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.see_all),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { onNavigateToTransactions() }
                                )
                            }
                        }

                        when (val state = transactionsState) {
                            is TransactionViewModel.TransactionsState.Loading -> {
                                item {
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            is TransactionViewModel.TransactionsState.Error -> {
                                item {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            }
                            is TransactionViewModel.TransactionsState.Success -> {
                                val recentTransactions = state.transactions.take(5)
                                if (recentTransactions.isEmpty()) {
                                    item { EmptyStateCard(onNavigateToAddTransaction) }
                                } else {
                                    items(recentTransactions, key = { it.id }) { transaction ->
                                        TransactionListItem(transaction, onTransactionClick, currencyCode)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
@Composable
fun QuickAddCard(
    text: String,
    onTextChanged: (String) -> Unit,
    isLoading: Boolean, 
    onSendClick: () -> Unit,
    habitualSuggestion: com.example.moneymanager.ui.viewmodel.HabitualSuggestion? = null,
    onHabitClick: ((com.example.moneymanager.ui.viewmodel.HabitualSuggestion) -> Unit)? = null,
    currencyCode: String = "USD"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = (-24).dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    placeholder = { Text(stringResource(R.string.quick_add_placeholder), style = MaterialTheme.typography.bodyMedium, color = TextGray.copy(alpha = 0.7f)) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = BackgroundGray.copy(alpha = 0.5f),
                        unfocusedContainerColor = BackgroundGray.copy(alpha = 0.5f)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if(!isLoading) onSendClick() }),
                    enabled = !isLoading
                )

                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = onSendClick,
                            modifier = Modifier
                                .size(48.dp)
                                .background(MediumGreen, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Quick Add", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        
        if (habitualSuggestion != null && onHabitClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AssistChip(
                onClick = { onHabitClick(habitualSuggestion) },
                label = {
                    val amountString = habitualSuggestion.amount.toCurrencyString(currencyCode)
                    val description = habitualSuggestion.description
                    Text(
                        stringResource(R.string.quick_add_format, description, amountString),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA17106)
                    ) 
                },
                leadingIcon = {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(16.dp))
                },
                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFFFF9E6)),
                border = BorderStroke(width = 1.dp, color = Color(0xFFFFD54F))
            )
        }
    }
}
@Composable
fun DashboardHeader(
    user: com.example.moneymanager.data.model.User?,
    transactionsState: TransactionViewModel.TransactionsState,
    onProfileClick: () -> Unit,
    currencyCode: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("dashboard_settings", android.content.Context.MODE_PRIVATE) }
    var isMonthlyView by remember { mutableStateOf(sharedPrefs.getBoolean("balance_monthly_view", true)) }

    val now = remember { java.util.Calendar.getInstance() }
    val curMonth = now.get(java.util.Calendar.MONTH) + 1
    val curYear = now.get(java.util.Calendar.YEAR)

    val displayTransactions = remember(transactionsState, isMonthlyView) {
        if (transactionsState is TransactionViewModel.TransactionsState.Success) {
            if (isMonthlyView) {
                transactionsState.transactions.filter {
                    val txYear = if (it.year != 0) it.year else {
                        val c = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
                        c.get(java.util.Calendar.YEAR)
                    }
                    val txMonth = if (it.month != 0) it.month else {
                        val c = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
                        c.get(java.util.Calendar.MONTH) + 1
                    }
                    txYear == curYear && txMonth == curMonth
                }
            } else {
                transactionsState.transactions
            }
        } else emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp) // Adjusted height for toggle pill
    ) {
        // Curved Green Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(MediumGreen)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 24.dp, end = 24.dp)
        ) {
            // Top Row: Greeting & Profile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.welcome_back),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = user?.displayName ?: stringResource(R.string.no_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.displayName?.firstOrNull()?.toString()?.uppercase() ?: "U",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mode Selector Pill: [Tháng này] | [Toàn thời gian]
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BackgroundGray)
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isMonthlyView) MediumGreen else Color.Transparent)
                                .clickable {
                                    isMonthlyView = true
                                    sharedPrefs.edit().putBoolean("balance_monthly_view", true).apply()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.view_this_month),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isMonthlyView) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isMonthlyView) Color.White else TextGray
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isMonthlyView) MediumGreen else Color.Transparent)
                                .clickable {
                                    isMonthlyView = false
                                    sharedPrefs.edit().putBoolean("balance_monthly_view", false).apply()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.view_all_time),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (!isMonthlyView) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isMonthlyView) Color.White else TextGray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(if (isMonthlyView) R.string.balance_this_month else R.string.balance_all_time),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextGray
                    )

                    val income = displayTransactions.filter { it.type == "income" }.sumOf { it.amount }
                    val expense = displayTransactions.filter { it.type == "expense" }.sumOf { it.amount }
                    val balanceAmount = income - expense

                    Text(
                        text = balanceAmount.toCurrencyString(currencyCode),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Income vs Expense Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FinanceIndicator(
                            label = stringResource(R.string.income),
                            amount = income.toCurrencyString(currencyCode),
                            icon = Icons.Default.ArrowUpward,
                            color = Color(0xFF4CAF50),
                            bgColor = Color(0xFFE8F5E9)
                        )

                        FinanceIndicator(
                            label = stringResource(R.string.expenses),
                            amount = expense.toCurrencyString(currencyCode),
                            icon = Icons.Default.ArrowDownward,
                            color = Color(0xFFF44336),
                            bgColor = Color(0xFFFFEBEE)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FinanceIndicator(
    label: String,
    amount: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextGray)
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onNavigateToStatistics: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { QuickActionItem(stringResource(R.string.action_category), Icons.Default.Category, Color(0xFFEF5350), onNavigateToCategories) }
            item { QuickActionItem(stringResource(R.string.nav_statistics), Icons.Default.BarChart, Color(0xFFFFA000), onNavigateToStatistics) }
            item { QuickActionItem(stringResource(R.string.action_chat), Icons.Default.SmartToy, Color(0xFF9C27B0), onNavigateToChat) }
        }
    }
}

@Composable
fun QuickActionItem(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = text, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

// ... (Các phần còn lại của file giữ nguyên: SpendingOverviewSection, DonutChart, TransactionListItem, EmptyStateCard) ...

@Composable
fun AiInsightSection(insightState: InsightUiState) {
    var expanded by remember { mutableStateOf(false) }

    when (insightState) {
        is InsightUiState.Loading -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .shimmer(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Box(modifier = Modifier
                        .width(150.dp)
                        .height(20.dp)
                        .background(Color.LightGray, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(Color.LightGray, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .background(Color.LightGray, RoundedCornerShape(4.dp)))
                }
            }
        }
        is InsightUiState.Success -> {
            val insight = insightState.insight
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clickable { expanded = !expanded },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Sparkle",
                            tint = Color(0xFF9C27B0), // Purple for AI
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_monthly_insight),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextGray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = insight.summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )

                    if (expanded && insight.actionableTip.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3E5F5), RoundedCornerShape(12.dp)) // Light purple bg
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.ai_tip_for_you),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7B1FA2) // Darker purple
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = insight.actionableTip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
        else -> {
            // Unavailable or InsufficientData: gracefully hide the section to declutter UI
        }
    }
}

@Composable
fun SpendingOverviewSection(
    transactionsState: TransactionViewModel.TransactionsState,
    onNavigateToStatistics: () -> Unit,
    currencyCode: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clickable { onNavigateToStatistics() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.expenses_breakdown),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (transactionsState is TransactionViewModel.TransactionsState.Success) {
                val expenses = transactionsState.transactions.filter { it.type == "expense" }
                if (expenses.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Chart
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChart(
                                expenses = expenses,
                                modifier = Modifier.size(120.dp)
                            )
                        }

                        // Legend (Top 3 Categories)
                        Column(modifier = Modifier.weight(1f)) {
                            val categories = expenses.groupBy { it.category }
                                .mapValues { it.value.sumOf { tx -> tx.amount } }
                                .toList()
                                .sortedByDescending { it.second }
                                .take(3)

                            categories.forEachIndexed { index, (cat, amount) ->
                                val color = getChartColor(index)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = cat, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                        Text(text = amount.toCurrencyString(currencyCode), style = MaterialTheme.typography.labelSmall, color = TextGray)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(stringResource(R.string.no_expense_data), style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    expenses: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val total = expenses.sumOf { it.amount }.toFloat()
    val grouped = expenses.groupBy { it.category }
        .mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }
        .toList()
        .sortedByDescending { it.second }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(expenses) {
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 30f
        val radius = size.minDimension / 2 - strokeWidth / 2
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f

        grouped.forEachIndexed { index, (_, amount) ->
            val sweepAngle = (amount / total) * 360f * animationProgress.value
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

fun getChartColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF00796B), // MediumGreen
        Color(0xFF00A79B), // LightGreen
        Color(0xFFFFC107), // Amber
        Color(0xFFEF5350), // Red
        Color(0xFF42A5F5)  // Blue
    )
    return colors[index % colors.size]
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: (String) -> Unit,
    currencyCode: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clickable { transaction.id.let { onClick(it) } },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (transaction.type == "income") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.type == "income") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (transaction.type == "income") Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(transaction.date.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }

            Text(
                text = (if (transaction.type == "income") "+" else "-") +
                        transaction.amount.toCurrencyString(currencyCode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == "income") Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun EmptyStateCard(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_transactions_yet),
            style = MaterialTheme.typography.bodyLarge,
            color = TextGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = MediumGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.add_first_transaction))
        }
    }
}

@Composable
fun SpendingVelocityCard(
    report: com.example.moneymanager.domain.model.DailyVelocityReport,
    currencyCode: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        border = BorderStroke(1.dp, Color(0xFFFFD54F)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFECB3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF57C00),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.velocity_alert_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.height(4.dp))
                val todayStr = report.todaySpent.toCurrencyString(currencyCode)
                val avgStr = report.dailyAverage.toCurrencyString(currencyCode)
                val projStr = report.projectedMonthTotal.toCurrencyString(currencyCode)
                val percentStr = String.format(Locale.getDefault(), "%.0f", report.percentOverAverage)
                Text(
                    text = stringResource(
                        R.string.velocity_alert_message,
                        todayStr,
                        percentStr,
                        avgStr,
                        projStr
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D4037),
                    lineHeight = 18.sp
                )
            }
        }
    }
}