package com.example.moneymanager.ui.screens.transaction

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable // Added this import
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onTransactionClick: (String) -> Unit,
    transactionViewModel: TransactionViewModel = hiltViewModel()
) {
    val transactionsState by transactionViewModel.transactionsState.collectAsState()
    val isSelectionMode by transactionViewModel.isSelectionMode.collectAsState()
    val selectedTransactionIds by transactionViewModel.selectedTransactionIds.collectAsState()
    val searchQuery by transactionViewModel.searchQuery.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    // Filter states
    var selectedTypeFilter by remember { mutableStateOf("all") } // all, income, expense
    var isMonthFilterExpanded by remember { mutableStateOf(false) }
    var selectedMonthFilter by remember { mutableStateOf("All Time") }

    val months = listOf(
        "All Time",
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Load initial data
    LaunchedEffect(Unit) {
        transactionViewModel.loadAllTransactions()
    }

    // Apply filters (Type/Month) - Search is handled by VM on top of this
    LaunchedEffect(selectedTypeFilter, selectedMonthFilter) {
        when {
            selectedTypeFilter != "all" && selectedMonthFilter != "All Time" -> {
                val monthIndex = months.indexOf(selectedMonthFilter)
                if (monthIndex > 0) {
                    val currentYear = java.time.Year.now().value
                    val yearMonth = java.time.YearMonth.of(currentYear, monthIndex)
                    transactionViewModel.loadTransactionsByTypeAndMonth(selectedTypeFilter, yearMonth)
                }
            }
            selectedTypeFilter != "all" -> transactionViewModel.loadTransactionsByType(selectedTypeFilter)
            selectedMonthFilter != "All Time" -> {
                val monthIndex = months.indexOf(selectedMonthFilter)
                if (monthIndex > 0) {
                    val currentYear = java.time.Year.now().value
                    val yearMonth = java.time.YearMonth.of(currentYear, monthIndex)
                    transactionViewModel.loadTransactionsByMonth(yearMonth)
                }
            }
            else -> transactionViewModel.loadAllTransactions()
        }
    }

    Scaffold(
        topBar = {
            when {
                isSelectionMode -> {
                    SelectionTopAppBar(
                        selectedCount = selectedTransactionIds.size,
                        totalCount = (transactionsState as? TransactionViewModel.TransactionsState.Success)?.transactions?.size ?: 0,
                        onClose = { transactionViewModel.toggleSelectionMode() },
                        onSelectAll = {
                            if (transactionsState is TransactionViewModel.TransactionsState.Success) {
                                val transactions = (transactionsState as TransactionViewModel.TransactionsState.Success).transactions
                                if (selectedTransactionIds.size == transactions.size) transactionViewModel.clearSelection()
                                else transactionViewModel.selectAllTransaction(transactions)
                            }
                        }
                    )
                }
                isSearchActive -> {
                    SearchTopAppBar(
                        query = searchQuery,
                        onQueryChange = { transactionViewModel.onSearchQueryChanged(it) },
                        onClose = {
                            isSearchActive = false
                            transactionViewModel.onSearchQueryChanged("") // Clear search when closing
                        }
                    )
                }
                else -> {
                    TopAppBar(
                        title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (isSelectionMode) {
                FloatingActionButton(
                    onClick = { showDeleteDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = onNavigateToAddTransaction,
                        containerColor = MediumGreen,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                    }
                    SmallFloatingActionButton(
                        onClick = { transactionViewModel.toggleSelectionMode() },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = TextGray
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Select Transactions")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Filters (Hide filters if search is active to avoid clutter, optional)
            if (!isSearchActive) {
                TransactionFilters(
                    selectedType = selectedTypeFilter,
                    onTypeSelected = { selectedTypeFilter = it },
                    selectedMonth = selectedMonthFilter,
                    onMonthSelected = { selectedMonthFilter = it },
                    months = months
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Transactions list
            when (transactionsState) {
                is TransactionViewModel.TransactionsState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MediumGreen)
                    }
                }
                is TransactionViewModel.TransactionsState.Error -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = (transactionsState as TransactionViewModel.TransactionsState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                is TransactionViewModel.TransactionsState.Success -> {
                    val transactions = (transactionsState as TransactionViewModel.TransactionsState.Success).transactions

                    if (transactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if(isSearchActive) Icons.Default.SearchOff else Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if(isSearchActive) "No results found" else "No transactions yet",
                                    color = TextGray
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(transactions, key = { it.id }) { transaction ->
                                TransactionListItem(
                                    transaction = transaction,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedTransactionIds.contains(transaction.id),
                                    onTransactionClick = {
                                        if (isSelectionMode) {
                                            transactionViewModel.toggleTransactionSelection(transaction.id)
                                        } else {
                                            onTransactionClick(transaction.id)
                                        }
                                    },
                                    onTransactionLongClick = {
                                        if (!isSelectionMode) {
                                            transactionViewModel.toggleSelectionMode()
                                            transactionViewModel.toggleTransactionSelection(transaction.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Transactions") },
            text = { Text("Delete ${selectedTransactionIds.size} selected transaction(s)? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        transactionViewModel.deleteSelectedTransactions()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search transactions...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp)
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, "Clear")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(
                    if (selectedCount == totalCount && totalCount > 0) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    "Select All"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFilters(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    selectedMonth: String,
    onMonthSelected: (String) -> Unit,
    months: List<String>
) {
    var isMonthExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedType == "all",
            onClick = { onTypeSelected("all") },
            label = { Text("All") }
        )
        FilterChip(
            selected = selectedType == "income",
            onClick = { onTypeSelected("income") },
            label = { Text("Income") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE8F5E9))
        )
        FilterChip(
            selected = selectedType == "expense",
            onClick = { onTypeSelected("expense") },
            label = { Text("Expense") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFEBEE))
        )

        Spacer(modifier = Modifier.weight(1f))

        ExposedDropdownMenuBox(
            expanded = isMonthExpanded,
            onExpandedChange = { isMonthExpanded = it }
        ) {
            Row(
                modifier = Modifier
                    .menuAnchor()
                    .clickable { isMonthExpanded = true }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedMonth,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Icon(Icons.Default.KeyboardArrowDown, null, tint = TextGray)
            }
            ExposedDropdownMenu(
                expanded = isMonthExpanded,
                onDismissRequest = { isMonthExpanded = false }
            ) {
                months.forEach { month ->
                    DropdownMenuItem(
                        text = { Text(month) },
                        onClick = {
                            onMonthSelected(month)
                            isMonthExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListItem(
    transaction: Transaction,
    onTransactionClick: () -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onTransactionLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onTransactionClick,
                onLongClick = onTransactionLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.White
        ),
        border = if (isSelected) BorderStroke(2.dp, MediumGreen) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onTransactionClick() },
                    colors = CheckboxDefaults.colors(checkedColor = MediumGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

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

                if (transaction.description.isNotEmpty()) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(transaction.date.toDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
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