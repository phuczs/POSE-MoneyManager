package com.example.moneymanager.ui.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.KeyboardActions    import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.ui.theme.BackgroundGray
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.ui.viewmodel.AuthViewModel
import com.example.moneymanager.ui.viewmodel.TransactionViewModel
import com.example.moneymanager.ui.viewmodel.BudgetViewModel
import androidx.compose.material.icons.filled.SmartToy
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
    budgetViewModel: BudgetViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)
    val transactionsState by transactionViewModel.transactionsState.collectAsState()
    val quickAddState by transactionViewModel.quickAddState.collectAsState()
    var quickAddText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

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
                    onProfileClick = onNavigateToProfile
                )
            }
            item {
                QuickAddCard(
                    text = quickAddText,
                    onTextChanged = { quickAddText = it },
                    isLoading = quickAddState is TransactionViewModel.QuickAddState.Loading, // Truyền trạng thái loading
                    onSendClick = {
                        if (quickAddText.isNotBlank()) {
                            transactionViewModel.processQuickAdd(quickAddText) // Gọi hàm xử lý tại chỗ
                        }
                    }
                )
            }

            // 2. Quick Actions (Đã thêm nút Chat vào đây)
            item {
                QuickActionsSection(
                    onNavigateToAddTransaction = onNavigateToAddTransaction,
                    onNavigateToTransactions = onNavigateToTransactions,
                    onNavigateToCategories = onNavigateToCategories,
                    onNavigateToBudgets = onNavigateToBudgets,
                    onNavigateToChat = onNavigateToChat // Truyền hàm điều hướng vào
                )
            }

            // 3. Statistics Pie Chart Section
            item {
                SpendingOverviewSection(
                    transactionsState = transactionsState,
                    onNavigateToStatistics = onNavigateToStatistics
                )
            }

            // 4. Recent Transactions Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediumGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToTransactions() }
                    )
                }
            }

            // 5. Transaction List
            when (val state = transactionsState) {
                is TransactionViewModel.TransactionsState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MediumGreen)
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
                        item {
                            EmptyStateCard(onNavigateToAddTransaction)
                        }
                    } else {
                        items(recentTransactions) { transaction ->
                            TransactionListItem(transaction, onTransactionClick)
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
    isLoading: Boolean, // Thêm tham số này
    onSendClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = (-24).dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = { Text("Thêm nhanh (vd: Ăn sáng 30k)", style = MaterialTheme.typography.bodyMedium, color = TextGray.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
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

            // Nút gửi biến thành Loading khi đang xử lý
            Box(contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(4.dp),
                        color = MediumGreen,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = onSendClick,
                        modifier = Modifier.size(48.dp).background(MediumGreen, CircleShape)
                    ) {
                        Icon(Icons.Filled.Send, "Quick Add", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
@Composable
fun DashboardHeader(
    user: com.example.moneymanager.data.model.User?,
    transactionsState: TransactionViewModel.TransactionsState,
    onProfileClick: () -> Unit
    // Đã xóa tham số onChatClick
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp) // Height for the curved background
    ) {
        // Curved Green Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
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
                        text = "Welcome back,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = user?.displayName ?: "User",
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

            Spacer(modifier = Modifier.height(24.dp))

            // Đã xóa nút Chat ở đây

            // Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextGray
                    )

                    val balance = if (transactionsState is TransactionViewModel.TransactionsState.Success) {
                        transactionsState.transactions.filter { it.type == "income" }.sumOf { it.amount } -
                                transactionsState.transactions.filter { it.type == "expense" }.sumOf { it.amount }
                    } else 0.0

                    Text(
                        text = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(balance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Income vs Expense Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Income
                        val income = if (transactionsState is TransactionViewModel.TransactionsState.Success) {
                            transactionsState.transactions.filter { it.type == "income" }.sumOf { it.amount }
                        } else 0.0

                        FinanceIndicator(
                            label = "Income",
                            amount = income,
                            icon = Icons.Default.ArrowUpward, // Arrow down into wallet
                            color = Color(0xFF4CAF50),
                            bgColor = Color(0xFFE8F5E9)
                        )

                        // Expense
                        val expense = if (transactionsState is TransactionViewModel.TransactionsState.Success) {
                            transactionsState.transactions.filter { it.type == "expense" }.sumOf { it.amount }
                        } else 0.0

                        FinanceIndicator(
                            label = "Expenses",
                            amount = expense,
                            icon = Icons.Default.ArrowDownward, // Arrow up out of wallet
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
    amount: Double,
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
                text = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToChat: () -> Unit // Thêm tham số này
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { QuickActionItem("Add", Icons.Default.Add, MediumGreen, onNavigateToAddTransaction) }
            item { QuickActionItem("Transactions", Icons.Default.History, Color(0xFFFFA000), onNavigateToTransactions) }
            item { QuickActionItem("Budgets", Icons.Default.PieChart, Color(0xFF5C6BC0), onNavigateToBudgets) }
            item { QuickActionItem("Category", Icons.Default.Category, Color(0xFFEF5350), onNavigateToCategories) }
            // Nút Chat mới được thêm vào đây
            item { QuickActionItem("AI Chat", Icons.Default.SmartToy, Color(0xFF9C27B0), onNavigateToChat) }
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
fun SpendingOverviewSection(
    transactionsState: TransactionViewModel.TransactionsState,
    onNavigateToStatistics: () -> Unit
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
                    text = "Expenses Breakdown",
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
                                        Text(text = NumberFormat.getCurrencyInstance().format(amount), style = MaterialTheme.typography.labelSmall, color = TextGray)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("No expense data available", style = MaterialTheme.typography.bodySmall, color = TextGray)
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
    onClick: (String) -> Unit
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
                        NumberFormat.getCurrencyInstance().format(transaction.amount),
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
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No transactions yet",
            style = MaterialTheme.typography.bodyLarge,
            color = TextGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = MediumGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Add First Transaction")
        }
    }
}