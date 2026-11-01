package com.jarvis.fileserver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.jarvis.fileserver.model.AdminStatusResponse
import com.jarvis.fileserver.ui.theme.CyanNeon
import com.jarvis.fileserver.ui.theme.EmeraldOnline
import com.jarvis.fileserver.ui.theme.LightBorder
import com.jarvis.fileserver.ui.theme.LightSurface
import com.jarvis.fileserver.ui.theme.LightTextPrimary
import com.jarvis.fileserver.ui.theme.LightTextSecondary
import com.jarvis.fileserver.ui.theme.Slate400
import com.jarvis.fileserver.ui.theme.Slate700
import com.jarvis.fileserver.ui.theme.Slate800
import com.jarvis.fileserver.ui.theme.Slate900
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginBottomSheet(
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetBg = if (isDarkTheme) Slate900 else LightSurface
    val textPrimary = if (isDarkTheme) Color.White else LightTextPrimary
    val textSecondary = if (isDarkTheme) Slate400 else LightTextSecondary
    val scope = rememberCoroutineScope()

    var hostUrl by remember { mutableStateOf("") }
    var adminToken by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var remoteStatus by remember { mutableStateOf<AdminStatusResponse?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (remoteStatus != null) "Remote Host Console" else "Login as Admin",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textSecondary
                    )
                }
            }

            Text(
                text = if (remoteStatus != null)
                    "Connected to remote storage host. You can monitor performance and create shares."
                else
                    "Connect this phone (Phone A) to your remote storage host (Phone B) to manage shares and monitor status.",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (remoteStatus == null) {
                // Connection Form
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = hostUrl,
                        onValueChange = { hostUrl = it },
                        label = { Text("Remote Host URL") },
                        placeholder = { Text("e.g. https://*.trycloudflare.com or http://192.168.1.50:8080") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = if (isDarkTheme) Slate700 else LightBorder,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        )
                    )

                    OutlinedTextField(
                        value = adminToken,
                        onValueChange = { adminToken = it },
                        label = { Text("Admin Secret / Bearer Token") },
                        placeholder = { Text("Copy from Phone B's Settings") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(
                                    imageVector = if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle token visibility",
                                    tint = textSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = if (isDarkTheme) Slate700 else LightBorder,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        )
                    )

                    errorMessage?.let { msg ->
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = Color(0xFFFCA5A5),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (hostUrl.isBlank() || adminToken.isBlank()) {
                                errorMessage = "Please enter both Host URL and Admin Secret"
                                return@Button
                            }
                            isConnecting = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val cleanUrl = hostUrl.trim().removeSuffix("/")
                                    val statusUrl = "$cleanUrl/api/admin/status"
                                    val result = withContext(Dispatchers.IO) {
                                        val conn = (URL(statusUrl).openConnection() as HttpURLConnection).apply {
                                            requestMethod = "GET"
                                            setRequestProperty("Authorization", "Bearer ${adminToken.trim()}")
                                            connectTimeout = 8000
                                            readTimeout = 8000
                                        }
                                        val code = conn.responseCode
                                        if (code == 200) {
                                            val body = conn.inputStream.bufferedReader().use { it.readText() }
                                            val json = Json { ignoreUnknownKeys = true }
                                            json.decodeFromString<AdminStatusResponse>(body)
                                        } else {
                                            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                                            throw Exception("Authentication failed: $err")
                                        }
                                    }
                                    remoteStatus = result
                                } catch (e: Exception) {
                                    errorMessage = "Connection error: ${e.localizedMessage}"
                                } finally {
                                    isConnecting = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isConnecting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF020617), strokeWidth = 2.dp)
                        } else {
                            Text("Connect to Remote Host", color = Color(0xFF020617), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Connected State: Telemetry Console
                val st = remoteStatus!!
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Host Info Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = st.deviceName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "ID: ${st.deviceId} • Uptime: ${formatUptime(st.uptimeSeconds)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                            Surface(
                                color = EmeraldOnline.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "ONLINE",
                                    color = EmeraldOnline,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Telemetry Grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TelemetryCard(
                            icon = if (st.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                            label = "Battery",
                            value = "${st.batteryPct}% ${if (st.isCharging) "(Charging)" else ""}",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            icon = Icons.Default.Thermostat,
                            label = "Temperature",
                            value = if (st.batteryTempCelsius > 0) "%.1f°C".format(st.batteryTempCelsius) else "N/A",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TelemetryCard(
                            icon = Icons.Default.People,
                            label = "Active Viewers",
                            value = "${st.activeViewersCount} viewer(s)",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            icon = Icons.Default.Wifi,
                            label = "Network",
                            value = st.networkType,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TelemetryCard(
                            icon = Icons.Default.Speed,
                            label = "Outbound Speed",
                            value = formatSpeed(st.uploadSpeedBps),
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            icon = Icons.Default.Link,
                            label = "Active Shares",
                            value = "${st.activeSharesCount} share(s)",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = { remoteStatus = null },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDarkTheme) Slate800 else Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disconnect from Host", color = textPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = Slate400)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
}

private fun formatSpeed(bps: Long): String {
    if (bps < 1024) return "$bps B/s"
    val kb = bps / 1024.0
    if (kb < 1024) return "%.1f KB/s".format(kb)
    val mb = kb / 1024.0
    return "%.2f MB/s".format(mb)
}

private fun formatUptime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
