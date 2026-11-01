package com.jarvis.fileserver.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.fileserver.model.ServerConfig
import com.jarvis.fileserver.ui.theme.AmberWarn
import com.jarvis.fileserver.ui.theme.CyanNeon
import com.jarvis.fileserver.ui.theme.EmeraldLive
import com.jarvis.fileserver.ui.theme.EmeraldOnline
import com.jarvis.fileserver.ui.theme.LightBorder
import com.jarvis.fileserver.ui.theme.LightSurface
import com.jarvis.fileserver.ui.theme.LightSurfaceElevated
import com.jarvis.fileserver.ui.theme.LightTextPrimary
import com.jarvis.fileserver.ui.theme.LightTextSecondary
import com.jarvis.fileserver.ui.theme.PurpleAccent
import com.jarvis.fileserver.ui.theme.Slate400
import com.jarvis.fileserver.ui.theme.Slate700
import com.jarvis.fileserver.ui.theme.Slate800
import com.jarvis.fileserver.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    currentConfig: ServerConfig,
    isDarkTheme: Boolean,
    isServerRunning: Boolean,
    onSaveConfig: (ServerConfig) -> Unit,
    onSelectDarkTheme: (Boolean) -> Unit,
    onPickFolder: () -> Unit,
    onPickFile: () -> Unit,
    onSelectAllStorage: () -> Unit,
    onOpenAdminLogin: () -> Unit,
    onOpenShareManagement: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultRoot = remember { android.os.Environment.getExternalStorageDirectory().absolutePath }

    var portText by remember { mutableStateOf(currentConfig.port.toString()) }
    var rootPath by remember { mutableStateOf(currentConfig.rootPath) }
    var authEnabled by remember { mutableStateOf(currentConfig.basicAuthEnabled) }
    var username by remember { mutableStateOf(currentConfig.authUsername) }
    var password by remember { mutableStateOf(currentConfig.authPassword) }
    var clipboardEnabled by remember { mutableStateOf(currentConfig.clipboardEnabled) }
    var readOnly by remember { mutableStateOf(currentConfig.readOnly) }
    var uploadDisabled by remember { mutableStateOf(currentConfig.uploadDisabled) }
    var remoteServerMode by remember { mutableStateOf(currentConfig.remoteServerModeEnabled) }
    var passwordVisible by remember { mutableStateOf(false) }
    var adminSecretVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentConfig.rootPath) {
        rootPath = currentConfig.rootPath
    }

    val sheetBg = if (isDarkTheme) Slate900 else LightSurface
    val textPrimary = if (isDarkTheme) Color.White else LightTextPrimary
    val textSecondary = if (isDarkTheme) Slate400 else LightTextSecondary
    val accentColor = if (isDarkTheme) CyanNeon else Color(0xFF0284C7)

    val parsedPort = portText.toIntOrNull() ?: currentConfig.port
    val hasChanges = parsedPort != currentConfig.port ||
            rootPath != currentConfig.rootPath ||
            authEnabled != currentConfig.basicAuthEnabled ||
            username != currentConfig.authUsername ||
            password != currentConfig.authPassword ||
            clipboardEnabled != currentConfig.clipboardEnabled ||
            readOnly != currentConfig.readOnly ||
            uploadDisabled != currentConfig.uploadDisabled ||
            remoteServerMode != currentConfig.remoteServerModeEnabled

    val scrollState = rememberScrollState()

    // When settings change while scrolled down, smoothly scroll to reveal the lower option above the hover bar
    LaunchedEffect(hasChanges) {
        if (hasChanges && scrollState.value > 80) {
            kotlinx.coroutines.delay(120)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
            Text(
                text = "SETTINGS & PREFERENCES",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = textPrimary
            )

            // Appearance Mode Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "APP APPEARANCE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold),
                    color = textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Dark Mode Option Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isDarkTheme) 2.dp else 1.dp,
                                color = if (isDarkTheme) accentColor else (if (isDarkTheme) Slate800 else LightBorder),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectDarkTheme(true) },
                        color = if (isDarkTheme) (if (isDarkTheme) Slate800 else LightSurfaceElevated) else (if (isDarkTheme) Slate900 else LightSurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = if (isDarkTheme) accentColor else textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Dark Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isDarkTheme) FontWeight.Bold else FontWeight.Normal),
                                color = if (isDarkTheme) textPrimary else textSecondary
                            )
                        }
                    }

                    // Light Mode Option Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (!isDarkTheme) 2.dp else 1.dp,
                                color = if (!isDarkTheme) accentColor else (if (isDarkTheme) Slate800 else LightBorder),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectDarkTheme(false) },
                        color = if (!isDarkTheme) (if (isDarkTheme) Slate800 else LightSurfaceElevated) else (if (isDarkTheme) Slate900 else LightSurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (!isDarkTheme) accentColor else textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Light Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (!isDarkTheme) FontWeight.Bold else FontWeight.Normal),
                                color = if (!isDarkTheme) textPrimary else textSecondary
                            )
                        }
                    }
                }
            }

            if (isServerRunning) {
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: Stop the active server before altering port, storage path, or auth parameters.",
                        color = Color(0xFFF59E0B),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Server Port
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it },
                label = { Text("Server Port") },
                singleLine = true,
                enabled = !isServerRunning,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedTextFieldColors(isDarkTheme)
            )

            // Storage Path
            OutlinedTextField(
                value = rootPath,
                onValueChange = { rootPath = it },
                label = { Text("Shared Storage Path") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedTextFieldColors(isDarkTheme)
            )

            // Scope Status Badge
            val targetFile = remember(rootPath) { java.io.File(rootPath) }
            val isSingleFile = targetFile.exists() && targetFile.isFile
            val isDefaultAll = rootPath.trimEnd('/') == defaultRoot.trimEnd('/')

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isDarkTheme) Slate800.copy(alpha = 0.6f) else LightSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate700 else LightBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = when {
                                    isSingleFile -> EmeraldOnline.copy(alpha = 0.15f)
                                    isDefaultAll -> CyanNeon.copy(alpha = 0.15f)
                                    else -> AmberWarn.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isSingleFile -> Icons.AutoMirrored.Filled.InsertDriveFile
                                isDefaultAll -> Icons.Default.PhoneAndroid
                                else -> Icons.Default.Folder
                            },
                            contentDescription = null,
                            tint = when {
                                isSingleFile -> EmeraldOnline
                                isDefaultAll -> CyanNeon
                                else -> AmberWarn
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isSingleFile -> "Specific File: ${targetFile.name}"
                                isDefaultAll -> "Entire Internal Storage"
                                else -> "Directory: ${targetFile.name}"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isSingleFile -> "${formatSettingsFileSize(targetFile.length())} • Only this file will be shared in browser"
                                isDefaultAll -> "All phone files and folders are accessible"
                                else -> "Only files inside this folder are accessible"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSingleFile) EmeraldOnline else textSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Storage Scope Selection (Single unified row with icons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Pick Folder
                ScopeActionChip(
                    label = "Pick Folder",
                    icon = Icons.Default.FolderOpen,
                    iconTint = CyanNeon,
                    isDarkTheme = isDarkTheme,
                    textPrimary = textPrimary,
                    onClick = onPickFolder
                )

                // 2. Pick File
                ScopeActionChip(
                    label = "Pick File",
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    iconTint = EmeraldOnline,
                    isDarkTheme = isDarkTheme,
                    textPrimary = textPrimary,
                    onClick = onPickFile
                )

                // 3. Preset Scopes
                val presets = listOf(
                    Triple("All Storage", defaultRoot, Icons.Default.Storage to AmberWarn),
                    Triple("Downloads", "$defaultRoot/Download", Icons.Default.Download to CyanNeon),
                    Triple("DCIM / Photos", "$defaultRoot/DCIM", Icons.Default.PhotoCamera to PurpleAccent),
                    Triple("Documents", "$defaultRoot/Documents", Icons.Default.Description to CyanNeon),
                    Triple("Movies", "$defaultRoot/Movies", Icons.Default.Movie to Color(0xFFFB923C)),
                    Triple("Music", "$defaultRoot/Music", Icons.Default.MusicNote to EmeraldLive)
                )

                presets.forEach { (label, path, iconInfo) ->
                    val (icon, tint) = iconInfo
                    val isSelected = rootPath.trimEnd('/') == path.trimEnd('/')
                    ScopePresetChip(
                        label = label,
                        icon = icon,
                        iconTint = tint,
                        isSelected = isSelected,
                        isDarkTheme = isDarkTheme,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = {
                            rootPath = path
                            if (path == defaultRoot) {
                                onSelectAllStorage()
                            }
                        }
                    )
                }
            }

            Text(
                text = "ACCESS & RESTRICTIONS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold),
                color = textSecondary
            )

            // Read Only Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (readOnly) accentColor.copy(alpha = 0.15f) else (if (isDarkTheme) Slate800 else LightSurfaceElevated),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditOff,
                            contentDescription = null,
                            tint = if (readOnly) accentColor else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Read Only",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary
                        )
                        Text(
                            text = "Prevents file rename and deletion from browser",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondary
                        )
                    }
                }

                Switch(
                    checked = readOnly,
                    onCheckedChange = { readOnly = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = textSecondary,
                        uncheckedTrackColor = if (isDarkTheme) Slate800 else LightBorder
                    )
                )
            }

            // Prevent File Upload
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (uploadDisabled) accentColor.copy(alpha = 0.15f) else (if (isDarkTheme) Slate800 else LightSurfaceElevated),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (uploadDisabled) accentColor else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Prevent File Upload",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary
                        )
                        Text(
                            text = "Blocks uploading files from web browsers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondary
                        )
                    }
                }

                Switch(
                    checked = uploadDisabled,
                    onCheckedChange = { uploadDisabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = textSecondary,
                        uncheckedTrackColor = if (isDarkTheme) Slate800 else LightBorder
                    )
                )
            }

            // Shared Clipboard Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (clipboardEnabled) accentColor.copy(alpha = 0.15f) else (if (isDarkTheme) Slate800 else LightSurfaceElevated),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = if (clipboardEnabled) accentColor else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Shared Clipboard",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary
                        )
                        Text(
                            text = "Two-way text sharing with website",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondary
                        )
                    }
                }

                Switch(
                    checked = clipboardEnabled,
                    onCheckedChange = { clipboardEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = textSecondary,
                        uncheckedTrackColor = if (isDarkTheme) Slate800 else LightBorder
                    )
                )
            }

            // Basic Authentication Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Require HTTP Basic Auth",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = textPrimary
                    )
                    Text(
                        text = "Prompts browser for credentials before access",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary
                    )
                }

                Switch(
                    checked = authEnabled,
                    onCheckedChange = { authEnabled = it },
                    enabled = !isServerRunning,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = textSecondary,
                        uncheckedTrackColor = if (isDarkTheme) Slate800 else LightBorder
                    )
                )
            }

            if (authEnabled) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Auth Username") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = accentColor)
                    },
                    singleLine = true,
                    enabled = !isServerRunning,
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedTextFieldColors(isDarkTheme)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Auth Password") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = accentColor)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = textSecondary
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !isServerRunning,
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedTextFieldColors(isDarkTheme)
                )
            }

            // Remote Server Mode (Mode B - Optional Advanced Feature)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (remoteServerMode) accentColor.copy(alpha = 0.15f) else (if (isDarkTheme) Slate800 else LightSurfaceElevated),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = if (remoteServerMode) accentColor else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Remote Server Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary
                        )
                        Text(
                            text = "Designates this phone as a remotely manageable storage host",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondary
                        )
                    }
                }

                Switch(
                    checked = remoteServerMode,
                    onCheckedChange = { remoteServerMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = textSecondary,
                        uncheckedTrackColor = if (isDarkTheme) Slate800 else LightBorder
                    )
                )
            }

            if (remoteServerMode) {
                Surface(
                    color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "HOST REGISTRATION CREDENTIALS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = accentColor
                        )
                        Text(
                            text = "Device ID: ${currentConfig.hostDeviceId.ifBlank { "wormhole-host" }}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            val displaySecret = currentConfig.adminSecret.ifBlank { "stark_admin_2026" }

                            Text(
                                text = "Admin Secret: ${if (adminSecretVisible) displaySecret else "••••••••••••••••••••"}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { adminSecretVisible = !adminSecretVisible }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = if (adminSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(displaySecret))
                                android.widget.Toast.makeText(context, "Admin secret copied", android.widget.Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Secret",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenShareManagement,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Manage Token Shares", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Administrator Remote Control Entry Point (Phone A -> Phone B)
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenAdminLogin()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate700 else LightBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Login as Admin (Manage Remote Host)", color = textPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            // Dynamic bottom clearance spacer so floating Apply button never obscures any controls
            val bottomSpacerHeight by animateDpAsState(
                targetValue = if (hasChanges) 100.dp else 24.dp,
                label = "settingsBottomClearance"
            )
            Spacer(modifier = Modifier.height(bottomSpacerHeight))
        }

        // Hovering Floating Apply Configuration Bar
        androidx.compose.animation.AnimatedVisibility(
            visible = hasChanges,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                shape = RoundedCornerShape(16.dp),
                color = (if (isDarkTheme) Color(0xFF0F172A) else Color.White).copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.8f)),
                shadowElevation = 14.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AmberWarn)
                            )
                            Text(
                                text = "Unsaved Changes",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                        }
                        Text(
                            text = "Tap Apply to update settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }

                    Button(
                        onClick = {
                            onSaveConfig(
                                currentConfig.copy(
                                    port = parsedPort,
                                    rootPath = rootPath,
                                    basicAuthEnabled = authEnabled,
                                    authUsername = username,
                                    authPassword = password,
                                    clipboardEnabled = clipboardEnabled,
                                    readOnly = readOnly,
                                    uploadDisabled = uploadDisabled,
                                    remoteServerModeEnabled = remoteServerMode
                                )
                            )
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            contentColor = Color(0xFF020617)
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Apply",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun outlinedTextFieldColors(isDarkTheme: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = if (isDarkTheme) CyanNeon else Color(0xFF0284C7),
    unfocusedBorderColor = if (isDarkTheme) Slate700 else LightBorder,
    focusedTextColor = if (isDarkTheme) Color.White else LightTextPrimary,
    unfocusedTextColor = if (isDarkTheme) Color.White else LightTextPrimary,
    disabledBorderColor = if (isDarkTheme) Slate800 else LightBorder,
    disabledTextColor = if (isDarkTheme) Slate400 else LightTextSecondary
)

private fun formatSettingsFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Composable
private fun ScopeActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isDarkTheme: Boolean,
    textPrimary: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkTheme) Slate800 else LightSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate700 else LightBorder),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = iconTint
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun ScopePresetChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        CyanNeon.copy(alpha = 0.18f)
    } else {
        if (isDarkTheme) Slate800 else LightSurfaceElevated
    }
    val borderColor = if (isSelected) {
        CyanNeon
    } else {
        if (isDarkTheme) Slate700 else LightBorder
    }
    val labelColor = if (isSelected) {
        if (isDarkTheme) CyanNeon else Color(0xFF0284C7)
    } else {
        textSecondary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) (if (isDarkTheme) CyanNeon else Color(0xFF0284C7)) else iconTint
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
