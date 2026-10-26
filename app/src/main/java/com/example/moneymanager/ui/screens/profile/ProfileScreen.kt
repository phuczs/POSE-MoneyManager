package com.example.moneymanager.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.example.moneymanager.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.moneymanager.data.model.User
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.viewmodel.AuthViewModel
import com.example.moneymanager.ui.viewmodel.SecurityViewModel
import com.example.moneymanager.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    securityViewModel: SecurityViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle(initialValue = null)
    val isNotificationsEnabled by authViewModel.isNotificationsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val isLockEnabled by securityViewModel.isLockEnabled.collectAsStateWithLifecycle()
    val lockTimeoutMinutes by securityViewModel.lockTimeoutMinutes.collectAsStateWithLifecycle()
    val isFlagSecureEnabled by securityViewModel.isFlagSecureEnabled.collectAsStateWithLifecycle()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showWidgetDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Permission Launcher for Android 13+ Notification Permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            authViewModel.toggleNotifications(isGranted)
        }
    )

    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.SignedOut,
            is AuthViewModel.AuthState.AccountDeleted -> {
                onNavigateToLogin()
            }
            is AuthViewModel.AuthState.ProfileUpdated -> {
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        // Header with profile picture and basic info
        ProfileHeader(
            user = currentUser,
            onEditPhotoClick = { showImagePickerDialog = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile options
        ProfileContent(
            user = currentUser,
            isNotificationsEnabled = isNotificationsEnabled,
            isLockEnabled = isLockEnabled,
            lockTimeoutMinutes = lockTimeoutMinutes,
            isFlagSecureEnabled = isFlagSecureEnabled,
            onToggleLock = { securityViewModel.setLockEnabled(it) },
            onTimeoutClick = { showTimeoutDialog = true },
            onToggleFlagSecure = { securityViewModel.setFlagSecureEnabled(it) },
            onEditNameClick = { showEditNameDialog = true },
            onChangePasswordClick = { showChangePasswordDialog = true },
            onDeleteAccountClick = { showDeleteAccountDialog = true },
            onSignOutClick = { authViewModel.signOut() },
            onNavigateToBudgets = onNavigateToBudgets,
            onLanguageClick = { showLanguageDialog = true },
            onThemeClick = { showThemeDialog = true },
            onWidgetLayoutClick = { showWidgetDialog = true },
            onToggleNotifications = { enabled ->
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        authViewModel.toggleNotifications(true)
                    }
                } else {
                    authViewModel.toggleNotifications(enabled)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // --- Dialogs ---

    if (showTimeoutDialog) {
        LockTimeoutDialog(
            currentTimeout = lockTimeoutMinutes,
            onDismiss = { showTimeoutDialog = false },
            onSelectTimeout = { securityViewModel.setLockTimeoutMinutes(it) }
        )
    }

    if (showEditNameDialog) {
        EditNameDialog(
            currentName = currentUser?.displayName ?: "",
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                authViewModel.updateDisplayName(newName)
                showEditNameDialog = false
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { currentPassword, newPassword ->
                authViewModel.updatePassword(currentPassword, newPassword)
                showChangePasswordDialog = false
            }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = {
                authViewModel.deleteAccount()
                showDeleteAccountDialog = false
            }
        )
    }

    if (showImagePickerDialog) {
        ImagePickerDialog(
            onDismiss = { showImagePickerDialog = false },
            onImageSelected = { imageUrl ->
                authViewModel.updateProfilePhoto(imageUrl)
                showImagePickerDialog = false
            }
        )
    }
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = authViewModel.currentLanguage.collectAsState(initial = "vi").value,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { languageCode ->
                authViewModel.setAppLanguage(languageCode)
                showLanguageDialog = false
            }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentHex = authViewModel.accentColorHex.collectAsState(initial = "").value,
            onDismiss = { showThemeDialog = false },
            onColorSelected = { hexStr ->
                authViewModel.setAccentColorHex(hexStr)
                showThemeDialog = false
            }
        )
    }

    if (showWidgetDialog) {
        WidgetLayoutDialog(
            currentOrder = authViewModel.dashboardWidgetsOrder.collectAsState(initial = emptyList()).value,
            onDismiss = { showWidgetDialog = false },
            onOrderSaved = { newOrder ->
                authViewModel.setDashboardWidgetsOrder(newOrder)
                showWidgetDialog = false
            }
        )
    }

    // Show loading overlay
    if (authState is AuthViewModel.AuthState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ProfileHeader(
    user: User?,
    onEditPhotoClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture
        Box(
            contentAlignment = Alignment.BottomEnd
        ) {
            val photoUrlString = user?.photoUrl?.toString()
            val isImageValid = remember(photoUrlString) { mutableStateOf(true) }

            if (photoUrlString != null && isImageValid.value) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrlString)
                        .crossfade(true)
                        .listener(
                            onError = { _, result ->
                                Log.e("ProfilePhoto", "Failed to load image", result.throwable)
                                isImageValid.value = false
                            }
                        )
                        .build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback to initial
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.displayName?.firstOrNull()?.toString()?.uppercase() ?: "U",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Edit photo button
            FloatingActionButton(
                onClick = onEditPhotoClick,
                modifier = Modifier.size(36.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Photo",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        Text(
            text = user?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.no_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Email
        Text(
            text = user?.email ?: "",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProfileContent(
    user: User?,
    isNotificationsEnabled: Boolean,
    isLockEnabled: Boolean,
    lockTimeoutMinutes: Int,
    isFlagSecureEnabled: Boolean,
    onToggleLock: (Boolean) -> Unit,
    onTimeoutClick: () -> Unit,
    onToggleFlagSecure: (Boolean) -> Unit,
    onEditNameClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onThemeClick: () -> Unit,
    onWidgetLayoutClick: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        SectionHeader(title = stringResource(R.string.account_settings))

        ProfileOption(
            icon = Icons.Default.Person,
            title = stringResource(R.string.edit_name),
            subtitle = user?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.add_your_name),
            onClick = onEditNameClick
        )

        ProfileOption(
            icon = Icons.Default.Email,
            title = stringResource(R.string.email),
            subtitle = user?.email ?: "",
            onClick = null // Email cannot be changed easily
        )

        ProfileOption(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.change_password),
            subtitle = stringResource(R.string.update_security),
            onClick = onChangePasswordClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = stringResource(R.string.app_settings))

        // Notification Toggle
        ProfileToggleOption(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.notifications),
            subtitle = if(isNotificationsEnabled) stringResource(R.string.enabled) else stringResource(R.string.disabled),
            isChecked = isNotificationsEnabled,
            onCheckedChange = onToggleNotifications
        )

        ProfileOption(
            icon = Icons.Default.Star,
            title = stringResource(R.string.nav_budgets),
            subtitle = stringResource(R.string.manage_your_budgets),
            onClick = onNavigateToBudgets
        )

        ProfileOption(
            icon = Icons.Default.Language,
            title = stringResource(R.string.language),
            subtitle = stringResource(R.string.language_options),
            onClick = onLanguageClick
        )

        ProfileOption(
            icon = Icons.Default.Palette,
            title = stringResource(R.string.theme_color),
            subtitle = stringResource(R.string.customize_accent_color),
            onClick = onThemeClick
        )

        ProfileOption(
            icon = Icons.Default.DashboardCustomize,
            title = stringResource(R.string.dashboard_layout),
            subtitle = stringResource(R.string.rearrange_widgets),
            onClick = onWidgetLayoutClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // App Security Section
        SectionHeader(title = stringResource(R.string.app_security))

        ProfileToggleOption(
            icon = Icons.Default.Security,
            title = stringResource(R.string.app_lock),
            subtitle = stringResource(R.string.app_lock_desc),
            isChecked = isLockEnabled,
            onCheckedChange = onToggleLock
        )

        if (isLockEnabled) {
            val timeoutLabel = when (lockTimeoutMinutes) {
                0 -> stringResource(R.string.lock_immediately)
                1 -> stringResource(R.string.lock_1_min)
                5 -> stringResource(R.string.lock_5_min)
                15 -> stringResource(R.string.lock_15_min)
                else -> "$lockTimeoutMinutes min"
            }
            ProfileOption(
                icon = Icons.Default.Timer,
                title = stringResource(R.string.lock_timeout),
                subtitle = timeoutLabel,
                onClick = onTimeoutClick
            )
        }

        ProfileToggleOption(
            icon = Icons.Default.VisibilityOff,
            title = stringResource(R.string.hide_in_recents),
            subtitle = stringResource(R.string.hide_in_recents_desc),
            isChecked = isFlagSecureEnabled,
            onCheckedChange = onToggleFlagSecure
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Danger Zone Section
        SectionHeader(title = "Danger Zone", isWarning = true)

        ProfileOption(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            title = stringResource(R.string.sign_out),
            subtitle = stringResource(R.string.sign_out_desc),
            onClick = onSignOutClick,
            isWarning = false
        )

        ProfileOption(
            icon = Icons.Default.DeleteForever,
            title = stringResource(R.string.delete_account),
            subtitle = stringResource(R.string.delete_account_desc),
            onClick = onDeleteAccountClick,
            isWarning = true
        )
    }
}

// --- Helper Composables ---

@Composable
private fun ProfileToggleOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(text = subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = MediumGreen, checkedTrackColor = MediumGreen.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    isWarning: Boolean = false
) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ProfileOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    isWarning: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isWarning) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// --- Dialogs Implementations ---

@Composable
private fun LockTimeoutDialog(
    currentTimeout: Int,
    onDismiss: () -> Unit,
    onSelectTimeout: (Int) -> Unit
) {
    val options = listOf(
        0 to stringResource(R.string.lock_immediately),
        1 to stringResource(R.string.lock_1_min),
        5 to stringResource(R.string.lock_5_min),
        15 to stringResource(R.string.lock_15_min)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lock_timeout)) },
        text = {
            Column {
                options.forEach { (minutes, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectTimeout(minutes)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTimeout == minutes,
                            onClick = {
                                onSelectTimeout(minutes)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = MediumGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.display_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_password)) },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.current_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text(
                        text = stringResource(R.string.password_mismatch),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newPassword == confirmPassword && newPassword.length >= 6) {
                        onConfirm(currentPassword, newPassword)
                    } else {
                        showError = true
                    }
                }
            ) {
                Text(stringResource(R.string.change))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                stringResource(R.string.delete_account),
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(
                stringResource(R.string.are_you_sure_delete_account),
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onImageSelected: (String) -> Unit
) {
    var imageUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_profile_photo)) },
        text = {
            Column {
                Text(stringResource(R.string.enter_image_url))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text(stringResource(R.string.image_url)) },
                    placeholder = { Text("https://example.com/image.jpg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.image_url_note),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (imageUrl.isNotBlank()) {
                        onImageSelected(imageUrl.trim())
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { 
            Text(
                text = stringResource(R.string.select_language_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageOption(
                    label = "Tiếng Việt",
                    flag = "🇻🇳",
                    isSelected = currentLanguage == "vi",
                    onClick = { onLanguageSelected("vi") }
                )
                LanguageOption(
                    label = "English",
                    flag = "🇺🇸",
                    isSelected = currentLanguage == "en",
                    onClick = { onLanguageSelected("en") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun LanguageOption(
    label: String,
    flag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = flag, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    currentHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val predefinedColors = listOf(
        Pair("Green (Default)", "#00796B"), // MediumGreen
        Pair("Violet", "#7F3DFF"), // Primary original
        Pair("Blue", "#2196F3"),
        Pair("Crimson", "#E91E63"),
        Pair("Orange", "#FF9800"),
        Pair("Teal", "#009688")
    )
    
    // Fallback if empty config is found, match to green.
    val actualCurrentHex = if(currentHex.isBlank()) "#00796B" else currentHex

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_accent_color)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                predefinedColors.forEach { (name, hex) ->
                    val color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onColorSelected(hex) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (hex == actualCurrentHex) FontWeight.Bold else FontWeight.Normal,
                            color = if (hex == actualCurrentHex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (hex == actualCurrentHex) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
fun WidgetLayoutDialog(
    currentOrder: List<String>,
    onDismiss: () -> Unit,
    onOrderSaved: (List<String>) -> Unit
) {
    val availableWidgets = listOf("AiInsight", "Overview", "RecentTransactions")
    val widgetNames = mapOf(
        "AiInsight" to "AI Monthly Insight",
        "Overview" to "Spending Pie Chart",
        "RecentTransactions" to "Recent Transactions List"
    )

    var workingOrder by remember { mutableStateOf(currentOrder) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dashboard_widgets)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.select_widgets_to_show),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                availableWidgets.forEach { widgetKey ->
                    val isChecked = workingOrder.contains(widgetKey)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                workingOrder = if (isChecked) {
                                    workingOrder.filter { it != widgetKey }
                                } else {
                                    workingOrder + widgetKey
                                }
                            }
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = widgetNames[widgetKey] ?: widgetKey,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onOrderSaved(workingOrder) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}