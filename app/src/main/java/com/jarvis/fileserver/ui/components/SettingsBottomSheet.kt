package com.jarvis.fileserver.ui.components

import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.fileserver.model.ServerConfig
import com.jarvis.fileserver.ui.theme.CyanNeon
import com.jarvis.fileserver.ui.theme.EmeraldLive
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
    isServerRunning: Boolean,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSaveConfig: (ServerConfig) -> Unit,
    onPickDirectory: () -> Unit,
    onPickFile: () -> Unit,
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
    var passwordVisible by remember { mutableStateOf(false) }

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
                    Button(
                        onClick = { onToggleDarkTheme(true) },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) CyanNeon else (if (isDarkTheme) Slate800 else LightSurfaceElevated),
                            contentColor = if (isDarkTheme) Slate900 else textPrimary
                        ),
                        border = if (!isDarkTheme) BorderStroke(1.dp, LightBorder) else null
                    ) {
                        Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dark Mode", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onToggleDarkTheme(false) },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDarkTheme) Color(0xFF0284C7) else Slate800,
                            contentColor = if (!isDarkTheme) Color.White else textPrimary
                        ),
                        border = if (isDarkTheme) BorderStroke(1.dp, Slate700) else null
                    ) {
                        Icon(imageVector = Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Light Mode", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isServerRunning) {
                Text(
                    text = "Note: Stop the active server before altering port, storage path, or auth parameters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) CyanNeon else Color(0xFF0284C7)
                )
            }

            // Port Configuration
            OutlinedTextField(
                value = portText,
                onValueChange = { if (it.length <= 5) portText = it },
                label = { Text("Server Port") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Numbers, contentDescription = null, tint = accentColor)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !isServerRunning,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedTextFieldColors(isDarkTheme)
            )

            // Storage Root Directory
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rootPath,
                    onValueChange = { rootPath = it },
                    label = { Text("Shared Folder / File Path") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = accentColor)
                    },
                    singleLine = true,
                    enabled = !isServerRunning,
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedTextFieldColors(isDarkTheme)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickDirectory,
                        modifier = Modifier.weight(1f).height(38.dp),
                        enabled = !isServerRunning
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp), tint = accentColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Folder", fontSize = 12.sp, color = textPrimary)
                    }

                    OutlinedButton(
                        onClick = onPickFile,
                        modifier = Modifier.weight(1f).height(38.dp),
                        enabled = !isServerRunning
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldLive)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick File", fontSize = 12.sp, color = textPrimary)
                    }

                    OutlinedButton(
                        onClick = { rootPath = Environment.getExternalStorageDirectory().absolutePath },
                        modifier = Modifier.height(38.dp),
                        enabled = !isServerRunning
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All", fontSize = 12.sp, color = textPrimary)
                    }
                }
            }

            // Security & Restrictions Section
            Text(
                text = "ACCESS & RESTRICTIONS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold),
                color = textSecondary
            )

            // Read Only Mode Toggle
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

            // Prevent File Upload Toggle
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

            Spacer(modifier = Modifier.height(8.dp))

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
                            uploadDisabled = uploadDisabled
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
                    color = if (isDarkTheme) Slate900 else Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun outlinedTextFieldColors(isDarkTheme: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = if (isDarkTheme) CyanNeon else Color(0xFF0284C7),
    unfocusedBorderColor = if (isDarkTheme) Slate700 else LightBorder,
    focusedLabelColor = if (isDarkTheme) CyanNeon else Color(0xFF0284C7),
    unfocusedLabelColor = if (isDarkTheme) Slate400 else LightTextSecondary,
    cursorColor = if (isDarkTheme) CyanNeon else Color(0xFF0284C7),
    focusedTextColor = if (isDarkTheme) Color.White else LightTextPrimary,
    unfocusedTextColor = if (isDarkTheme) Color.White else LightTextPrimary,
    disabledTextColor = if (isDarkTheme) Slate400 else LightTextSecondary,
    disabledBorderColor = if (isDarkTheme) Slate800 else LightBorder
)
