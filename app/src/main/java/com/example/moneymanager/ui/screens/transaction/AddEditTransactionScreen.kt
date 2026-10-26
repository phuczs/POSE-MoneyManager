package com.example.moneymanager.ui.screens.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import com.example.moneymanager.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.model.Category
import com.example.moneymanager.data.model.Transaction
import com.example.moneymanager.domain.model.RecurringFrequency
import com.example.moneymanager.domain.usecase.recurring.calculateNextDate
import com.example.moneymanager.ui.screens.category.CategorySelector
import com.example.moneymanager.ui.theme.BackgroundGray
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray // Explicit import
import com.example.moneymanager.ui.viewmodel.CategoryViewModel
import com.example.moneymanager.ui.viewmodel.RecurringViewModel
import com.example.moneymanager.ui.viewmodel.TransactionViewModel
import com.example.moneymanager.util.toDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.moneymanager.ui.components.keyboard.MoneyKeypad
import com.example.moneymanager.ui.components.keyboard.MoneyKeypadHelper
import com.example.moneymanager.ui.viewmodel.AuthViewModel
import com.example.moneymanager.util.toCurrencyString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: String? = null,
    onNavigateBack: () -> Unit,
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    recurringViewModel: RecurringViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Currency & Language
    val currentLanguage by authViewModel.currentLanguage.collectAsState(initial = "vi")
    val currencyCode = if (currentLanguage == "vi") "VND" else "USD"

    // Transaction state
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("expense") } // Default to expense
    var selectedDate by remember { mutableStateOf(Calendar.getInstance().time) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var isKeypadVisible by remember { mutableStateOf(true) }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }

    // Data Observables
    val categoriesState by categoryViewModel.categoriesState.collectAsState()
    val transaction by transactionViewModel.currentTransaction.collectAsState()
    val transactionsState by transactionViewModel.transactionsState.collectAsState()
    val aiCategoryPrediction by transactionViewModel.aiCategoryPrediction.collectAsState()

    // Date Helpers
    val calendar = Calendar.getInstance().apply { time = selectedDate }
    val month = calendar.get(Calendar.MONTH) + 1
    val year = calendar.get(Calendar.YEAR)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.time
    )
    var showDatePicker by remember { mutableStateOf(false) }

    // Dynamic Colors based on Type
    // Income = Emerald (MediumGreen), Expense = Ruby (Red)
    val primaryColor by animateColorAsState(
        targetValue = if (transactionType == "income") MediumGreen else Color(0xFFD32F2F),
        animationSpec = tween(300), label = "primaryColor"
    )

    // --- Logic Handlers ---

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Date(millis)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.ok), color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel), color = TextGray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LaunchedEffect(transactionType) {
        categoryViewModel.loadCategoriesByType(transactionType)
        if (transactionId == null) {
            selectedCategory = null
        }
    }

    LaunchedEffect(categoriesState) {
        if (categoriesState is CategoryViewModel.CategoriesState.Success) {
            val categories = (categoriesState as CategoryViewModel.CategoriesState.Success).categories
            if (categories.isEmpty()) {
                categoryViewModel.createDefaultCategories()
            }
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            transactionViewModel.getTransactionById(transactionId)
        }
    }

    LaunchedEffect(transaction) {
        transaction?.let {
            amount = it.amount.toString()
            description = it.description
            transactionType = it.type
            selectedCategory = categoriesState.let { state ->
                if (state is CategoryViewModel.CategoriesState.Success) {
                    state.categories.find { category -> category.name == it.category }
                } else null
            }
            selectedDate = it.date.toDate()
        }
    }

    // --- UI Content ---

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (transactionId == null) stringResource(R.string.add_transaction) else stringResource(R.string.edit_transaction),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundGray
                )
            )
        },
        bottomBar = {
            if (isKeypadVisible) {
                MoneyKeypad(
                    currentAmount = amount,
                    onAmountChanged = { newAmount ->
                        amount = newAmount
                        if (description.isNotBlank()) {
                            val amountVal = MoneyKeypadHelper.evaluateExpression(newAmount)
                            transactionViewModel.predictCategory(description, amountVal)
                        }
                    },
                    onDonePressed = {
                        val eval = MoneyKeypadHelper.evaluateExpression(amount)
                        amount = if (eval > 0) {
                            if (eval % 1.0 == 0.0) eval.toLong().toString() else eval.toString()
                        } else ""
                        isKeypadVisible = false
                    },
                    primaryColor = primaryColor,
                    currencyCode = currencyCode
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Type Switcher (Emerald vs Ruby)
            TransactionTypeSwitcher(
                currentType = transactionType,
                onTypeSelected = { transactionType = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // 2. Amount Input (Big & Central Interactive Display)
                val evaluatedAmount = remember(amount) { MoneyKeypadHelper.evaluateExpression(amount) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isKeypadVisible) primaryColor.copy(alpha = 0.08f) else Color.White
                        )
                        .clickable {
                            isKeypadVisible = true
                            keyboardController?.hide()
                        }
                        .padding(vertical = 16.dp, horizontal = 20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.enter_amount),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val displayValue = if (amount.isBlank()) {
                        "0"
                    } else if (amount.contains('+') || amount.contains('-')) {
                        amount
                    } else {
                        evaluatedAmount.toCurrencyString(currencyCode)
                    }

                    Text(
                        text = displayValue,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )

                    if (amount.contains('+') || amount.contains('-')) {
                        Text(
                            text = "= ${evaluatedAmount.toCurrencyString(currencyCode)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 3. Category Selector & AI Suggestion
                Column {
                    CategorySelector(
                        selectedCategory = selectedCategory,
                        transactionType = transactionType,
                        onCategorySelected = { category ->
                            selectedCategory = category
                            transactionViewModel.clearCategoryPrediction()
                        }
                    )

                    // AI Suggestion Chip
                    if (aiCategoryPrediction != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(primaryColor.copy(alpha = 0.1f))
                                .clickable {
                                    if (categoriesState is CategoryViewModel.CategoriesState.Success) {
                                        val cats = (categoriesState as CategoryViewModel.CategoriesState.Success).categories
                                        selectedCategory = cats.find { it.name.equals(aiCategoryPrediction, ignoreCase = true) }
                                    }
                                    transactionViewModel.clearCategoryPrediction()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "✦",
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.ai_suggests),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                            Text(
                                text = aiCategoryPrediction ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(R.string.tap_to_apply),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                        }
                    }
                }

                // 4. Date Picker
                ClickableTextField(
                    value = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(selectedDate),
                    label = stringResource(R.string.date),
                    icon = Icons.Default.CalendarToday,
                    primaryColor = primaryColor,
                    onClick = {
                        isKeypadVisible = false
                        showDatePicker = true
                    }
                )

                // 5. Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        // Trigger AI prediction debounce
                        val amountVal = MoneyKeypadHelper.evaluateExpression(amount)
                        transactionViewModel.predictCategory(it, amountVal)
                    },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = primaryColor)
                    },
                    minLines = 2,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 6. Recurring Transaction Toggle (Only when creating new transaction)
                if (transactionId == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (isRecurring) primaryColor.copy(alpha = 0.5f) else Color(0xFFEEEEEE))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isRecurring) primaryColor.copy(alpha = 0.12f) else Color(0xFFF5F5F5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = null,
                                            tint = if (isRecurring) primaryColor else TextGray,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.repeat_transaction),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.repeat_transaction_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextGray
                                        )
                                    }
                                }
                                Switch(
                                    checked = isRecurring,
                                    onCheckedChange = { isRecurring = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = primaryColor
                                    )
                                )
                            }

                            AnimatedVisibility(
                                visible = isRecurring,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.frequency),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextGray
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
                                                color = if (isSelected) primaryColor else Color(0xFFF5F7F8),
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
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 7. Save Button
                Button(
                    onClick = {
                        val amountValue = MoneyKeypadHelper.evaluateExpression(amount)
                        if (amountValue > 0) {
                            val categoryName = selectedCategory?.name?.takeIf { it.isNotBlank() } ?: "Other"

                            val transactionObj = Transaction(
                                id = transactionId ?: "",
                                amount = amountValue,
                                type = transactionType,
                                category = categoryName,
                                description = description.trim(),
                                date = selectedDate.time, // epoch-ms Long (domain model)
                                month = month,
                                year = year
                            )

                            if (transactionId == null) {
                                transactionViewModel.addTransaction(transactionObj)
                                if (isRecurring) {
                                    val nextDate = calculateNextDate(selectedDate.time, selectedFrequency)
                                    recurringViewModel.saveRecurringTransaction(
                                        title = description.trim().ifBlank { categoryName },
                                        amount = amountValue,
                                        type = transactionType,
                                        category = categoryName,
                                        frequency = selectedFrequency,
                                        startDate = selectedDate.time,
                                        nextExecutionDate = nextDate,
                                        description = description.trim()
                                    )
                                }
                            } else {
                                transactionViewModel.updateTransaction(transactionObj)
                            }
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    ),
                    enabled = MoneyKeypadHelper.evaluateExpression(amount) > 0
                ) {
                    if (transactionsState is TransactionViewModel.TransactionsState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (transactionId == null) stringResource(R.string.save_transaction) else stringResource(R.string.update_transaction),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionTypeSwitcher(
    currentType: String,
    onTypeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Income Tab (Green)
        TypeTab(
            text = stringResource(R.string.income),
            isSelected = currentType == "income",
            selectedColor = MediumGreen, // Emerald
            modifier = Modifier.weight(1f),
            onClick = { onTypeSelected("income") }
        )

        // Expense Tab (Red)
        TypeTab(
            text = stringResource(R.string.expense),
            isSelected = currentType == "expense",
            selectedColor = Color(0xFFD32F2F), // Ruby Red
            modifier = Modifier.weight(1f),
            onClick = { onTypeSelected("expense") }
        )
    }
}

@Composable
fun TypeTab(
    text: String,
    isSelected: Boolean,
    selectedColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else Color.Transparent,
        animationSpec = tween(300), label = "bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextGray,
        animationSpec = tween(300), label = "text"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun ClickableTextField(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primaryColor: Color,
    onClick: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryColor,
            unfocusedBorderColor = Color.LightGray,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            disabledBorderColor = Color.LightGray,
            disabledTextColor = Color.Black,
            disabledLabelColor = Color.Gray,
            disabledLeadingIconColor = primaryColor
        ),
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = primaryColor)
        },
        enabled = false, // Disable typing, handle click on Modifier
        readOnly = true
    )
}