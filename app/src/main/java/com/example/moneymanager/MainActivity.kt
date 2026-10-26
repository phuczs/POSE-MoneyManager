package com.example.moneymanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.moneymanager.ui.navigation.MainContainer
import com.example.moneymanager.ui.theme.MoneyManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.ui.viewmodel.AuthViewModel
import com.example.moneymanager.util.LocaleHelper
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalConfiguration

import androidx.fragment.app.FragmentActivity
import android.view.WindowManager
import com.example.moneymanager.ui.viewmodel.SecurityViewModel
import com.example.moneymanager.ui.screens.auth.AppLockScreen

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val sharedPrefs = newBase.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val languageCode = sharedPrefs.getString("app_language", "vi") ?: "vi"
        super.attachBaseContext(LocaleHelper.setLocale(newBase, languageCode))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            val securityViewModel: SecurityViewModel = hiltViewModel()
            val accentColorHex by authViewModel.accentColorHex.collectAsState()
            val currentLanguage by authViewModel.currentLanguage.collectAsState()
            val isLocked by securityViewModel.isLocked.collectAsState()
            val isFlagSecure by securityViewModel.isFlagSecureEnabled.collectAsState()

            LaunchedEffect(isFlagSecure) {
                if (isFlagSecure) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            
            val customAccentColor = remember(accentColorHex) {
                if (accentColorHex.isNotBlank()) {
                    try {
                        Color(android.graphics.Color.parseColor(accentColorHex))
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val context = LocalContext.current
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { }
                )
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val context = LocalContext.current
            
            val initialLanguage = remember { currentLanguage }
            LaunchedEffect(currentLanguage) {
                if (currentLanguage != initialLanguage) {
                    (context as? android.app.Activity)?.recreate()
                }
            }
            
            val updatedContext = remember(currentLanguage, context) {
                val localeContext = LocaleHelper.setLocale(context, currentLanguage)
                object : android.content.ContextWrapper(context) {
                    override fun getResources() = localeContext.resources
                }
            }
            val updatedConfiguration = updatedContext.resources.configuration

            androidx.compose.runtime.CompositionLocalProvider(
                LocalContext provides updatedContext,
                LocalConfiguration provides updatedConfiguration
            ) {
                MoneyManagerTheme(customAccentColor = customAccentColor) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (isLocked) {
                            AppLockScreen(onUnlock = { securityViewModel.unlock() })
                        } else {
                            val navController = rememberNavController()
                            
                            LaunchedEffect(intent) {
                                val target = intent.getStringExtra(com.example.moneymanager.widget.MoneyManagerWidgetProvider.EXTRA_NAVIGATE_TO)
                                if (target != null) {
                                    intent.removeExtra(com.example.moneymanager.widget.MoneyManagerWidgetProvider.EXTRA_NAVIGATE_TO)
                                    when (target) {
                                        com.example.moneymanager.widget.MoneyManagerWidgetProvider.NAV_ADD_TRANSACTION -> {
                                            navController.navigate(com.example.moneymanager.ui.navigation.Screen.AddTransaction.route)
                                        }
                                        com.example.moneymanager.widget.MoneyManagerWidgetProvider.NAV_CHAT -> {
                                            navController.navigate(com.example.moneymanager.ui.navigation.Screen.Chat.route)
                                        }
                                    }
                                }
                            }

                            MainContainer(navController = navController)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}