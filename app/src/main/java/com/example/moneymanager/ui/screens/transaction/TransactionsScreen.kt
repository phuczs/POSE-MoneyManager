package com.example.moneymanager.ui.screens.transaction

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.moneymanager.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.domain.model.Category
import com.example.moneymanager.domain.model.RecurringFrequency
import com.example.moneymanager.domain.model.RecurringTransaction
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.ui.components.SyncStatusIndicator
import com.example.moneymanager.ui.viewmodel.TransactionViewModel
import com.example.moneymanager.ui.viewmodel.AuthViewModel
import com.example.moneymanager.ui.viewmodel.CategoryViewModel
import com.example.moneymanager.ui.viewmodel.RecurringUiState
import com.example.moneymanager.ui.viewmodel.RecurringViewModel
import com.example.moneymanager.util.toCurrencyString
import com.example.moneymanager.util.toDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onTransactionClick: (String) -> Unit,
    initialTab: String = "transactions",
    authViewModel: AuthViewModel = hiltViewModel(),
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    recurringViewModel: RecurringViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var selectedMainTab by remember(initialTab) { mutableStateOf(initialTab) }
    val transactionsState by transactionViewModel.transactionsState.collectAsState()
    val syncState by transactionViewModel.syncState.collectAsState()
    val categoriesState by categoryViewModel.categoriesState.collectAsState()
    val isSelectionMode by transactionViewModel.isSelectionMode.collectAsState()
    val selectedTransactionIds by transactionViewModel.selectedTransactionIds.collectAsState()
    val searchQuery by transactionViewModel.searchQuery.collectAsState()
    val searchFilterState by transactionViewModel.searchFilterState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Recurring state
    val recurringUiState by recurringViewModel.uiState.collectAsStateWithLifecycle()
    var showAddEditRecurringSheet by remember { mutableStateOf(false) }
    var recurringToEdit by remember { mutableStateOf<RecurringTransaction?>(null) }
    var recurringToDelete by remember { mutableStateOf<RecurringTransaction?>(null) }
    var recurringTypeFilter by remember { mutableStateOf("all") }

    // Paging 3 — collect the paged stream
    val pagedTransactions: LazyPagingItems<Transaction> = transactionViewModel.pagedTransactions.collectAsLazyPagingItems()

    val allCategories = (categoriesState as? CategoryViewModel.CategoriesState.Success)?.categories ?: emptyList()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Filter states
    var selectedTypeFilter by remember { mutableStateOf("all") } // all, income, expense
    var isMonthFilterExpanded by remember { mutableStateOf(false) }
    val currentLanguage by authViewModel.currentLanguage.collectAsState(initial = "vi")
    val currencyCode = if (currentLanguage == "vi") "VND" else "USD"

    val months = listOf(
        stringResource(R.string.all_time),
        stringResource(R.string.january), stringResource(R.string.february), stringResource(R.string.march), stringResource(R.string.april), stringResource(R.string.may), stringResource(R.string.june),
        stringResource(R.string.july), stringResource(R.string.august), stringResource(R.string.september), stringResource(R.string.october), stringResource(R.string.november), stringResource(R.string.december)
    )
    var selectedMonthFilter by remember { mutableStateOf(months[0]) }

    // Load initial data
    LaunchedEffect(Unit) {
        transactionViewModel.loadAllTransactions()
    }

    // Apply Paging 3 filter whenever type or month selection changes
    LaunchedEffect(selectedTypeFilter, selectedMonthFilter) {
        val monthIndex = months.indexOf(selectedMonthFilter).takeIf { it > 0 }
        val currentYear = if (monthIndex != null) java.time.Year.now().value else null
        transactionViewModel.setPagedFilter(
            type = if (selectedTypeFilter != "all") selectedTypeFilter else null,
            month = monthIndex,
            year = currentYear
        )
        // Also keep the old search-compatible cache in sync
        when {
            selectedTypeFilter != "all" && selectedMonthFilter != months[0] -> {
                if (monthIndex != null) {
                    transactionViewModel.loadTransactionsByTypeAndMonth(selectedTypeFilter,
                        java.time.YearMonth.of(currentYear!!, monthIndex))
                }
            }
            selectedTypeFilter != "all" -> transactionViewModel.loadTransactionsByType(selectedTypeFilter)
            selectedMonthFilter != months[0] -> {
                if (monthIndex != null) {
                    transactionViewModel.loadTransactionsByMonth(
                        java.time.YearMonth.of(java.time.Year.now().value, monthIndex))
                }
            }
            else -> transactionViewModel.loadAllTransactions()
        }
    }

    Scaffold(
        topBar = {
            when {
                selectedMainTab == "transactions" && isSelectionMode -> {
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
                selectedMainTab == "transactions" && isSearchActive -> {
                    SearchTopAppBar(
                        query = searchQuery,
                        onQueryChange = { transactionViewModel.onSearchQueryChanged(it) },
                        onClose = {
                            isSearchActive = false
                            transactionViewModel.onSearchQueryChanged("") // Clear search when closing
                        },
                        isFilterActive = searchFilterState.isActive,
                        onFilterClick = { showFilterSheet = true }
                    )
                }
                selectedMainTab == "recurring" -> {
                    TopAppBar(
                        title = { Text(stringResource(R.string.recurring_transactions), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                recurringViewModel.processDueItems()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (currentLanguage == "vi") "Đã kiểm tra và xử lý các giao dịch định kỳ đến hạn"
                                        else "Checked and processed due recurring transactions"
                                    )
                                }
                            }) {
                                Icon(Icons.Default.Sync, contentDescription = "Process Due Items")
                            }
                            IconButton(onClick = {
                                recurringToEdit = null
                                showAddEditRecurringSheet = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_recurring))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White
                        )
                    )
                }
                else -> {
                    TopAppBar(
                        title = { Text(stringResource(R.string.transactions), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            SyncStatusIndicator(
                                syncState = syncState,
                                onTriggerSync = { transactionViewModel.triggerSync() }
                            )
                            IconButton(onClick = { showExportSheet = true }) {
                                Icon(Icons.Default.IosShare, contentDescription = stringResource(R.string.export_data))
                            }
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { showFilterSheet = true }) {
                                BadgedBox(
                                    badge = {
                                        if (searchFilterState.isActive) {
                                            Badge { Text("!") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                                }
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
            if (selectedMainTab == "transactions") {
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
                            Icon(Icons.Default.Checklist, contentDescription = "Select Transactions")
                        }
                    }
                }
            } else {
                FloatingActionButton(
                    onClick = {
                        recurringToEdit = null
                        showAddEditRecurringSheet = true
                    },
                    containerColor = MediumGreen,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_recurring))
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
            // Segmented Tab Switcher (Transactions vs Recurring)
            if (!isSearchActive && !searchFilterState.isActive && !isSelectionMode) {
                SegmentedTabSelector(
                    selectedTab = selectedMainTab,
                    onTabSelected = { selectedMainTab = it }
                )
            }

            if (selectedMainTab == "transactions") {
                // Filters (Hide basic filters if search or advanced filter is active)
                if (!isSearchActive && !searchFilterState.isActive) {
                    TransactionFilters(
                        selectedType = selectedTypeFilter,
                        onTypeSelected = { selectedTypeFilter = it },
                        selectedMonth = selectedMonthFilter,
                        onMonthSelected = { selectedMonthFilter = it },
                        months = months
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (searchFilterState.isActive) {
                    // Active filter banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.filter_transactions) + " (Active)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { transactionViewModel.clearAdvancedFilter() }) {
                            Text(stringResource(R.string.filter_reset), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Paged transactions list — Room-backed, offline-first
                when {
                    isSearchActive || searchFilterState.isActive -> {
                        // Search & advanced filter query results
                        when (transactionsState) {
                            is TransactionViewModel.TransactionsState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
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
                                    EmptyTransactionsPlaceholder(isSearchActive = true)
                                } else {
                                    PagedTransactionList(
                                        transactions = transactions,
                                        allCategories = allCategories,
                                        isSelectionMode = isSelectionMode,
                                        selectedTransactionIds = selectedTransactionIds,
                                        currencyCode = currencyCode,
                                        onTransactionClick = onTransactionClick,
                                        transactionViewModel = transactionViewModel
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        PagedTransactionListWithPaging(
                            pagedTransactions = pagedTransactions,
                            allCategories = allCategories,
                            isSelectionMode = isSelectionMode,
                            selectedTransactionIds = selectedTransactionIds,
                            currencyCode = currencyCode,
                            onTransactionClick = onTransactionClick,
                            transactionViewModel = transactionViewModel
                        )
                    }
                }
            } else {
                // RECURRING TRANSACTIONS CONTENT
                RecurringTransactionsTabContent(
                    uiState = recurringUiState,
                    allCategories = allCategories,
                    currencyCode = currencyCode,
                    selectedTypeFilter = recurringTypeFilter,
                    onTypeFilterSelected = { recurringTypeFilter = it },
                    onToggleActive = { id, active -> recurringViewModel.toggleActive(id, active) },
                    onEdit = { item ->
                        recurringToEdit = item
                        showAddEditRecurringSheet = true
                    },
                    onDelete = { item ->
                        recurringToDelete = item
                    },
                    onAddNew = {
                        recurringToEdit = null
                        showAddEditRecurringSheet = true
                    }
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.delete_transactions)) },
            text = { Text(stringResource(R.string.delete_confirmation_format, selectedTransactionIds.size)) },
            confirmButton = {
                Button(
                    onClick = {
                        transactionViewModel.deleteSelectedTransactions()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showFilterSheet) {
        AdvancedFilterBottomSheet(
            filterState = searchFilterState,
            categories = allCategories,
            currencyCode = currencyCode,
            onApply = { type, category, min, max ->
                transactionViewModel.applyAdvancedFilter(
                    type = type,
                    category = category,
                    minAmount = min,
                    maxAmount = max
                )
            },
            onReset = {
                transactionViewModel.clearAdvancedFilter()
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showExportSheet) {
        com.example.moneymanager.ui.components.ExportOptionsBottomSheet(
            onDismiss = { showExportSheet = false },
            onExport = { format, range ->
                showExportSheet = false
                transactionViewModel.exportTransactions(context, format, range) { uri, mimeType ->
                    android.widget.Toast.makeText(
                        context,
                        "File saved to Downloads folder!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(android.content.Intent.createChooser(intent, "Share Export File"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }

    if (recurringToDelete != null) {
        val item = recurringToDelete!!
        AlertDialog(
            onDismissRequest = { recurringToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_recurring_confirm) + "\n\n\"${item.title}\"") },
            confirmButton = {
                Button(
                    onClick = {
                        recurringViewModel.deleteItem(item.id)
                        recurringToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (currentLanguage == "vi") "Đã xóa giao dịch định kỳ"
                                else "Deleted recurring transaction"
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { recurringToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddEditRecurringSheet) {
        AddEditRecurringBottomSheet(
            initialItem = recurringToEdit,
            allCategories = allCategories,
            currencyCode = currencyCode,
            onDismiss = {
                showAddEditRecurringSheet = false
                recurringToEdit = null
            },
            onSave = { id, title, amount, type, category, frequency, startDate, description ->
                recurringViewModel.saveRecurringTransaction(
                    id = id,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    frequency = frequency,
                    startDate = startDate,
                    nextExecutionDate = recurringToEdit?.nextExecutionDate,
                    isActive = recurringToEdit?.isActive ?: true,
                    description = description
                )
                showAddEditRecurringSheet = false
                recurringToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (currentLanguage == "vi") "Đã lưu giao dịch định kỳ thành công"
                        else "Recurring transaction saved successfully"
                    )
                }
            }
        )
    }
}

// ─── Paging 3 List Composables ────────────────────────────────────────────────

@Composable
private fun EmptyTransactionsPlaceholder(isSearchActive: Boolean) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isSearchActive) Icons.Default.SearchOff else Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearchActive) stringResource(R.string.no_results_found) else stringResource(R.string.no_transactions),
                color = TextGray
            )
        }
    }
}

/** Renders a grouped + divided list for the search-active case (uses in-memory list). */
@Composable
private fun PagedTransactionList(
    transactions: List<Transaction>,
    allCategories: List<com.example.moneymanager.data.model.Category>,
    isSelectionMode: Boolean,
    selectedTransactionIds: Set<String>,
    currencyCode: String,
    onTransactionClick: (String) -> Unit,
    transactionViewModel: TransactionViewModel
) {
    val grouped = remember(transactions) {
        transactions.groupBy {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it.date.toDate())
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
    ) {
        grouped.forEach { (dateStr, txList) ->
            item(key = "header_$dateStr") {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp)
                )
            }
            itemsIndexed(txList, key = { _, tx -> tx.id }) { index, transaction ->
                TransactionListItem(
                    transaction = transaction,
                    categoryInfo = allCategories.find { it.name.equals(transaction.category, ignoreCase = true) },
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedTransactionIds.contains(transaction.id),
                    onTransactionClick = {
                        if (isSelectionMode) transactionViewModel.toggleTransactionSelection(transaction.id)
                        else onTransactionClick(transaction.id)
                    },
                    onTransactionLongClick = {
                        if (!isSelectionMode) {
                            transactionViewModel.toggleSelectionMode()
                            transactionViewModel.toggleTransactionSelection(transaction.id)
                        }
                    },
                    currencyCode = currencyCode
                )
                if (index < txList.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/** Renders a Paging-3-backed grouped list for the normal (non-search) case. */
@Composable
private fun PagedTransactionListWithPaging(
    pagedTransactions: LazyPagingItems<Transaction>,
    allCategories: List<com.example.moneymanager.data.model.Category>,
    isSelectionMode: Boolean,
    selectedTransactionIds: Set<String>,
    currencyCode: String,
    onTransactionClick: (String) -> Unit,
    transactionViewModel: TransactionViewModel
) {
    when (pagedTransactions.loadState.refresh) {
        is LoadState.Loading -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        is LoadState.Error -> {
            val error = (pagedTransactions.loadState.refresh as LoadState.Error).error
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(R.string.failed_to_load_format, error.localizedMessage), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { pagedTransactions.retry() }) { Text(stringResource(R.string.retry)) }
                }
            }
        }
        else -> {
            if (pagedTransactions.itemCount == 0) {
                EmptyTransactionsPlaceholder(isSearchActive = false)
                return
            }

            // Pre-compute which indices need a date header (based on currently loaded items)
            val dateHeaderIndices = remember(pagedTransactions.itemCount) {
                val headerSet = mutableSetOf<Int>()
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                var lastDate = ""
                for (i in 0 until pagedTransactions.itemCount) {
                    val tx = pagedTransactions.peek(i)
                    if (tx != null) {
                        val dateStr = sdf.format(tx.date.toDate())
                        if (dateStr != lastDate) {
                            headerSet.add(i)
                            lastDate = dateStr
                        }
                    }
                }
                headerSet
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
            ) {
                items(
                    count = pagedTransactions.itemCount,
                    key = { index -> pagedTransactions.peek(index)?.id ?: "placeholder_$index" }
                ) { index ->
                    val transaction = pagedTransactions[index] ?: return@items

                    // Date header
                    if (index in dateHeaderIndices) {
                        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(transaction.date.toDate())
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextGray,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp)
                        )
                    }

                    TransactionListItem(
                        transaction = transaction,
                        categoryInfo = allCategories.find { it.name.equals(transaction.category, ignoreCase = true) },
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedTransactionIds.contains(transaction.id),
                        onTransactionClick = {
                            if (isSelectionMode) transactionViewModel.toggleTransactionSelection(transaction.id)
                            else onTransactionClick(transaction.id)
                        },
                        onTransactionLongClick = {
                            if (!isSelectionMode) {
                                transactionViewModel.toggleSelectionMode()
                                transactionViewModel.toggleTransactionSelection(transaction.id)
                            }
                        },
                        currencyCode = currencyCode
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                // Page-load footer spinner
                if (pagedTransactions.loadState.append is LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    isFilterActive: Boolean = false,
    onFilterClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_transactions)) },
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
            IconButton(onClick = onFilterClick) {
                BadgedBox(
                    badge = {
                        if (isFilterActive) {
                            Badge { Text("!") }
                        }
                    }
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }
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
        title = { Text(stringResource(R.string.selected_count_format, selectedCount)) },
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val options = listOf("all" to stringResource(R.string.all), "income" to stringResource(R.string.income), "expense" to stringResource(R.string.expense))
            options.forEach { (id, label) ->
                val isSelected = selectedType == id
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable { onTypeSelected(id) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ExposedDropdownMenuBox(
                expanded = isMonthExpanded,
                onExpandedChange = { isMonthExpanded = it }
            ) {
                Row(
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListItem(
    transaction: Transaction,
    categoryInfo: com.example.moneymanager.data.model.Category?,
    onTransactionClick: () -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onTransactionLongClick: () -> Unit,
    currencyCode: String
) {
    val customColorHex = categoryInfo?.colorHex ?: ""
    val customColor = if (customColorHex.isNotBlank()) {
        try { Color(android.graphics.Color.parseColor(customColorHex)) } catch (e: Exception) { null }
    } else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .combinedClickable(
                onClick = onTransactionClick,
                onLongClick = onTransactionLongClick
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
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
                .clip(CircleShape)
                .background(
                    customColor?.copy(alpha = 0.15f) ?: if (transaction.type == "income") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (categoryInfo?.iconName != null && categoryInfo.iconName.isNotBlank()) {
                Text(text = categoryInfo.iconName, fontSize = 20.sp)
            } else {
                Icon(
                    imageVector = if (transaction.type == "income") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = customColor ?: if (transaction.type == "income") Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (transaction.description.isNotEmpty()) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = (if (transaction.type == "income") "+" else "-") +
                    transaction.amount.toCurrencyString(currencyCode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (transaction.type == "income") Color(0xFF4CAF50) else Color(0xFFE53935)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilterBottomSheet(
    filterState: com.example.moneymanager.ui.viewmodel.SearchFilterState,
    categories: List<com.example.moneymanager.domain.model.Category>,
    currencyCode: String,
    onApply: (type: String, category: String, minAmount: Double?, maxAmount: Double?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(filterState.type) }
    var selectedCategory by remember { mutableStateOf(filterState.category) }
    var minAmountText by remember {
        mutableStateOf(filterState.minAmount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }
    var maxAmountText by remember {
        mutableStateOf(filterState.maxAmount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_transactions),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    selectedType = ""
                    selectedCategory = ""
                    minAmountText = ""
                    maxAmountText = ""
                    onReset()
                    onDismiss()
                }) {
                    Text(stringResource(R.string.filter_reset), color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Type Filter
            Text(
                text = stringResource(R.string.category),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType.isEmpty(),
                    onClick = { selectedType = "" },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
                FilterChip(
                    selected = selectedType == "expense",
                    onClick = { selectedType = "expense" },
                    label = { Text(stringResource(R.string.expense)) }
                )
                FilterChip(
                    selected = selectedType == "income",
                    onClick = { selectedType = "income" },
                    label = { Text(stringResource(R.string.income)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Range Filter
            Text(
                text = stringResource(R.string.filter_amount_range) + " ($currencyCode)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minAmountText,
                    onValueChange = { minAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.filter_min_amount)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = maxAmountText,
                    onValueChange = { maxAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.filter_max_amount)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories Filter
            if (categories.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.category),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory.isEmpty(),
                            onClick = { selectedCategory = "" },
                            label = { Text(stringResource(R.string.filter_all)) }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = {
                                selectedCategory = if (selectedCategory == cat.name) "" else cat.name
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply Button
            Button(
                onClick = {
                    val min = minAmountText.toDoubleOrNull()
                    val max = maxAmountText.toDoubleOrNull()
                    onApply(selectedType, selectedCategory, min, max)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MediumGreen)
            ) {
                Text(stringResource(R.string.filter_apply), fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ─── Recurring Transactions Composables ──────────────────────────────────────────

@Composable
fun SegmentedTabSelector(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF1F3F4)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 1: Regular Transactions
            val isTransactions = selectedTab == "transactions"
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTabSelected("transactions") },
                color = if (isTransactions) Color.White else Color.Transparent,
                shadowElevation = if (isTransactions) 2.dp else 0.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = if (isTransactions) MediumGreen else TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tab_transactions),
                        fontWeight = if (isTransactions) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTransactions) MediumGreen else TextGray,
                        fontSize = 14.sp
                    )
                }
            }

            // Tab 2: Recurring Transactions
            val isRecurring = selectedTab == "recurring"
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTabSelected("recurring") },
                color = if (isRecurring) Color.White else Color.Transparent,
                shadowElevation = if (isRecurring) 2.dp else 0.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = if (isRecurring) MediumGreen else TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tab_recurring),
                        fontWeight = if (isRecurring) FontWeight.Bold else FontWeight.Medium,
                        color = if (isRecurring) MediumGreen else TextGray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RecurringTransactionsTabContent(
    uiState: RecurringUiState,
    allCategories: List<Category>,
    currencyCode: String,
    selectedTypeFilter: String,
    onTypeFilterSelected: (String) -> Unit,
    onToggleActive: (id: String, active: Boolean) -> Unit,
    onEdit: (RecurringTransaction) -> Unit,
    onDelete: (RecurringTransaction) -> Unit,
    onAddNew: () -> Unit
) {
    when (uiState) {
        is RecurringUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MediumGreen)
            }
        }
        is RecurringUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is RecurringUiState.Success -> {
            val items = uiState.items
            if (items.isEmpty()) {
                EmptyRecurringView(onAddNew = onAddNew)
            } else {
                val filteredItems = remember(items, selectedTypeFilter) {
                    when (selectedTypeFilter) {
                        "expense" -> items.filter { it.type.equals("expense", ignoreCase = true) }
                        "income" -> items.filter { it.type.equals("income", ignoreCase = true) }
                        else -> items
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
                ) {
                    // Summary card at the top
                    item {
                        RecurringCommitmentCard(
                            items = items,
                            currencyCode = currencyCode
                        )
                    }

                    // Filter chips: All, Expense, Income
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filterOptions = listOf(
                                "all" to stringResource(R.string.filter_all),
                                "expense" to stringResource(R.string.expense),
                                "income" to stringResource(R.string.income)
                            )
                            filterOptions.forEach { (key, label) ->
                                val isSelected = selectedTypeFilter == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onTypeFilterSelected(key) },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MediumGreen.copy(alpha = 0.15f),
                                        selectedLabelColor = MediumGreen
                                    )
                                )
                            }
                        }
                    }

                    if (filteredItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_recurring_transactions),
                                    color = TextGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        items(filteredItems, key = { it.id }) { item ->
                            val categoryInfo = allCategories.find { it.name.equals(item.category, ignoreCase = true) }
                            RecurringItemCard(
                                item = item,
                                categoryInfo = categoryInfo,
                                currencyCode = currencyCode,
                                onToggleActive = { active -> onToggleActive(item.id, active) },
                                onEdit = { onEdit(item) },
                                onDelete = { onDelete(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringCommitmentCard(
    items: List<RecurringTransaction>,
    currencyCode: String
) {
    val activeItems = items.filter { it.isActive }
    val monthlyExpense = remember(activeItems) {
        activeItems.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { item ->
            when (item.frequency) {
                RecurringFrequency.DAILY -> item.amount * 30.0
                RecurringFrequency.WEEKLY -> item.amount * 4.33
                RecurringFrequency.MONTHLY -> item.amount
                RecurringFrequency.YEARLY -> item.amount / 12.0
            }
        }
    }
    val monthlyIncome = remember(activeItems) {
        activeItems.filter { it.type.equals("income", ignoreCase = true) }.sumOf { item ->
            when (item.frequency) {
                RecurringFrequency.DAILY -> item.amount * 30.0
                RecurringFrequency.WEEKLY -> item.amount * 4.33
                RecurringFrequency.MONTHLY -> item.amount
                RecurringFrequency.YEARLY -> item.amount / 12.0
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFEFEFEF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MediumGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MediumGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.recurring_summary),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MediumGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = stringResource(R.string.active_count_format, activeItems.size),
                        color = MediumGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Monthly Expense
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.recurring_expense_monthly),
                        fontSize = 12.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (monthlyExpense > 0) "- " + monthlyExpense.toCurrencyString(currencyCode) else 0.0.toCurrencyString(currencyCode),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    )
                }

                // Monthly Income
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.recurring_income_monthly),
                        fontSize = 12.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (monthlyIncome > 0) "+ " + monthlyIncome.toCurrencyString(currencyCode) else 0.0.toCurrencyString(currencyCode),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
fun RecurringItemCard(
    item: RecurringTransaction,
    categoryInfo: Category?,
    currencyCode: String,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isExpense = item.type.equals("expense", ignoreCase = true)
    val customColorHex = categoryInfo?.colorHex ?: ""
    val customColor = if (customColorHex.isNotBlank()) {
        try { Color(android.graphics.Color.parseColor(customColorHex)) } catch (e: Exception) { null }
    } else null

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val nextDateFormatted = remember(item.nextExecutionDate) {
        dateFormat.format(Date(item.nextExecutionDate))
    }

    val frequencyText = when (item.frequency) {
        RecurringFrequency.DAILY -> stringResource(R.string.frequency_daily)
        RecurringFrequency.WEEKLY -> stringResource(R.string.frequency_weekly)
        RecurringFrequency.MONTHLY -> stringResource(R.string.frequency_monthly)
        RecurringFrequency.YEARLY -> stringResource(R.string.frequency_yearly)
    }

    val freqColor = when (item.frequency) {
        RecurringFrequency.DAILY -> Color(0xFFE65100) to Color(0xFFFFF3E0)
        RecurringFrequency.WEEKLY -> Color(0xFF1565C0) to Color(0xFFE3F2FD)
        RecurringFrequency.MONTHLY -> Color(0xFF6A1B9A) to Color(0xFFF3E5F5)
        RecurringFrequency.YEARLY -> Color(0xFF2E7D32) to Color(0xFFE8F5E9)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, if (item.isActive) Color(0xFFEEEEEE) else Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon with emoji
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            customColor?.copy(alpha = 0.15f)
                                ?: if (isExpense) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (categoryInfo?.iconName != null && categoryInfo.iconName.isNotBlank()) {
                        Text(text = categoryInfo.iconName, fontSize = 20.sp)
                    } else {
                        Icon(
                            imageVector = if (isExpense) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = customColor ?: if (isExpense) Color(0xFFE53935) else Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isActive) TextPrimary else TextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.category.ifBlank { stringResource(R.string.all) },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                        Text(text = "•", color = TextGray, fontSize = 11.sp)
                        // Frequency badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = freqColor.second
                        ) {
                            Text(
                                text = frequencyText,
                                color = freqColor.first,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Amount
                Text(
                    text = (if (isExpense) "-" else "+") + item.amount.toCurrencyString(currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (!item.isActive) TextGray else if (isExpense) Color(0xFFE53935) else Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF8F9FA), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: Next due date + Actions (Edit, Delete, Switch)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = stringResource(R.string.next_due_format, nextDateFormatted),
                        fontSize = 12.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Switch(
                        checked = item.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MediumGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCFD8DC)
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyRecurringView(onAddNew: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MediumGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = MediumGreen,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.recurring_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.recurring_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddNew,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MediumGreen),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add_first_recurring),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringBottomSheet(
    initialItem: RecurringTransaction?,
    allCategories: List<Category>,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (
        id: String,
        title: String,
        amount: Double,
        type: String,
        category: String,
        frequency: RecurringFrequency,
        startDate: Long,
        description: String
    ) -> Unit
) {
    var title by remember(initialItem) { mutableStateOf(initialItem?.title ?: "") }
    var amountText by remember(initialItem) {
        mutableStateOf(
            initialItem?.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var type by remember(initialItem) { mutableStateOf(initialItem?.type ?: "expense") }
    val isExpense = type.equals("expense", ignoreCase = true)

    val availableCategories = remember(allCategories, type) {
        allCategories.filter { it.type.equals(type, ignoreCase = true) }
    }

    var selectedCategoryName by remember(initialItem, availableCategories) {
        mutableStateOf(
            initialItem?.category ?: availableCategories.firstOrNull()?.name ?: ""
        )
    }

    var selectedFrequency by remember(initialItem) {
        mutableStateOf(initialItem?.frequency ?: RecurringFrequency.MONTHLY)
    }

    var description by remember(initialItem) { mutableStateOf(initialItem?.description ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialItem != null) stringResource(R.string.edit_recurring)
                    else stringResource(R.string.add_recurring),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Income / Expense Switcher
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F3F4)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    // Expense button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                type = "expense"
                                val newFirst = allCategories.firstOrNull { it.type.equals("expense", ignoreCase = true) }?.name ?: ""
                                if (allCategories.none { it.name.equals(selectedCategoryName, ignoreCase = true) && it.type.equals("expense", ignoreCase = true) }) {
                                    selectedCategoryName = newFirst
                                }
                            },
                        color = if (isExpense) Color.White else Color.Transparent,
                        shadowElevation = if (isExpense) 2.dp else 0.dp,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.expense),
                                fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                                color = if (isExpense) Color(0xFFE53935) else TextGray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Income button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                type = "income"
                                val newFirst = allCategories.firstOrNull { it.type.equals("income", ignoreCase = true) }?.name ?: ""
                                if (allCategories.none { it.name.equals(selectedCategoryName, ignoreCase = true) && it.type.equals("income", ignoreCase = true) }) {
                                    selectedCategoryName = newFirst
                                }
                            },
                        color = if (!isExpense) Color.White else Color.Transparent,
                        shadowElevation = if (!isExpense) 2.dp else 0.dp,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.income),
                                fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isExpense) Color(0xFF4CAF50) else TextGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount field
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text(stringResource(R.string.amount)) },
                suffix = { Text(currencyCode, fontWeight = FontWeight.Bold, color = MediumGreen) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediumGreen,
                    focusedLabelColor = MediumGreen
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.title)) },
                placeholder = { Text("VD: Netflix, Tiền thuê nhà, Tiền mạng...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediumGreen,
                    focusedLabelColor = MediumGreen
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category picker
            Text(
                text = stringResource(R.string.category),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableCategories, key = { it.id }) { cat ->
                    val isSelected = cat.name.equals(selectedCategoryName, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MediumGreen.copy(alpha = 0.15f) else Color(0xFFF5F7F8),
                        border = if (isSelected) BorderStroke(1.5.dp, MediumGreen) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategoryName = cat.name }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            if (!cat.iconName.isNullOrBlank()) {
                                Text(text = cat.iconName, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = cat.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MediumGreen else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frequency picker
            Text(
                text = stringResource(R.string.frequency),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RecurringFrequency.entries.forEach { freq ->
                    val isSelected = selectedFrequency == freq
                    val label = when (freq) {
                        RecurringFrequency.DAILY -> stringResource(R.string.frequency_daily)
                        RecurringFrequency.WEEKLY -> stringResource(R.string.frequency_weekly)
                        RecurringFrequency.MONTHLY -> stringResource(R.string.frequency_monthly)
                        RecurringFrequency.YEARLY -> stringResource(R.string.frequency_yearly)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MediumGreen else Color(0xFFF5F7F8),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFrequency = freq }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Description / Note field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.notes)) },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediumGreen,
                    focusedLabelColor = MediumGreen
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            val amountValue = amountText.toDoubleOrNull() ?: 0.0
            val isValid = title.isNotBlank() && amountValue > 0

            Button(
                onClick = {
                    if (isValid) {
                        onSave(
                            initialItem?.id ?: "",
                            title,
                            amountValue,
                            type,
                            selectedCategoryName,
                            selectedFrequency,
                            initialItem?.startDate ?: System.currentTimeMillis(),
                            description
                        )
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediumGreen,
                    disabledContainerColor = Color(0xFFCFD8DC)
                )
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}