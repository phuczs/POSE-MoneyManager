package com.example.moneymanager.ui.navigation


sealed class Screen(val route: String) {
    // Auth screens
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    
    // Main app screens
    object Dashboard : Screen("dashboard_screen")
    object Transactions : Screen("transactions_screen")
    object AddTransaction : Screen("add_transaction_screen")
    object EditTransaction : Screen("edit_transaction_screen/{transactionId}") {
        fun createRoute(transactionId: String): String {
            return "edit_transaction_screen/$transactionId"
        }
    }
    object Categories : Screen("categories_screen")
    object Profile : Screen("profile_screen")
    object Statistics : Screen("statistics")
    object Budgets : Screen("budgets_screen")
    object Chat : Screen("chat_screen")
}
