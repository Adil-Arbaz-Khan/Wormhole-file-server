package com.jarvis.fileserver.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.jarvis.fileserver.ui.theme.EmeraldOnline
import com.jarvis.fileserver.ui.theme.LightBorder
import com.jarvis.fileserver.ui.theme.LightSurface
import com.jarvis.fileserver.ui.theme.LightSurfaceElevated
import com.jarvis.fileserver.ui.theme.LightTextPrimary
import com.jarvis.fileserver.ui.theme.LightTextSecondary
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

    val sheetBg = if (isDarkTheme) Slate900 else LightSurface
    val textPrimary = if (isDarkTheme) Color.White else LightTextPrimary
    val textSecondary = if (isDarkTheme) Slate400 else LightTextSecondary
    val accentColor = if (isDarkTheme) CyanNeon else Color(0xFF0284C7)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
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
                enabled = !isServerRunning,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedTextFieldColors(isDarkTheme)
            )

            // Storage Quick Scope Buttons (Equal, uniform 3-tile row with no text wrapping)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPickFolder,
                    enabled = !isServerRunning,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Slate800 else LightSurfaceElevated,
                        contentColor = textPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate700 else LightBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = CyanNeon
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pick Folder",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Button(
                    onClick = onPickFile,
                    enabled = !isServerRunning,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Slate800 else LightSurfaceElevated,
                        contentColor = textPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate700 else LightBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = EmeraldOnline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pick File",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Button(
                    onClick = onSelectAllStorage,
                    enabled = !isServerRunning,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Slate800 else LightSurfaceElevated,
                        contentColor = textPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate700 else LightBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AmberWarn
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "All Storage",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
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

            Spacer(modifier = Modifier.height(4.dp))

            // Save Button
            Button(
                onClick = {
                    val parsedPort = portText.toIntOrNull() ?: 8080
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(
                    text = "Apply Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF020617)
                )
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
