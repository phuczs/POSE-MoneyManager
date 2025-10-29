package com.example.moneymanager.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.moneymanager.ui.screens.auth.LoginScreen
import com.example.moneymanager.ui.screens.auth.RegisterScreen
import com.example.moneymanager.ui.screens.category.CategoriesScreen
import com.example.moneymanager.ui.screens.dashboard.DashboardScreen
import com.example.moneymanager.ui.screens.profile.ProfileScreen
import com.example.moneymanager.ui.screens.statistics.StatisticsScreen
import com.example.moneymanager.ui.screens.transaction.AddEditTransactionScreen
import com.example.moneymanager.ui.screens.transaction.TransactionsScreen
import com.example.moneymanager.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState(initial = false)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    // Only show navigation bar for authenticated users
    if (isAuthenticated) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = Color(0xFF6366F1)
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Dashboard",
                                tint = if (selectedTabIndex == 0) Color(0xFF6366F1) else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                "Dashboard",
                                color = if (selectedTabIndex == 0) Color(0xFF6366F1) else Color.Gray
                            )
                        },
                        selected = selectedTabIndex == 0,
                        onClick = {
                            selectedTabIndex = 0
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Transactions",
                                tint = if (selectedTabIndex == 1) Color(0xFF6366F1) else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                "Transactions",
                                color = if (selectedTabIndex == 1) Color(0xFF6366F1) else Color.Gray
                            )
                        },
                        selected = selectedTabIndex == 1,
                        onClick = {
                            selectedTabIndex = 1
                            navController.navigate(Screen.Transactions.route)
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Transaction",
                                tint = if (selectedTabIndex == 2) Color(0xFF6366F1) else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                "Add",
                                color = if (selectedTabIndex == 2) Color(0xFF6366F1) else Color.Gray
                            )
                        },
                        selected = selectedTabIndex == 2,
                        onClick = {
                            selectedTabIndex = 2
                            navController.navigate(Screen.AddTransaction.route)
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Categories",
                                tint = if (selectedTabIndex == 3) Color(0xFF6366F1) else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                "Categories",
                                color = if (selectedTabIndex == 3) Color(0xFF6366F1) else Color.Gray
                            )
                        },
                        selected = selectedTabIndex == 3,
                        onClick = {
                            selectedTabIndex = 3
                            navController.navigate(Screen.Categories.route)
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = if (selectedTabIndex == 4) Color(0xFF6366F1) else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                "Profile",
                                color = if (selectedTabIndex == 4) Color(0xFF6366F1) else Color.Gray
                            )
                        },
                        selected = selectedTabIndex == 4,
                        onClick = {
                            selectedTabIndex = 4
                            navController.navigate(Screen.Profile.route)
                        }
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
        // No navigation bar for unauthenticated users
        NavGraph(
            navController = navController,
            authViewModel = authViewModel
        )
    }
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
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
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
                onNavigateToStatistics = { navController.navigate(Screen.Statistics.route)}
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
                }
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onNavigateBack = {navController.popBackStack()}
            )
        }
    }
}