package com.jarvis.fileserver.ui.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.fileserver.R
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
import com.jarvis.fileserver.ui.theme.LightBg
import com.jarvis.fileserver.ui.theme.LightBorder
import com.jarvis.fileserver.ui.theme.LightSurface
import com.jarvis.fileserver.ui.theme.LightSurfaceElevated
import com.jarvis.fileserver.ui.theme.LightTextPrimary
import com.jarvis.fileserver.ui.theme.LightTextSecondary
import com.jarvis.fileserver.ui.theme.Slate700
import com.jarvis.fileserver.ui.theme.Slate800
import com.jarvis.fileserver.ui.theme.Slate900
import com.jarvis.fileserver.ui.theme.Slate950
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Full First-Class Screen for Remote Host Control and Management.
 * Replaces the temporary bottom sheet with a persistent main page
 * complete with a corner Logout button to return to standard sharing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteHostControlScreen(
    onLogout: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val textPrimary = if (isDarkTheme) Color.White else LightTextPrimary
    val textSecondary = if (isDarkTheme) Color(0xFF94A3B8) else LightTextSecondary
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val jsonParser = remember { Json { ignoreUnknownKeys = true } }

    val prefs = remember { context.getSharedPreferences("wormhole_admin_prefs", Context.MODE_PRIVATE) }
    var hostUrl by remember { mutableStateOf(prefs.getString("saved_host_url", "") ?: "") }
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
        bodyJson: String? = null
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(hostUrl)
        val fullUrl = "$base$subPath"
        val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Authorization", "Bearer ${adminToken.trim()}")
            setRequestProperty("Accept", "application/json")
            if (bodyJson != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(bodyJson.toByteArray()) }
            }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        Pair(code, text)
    }

    // Load Directory Items from Remote Host
    fun loadDirectory(path: String) {
        scope.launch {
            isLoadingBrowse = true
            browseError = null
            try {
                val encoded = URLEncoder.encode(path, "UTF-8")
                val (code, responseText) = executeAdminRequest("/api/admin/browse?path=$encoded")
                if (code == 200) {
                    val listing = jsonParser.decodeFromString<DirectoryListingResponse>(responseText)
                    browseItems = listing.items.sortedWith(compareBy<FileItemDto> { !it.isDirectory }.thenBy { it.name.lowercase() })
                    currentBrowsePath = path
                } else {
                    browseError = "Failed to load directory ($code): $responseText"
                }
            } catch (e: Exception) {
                browseError = "Browse error: ${e.localizedMessage ?: "Connection error"}"
            } finally {
                isLoadingBrowse = false
            }
        }
    }

    // Load Active Shares from Remote Host
    fun loadShares() {
        scope.launch {
            isLoadingShares = true
            try {
                val (code, responseText) = executeAdminRequest("/api/admin/shares")
                if (code == 200) {
                    sharesList = jsonParser.decodeFromString<List<ShareRecord>>(responseText)
                }
            } catch (e: Exception) {
                // ignore or toast
            } finally {
                isLoadingShares = false
            }
        }
    }

    // Revoke an Active Share
    fun revokeShare(shareId: String) {
        scope.launch {
            try {
                val (code, _) = executeAdminRequest("/api/admin/shares/$shareId", method = "DELETE")
                if (code == 200) {
                    Toast.makeText(context, "Access revoked successfully", Toast.LENGTH_SHORT).show()
                    loadShares()
                } else {
                    Toast.makeText(context, "Failed to revoke share ($code)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Revoke error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Update Remote Security Policies
    fun updateSecuritySettings(newReadOnly: Boolean, newUploadDisabled: Boolean) {
        scope.launch {
            isUpdatingSecurity = true
            try {
                val req = AdminSecuritySettingsRequest(readOnly = newReadOnly, uploadDisabled = newUploadDisabled)
                val body = jsonParser.encodeToString(req)
                val (code, _) = executeAdminRequest("/api/admin/security", method = "POST", bodyJson = body)
                if (code == 200) {
                    remoteReadOnly = newReadOnly
                    remoteUploadDisabled = newUploadDisabled
                    Toast.makeText(context, "Host security policy updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to update security ($code)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Security update error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdatingSecurity = false
            }
        }
    }

    // Create a new Share Link on the Remote Host
    fun submitCreateShare() {
        val target = itemToShare ?: return
        scope.launch {
            isCreatingShare = true
            try {
                val req = CreateShareRequest(
                    path = target.relativePath,
                    name = shareCustomName.ifBlank { target.name },
                    password = sharePassword.ifBlank { null },
                    allowDownload = shareAllowDownload
                )
                val body = jsonParser.encodeToString(req)
                val (code, responseText) = executeAdminRequest("/api/admin/shares", method = "POST", bodyJson = body)
                if (code == 200) {
                    val created = jsonParser.decodeFromString<ShareCreatedResponse>(responseText)
                    val base = normalizeUrl(hostUrl)
                    generatedShareUrl = if (created.shareUrl.isNotBlank()) {
                        if (created.shareUrl.startsWith("http")) created.shareUrl else "$base${created.shareUrl}"
                    } else if (created.share != null) {
                        "$base/s/${created.share.token}"
                    } else {
                        base
                    }
                    loadShares()
                } else {
                    Toast.makeText(context, "Failed to create share ($code)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Creation error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isCreatingShare = false
            }
        }
    }

    fun attemptLogin() {
        if (hostUrl.isBlank() || adminToken.isBlank()) {
            errorMessage = "Please enter both Host URL and Admin Secret"
            return
        }

        val normalized = normalizeUrl(hostUrl)
        isConnecting = true
        errorMessage = null

        scope.launch {
            try {
                val (code, responseText) = executeAdminRequest("/api/admin/status")
                if (code == 200) {
                    val status = jsonParser.decodeFromString<AdminStatusResponse>(responseText)
                    remoteStatus = status
                    prefs.edit().putString("saved_host_url", normalized).apply()
                    Toast.makeText(context, "Connected to ${status.deviceName}", Toast.LENGTH_SHORT).show()
                } else if (code == 401) {
                    errorMessage = "Unauthorized admin access: Invalid secret or token."
                } else {
                    errorMessage = "Host returned HTTP error $code: $responseText"
                }
            } catch (e: Exception) {
                errorMessage = "Connection error: ${e.localizedMessage ?: "Failed to reach host"}"
            } finally {
                isConnecting = false
            }
        }
    }

    LaunchedEffect(remoteStatus) {
        if (remoteStatus != null) {
            remoteReadOnly = remoteStatus!!.readOnly
            remoteUploadDisabled = remoteStatus!!.uploadDisabled
            loadDirectory("/")
            loadShares()
        }
    }

    // Android System Back Gesture Handler
    BackHandler {
        if (remoteStatus != null && selectedTab == 0 && currentBrowsePath != "/" && currentBrowsePath.isNotBlank()) {
            val parent = currentBrowsePath.substringBeforeLast('/', "").ifBlank { "/" }
            loadDirectory(parent)
        } else {
            remoteStatus = null
            onLogout()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (isDarkTheme) R.drawable.ic_wormhole_logo else R.drawable.ic_wormhole_logo_dark
                            ),
                            contentDescription = "Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (remoteStatus != null) "Remote Console" else "Admin Login",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (remoteStatus != null) EmeraldOnline else Color.Gray)
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = {
                            remoteStatus = null
                            onLogout()
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (remoteStatus != null) Color(0xFFEF4444).copy(alpha = 0.7f) else CyanNeon.copy(alpha = 0.5f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (remoteStatus != null) Color(0xFFEF4444) else CyanNeon
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (remoteStatus != null) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (remoteStatus != null) "Logout" else "Back",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (isDarkTheme) Slate950 else LightSurface
                )
            )
        },
        containerColor = if (isDarkTheme) Slate950 else LightBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            if (remoteStatus == null) {
                // ==================== LOGIN FORM (MAIN PAGE VIEW) ====================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isDarkTheme) Slate900 else LightSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate800 else LightBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Remote Host Controller",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Connect this phone to your remote storage host device",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textSecondary
                                    )
                                }
                            }

                            // Host URL input
                            OutlinedTextField(
                                value = hostUrl,
                                onValueChange = { hostUrl = it },
                                label = { Text("Remote Host URL") },
                                placeholder = { Text("https://*.trycloudflare.com or http://192.168.1.50:8080") },
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
                                    focusedLabelColor = CyanNeon,
                                    unfocusedLabelColor = textSecondary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Admin Secret input
                            OutlinedTextField(
                                value = adminToken,
                                onValueChange = { adminToken = it },
                                label = { Text("Admin Secret / Passphrase") },
                                placeholder = { Text("stark_admin_2026 or Host Secret") },
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
                                                contentDescription = if (tokenVisible) "Hide secret" else "Show secret",
                                                tint = textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanNeon,
                                    unfocusedBorderColor = if (isDarkTheme) Slate700 else LightBorder,
                                    focusedLabelColor = CyanNeon,
                                    unfocusedLabelColor = textSecondary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Quick Fill Master Passphrase Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Use stark_admin_2026",
                                    fontSize = 11.sp,
                                    color = CyanNeon,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { adminToken = "stark_admin_2026" }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (errorMessage != null) {
                                Surface(
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = errorMessage!!,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { attemptLogin() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                enabled = !isConnecting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanNeon,
                                    contentColor = Color(0xFF020617)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isConnecting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFF020617),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Authenticating with Host...", fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Connect to Remote Host", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }

                    // Security Info Note
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = (if (isDarkTheme) Slate900 else LightSurface).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "💡 Host Connection Notes:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                            Text(
                                text = "• Find the Host URL and Host Secret on Phone B under Settings → Remote Server Mode.",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                            Text(
                                text = "• You can also use the master passphrase stark_admin_2026 to connect securely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }
                    }
                }
            } else {
                // ==================== AUTHENTICATED COMMAND CONSOLE (3 TABS) ====================
                val status = remoteStatus!!

                // Connected Header Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDarkTheme) Color(0xFF0B132B) else LightSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldOnline)
                            )
                            Column {
                                Text(
                                    text = status.deviceName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimary
                                )
                                Text(
                                    text = "Host ID: " + status.deviceId.take(12) + "...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = CyanNeon
                                )
                            }
                        }

                        // Connected Visitors Counter
                        Surface(
                            color = CyanNeon.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "${status.activeViewersCount} viewer(s)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeon
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = CyanNeon,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CyanNeon,
                            height = 3.dp
                        )
                    },
                    divider = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(if (isDarkTheme) Slate800 else LightBorder)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Remote Files", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        selectedContentColor = CyanNeon,
                        unselectedContentColor = textSecondary
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; loadShares() },
                        text = { Text("Active Shares (${sharesList.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        selectedContentColor = CyanNeon,
                        unselectedContentColor = textSecondary
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Host Control", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        selectedContentColor = CyanNeon,
                        unselectedContentColor = textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TAB CONTENT
                when (selectedTab) {
                    0 -> {
                        // ==================== TAB 0: REMOTE FILE BROWSER ====================
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Breadcrumb Bar
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isDarkTheme) Slate900 else LightSurface,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate800 else LightBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
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
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Parent directory",
                                                    tint = CyanNeon
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Host: $currentBrowsePath",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(onClick = { loadDirectory(currentBrowsePath) }, modifier = Modifier.size(28.dp)) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = textSecondary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isLoadingBrowse) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = CyanNeon)
                                }
                            } else if (browseError != null) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                        Text(text = browseError!!, color = Color(0xFFFCA5A5), fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(onClick = { loadDirectory(currentBrowsePath) }) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            } else if (browseItems.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = "This folder is empty", color = textSecondary, fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
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
                                                        imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                                        contentDescription = null,
                                                        tint = if (item.isDirectory) AmberWarn else CyanNeon,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Column(modifier = Modifier.weight(1f, fill = false)) {
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
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
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
                        Column(modifier = Modifier.fillMaxSize()) {
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
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = CyanNeon)
                                }
                            } else if (sharesList.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = textSecondary, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No active shared links", color = textSecondary, fontSize = 13.sp)
                                        Text("Select any file or folder from 'Remote Files' to share", color = textSecondary, fontSize = 11.sp)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
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
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (share.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
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
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    // Copy Share Link Button
                                                    OutlinedButton(
                                                        onClick = {
                                                            val base = normalizeUrl(hostUrl)
                                                            val link = "$base/s/${share.token}"
                                                            clipboardManager.setText(AnnotatedString(link))
                                                            Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f))
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Copy Link", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    // Revoke Share Button (Disconnects visitors instantly)
                                                    if (!share.isRevoked) {
                                                        Button(
                                                            onClick = { revokeShare(share.id) },
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                                                contentColor = Color(0xFFFCA5A5)
                                                            )
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Revoke Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "REMOTE SECURITY POLICIES",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = CyanNeon
                            )

                            // Read Only Mode Toggle
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isDarkTheme) Color(0xFF0F172A) else LightSurfaceElevated,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.EditOff, contentDescription = null, tint = AmberWarn)
                                        Column {
                                            Text("Enforce Read-Only Mode", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = textPrimary)
                                            Text("Prevents all visitors from renaming or deleting host files", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                                        }
                                    }
                                    Switch(
                                        checked = remoteReadOnly,
                                        onCheckedChange = { updateSecuritySettings(it, remoteUploadDisabled) },
                                        enabled = !isUpdatingSecurity,
                                        colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.4f))
                                    )
                                }
                            }

                            // Block Uploads Toggle
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isDarkTheme) Color(0xFF0F172A) else LightSurfaceElevated,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFEF4444))
                                        Column {
                                            Text("Disable File Uploads", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = textPrimary)
                                            Text("Rejects incoming uploads from connected browsers", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                                        }
                                    }
                                    Switch(
                                        checked = remoteUploadDisabled,
                                        onCheckedChange = { updateSecuritySettings(remoteReadOnly, it) },
                                        enabled = !isUpdatingSecurity,
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFEF4444), checkedTrackColor = Color(0xFFEF4444).copy(alpha = 0.4f))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "LIVE HARDWARE & NETWORK TELEMETRY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = CyanNeon
                            )

                            // Telemetry Cards Grid
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TelemetryMetricCard(
                                    title = "Battery",
                                    value = "${status.batteryPct}%",
                                    subtitle = if (status.isCharging) "Charging" else "On Battery",
                                    icon = if (status.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                    color = if (status.batteryPct > 20) EmeraldOnline else AmberWarn,
                                    modifier = Modifier.weight(1f),
                                    isDarkTheme = isDarkTheme
                                )
                                TelemetryMetricCard(
                                    title = "Temperature",
                                    value = "%.1f°C".format(status.batteryTempCelsius),
                                    subtitle = if (status.batteryTempCelsius > 45.0f) "Running Warm" else "Optimal",
                                    icon = Icons.Default.Thermostat,
                                    color = if (status.batteryTempCelsius > 45.0f) Color(0xFFEF4444) else CyanNeon,
                                    modifier = Modifier.weight(1f),
                                    isDarkTheme = isDarkTheme
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TelemetryMetricCard(
                                    title = "Throughput",
                                    value = formatBytes(status.totalBytesSent),
                                    subtitle = "Total transferred",
                                    icon = Icons.Default.Speed,
                                    color = CyanNeon,
                                    modifier = Modifier.weight(1f),
                                    isDarkTheme = isDarkTheme
                                )
                                TelemetryMetricCard(
                                    title = "Network",
                                    value = status.networkType,
                                    subtitle = "Active Connection",
                                    icon = if (status.networkType == "WIFI") Icons.Default.Wifi else Icons.Default.Dns,
                                    color = EmeraldOnline,
                                    modifier = Modifier.weight(1f),
                                    isDarkTheme = isDarkTheme
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }

    // ==================== SHARE LINK GENERATION MODAL DIALOG ====================
    if (itemToShare != null) {
        AlertDialog(
            onDismissRequest = { itemToShare = null; generatedShareUrl = null },
            title = {
                Text(
                    text = if (generatedShareUrl == null) "Create Share Link" else "Share Ready!",
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (generatedShareUrl == null) {
                        Text(
                            text = "Set permissions for: ${itemToShare!!.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )

                        OutlinedTextField(
                            value = shareCustomName,
                            onValueChange = { shareCustomName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = if (isDarkTheme) Slate700 else LightBorder
                            )
                        )

                        OutlinedTextField(
                            value = sharePassword,
                            onValueChange = { sharePassword = it },
                            label = { Text("Password (Optional)") },
                            placeholder = { Text("Leave blank for open access") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = if (isDarkTheme) Slate700 else LightBorder
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Allow File Download", style = MaterialTheme.typography.bodyMedium, color = textPrimary)
                                Text("Toggle off for stream-only mode", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                            }
                            Switch(
                                checked = shareAllowDownload,
                                onCheckedChange = { shareAllowDownload = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.4f))
                            )
                        }
                    } else {
                        // Generated Link Result
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CyanNeon.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = generatedShareUrl!!,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                    color = CyanNeon
                                )
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(generatedShareUrl!!))
                                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF020617))
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Share URL", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (generatedShareUrl == null) {
                    Button(
                        onClick = { submitCreateShare() },
                        enabled = !isCreatingShare,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF020617))
                    ) {
                        if (isCreatingShare) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF020617), strokeWidth = 2.dp)
                        } else {
                            Text("Generate Link", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(onClick = { itemToShare = null; generatedShareUrl = null }) {
                        Text("Done")
                    }
                }
            },
            dismissButton = {
                if (generatedShareUrl == null) {
                    OutlinedButton(onClick = { itemToShare = null }) {
                        Text("Cancel")
                    }
                }
            },
            containerColor = if (isDarkTheme) Slate900 else LightSurface
        )
    }
}

/**
 * Backward compatibility alias for any existing caller
 */
@Composable
fun AdminLoginBottomSheet(
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = true
) {
    RemoteHostControlScreen(
        onLogout = onDismiss,
        isDarkTheme = isDarkTheme
    )
}

@Composable
private fun TelemetryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = modifier,
        color = if (isDarkTheme) Color(0xFF0F172A) else LightSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Slate800 else LightBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isDarkTheme) Color(0xFF94A3B8) else LightTextSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDarkTheme) Color.White else LightTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = color
            )
        }
    }
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
