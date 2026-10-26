package com.example.moneymanager.ui.screens.recurring

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.ui.screens.transaction.TransactionsScreen
import com.example.moneymanager.ui.viewmodel.RecurringViewModel

/**
 * Screen wrapper for recurring transactions.
 * Delegates directly to [TransactionsScreen] with the "recurring" tab selected,
 * providing a consistent, unified and modern UI across the app.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecurringTransactionsScreen(
    viewModel: RecurringViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    TransactionsScreen(
        onNavigateBack = onBackClick,
        onNavigateToAddTransaction = {},
        onTransactionClick = {},
        initialTab = "recurring",
        recurringViewModel = viewModel
    )
}
