package com.example.moneymanager.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.moneymanager.ui.screens.auth.AuthScreen
import com.example.moneymanager.ui.screens.budget.BudgetScreen
import com.example.moneymanager.ui.screens.category.CategoriesScreen
import com.example.moneymanager.ui.screens.dashboard.DashboardScreen
import com.example.moneymanager.ui.screens.profile.ProfileScreen
import com.example.moneymanager.ui.screens.statistics.StatisticsScreen
import com.example.moneymanager.ui.screens.transaction.AddEditTransactionScreen
import com.example.moneymanager.ui.screens.transaction.TransactionsScreen
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.viewmodel.AuthViewModel
import com.example.moneymanager.ui.screens.chat.ChatScreen
import com.example.moneymanager.ui.viewmodel.ChatViewModel // Đảm bảo đã import ViewModel này


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState(initial = false)
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    if (isAuthenticated) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(80.dp)
                ) {
                    // 1. Dashboard
                    NavBarItem(
                        selected = selectedTabIndex == 0,
                        onClick = {
                            selectedTabIndex = 0
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        },
                        icon = Icons.Default.Home,
                        label = "Home"
                    )

                    // 2. Transactions
                    NavBarItem(
                        selected = selectedTabIndex == 1,
                        onClick = {
                            selectedTabIndex = 1
                            navController.navigate(Screen.Transactions.route)
                        },
                        icon = Icons.Default.History,
                        label = "Transactions"
                    )

                    // 3. VIVID ADD BUTTON (Center)
                    NavigationBarItem(
                        selected = false, // Never "selected" in the traditional sense
                        onClick = {
                            selectedTabIndex = 2
                            navController.navigate(Screen.AddTransaction.route)
                        },
                        icon = {
                            // Custom "Floating" Look
                            Box(
                                modifier = Modifier
                                    .size(56.dp) // Large touch target
                                    .shadow(8.dp, CircleShape) // Add depth
                                    .clip(CircleShape)
                                    .background(MediumGreen), // Vivid Emerald Background
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Transaction",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp) // Large Icon
                                )
                            }
                        },
                        label = { /* No label for cleaner look */ },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent // Remove selection pill
                        )
                    )

                    // 4. Budgets
                    NavBarItem(
                        selected = selectedTabIndex == 3,
                        onClick = {
                            selectedTabIndex = 3
                            navController.navigate(Screen.Budgets.route)
                        },
                        icon = Icons.Default.PieChart,
                        label = "Budgets"
                    )

                    // 5. Profile
                    NavBarItem(
                        selected = selectedTabIndex == 4,
                        onClick = {
                            selectedTabIndex = 4
                            navController.navigate(Screen.Profile.route)
                        },
                        icon = Icons.Default.Person,
                        label = "Profile"
                    )
                }
            }
        ) { paddingValues ->
            NavGraph(
                navController = navController,
                authViewModel = authViewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }
    } else {
        NavGraph(
            navController = navController,
            authViewModel = authViewModel
        )
    }
}

@Composable
fun RowScope.NavBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        },
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MediumGreen,
            selectedTextColor = MediumGreen,
            indicatorColor = MediumGreen.copy(alpha = 0.1f), // Subtle pill background
            unselectedIconColor = TextGray,
            unselectedTextColor = TextGray
        )
    )
}

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState(initial = false)
    val authState by authViewModel.authState.collectAsState()

    // Handle auth state changes
    when (authState) {
        is AuthViewModel.AuthState.SignedOut,
        is AuthViewModel.AuthState.AccountDeleted -> {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
            authViewModel.resetState()
        }
        else -> {}
    }

    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) Screen.Dashboard.route else Screen.Login.route,
        modifier = modifier
    ) {
        // Auth screens
        composable(Screen.Login.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Main app screens
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                },
                onNavigateToStatistics = { navController.navigate(Screen.Statistics.route)},
                onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                onNavigateToChat = { navController.navigate(Screen.Chat.route) }
            )
        }

        composable(Screen.Transactions.route) {
            TransactionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                }
            )
        }

        composable(Screen.AddTransaction.route) {
            AddEditTransactionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.EditTransaction.route) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            AddEditTransactionScreen(
                transactionId = transactionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) }
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onNavigateBack = {navController.popBackStack()}
            )
        }

        composable(Screen.Budgets.route) {
            BudgetScreen()
        }
        composable(Screen.Chat.route) {
            // 1. Lấy instance của ChatViewModel thông qua Hilt
            val chatViewModel: ChatViewModel = hiltViewModel()

            ChatScreen(
                // 2. Truyền viewModel vào
               viewModel = chatViewModel,
                // 3. Đổi tên tham số 'onNavigateBack' thành 'onClose' để khớp với định nghĩa của ChatScreen
                onClose = { navController.popBackStack() }
            )
        }
    }
}