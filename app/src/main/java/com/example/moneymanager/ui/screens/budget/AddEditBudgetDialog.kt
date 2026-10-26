package com.example.moneymanager.ui.screens.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.R
import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.model.BudgetPeriod
import com.example.moneymanager.ui.components.keyboard.MoneyKeypad
import com.example.moneymanager.ui.components.keyboard.MoneyKeypadHelper
import com.example.moneymanager.ui.theme.BackgroundGray
import com.example.moneymanager.ui.theme.DarkGreen
import com.example.moneymanager.ui.theme.Error
import com.example.moneymanager.ui.theme.LightGreen
import com.example.moneymanager.ui.theme.LightRed
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary
import com.example.moneymanager.ui.viewmodel.AuthViewModel
import com.example.moneymanager.ui.viewmodel.CategoryViewModel
import com.example.moneymanager.util.toCurrencyString
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetDialog(
    budget: Budget?,
    onDismiss: () -> Unit,
    onConfirm: (Budget) -> Unit,
    onDelete: () -> Unit,
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val categoriesState by categoryViewModel.categoriesState.collectAsState()
    val currentLanguage by authViewModel.currentLanguage.collectAsState()
    val currencyCode = if (currentLanguage == "vi") "VND" else "USD"
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        categoryViewModel.loadCategoriesByType("expense")
    }

    var selectedCategory by remember { mutableStateOf(budget?.category ?: "") }
    var amount by remember { mutableStateOf(budget?.allocatedAmount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var isKeypadVisible by remember { mutableStateOf(budget == null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf(budget?.period ?: BudgetPeriod.MONTHLY) }
    var isRecurring by remember { mutableStateOf(budget?.isRecurring ?: false) }
    var rolloverEnabled by remember { mutableStateOf(budget?.rolloverEnabled ?: false) }
    var notes by remember { mutableStateOf(budget?.notes ?: "") }
    var customStartDate by remember { mutableStateOf(budget?.startDate ?: System.currentTimeMillis()) }
    var customEndDate by remember { mutableStateOf(budget?.endDate ?: System.currentTimeMillis()) }
    var pickingCustomEnd by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = budget?.startDate ?: System.currentTimeMillis()
    )

    // Entrance Animation trigger
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateIn = true
    }

    // Pulsing animation for sparkle header badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = animateIn,
            enter = scaleIn(
                initialScale = 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 3 }),
            exit = scaleOut(targetScale = 0.85f) + fadeOut(tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(28.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // --- HEADER BRANDING WITH GRADIENT & PULSING BADGE ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(DarkGreen, MediumGreen, LightGreen)
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Sparkling pulsing badge icon
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .scale(pulseScale)
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (budget == null) stringResource(R.string.add_budget) else stringResource(R.string.edit_budget),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp
                                        ),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = if (currentLanguage == "vi") "Thiết lập hạn mức chi tiêu thông minh" else "Set smart spending budget limit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Close Button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // --- FORM BODY CONTENT ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {

                        // 1. CATEGORY SELECTION
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MediumGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.category),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }

                            if (budget == null) {
                                // Expense Category Chip Carousel for Add mode
                                if (categoriesState is CategoryViewModel.CategoriesState.Success) {
                                    val categoryList = (categoriesState as CategoryViewModel.CategoriesState.Success).categories
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(categoryList) { category ->
                                            val isSelected = selectedCategory == category.name
                                            Surface(
                                                onClick = { selectedCategory = category.name },
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isSelected) MediumGreen else BackgroundGray,
                                                border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                                shadowElevation = if (isSelected) 4.dp else 0.dp
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                                ) {
                                                    Text(
                                                        text = getCategoryEmoji(category.name),
                                                        fontSize = 16.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = category.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        ),
                                                        color = if (isSelected) Color.White else TextPrimary
                                                    )
                                                    if (isSelected) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Dropdown fallback/alternative
                                ExposedDropdownMenuBox(
                                    expanded = isCategoryExpanded,
                                    onExpandedChange = { isCategoryExpanded = !isCategoryExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedCategory.ifEmpty { stringResource(R.string.action_category) },
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MediumGreen,
                                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isCategoryExpanded,
                                        onDismissRequest = { isCategoryExpanded = false }
                                    ) {
                                        if (categoriesState is CategoryViewModel.CategoriesState.Success) {
                                            (categoriesState as CategoryViewModel.CategoriesState.Success).categories.forEach { category ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(getCategoryEmoji(category.name), fontSize = 16.sp)
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(category.name)
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedCategory = category.name
                                                        isCategoryExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Locked Category display for Edit mode
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = BackgroundGray,
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Text(getCategoryEmoji(selectedCategory), fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = selectedCategory,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = TextGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. ALLOCATED AMOUNT INPUT & CUSTOM MONEY KEYPAD
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MediumGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.allocated_amount),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                }

                                // Keypad Toggle Badge
                                Surface(
                                    onClick = {
                                        isKeypadVisible = !isKeypadVisible
                                        if (isKeypadVisible) {
                                            keyboardController?.hide()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isKeypadVisible) MediumGreen else BackgroundGray,
                                    border = BorderStroke(1.dp, if (isKeypadVisible) MediumGreen else Color.LightGray.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isKeypadVisible) Icons.Default.KeyboardArrowUp else Icons.Default.Keyboard,
                                            contentDescription = "Toggle Keypad",
                                            tint = if (isKeypadVisible) Color.White else MediumGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isKeypadVisible) "Bàn phím" else "Mở bàn phím",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isKeypadVisible) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }

                            val evaluatedAmount = remember(amount) { MoneyKeypadHelper.evaluateExpression(amount) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        isKeypadVisible = true
                                        keyboardController?.hide()
                                    }
                            ) {
                                OutlinedTextField(
                                    value = amount,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    placeholder = { Text(if (currencyCode == "VND") "e.g. 5000000" else "e.g. 500") },
                                    trailingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = if (currencyCode == "VND") "₫" else "$",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MediumGreen
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    isKeypadVisible = !isKeypadVisible
                                                    if (isKeypadVisible) {
                                                        keyboardController?.hide()
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isKeypadVisible) Icons.Default.KeyboardArrowUp else Icons.Default.Keyboard,
                                                    contentDescription = "Keypad",
                                                    tint = MediumGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = TextPrimary,
                                        disabledBorderColor = if (isKeypadVisible) MediumGreen else Color.LightGray.copy(alpha = 0.6f),
                                        disabledPlaceholderColor = TextGray,
                                        disabledTrailingIconColor = MediumGreen,
                                        disabledContainerColor = if (isKeypadVisible) MediumGreen.copy(alpha = 0.06f) else Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Live formatted preview string
                            if (amount.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "= ${evaluatedAmount.toCurrencyString(currencyCode)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MediumGreen
                                    ),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom MoneyKeypad expandable panel
                            AnimatedVisibility(
                                visible = isKeypadVisible,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    MoneyKeypad(
                                        currentAmount = amount,
                                        onAmountChanged = { newAmount ->
                                            amount = newAmount
                                        },
                                        onDonePressed = {
                                            val eval = MoneyKeypadHelper.evaluateExpression(amount)
                                            amount = if (eval > 0) {
                                                if (eval % 1.0 == 0.0) eval.toLong().toString() else eval.toString()
                                            } else ""
                                            isKeypadVisible = false
                                        },
                                        primaryColor = MediumGreen,
                                        currencyCode = currencyCode
                                    )
                                }
                            }

                            // Quick Amount Preset Chips (+500K, +1M, +2M, +5M) when Keypad is not expanded
                            if (!isKeypadVisible) {
                                val presets = if (currencyCode == "VND") {
                                    listOf(
                                        "+500K" to 500_000.0,
                                        "+1M" to 1_000_000.0,
                                        "+2M" to 2_000_000.0,
                                        "+5M" to 5_000_000.0
                                    )
                                } else {
                                    listOf(
                                        "+$25" to 25.0,
                                        "+$50" to 50.0,
                                        "+$100" to 100.0,
                                        "+$250" to 250.0
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    presets.forEach { (label, presetVal) ->
                                        Surface(
                                            onClick = {
                                                val currentVal = MoneyKeypadHelper.evaluateExpression(amount)
                                                val newVal = currentVal + presetVal
                                                amount = if (newVal % 1.0 == 0.0) newVal.toLong().toString() else newVal.toString()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = MediumGreen.copy(alpha = 0.1f),
                                            border = BorderStroke(1.dp, MediumGreen.copy(alpha = 0.3f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MediumGreen
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. BUDGET PERIOD & DATE RANGE
                        val (computedStartMs, computedEndMs) = remember(
                            selectedPeriod,
                            datePickerState.selectedDateMillis,
                            customStartDate,
                            customEndDate
                        ) {
                            if (selectedPeriod == BudgetPeriod.CUSTOM) {
                                val s = Calendar.getInstance().apply {
                                    timeInMillis = customStartDate
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                val e = Calendar.getInstance().apply {
                                    timeInMillis = customEndDate.coerceAtLeast(customStartDate)
                                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                                }.timeInMillis
                                Pair(s, e)
                            } else {
                                val anchor = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                                val cal = Calendar.getInstance().apply { timeInMillis = anchor }
                                when (selectedPeriod) {
                                    BudgetPeriod.WEEKLY -> {
                                        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                                        val s = cal.timeInMillis
                                        cal.add(Calendar.DAY_OF_YEAR, 6)
                                        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                                        Pair(s, cal.timeInMillis)
                                    }
                                    BudgetPeriod.BI_WEEKLY -> {
                                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                                        val s = cal.timeInMillis
                                        cal.add(Calendar.DAY_OF_YEAR, 13)
                                        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                                        Pair(s, cal.timeInMillis)
                                    }
                                    BudgetPeriod.MONTHLY -> {
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                                        val s = cal.timeInMillis
                                        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                                        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                                        Pair(s, cal.timeInMillis)
                                    }
                                    BudgetPeriod.QUARTERLY -> {
                                        val month = cal.get(Calendar.MONTH)
                                        val qStart = (month / 3) * 3
                                        cal.set(Calendar.MONTH, qStart)
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                                        val s = cal.timeInMillis
                                        cal.set(Calendar.MONTH, qStart + 2)
                                        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                                        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                                        Pair(s, cal.timeInMillis)
                                    }
                                    BudgetPeriod.CUSTOM -> Pair(anchor, anchor)
                                }
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MediumGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.budget_period),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }

                            // Period chips selector
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(BudgetPeriod.entries.toTypedArray()) { period ->
                                    val isSelected = selectedPeriod == period
                                    val label = when (period) {
                                        BudgetPeriod.WEEKLY -> stringResource(R.string.period_weekly)
                                        BudgetPeriod.BI_WEEKLY -> stringResource(R.string.period_bi_weekly)
                                        BudgetPeriod.MONTHLY -> stringResource(R.string.period_monthly)
                                        BudgetPeriod.QUARTERLY -> stringResource(R.string.period_quarterly)
                                        BudgetPeriod.CUSTOM -> stringResource(R.string.period_custom)
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedPeriod = period },
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
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MediumGreen else Color.LightGray.copy(alpha = 0.4f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Date range card
                            Card(
                                onClick = {
                                    if (selectedPeriod == BudgetPeriod.CUSTOM) {
                                        pickingCustomEnd = false
                                    }
                                    showDatePicker = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = BackgroundGray),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = formatPeriodDisplay(selectedPeriod, computedStartMs, computedEndMs),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = stringResource(R.string.select_date),
                                        tint = MediumGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. AUTO-RENEW & ROLLOVER SETTINGS
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = BackgroundGray.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
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
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.auto_recurring),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = stringResource(R.string.auto_recurring_desc),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextGray
                                        )
                                    }
                                    Switch(
                                        checked = isRecurring,
                                        onCheckedChange = { isRecurring = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MediumGreen
                                        )
                                    )
                                }

                                AnimatedVisibility(visible = isRecurring) {
                                    Column {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            color = Color.LightGray.copy(alpha = 0.3f)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE8F5E9)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Savings,
                                                    contentDescription = null,
                                                    tint = DarkGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(R.string.rollover_surplus),
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = stringResource(R.string.rollover_surplus_desc),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextGray
                                                )
                                            }
                                            Switch(
                                                checked = rolloverEnabled,
                                                onCheckedChange = { rolloverEnabled = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = MediumGreen
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5. NOTES (OPTIONAL)
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(stringResource(R.string.budget_notes)) },
                            placeholder = { Text(stringResource(R.string.budget_notes_placeholder)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MediumGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MediumGreen,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = BackgroundGray
                            ),
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // --- ACTION BUTTONS (CONFIRM / DELETE / CANCEL) ---
                        val evaluatedAmount = MoneyKeypadHelper.evaluateExpression(amount)
                        val isFormValid = selectedCategory.isNotBlank() && evaluatedAmount > 0

                        Button(
                            onClick = {
                                val allocated = MoneyKeypadHelper.evaluateExpression(amount)
                                if (selectedCategory.isNotBlank() && allocated > 0) {
                                    val newBudget = budget?.copy(
                                        allocatedAmount = allocated,
                                        startDate = computedStartMs,
                                        endDate = computedEndMs,
                                        period = selectedPeriod,
                                        isRecurring = isRecurring,
                                        rolloverEnabled = rolloverEnabled,
                                        notes = notes.trim()
                                    ) ?: Budget(
                                        category = selectedCategory,
                                        allocatedAmount = allocated,
                                        startDate = computedStartMs,
                                        endDate = computedEndMs,
                                        period = selectedPeriod,
                                        isRecurring = isRecurring,
                                        rolloverEnabled = rolloverEnabled,
                                        notes = notes.trim()
                                    )
                                    onConfirm(newBudget)
                                }
                            },
                            enabled = isFormValid,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MediumGreen,
                                disabledContainerColor = Color.LightGray.copy(alpha = 0.4f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (budget == null) Icons.Default.Add else Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (budget == null) stringResource(R.string.add_budget) else stringResource(R.string.save),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // Integrated Delete button if in edit mode
                        if (budget != null) {
                            Button(
                                onClick = onDelete,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LightRed,
                                    contentColor = Error
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.delete_budget),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Error
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                pickingCustomEnd = false
                showDatePicker = false
            },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { picked ->
                        if (selectedPeriod == BudgetPeriod.CUSTOM) {
                            if (!pickingCustomEnd) {
                                customStartDate = picked
                                pickingCustomEnd = true
                                return@TextButton
                            } else {
                                customEndDate = picked
                                pickingCustomEnd = false
                            }
                        }
                    }
                    showDatePicker = false
                }) {
                    Text(
                        text = if (selectedPeriod == BudgetPeriod.CUSTOM && !pickingCustomEnd) "Chọn ngày kết thúc" else stringResource(R.string.ok),
                        fontWeight = FontWeight.Bold,
                        color = MediumGreen
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pickingCustomEnd = false
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    if (selectedPeriod == BudgetPeriod.CUSTOM) {
                        Text(
                            text = if (!pickingCustomEnd) "Chọn ngày bắt đầu" else "Chọn ngày kết thúc",
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            )
        }
    }
}

private fun formatPeriodDisplay(
    period: BudgetPeriod,
    startMs: Long,
    endMs: Long
): String {
    val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return when (period) {
        BudgetPeriod.WEEKLY -> "Tuần: ${dateFmt.format(Date(startMs))} - ${dateFmt.format(Date(endMs))}"
        BudgetPeriod.BI_WEEKLY -> "2 tuần: ${dateFmt.format(Date(startMs))} - ${dateFmt.format(Date(endMs))}"
        BudgetPeriod.MONTHLY -> "${monthFmt.format(Date(startMs))} (${dateFmt.format(Date(startMs))} - ${dateFmt.format(Date(endMs))})"
        BudgetPeriod.QUARTERLY -> "Quý: ${dateFmt.format(Date(startMs))} - ${dateFmt.format(Date(endMs))}"
        BudgetPeriod.CUSTOM -> "${dateFmt.format(Date(startMs))} - ${dateFmt.format(Date(endMs))}"
    }
}

private fun getCategoryEmoji(categoryName: String): String {
    val name = categoryName.lowercase(Locale.getDefault())
    return when {
        name.contains("ăn") || name.contains("food") || name.contains("uống") || name.contains("dine") -> "🍔"
        name.contains("di chuyển") || name.contains("travel") || name.contains("xe") || name.contains("transport") -> "🚗"
        name.contains("giải trí") || name.contains("entertain") || name.contains("chơi") || name.contains("game") -> "🎮"
        name.contains("mua sắm") || name.contains("shop") || name.contains("sắm") -> "🛍️"
        name.contains("y tế") || name.contains("sức khỏe") || name.contains("health") || name.contains("thuốc") -> "💊"
        name.contains("học") || name.contains("edu") || name.contains("sách") -> "📚"
        name.contains("nhà") || name.contains("home") || name.contains("phòng") -> "🏠"
        name.contains("điện") || name.contains("nước") || name.contains("bill") || name.contains("hóa đơn") -> "⚡"
        name.contains("lương") || name.contains("salary") -> "💵"
        else -> "🏷️"
    }
}

private fun formatDate(millis: Long): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
}

