package com.example.moneymanager.ui.screens.transaction

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Filter states
    var selectedTypeFilter by remember { mutableStateOf("all") } // all, income, expense
    var isMonthFilterExpanded by remember { mutableStateOf(false) }
    var selectedMonthFilter by remember { mutableStateOf("All Time") }
    
    val months = listOf(
        "All Time",
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    // Load transactions on initial composition
    LaunchedEffect(Unit) {
        transactionViewModel.loadAllTransactions()
    }
    
    // Apply filters when they change
    LaunchedEffect(selectedTypeFilter, selectedMonthFilter) {
        when {
            // Both type and month selected
            selectedTypeFilter != "all" && selectedMonthFilter != "All Time" -> {
                val monthIndex = months.indexOf(selectedMonthFilter) // e.g., "January" = 1
                if (monthIndex > 0) { // Valid month found
                    val currentYear = java.time.Year.now().value
                    val yearMonth = java.time.YearMonth.of(currentYear, monthIndex) // monthIndex is already correct (1-12)
                    transactionViewModel.loadTransactionsByTypeAndMonth(selectedTypeFilter, yearMonth)
                }
            }
            // Only type selected
            selectedTypeFilter != "all" -> {
                transactionViewModel.loadTransactionsByType(selectedTypeFilter)
            }
            // Only month selected
            selectedMonthFilter != "All Time" -> {
                val monthIndex = months.indexOf(selectedMonthFilter) // e.g., "January" = 1
                if (monthIndex > 0) { // Valid month found
                    val currentYear = java.time.Year.now().value
                    val yearMonth = java.time.YearMonth.of(currentYear, monthIndex) // monthIndex is already correct (1-12)
                    transactionViewModel.loadTransactionsByMonth(yearMonth)
                }
            }
            // No filters (all time, all types)
            else -> {
                transactionViewModel.loadAllTransactions()
            }
        }
    }
    
    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedTransactionIds.size} selected")},
                    navigationIcon = {
                        IconButton(onClick = { transactionViewModel.toggleSelectionMode()}) {
                            Icon(Icons.Default.Close, "Exit selection mode")
                        }
                    },
                    actions = {
                        if (transactionsState is TransactionViewModel.TransactionsState.Success) {
                            val transactions = (transactionsState as TransactionViewModel.TransactionsState.Success).transactions
                            val allSelected = selectedTransactionIds.size == transactions.size

                            IconButton(
                                onClick = {
                                    if (allSelected) {
                                        transactionViewModel.clearSelection()
                                    } else {
                                        transactionViewModel.selectAllTransaction(transactions)
                                    }
                                }
                            ) {
                                Icon(
                                    if (selectedTransactionIds.size == transactions.size)
                                    Icons.Default.CheckBox
                                    else
                                        Icons.Default.CheckBoxOutlineBlank,
                                    "Select all"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Transactions") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (isSelectionMode) {
                //delete fab in selection mode
                FloatingActionButton(
                    onClick = { showDeleteDialog = true },
                    containerColor = MaterialTheme.colorScheme.scrim,
                    contentColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                }
            } else {
                //normal fabs
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    //add
                    FloatingActionButton(
                        onClick = onNavigateToAddTransaction,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                    }
                    //delete
                    FloatingActionButton(
                        onClick = {transactionViewModel.toggleSelectionMode()},
                        containerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        contentColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Transactions")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.primary
                )

                // Type filter chips
                FilterChip(
                    selected = selectedTypeFilter == "all",
                    onClick = { selectedTypeFilter = "all" },
                    label = { Text("All") }
                )

                FilterChip(
                    selected = selectedTypeFilter == "income",
                    onClick = { selectedTypeFilter = "income" },
                    label = { Text("Income") }
                )

                FilterChip(
                    selected = selectedTypeFilter == "expense",
                    onClick = { selectedTypeFilter = "expense" },
                    label = { Text("Expense") }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Month filter dropdown
                ExposedDropdownMenuBox(
                    expanded = isMonthFilterExpanded,
                    onExpandedChange = { isMonthFilterExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMonthFilter,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .widthIn(min = 220.dp)
                            .padding(vertical = 4.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.5.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.onSecondary,
                            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = Color.Transparent
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = isMonthFilterExpanded,
                        onDismissRequest = { isMonthFilterExpanded = false }
                    ) {
                        months.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(month, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = {
                                    selectedMonthFilter = month
                                    isMonthFilterExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transactions list
            when (transactionsState) {
                is TransactionViewModel.TransactionsState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                is TransactionViewModel.TransactionsState.Error -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = (transactionsState as TransactionViewModel.TransactionsState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is TransactionViewModel.TransactionsState.Success -> {
                    val transactions = (transactionsState as TransactionViewModel.TransactionsState.Success).transactions

                    if (transactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "No transactions found",
                                modifier = Modifier.align(Alignment.Center),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(transactions, key = {it.id}) { transaction ->
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
            onDismissRequest = {showDeleteDialog = false},
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete Transactions")
            },
            text = {
                Text(
                    "Are you sure you want to delete ${selectedTransactionIds.size} " +
                    "transaction${if (selectedTransactionIds.size > 1) "s" else ""}? " +
                    "This action cannot be undone"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        transactionViewModel.deleteSelectedTransactions()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {showDeleteDialog = false}) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
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
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = {onTransactionClick()}
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            // Transaction type indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (transaction.type == "income") Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else Color(0xFFF44336).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.type == "income") Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = transaction.type,
                    tint = if (transaction.type == "income") Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (transaction.description.isNotEmpty()) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                transaction.date.let { date ->
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Text(
                        text = dateFormat.format(date.toDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
            Text(
                text = formatter.format(transaction.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == "income") Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}