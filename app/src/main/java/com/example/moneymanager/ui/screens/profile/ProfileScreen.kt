package com.example.moneymanager.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.moneymanager.data.model.User
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle(initialValue = null)
    val isNotificationsEnabled by authViewModel.isNotificationsEnabled.collectAsStateWithLifecycle(initialValue = true)

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }

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
            onEditNameClick = { showEditNameDialog = true },
            onChangePasswordClick = { showChangePasswordDialog = true },
            onDeleteAccountClick = { showDeleteAccountDialog = true },
            onSignOutClick = { authViewModel.signOut() },
            onNavigateToBudgets = onNavigateToBudgets,
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
            text = user?.displayName?.takeIf { it.isNotBlank() } ?: "No Name",
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
    onEditNameClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        SectionHeader(title = "Account Settings")

        ProfileOption(
            icon = Icons.Default.Person,
            title = "Edit Name",
            subtitle = user?.displayName?.takeIf { it.isNotBlank() } ?: "Add your name",
            onClick = onEditNameClick
        )

        ProfileOption(
            icon = Icons.Default.Email,
            title = "Email",
            subtitle = user?.email ?: "",
            onClick = null // Email cannot be changed easily
        )

        ProfileOption(
            icon = Icons.Default.Lock,
            title = "Change Password",
            subtitle = "Update your security",
            onClick = onChangePasswordClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = "App Settings")

        // Notification Toggle
        ProfileToggleOption(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            subtitle = if(isNotificationsEnabled) "Enabled" else "Disabled",
            isChecked = isNotificationsEnabled,
            onCheckedChange = onToggleNotifications
        )

        ProfileOption(
            icon = Icons.Default.Star,
            title = "Budgets",
            subtitle = "Manage your budgets",
            onClick = onNavigateToBudgets
        )

        ProfileOption(
            icon = Icons.Default.Language,
            title = "Language",
            subtitle = "English (US)",
            onClick = { /* TODO: Implement Language Selection */ }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Danger Zone Section
        SectionHeader(title = "Danger Zone", isWarning = true)

        ProfileOption(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            title = "Sign Out",
            subtitle = "Sign out of your account",
            onClick = onSignOutClick,
            isWarning = false
        )

        ProfileOption(
            icon = Icons.Default.DeleteForever,
            title = "Delete Account",
            subtitle = "Permanently delete your account and data",
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
private fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
        title = { Text("Change Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
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
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text(
                        text = "Passwords do not match or too short (min 6 chars)",
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
                Text("Change")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
                "Delete Account",
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(
                "Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently lost.",
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
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
        title = { Text("Update Profile Photo") },
        text = {
            Column {
                Text("Enter an image URL:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL") },
                    placeholder = { Text("https://example.com/image.jpg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: For real implementation, integrate with system image picker.",
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}