package com.example.moneymanager.ui.screens.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.BudgetPeriod
import com.example.moneymanager.domain.model.BudgetStatus
import com.example.moneymanager.domain.model.BudgetWithProgress
import com.example.moneymanager.ui.screens.budget.components.BudgetHealthScoreCard
import com.example.moneymanager.ui.screens.budget.components.CategoryInsightCard
import com.example.moneymanager.ui.theme.BackgroundGray
import com.example.moneymanager.ui.theme.DarkGreen
import com.example.moneymanager.ui.theme.Error
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.ui.viewmodel.BudgetViewModel
import com.example.moneymanager.ui.viewmodel.BudgetsUiState
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.moneymanager.ui.viewmodel.AuthViewModel
import com.example.moneymanager.util.toCurrencyString
import androidx.compose.ui.res.stringResource
import java.util.Date
import com.example.moneymanager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedBudget by remember { mutableStateOf<Budget?>(null) }
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }
    var showAiPredictiveSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var historyCategory by remember { mutableStateOf("") }
    val budgetHistory by viewModel.budgetHistory.collectAsState()
    var selectedCycleFilter by remember { mutableStateOf<BudgetPeriod?>(null) }
    var isHealthySectionExpanded by remember { mutableStateOf(true) }
    
    val currentLanguage by authViewModel.currentLanguage.collectAsState(initial = "vi")
    val currencyCode = if (currentLanguage == "vi") "VND" else "USD"

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.budgets),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundGray
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // AI Suggestions FAB
                FloatingActionButton(
                    onClick = { showAiPredictiveSheet = true },
                    containerColor = Color(0xFFF3E5F5), // Light Purple
                    contentColor = Color(0xFF9C27B0), // Purple
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = stringResource(R.string.ai_budget_suggestions))
                }
                
                // Add Manual Budget FAB
                FloatingActionButton(
                    onClick = {
                        selectedBudget = null
                        showAddEditDialog = true
                    },
                    containerColor = MediumGreen,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_budget))
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is BudgetsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MediumGreen)
                    }
                }
                is BudgetsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is BudgetsUiState.Success -> {
                    val filteredBudgets = remember(state.budgets, selectedCycleFilter) {
                        if (selectedCycleFilter == null) state.budgets
                        else state.budgets.filter { it.budget.period == selectedCycleFilter }
                    }

                    val attentionBudgets = remember(filteredBudgets) {
                        filteredBudgets.filter { it.status == BudgetStatus.Warning || it.status == BudgetStatus.Over }
                    }

                    val healthyBudgets = remember(filteredBudgets) {
                        filteredBudgets.filter { it.status == BudgetStatus.Normal }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp), // Space for FAB
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Budget Health Score Hero Component
                        item {
                            BudgetHealthScoreCard(
                                budgets = state.budgets,
                                overallBudget = state.overallBudget,
                                overallSpent = state.overallSpent,
                                currencyCode = currencyCode
                            )
                        }

                        // 2. Cycle Filter Chips Row
                        item {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCycleFilter == null,
                                        onClick = { selectedCycleFilter = null },
                                        label = {
                                            Text(
                                                text = stringResource(R.string.all_cycles),
                                                fontWeight = if (selectedCycleFilter == null) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MediumGreen.copy(alpha = 0.15f),
                                            selectedLabelColor = DarkGreen,
                                            containerColor = BackgroundGray,
                                            labelColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                items(BudgetPeriod.entries.toTypedArray()) { period ->
                                    val isSelected = selectedCycleFilter == period
                                    val label = when (period) {
                                        BudgetPeriod.WEEKLY -> stringResource(R.string.period_weekly)
                                        BudgetPeriod.BI_WEEKLY -> stringResource(R.string.period_bi_weekly)
                                        BudgetPeriod.MONTHLY -> stringResource(R.string.period_monthly)
                                        BudgetPeriod.QUARTERLY -> stringResource(R.string.period_quarterly)
                                        BudgetPeriod.CUSTOM -> stringResource(R.string.period_custom)
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedCycleFilter = if (selectedCycleFilter == period) null else period
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MediumGreen.copy(alpha = 0.15f),
                                            selectedLabelColor = DarkGreen,
                                            containerColor = BackgroundGray,
                                            labelColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        // 3. Grouped Budget Sections
                        if (filteredBudgets.isEmpty()) {
                            item {
                                EmptyState()
                            }
                        } else {
                            // Section A: Needs Attention
                            if (attentionBudgets.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${stringResource(R.string.needs_attention)} (${attentionBudgets.size})",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFD32F2F)
                                        )
                                    }
                                }
                                items(attentionBudgets) { budgetWP ->
                                    BudgetListItem(
                                        budgetWP = budgetWP,
                                        currencyCode = currencyCode,
                                        onEditClick = {
                                            selectedBudget = budgetWP.budget
                                            showAddEditDialog = true
                                        },
                                        onDeleteClick = {
                                            budgetToDelete = budgetWP.budget
                                            showDeleteConfirmDialog = true
                                        },
                                        onViewHistoryClick = {
                                            historyCategory = budgetWP.budget.category
                                            viewModel.loadBudgetHistory(budgetWP.budget.category)
                                            showHistorySheet = true
                                        }
                                    )
                                }
                            }

                            // Section B: On Track / Healthy
                            if (healthyBudgets.isNotEmpty()) {
                                item {
                                    Surface(
                                        onClick = { isHealthySectionExpanded = !isHealthySectionExpanded },
                                        color = Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = DarkGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${stringResource(R.string.on_track_group)} (${healthyBudgets.size})",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isHealthySectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = TextGray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                if (isHealthySectionExpanded) {
                                    items(healthyBudgets) { budgetWP ->
                                        BudgetListItem(
                                            budgetWP = budgetWP,
                                            currencyCode = currencyCode,
                                            onEditClick = {
                                                selectedBudget = budgetWP.budget
                                                showAddEditDialog = true
                                            },
                                            onDeleteClick = {
                                                budgetToDelete = budgetWP.budget
                                                showDeleteConfirmDialog = true
                                            },
                                            onViewHistoryClick = {
                                                historyCategory = budgetWP.budget.category
                                                viewModel.loadBudgetHistory(budgetWP.budget.category)
                                                showHistorySheet = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditBudgetDialog(
            budget = selectedBudget,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { budget ->
                if (selectedBudget == null) viewModel.saveBudget(budget) else viewModel.updateBudget(budget)
                showAddEditDialog = false
            },
            onDelete = {
                selectedBudget?.id?.let {
                    budgetToDelete = selectedBudget
                    showDeleteConfirmDialog = true
                }
                showAddEditDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteBudgetConfirmationDialog(
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                budgetToDelete?.id?.let { viewModel.deleteBudget(it) }
                showDeleteConfirmDialog = false
            }
        )
    }

    if (showAiPredictiveSheet) {
        PredictiveBudgetSheet(
            onDismiss = { showAiPredictiveSheet = false },
            authViewModel = authViewModel
        )
    }

    if (showHistorySheet) {
        BudgetHistorySheet(
            category = historyCategory,
            historyEntries = budgetHistory,
            currencyCode = currencyCode,
            onDismiss = { showHistorySheet = false }
        )
    }
}

@Composable
private fun OverallBudgetSummary(state: BudgetsUiState.Success,currencyCode: String) {
    val remaining = state.overallBudget - state.overallSpent
    val progress = if (state.overallBudget > 0) (state.overallSpent / state.overallBudget).toFloat() else 0f

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000),
        label = "ProgressAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MediumGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.remaining_total),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = remaining.toCurrencyString(currencyCode),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Circular Progress Indicator for Total
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(56.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        strokeWidth = 4.dp,
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(56.dp),
                        color = Color.White,
                        strokeWidth = 4.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.spent),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = state.overallSpent.toCurrencyString(currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.total_budget),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = state.overallBudget.toCurrencyString(currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetListItem(
    budgetWP: BudgetWithProgress,
    currencyCode: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onViewHistoryClick: () -> Unit
) {
    // Determine colors and messages based on status
    val (statusColor, statusBg, statusText) = when (budgetWP.status) {
        BudgetStatus.Normal -> Triple(MediumGreen, Color(0xFFE8F5E9), stringResource(R.string.on_track))
        BudgetStatus.Warning -> Triple(Color(0xFFFFA000), Color(0xFFFFF8E1), stringResource(R.string.warning))
        BudgetStatus.Over -> Triple(Color(0xFFD32F2F), Color(0xFFFFEBEE), stringResource(R.string.over_budget))
    }

    val remaining = budgetWP.budget.effectiveAmount - budgetWP.spentAmount
    var expanded by remember { mutableStateOf(false) }
    var isCardExpanded by remember { mutableStateOf(false) }

    val periodLabel = when (budgetWP.budget.period) {
        BudgetPeriod.WEEKLY -> stringResource(R.string.period_weekly)
        BudgetPeriod.BI_WEEKLY -> stringResource(R.string.period_bi_weekly)
        BudgetPeriod.MONTHLY -> stringResource(R.string.period_monthly)
        BudgetPeriod.QUARTERLY -> stringResource(R.string.period_quarterly)
        BudgetPeriod.CUSTOM -> stringResource(R.string.period_custom)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { isCardExpanded = !isCardExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category Icon + Name & Tags + Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Initial Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(statusBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = budgetWP.budget.category.firstOrNull()?.toString() ?: "?",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = budgetWP.budget.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        // Period Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BackgroundGray
                        ) {
                            Text(
                                text = periodLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextGray,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Auto-renew icon
                        if (budgetWP.budget.isRecurring) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Auto-renew",
                                tint = MediumGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )

                        // Rollover indicator
                        if (budgetWP.budget.rolloverAmount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.rollover_tag, budgetWP.budget.rolloverAmount.toCurrencyString(currencyCode)),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = DarkGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Dropdown Menu
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options), tint = TextGray)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = Color.White
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.view_history)) },
                            onClick = {
                                expanded = false
                                onViewHistoryClick()
                            },
                            leadingIcon = { Icon(Icons.Default.History, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                expanded = false
                                onEditClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                expanded = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.remaining),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextGray
                    )
                    Text(
                        text = remaining.toCurrencyString(currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining < 0) MaterialTheme.colorScheme.error else TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDateRange(budgetWP.budget.startDate, budgetWP.budget.endDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                    Text(
                        text = stringResource(
                            R.string.spent_of_allocated,
                            budgetWP.spentAmount.toCurrencyString(currencyCode),
                            budgetWP.budget.effectiveAmount.toCurrencyString(currencyCode)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { budgetWP.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = BackgroundGray,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Smart Intelligence Info Row (Days remaining & Daily Allowance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.days_left_format, budgetWP.daysRemaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )

                if (budgetWP.daysRemaining > 0 && remaining > 0) {
                    Text(
                        text = stringResource(R.string.daily_allowance_format, budgetWP.dailyAllowance.toCurrencyString(currencyCode)),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MediumGreen
                    )
                }
            }

            // Projected overspend alert (if at risk)
            if (budgetWP.projectedOverspend > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.projected_overspend_format, budgetWP.projectedOverspend.toCurrencyString(currencyCode)),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            // Optional Note / Memo preview
            if (budgetWP.budget.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = budgetWP.budget.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
            }

            // Expandable Category Insight Drawer
            AnimatedVisibility(
                visible = isCardExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    CategoryInsightCard(
                        budgetWP = budgetWP,
                        currencyCode = currencyCode,
                        onViewHistoryClick = onViewHistoryClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCardExpanded) stringResource(R.string.collapse_details) else stringResource(R.string.tap_to_view_details),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = TextGray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(BackgroundGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_budgets_set),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_budget_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DeleteBudgetConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = { Text(stringResource(R.string.delete_budget)) },
        text = { Text(stringResource(R.string.delete_budget_confirm)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}



private fun formatDateRange(start: Long, end: Long): String {
    val format = SimpleDateFormat("MMM dd", Locale.getDefault())
    return "${format.format(java.util.Date(start))} - ${format.format(java.util.Date(end))}"
}