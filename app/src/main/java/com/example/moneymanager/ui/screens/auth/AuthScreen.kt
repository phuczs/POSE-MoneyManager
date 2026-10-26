package com.example.moneymanager.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.ui.theme.*
import com.example.moneymanager.ui.viewmodel.AuthViewModel
import androidx.compose.ui.res.stringResource
import com.example.moneymanager.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

enum class AuthTab {
    LOGIN, REGISTER
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val currentLanguage by authViewModel.currentLanguage.collectAsState(initial = "vi")
    var currentTab by remember { mutableStateOf(AuthTab.LOGIN) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Google Sign In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    authViewModel.signInWithGoogleAccount(account)
                } catch (e: ApiException) {
                    // Handle error
                }
            }
        }
    )

    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetEmailError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Success) {
            onAuthSuccess()
        } else if (authState is AuthViewModel.AuthState.PasswordResetSent) {
            val message = context.getString(R.string.password_reset_sent)
            snackbarHostState.showSnackbar(message)
            authViewModel.resetState()
        }
    }

    Scaffold(
        containerColor = BackgroundGray, // From your Color.kt
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Emerald Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(MediumGreen) // Emerald Theme
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.journey_financial_freedom),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Language Toggle Dropdown
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .clickable { languageMenuExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == "vi") "🇻🇳" else "🇺🇸",
                            fontSize = 16.sp
                        )
                        Text(
                            text = currentLanguage.uppercase(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("🇻🇳", fontSize = 20.sp)
                                    Text(
                                        "Tiếng Việt",
                                        fontWeight = if (currentLanguage == "vi") FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentLanguage == "vi") MediumGreen else Color.Black
                                    )
                                }
                            },
                            onClick = {
                                authViewModel.setAppLanguage("vi")
                                languageMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("🇺🇸", fontSize = 20.sp)
                                    Text(
                                        "English",
                                        fontWeight = if (currentLanguage == "en") FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentLanguage == "en") MediumGreen else Color.Black
                                    )
                                }
                            },
                            onClick = {
                                authViewModel.setAppLanguage("en")
                                languageMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Custom Toggle Tabs
            AuthTabSelector(
                selectedTab = currentTab,
                onTabSelected = { currentTab = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Form Content with Animation
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (targetState == AuthTab.REGISTER) {
                        // Slide Left (Login -> Register)
                        (slideInHorizontally(animationSpec = tween(300)) { width -> width } +
                                fadeIn(animationSpec = tween(300))) with
                                (slideOutHorizontally(animationSpec = tween(300)) { width -> -width } +
                                        fadeOut(animationSpec = tween(300)))
                    } else {
                        // Slide Right (Register -> Login)
                        (slideInHorizontally(animationSpec = tween(300)) { width -> -width } +
                                fadeIn(animationSpec = tween(300))) with
                                (slideOutHorizontally(animationSpec = tween(300)) { width -> width } +
                                        fadeOut(animationSpec = tween(300)))
                    }
                },
                label = "AuthTransition"
            ) { tab ->
                when (tab) {
                    AuthTab.LOGIN -> LoginContent(
                        authViewModel = authViewModel,
                        isLoading = authState is AuthViewModel.AuthState.Loading,
                        onGoogleClick = {
                            val signInIntent = authViewModel.getGoogleSignInClient().signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        },
                        onForgotPassword = { email ->
                            resetEmail = email
                            resetEmailError = null
                            showResetDialog = true
                        }
                    )
                    AuthTab.REGISTER -> RegisterContent(
                        authViewModel = authViewModel,
                        isLoading = authState is AuthViewModel.AuthState.Loading
                    )
                }
            }

            // Error Message Display
            if (authState is AuthViewModel.AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = (authState as AuthViewModel.AuthState.Error).message,
                        color = Color(0xFFD32F2F),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
        
        if (showResetDialog) {
            PasswordResetDialog(
                email = resetEmail,
                onEmailChange = { 
                    resetEmail = it
                    resetEmailError = null
                },
                onDismiss = { showResetDialog = false },
                onConfirm = {
                    if (android.util.Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                        authViewModel.resetPassword(resetEmail)
                        showResetDialog = false
                    } else {
                        resetEmailError = context.getString(R.string.invalid_email)
                    }
                },
                errorMessage = resetEmailError
            )
        }
    }
}

@Composable
fun AuthTabSelector(
    selectedTab: AuthTab,
    onTabSelected: (AuthTab) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabButton(
                text = stringResource(R.string.log_in),
                isSelected = selectedTab == AuthTab.LOGIN,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(AuthTab.LOGIN) }
            )
            TabButton(
                text = stringResource(R.string.sign_up),
                isSelected = selectedTab == AuthTab.REGISTER,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(AuthTab.REGISTER) }
            )
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) MediumGreen else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else TextGray,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun LoginContent(
    authViewModel: AuthViewModel,
    isLoading: Boolean,
    onGoogleClick: () -> Unit,
    onForgotPassword: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        EmeraldTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.email_address),
            icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        EmeraldTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.password),
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordToggle = { passwordVisible = !passwordVisible }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.forgot_password),
            color = MediumGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.End)
                .clickable {
                    onForgotPassword(email)
                }
        )

        Spacer(modifier = Modifier.height(32.dp))

        EmeraldButton(
            text = stringResource(R.string.log_in),
            isLoading = isLoading,
            onClick = { authViewModel.login(email, password) }
        )

        Spacer(modifier = Modifier.height(24.dp))
        DividerWithText()
        Spacer(modifier = Modifier.height(24.dp))

        GoogleButton(onClick = onGoogleClick)
    }
}

@Composable
fun RegisterContent(
    authViewModel: AuthViewModel,
    isLoading: Boolean
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        EmeraldTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.email_address),
            icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        EmeraldTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.password),
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordToggle = { passwordVisible = !passwordVisible }
        )

        Spacer(modifier = Modifier.height(16.dp))

        EmeraldTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = stringResource(R.string.confirm_password),
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            passwordVisible = confirmPasswordVisible,
            onPasswordToggle = { confirmPasswordVisible = !confirmPasswordVisible }
        )

        if (errorText != null) {
            Text(
                text = errorText!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        EmeraldButton(
            text = stringResource(R.string.sign_up),
            isLoading = isLoading,
            onClick = {
                if (password == confirmPassword) {
                    errorText = null
                    authViewModel.register(email, password)
                } else {
                    errorText = context.getString(R.string.passwords_do_not_match)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.terms_and_policy),
            color = TextGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- Reusable Emerald UI Components ---

@Composable
fun EmeraldTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MediumGreen) },
        trailingIcon = if (isPassword && onPasswordToggle != null) {
            {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = TextGray
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MediumGreen,
            unfocusedBorderColor = Color.LightGray,
            focusedLabelColor = MediumGreen,
            cursorColor = MediumGreen,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        singleLine = true
    )
}

@Composable
fun EmeraldButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MediumGreen,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
        } else {
            Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GoogleButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Text(" " + stringResource(R.string.sign_in_google), color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DividerWithText() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
        Text(
            text = stringResource(R.string.or),
            color = TextGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
    }
}