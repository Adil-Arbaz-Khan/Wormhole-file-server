package com.jarvis.fileserver.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jarvis.fileserver.R
import com.jarvis.fileserver.model.ServerState
import com.jarvis.fileserver.ui.components.ClipboardBottomSheet
import com.jarvis.fileserver.ui.components.ConnectionAddressesCard
import com.jarvis.fileserver.ui.components.HeroServerCard
import com.jarvis.fileserver.ui.components.QrCodeDialog
import com.jarvis.fileserver.ui.components.SettingsBottomSheet
import com.jarvis.fileserver.ui.components.SharedScopeCard
import com.jarvis.fileserver.ui.components.TunnelControlsCard
import com.jarvis.fileserver.ui.theme.AmberWarn
import com.jarvis.fileserver.ui.theme.CyanNeon
import com.jarvis.fileserver.ui.theme.JarvisFileServerTheme
import com.jarvis.fileserver.ui.theme.LightBg
import com.jarvis.fileserver.ui.theme.LightSurface
import com.jarvis.fileserver.ui.theme.LightTextPrimary
import com.jarvis.fileserver.ui.theme.LightTextSecondary
import com.jarvis.fileserver.ui.theme.Slate400
import com.jarvis.fileserver.ui.theme.Slate950
import com.jarvis.fileserver.util.PathUtils
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Handled */ }

    private val legacyStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Handled */ }

    private val pickFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val resolved = PathUtils.getPathFromTreeUri(it)
            if (!resolved.isNullOrBlank()) {
                viewModel.setSharedPath(resolved)
                Toast.makeText(this, "Shared folder: ${File(resolved).name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val resolved = PathUtils.getPathFromFileUri(this, it)
            if (!resolved.isNullOrBlank()) {
                viewModel.setSharedPath(resolved)
                Toast.makeText(this, "Shared file: ${File(resolved).name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()
        handleIntent(intent)

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            JarvisFileServerTheme(isDarkTheme = isDarkTheme) {
                DashboardScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    hasStoragePermission = hasAllFilesPermission(),
                    onRequestStoragePermission = { requestAllFilesPermission() },
                    onCopyUrl = { copyToClipboard(it) },
                    onOpenUrl = { openBrowser(it) },
                    onPickFolder = { pickFolderLauncher.launch(null) },
                    onPickFile = { pickFileLauncher.launch("*/*") }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // Handle Share Target from other apps
        if (intent.action == Intent.ACTION_SEND) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            uri?.let {
                val resolved = PathUtils.getPathFromFileUri(this, it)
                if (!resolved.isNullOrBlank()) {
                    viewModel.setSharedPath(resolved)
                    Toast.makeText(this, "Sharing: ${File(resolved).name}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (intent.getBooleanExtra("autostart", false)) {
            if (viewModel.serverState.value !is ServerState.Running) {
                viewModel.toggleServer()
            }
        }
        if (intent.hasExtra("toggle_tunnel")) {
            val enable = intent.getBooleanExtra("toggle_tunnel", false)
            viewModel.toggleTunnel(enable)
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            legacyStoragePermissionLauncher.launch(permissions)
        }
    }

    private fun hasAllFilesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("File Server URL", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No browser application found", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    hasStoragePermission: Boolean,
    onRequestStoragePermission: () -> Unit,
    onCopyUrl: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onPickFolder: () -> Unit,
    onPickFile: () -> Unit
) {
    val serverState by viewModel.serverState.collectAsState()
    val tunnelState by viewModel.tunnelState.collectAsState()
    val config by viewModel.config.collectAsState()
    val uptimeSeconds by viewModel.uptimeSeconds.collectAsState()
    val activeQrData by viewModel.activeQrData.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val clipboardEnabled by viewModel.clipboardEnabled.collectAsState()
    val unreadClipboardCount by viewModel.unreadClipboardCount.collectAsState()
    val isClipboardOpen by viewModel.isClipboardOpen.collectAsState()
    val clipboardItems by viewModel.clipboardItems.collectAsState()

    val isRunning = serverState is ServerState.Running

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (isDarkTheme) R.drawable.ic_wormhole_logo else R.drawable.ic_wormhole_logo_dark
                            ),
                            contentDescription = "Wormhole Logo",
                            modifier = Modifier
                                .size(28.dp)
                                .offset(y = 1.5.dp) // Optical centering with text cap-height
                        )
                        Text(
                            text = "Wormhole",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else LightTextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                },
                actions = {
                    if (clipboardEnabled && isRunning) {
                        BadgedBox(
                            badge = {
                                if (unreadClipboardCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = "$unreadClipboardCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = { viewModel.openClipboard() }) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Shared Clipboard",
                                    tint = if (isDarkTheme) CyanNeon else Color(0xFF0284C7)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { viewModel.openSettings() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (isDarkTheme) Slate400 else LightTextSecondary
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Permission Warning Banner
            if (!hasStoragePermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, AmberWarn.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    color = AmberWarn.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AmberWarn,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "All Files Access Required",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isDarkTheme) Color.White else LightTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "To host internal storage, grant Android 11+ storage management permission.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDarkTheme) Slate400 else LightTextSecondary
                            )
                        }
                        Button(
                            onClick = onRequestStoragePermission,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarn)
                        ) {
                            Text("Grant", color = Slate950, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 1. Hero Server Card
            HeroServerCard(
                serverState = serverState,
                uptimeSeconds = uptimeSeconds,
                onToggleServer = { viewModel.toggleServer() },
                isDarkTheme = isDarkTheme
            )

            // 2. Shared Scope Selection Card (Folder / File Picker)
            SharedScopeCard(
                currentRootPath = config.rootPath,
                onSelectPath = { viewModel.setSharedPath(it) },
                onPickFolder = onPickFolder,
                onPickFile = onPickFile,
                isDarkTheme = isDarkTheme
            )

            // 3. Connection Addresses Card
            if (serverState is ServerState.Running) {
                val running = serverState as ServerState.Running
                ConnectionAddressesCard(
                    localInterfaces = running.localInterfaces,
                    tunnelState = tunnelState,
                    onCopyUrl = onCopyUrl,
                    onOpenUrl = onOpenUrl,
                    onShowQr = { title, url -> viewModel.showQr(title, url) },
                    isDarkTheme = isDarkTheme
                )
            }

            // 4. Cloudflare Quick Tunnel Controls Card
            TunnelControlsCard(
                tunnelState = tunnelState,
                isServerRunning = isRunning,
                onToggleTunnel = { viewModel.toggleTunnel(it) },
                onCopyUrl = onCopyUrl,
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // QR Code Dialog
    activeQrData?.let { (title, url) ->
        QrCodeDialog(
            title = title,
            url = url,
            onCopyUrl = onCopyUrl,
            onDismiss = { viewModel.dismissQr() }
        )
    }

    // Settings Bottom Sheet
    if (isSettingsOpen) {
        SettingsBottomSheet(
            currentConfig = config,
            isServerRunning = isRunning,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = { viewModel.setDarkTheme(it) },
            onSaveConfig = { viewModel.updateConfig(it) },
            onPickDirectory = onPickFolder,
            onPickFile = onPickFile,
            onDismiss = { viewModel.closeSettings() }
        )
    }

    // Shared Clipboard Bottom Sheet
    if (isClipboardOpen) {
        ClipboardBottomSheet(
            items = clipboardItems,
            onSendText = { viewModel.sendClipboardText(it) },
            onDeleteItem = { viewModel.removeClipboardItem(it) },
            onClearAll = { viewModel.clearAllClipboard() },
            onDismiss = { viewModel.closeClipboard() },
            isDarkTheme = isDarkTheme
        )
    }
}
