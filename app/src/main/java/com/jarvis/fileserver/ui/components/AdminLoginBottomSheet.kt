package com.jarvis.fileserver.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.fileserver.data.ShareRecord
import com.jarvis.fileserver.model.AdminSecuritySettingsRequest
import com.jarvis.fileserver.model.AdminStatusResponse
import com.jarvis.fileserver.model.CreateShareRequest
import com.jarvis.fileserver.model.DirectoryListingResponse
import com.jarvis.fileserver.model.FileItemDto
import com.jarvis.fileserver.model.OperationResponse
import com.jarvis.fileserver.model.ShareCreatedResponse
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val jsonParser = remember { Json { ignoreUnknownKeys = true } }

    var hostUrl by remember { mutableStateOf("") }
    var adminToken by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var remoteStatus by remember { mutableStateOf<AdminStatusResponse?>(null) }

    // Active Admin Console Tabs: 0 -> Remote Files, 1 -> Active Shares, 2 -> Host Control & Telemetry
    var selectedTab by remember { mutableIntStateOf(0) }

    // Remote File Browser State
    var currentBrowsePath by remember { mutableStateOf("/") }
    var browseItems by remember { mutableStateOf<List<FileItemDto>>(emptyList()) }
    var isLoadingBrowse by remember { mutableStateOf(false) }
    var browseError by remember { mutableStateOf<String?>(null) }

    // Active Shares State
    var sharesList by remember { mutableStateOf<List<ShareRecord>>(emptyList()) }
    var isLoadingShares by remember { mutableStateOf(false) }

    // Security Settings State
    var remoteReadOnly by remember { mutableStateOf(false) }
    var remoteUploadDisabled by remember { mutableStateOf(false) }
    var isUpdatingSecurity by remember { mutableStateOf(false) }

    // Create Share Dialog State
    var itemToShare by remember { mutableStateOf<FileItemDto?>(null) }
    var shareCustomName by remember { mutableStateOf("") }
    var sharePassword by remember { mutableStateOf("") }
    var shareAllowDownload by remember { mutableStateOf(true) }
    var isCreatingShare by remember { mutableStateOf(false) }
    var generatedShareUrl by remember { mutableStateOf<String?>(null) }

    fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim().removeSuffix("/")
        return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            if (trimmed.contains("trycloudflare.com")) "https://$trimmed" else "http://$trimmed"
        } else {
            trimmed
        }
    }

    // HTTP Helper function
    suspend fun executeAdminRequest(
        subPath: String,
        method: String = "GET",
        jsonBody: String? = null
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(hostUrl)
        val fullUrl = "$base$subPath"
        val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer ${adminToken.trim()}")
            if (jsonBody != null) {
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            connectTimeout = 8000
            readTimeout = 8000
        }

        if (jsonBody != null) {
            conn.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
            }
        }

        val code = conn.responseCode
        val body = if (code in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
        }
        Pair(code, body)
    }

    // Refresh directory listing
    fun loadDirectory(path: String) {
        isLoadingBrowse = true
        browseError = null
        scope.launch {
            try {
                val encoded = URLEncoder.encode(path, "UTF-8")
                val (code, body) = executeAdminRequest("/api/admin/browse?path=$encoded")
                if (code == 200) {
                    val resp = jsonParser.decodeFromString<DirectoryListingResponse>(body)
                    browseItems = resp.items
                    currentBrowsePath = resp.currentPath
                } else {
                    browseError = "Failed to list folder: HTTP $code"
                }
            } catch (e: Exception) {
                browseError = e.localizedMessage ?: "Network error"
            } finally {
                isLoadingBrowse = false
            }
        }
    }

    // Refresh active shares
    fun loadShares() {
        isLoadingShares = true
        scope.launch {
            try {
                val (code, body) = executeAdminRequest("/api/admin/shares")
                if (code == 200) {
                    sharesList = jsonParser.decodeFromString<List<ShareRecord>>(body)
                }
            } catch (_: Exception) {
            } finally {
                isLoadingShares = false
            }
        }
    }

    // Revoke a share
    fun revokeShare(shareId: String) {
        scope.launch {
            try {
                val (code, _) = executeAdminRequest("/api/admin/shares/$shareId", method = "DELETE")
                if (code == 200) {
                    Toast.makeText(context, "Access revoked immediately", Toast.LENGTH_SHORT).show()
                    loadShares()
                } else {
                    Toast.makeText(context, "Failed to revoke: HTTP $code", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Update Security Policy (Remote Toggles)
    fun updateSecurity(readOnly: Boolean, uploadDisabled: Boolean) {
        isUpdatingSecurity = true
        scope.launch {
            try {
                val payload = jsonParser.encodeToString(AdminSecuritySettingsRequest(readOnly, uploadDisabled))
                val (code, _) = executeAdminRequest("/api/admin/security", method = "POST", jsonBody = payload)
                if (code == 200) {
                    remoteReadOnly = readOnly
                    remoteUploadDisabled = uploadDisabled
                    Toast.makeText(context, "Host security policy updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Update failed: HTTP $code", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdatingSecurity = false
            }
        }
    }

    // Initial load when connected
    LaunchedEffect(remoteStatus) {
        if (remoteStatus != null) {
            remoteReadOnly = remoteStatus!!.readOnly
            remoteUploadDisabled = remoteStatus!!.uploadDisabled
            loadDirectory("/")
            loadShares()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (remoteStatus != null) Modifier.fillMaxHeight(0.92f) else Modifier)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
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

            if (remoteStatus == null) {
                // ==================== LOGIN FORM ====================
                Text(
                    text = "Connect this phone (Phone A) to your remote storage host (Phone B) to manage shares and monitor status.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = hostUrl,
                        onValueChange = { hostUrl = it },
                        label = { Text("Remote Host URL") },
                        placeholder = { Text("e.g. https://*.trycloudflare.com or http://192.168.1.50:8080") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (hostUrl.isEmpty()) {
                                IconButton(onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        hostUrl = clip.trim()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste URL",
                                        tint = CyanNeon,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
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
                        placeholder = { Text("Enter stark_admin_2026 or paste from Host") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row {
                                if (adminToken.isEmpty()) {
                                    IconButton(onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            adminToken = clip.trim()
                                        } else {
                                            adminToken = "stark_admin_2026"
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste Secret",
                                            tint = CyanNeon,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                    Icon(
                                        imageVector = if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle token visibility",
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        supportingText = {
                            Text(
                                text = "Default master key: stark_admin_2026",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanNeon
                            )
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
                                    val base = normalizeUrl(hostUrl)
                                    val statusUrl = "$base/api/admin/status"
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
                                            jsonParser.decodeFromString<AdminStatusResponse>(body)
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
                // ==================== CONNECTED ADMIN CONSOLE ====================
                val st = remoteStatus!!

                // Top Host Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDarkTheme) Color(0xFF0F172A) else LightSurfaceElevated,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(EmeraldOnline, CircleShape))
                                Text(
                                    text = st.deviceName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimary
                                )
                            }
                            Text(
                                text = "Host: ${st.deviceId} • Up: ${formatUptime(st.uptimeSeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }

                        OutlinedButton(
                            onClick = { remoteStatus = null },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate700 else LightBorder)
                        ) {
                            Text("Disconnect", fontSize = 12.sp, color = textSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                val tabs = listOf("📁 Remote Files", "🔗 Active Shares", "🛡️ Host Control")
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = CyanNeon,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CyanNeon,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                if (index == 0 && browseItems.isEmpty()) loadDirectory(currentBrowsePath)
                                if (index == 1) loadShares()
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == index) CyanNeon else textSecondary
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // ==================== TAB 0: REMOTE FILE BROWSER ====================
                        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            // Path Breadcrumb & Navigation Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (currentBrowsePath != "/" && currentBrowsePath.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val parent = currentBrowsePath.substringBeforeLast('/', "").ifBlank { "/" }
                                                loadDirectory(parent)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go up", tint = CyanNeon)
                                        }
                                    }
                                    Text(
                                        text = currentBrowsePath,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { loadDirectory(currentBrowsePath) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = textSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isLoadingBrowse) {
                                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = CyanNeon)
                                }
                            } else if (browseError != null) {
                                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    Text(browseError!!, color = Color(0xFFFCA5A5), fontSize = 13.sp)
                                }
                            } else if (browseItems.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    Text("This folder is empty", color = textSecondary, fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(browseItems) { item ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    if (item.isDirectory) {
                                                        loadDirectory(item.relativePath)
                                                    } else {
                                                        itemToShare = item
                                                        shareCustomName = item.name
                                                    }
                                                },
                                            color = if (isDarkTheme) Color(0xFF0F172A) else LightSurfaceElevated,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                                        contentDescription = null,
                                                        tint = if (item.isDirectory) AmberWarn else CyanNeon,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = item.name,
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                            color = textPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = if (item.isDirectory) "Folder" else formatBytes(item.size),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = textSecondary
                                                        )
                                                    }
                                                }

                                                // Create Share Link Button
                                                OutlinedButton(
                                                    onClick = {
                                                        itemToShare = item
                                                        shareCustomName = item.name
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f))
                                                ) {
                                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Share Link", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // ==================== TAB 1: ACTIVE SHARES & ACCESS CONTROL ====================
                        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CONTROL CONNECTED ACCESS (${sharesList.size} active)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                    color = CyanNeon
                                )
                                IconButton(onClick = { loadShares() }, modifier = Modifier.size(28.dp)) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = textSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isLoadingShares) {
                                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = CyanNeon)
                                }
                            } else if (sharesList.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = textSecondary, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No active shared links", color = textSecondary, fontSize = 13.sp)
                                        Text("Select any file or folder from 'Remote Files' to share", color = textSecondary, fontSize = 11.sp)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(sharesList) { share ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = if (isDarkTheme) Color(0xFF0F172A) else LightSurfaceElevated,
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (share.isRevoked) Color(0xFFEF4444).copy(alpha = 0.3f) else (if (isDarkTheme) Slate800 else LightBorder)
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                                        Icon(
                                                            imageVector = if (share.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                                            contentDescription = null,
                                                            tint = if (share.isDirectory) AmberWarn else CyanNeon,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Text(
                                                            text = share.name,
                                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = textPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    // Status Badges
                                                    if (share.isRevoked) {
                                                        Surface(color = Color(0xFFEF4444).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                                            Text("REVOKED", color = Color(0xFFFCA5A5), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                    } else {
                                                        Surface(color = EmeraldOnline.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                                            Text("${share.accessCount} visit(s)", color = EmeraldOnline, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                }

                                                // URL and Badges Row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "/s/${share.token}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                        color = textSecondary,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (share.isPasswordProtected) {
                                                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Password protected", tint = AmberWarn, modifier = Modifier.size(14.dp))
                                                    }
                                                    if (!share.allowDownload) {
                                                        Text("Stream-only", fontSize = 10.sp, color = textSecondary)
                                                    }
                                                }

                                                // Action Buttons Row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Copy Public URL
                                                    OutlinedButton(
                                                        onClick = {
                                                            val base = normalizeUrl(hostUrl)
                                                            val publicUrl = "$base/s/${share.token}"
                                                            clipboardManager.setText(AnnotatedString(publicUrl))
                                                            Toast.makeText(context, "Share link copied to clipboard", Toast.LENGTH_SHORT).show()
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f))
                                                    ) {
                                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Copy Link", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    // Revoke / Block Access Button
                                                    if (!share.isRevoked) {
                                                        Button(
                                                            onClick = { revokeShare(share.id) },
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier.weight(1f),
                                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.85f))
                                                        ) {
                                                            Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Revoke Access", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // ==================== TAB 2: HOST CONTROL & TELEMETRY ====================
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Remote Host Security Policy Toggles
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isDarkTheme) Color(0xFF0F172A) else LightSurfaceElevated,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "REMOTE SECURITY POLICIES",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                        color = CyanNeon
                                    )

                                    // Read-Only Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Enforce Read-Only Mode", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = textPrimary)
                                            Text("Blocks connected users from deleting or renaming files", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                                        }
                                        Switch(
                                            checked = remoteReadOnly,
                                            onCheckedChange = { updateSecurity(it, remoteUploadDisabled) },
                                            enabled = !isUpdatingSecurity,
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CyanNeon)
                                        )
                                    }

                                    // Upload Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Block Visitor Uploads", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = textPrimary)
                                            Text("Prevents connected devices from uploading files", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                                        }
                                        Switch(
                                            checked = remoteUploadDisabled,
                                            onCheckedChange = { updateSecurity(remoteReadOnly, it) },
                                            enabled = !isUpdatingSecurity,
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CyanNeon)
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
                                    value = "${st.activeViewersCount} visitor(s)",
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
                        }
                    }
                }
            }
        }
    }

    // ==================== CREATE SHARE LINK DIALOG ====================
    itemToShare?.let { targetItem ->
        AlertDialog(
            onDismissRequest = {
                if (!isCreatingShare) {
                    itemToShare = null
                    generatedShareUrl = null
                    sharePassword = ""
                }
            },
            title = {
                Text(
                    text = if (generatedShareUrl != null) "Share Link Created" else "Create Share Link",
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (generatedShareUrl != null) {
                        Surface(
                            color = EmeraldOnline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Public Access URL:", fontSize = 11.sp, color = EmeraldOnline, fontWeight = FontWeight.Bold)
                                Text(
                                    text = generatedShareUrl!!,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = textPrimary
                                )
                            }
                        }
                        Text(
                            text = "Connected devices can open this URL directly in any web browser without needing server login.",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    } else {
                        Text(
                            text = "Generate a secure tokenized link for: ${targetItem.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )

                        OutlinedTextField(
                            value = shareCustomName,
                            onValueChange = { shareCustomName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon)
                        )

                        OutlinedTextField(
                            value = sharePassword,
                            onValueChange = { sharePassword = it },
                            label = { Text("Password Protection (Optional)") },
                            placeholder = { Text("Leave blank for public access") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Allow File Download", style = MaterialTheme.typography.bodyMedium, color = textPrimary)
                                Text(
                                    text = if (shareAllowDownload) "Full download permitted" else "Stream / preview only",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSecondary
                                )
                            }
                            Switch(
                                checked = shareAllowDownload,
                                onCheckedChange = { shareAllowDownload = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CyanNeon)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (generatedShareUrl != null) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(generatedShareUrl!!))
                            Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                            itemToShare = null
                            generatedShareUrl = null
                            sharePassword = ""
                            loadShares()
                            selectedTab = 1
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF020617), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Link & Done", color = Color(0xFF020617), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            isCreatingShare = true
                            scope.launch {
                                try {
                                    val req = CreateShareRequest(
                                        path = targetItem.relativePath,
                                        name = shareCustomName.ifBlank { targetItem.name },
                                        password = sharePassword.ifBlank { null },
                                        allowDownload = shareAllowDownload
                                    )
                                    val payload = jsonParser.encodeToString(req)
                                    val (code, body) = executeAdminRequest("/api/admin/shares", method = "POST", jsonBody = payload)
                                    if (code in 200..201) {
                                        val resp = jsonParser.decodeFromString<ShareCreatedResponse>(body)
                                        val base = normalizeUrl(hostUrl)
                                        generatedShareUrl = "$base/s/${resp.share?.token ?: ""}"
                                    } else {
                                        Toast.makeText(context, "Failed: HTTP $code", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isCreatingShare = false
                                }
                            }
                        },
                        enabled = !isCreatingShare,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        if (isCreatingShare) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF020617), strokeWidth = 2.dp)
                        } else {
                            Text("Generate Share Link", color = Color(0xFF020617), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                if (generatedShareUrl == null) {
                    OutlinedButton(
                        onClick = { itemToShare = null },
                        enabled = !isCreatingShare
                    ) {
                        Text("Cancel")
                    }
                }
            },
            containerColor = sheetBg
        )
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
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate400)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatUptime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return "%02d:%02d:%02d".format(hrs, mins, secs)
}

private fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 B/s"
    val kb = bytesPerSec / 1024.0
    if (kb < 1024) return "%.1f KB/s".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB/s".format(mb)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}
