package com.jarvis.fileserver.ui

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.fileserver.analytics.ActivityEvent
import com.jarvis.fileserver.analytics.ActivityLogger
import com.jarvis.fileserver.analytics.TrafficMonitor
import com.jarvis.fileserver.analytics.TrafficSnapshot
import com.jarvis.fileserver.clipboard.ClipboardRepository
import com.jarvis.fileserver.data.ShareRecord
import com.jarvis.fileserver.data.ShareRepository
import com.jarvis.fileserver.model.ClipboardItem
import com.jarvis.fileserver.model.ServerConfig
import com.jarvis.fileserver.model.ServerState
import com.jarvis.fileserver.model.TunnelState
import com.jarvis.fileserver.security.SecurityUtils
import com.jarvis.fileserver.service.HttpServerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("wormhole_prefs", Context.MODE_PRIVATE)

    val serverState: StateFlow<ServerState> = HttpServerService.serverState
    val tunnelState: StateFlow<TunnelState> = HttpServerService.tunnelState

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("pref_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _clipboardEnabled = MutableStateFlow(prefs.getBoolean("pref_clipboard_enabled", false))
    val clipboardEnabled: StateFlow<Boolean> = _clipboardEnabled.asStateFlow()

    private val _readOnly = MutableStateFlow(prefs.getBoolean("pref_read_only", false))
    val readOnly: StateFlow<Boolean> = _readOnly.asStateFlow()

    private val _uploadDisabled = MutableStateFlow(prefs.getBoolean("pref_upload_disabled", false))
    val uploadDisabled: StateFlow<Boolean> = _uploadDisabled.asStateFlow()

    private val _remoteServerMode = MutableStateFlow(prefs.getBoolean("pref_remote_server_mode", false))
    val remoteServerMode: StateFlow<Boolean> = _remoteServerMode.asStateFlow()

    private val _networkPref = MutableStateFlow(prefs.getString("pref_network_pref", "ANY") ?: "ANY")
    val networkPreference: StateFlow<String> = _networkPref.asStateFlow()

    // Host Device Identity & Security
    private val hostSecurity = SecurityUtils.getOrCreateHostDeviceSecret(application)
    val hostDeviceId: String = hostSecurity.first
    val hostAdminSecret: String = hostSecurity.second

    val clipboardItems: StateFlow<List<ClipboardItem>> = ClipboardRepository.items
    val unreadClipboardCount: StateFlow<Int> = ClipboardRepository.unreadCount

    val trafficSnapshot: StateFlow<TrafficSnapshot> = TrafficMonitor.snapshot
    val activityEvents: StateFlow<List<ActivityEvent>> = ActivityLogger.events
    val activeShares: StateFlow<List<ShareRecord>> = ShareRepository.sharesFlow

    private val _isClipboardOpen = MutableStateFlow(false)
    val isClipboardOpen: StateFlow<Boolean> = _isClipboardOpen.asStateFlow()

    private val _isActivitySheetOpen = MutableStateFlow(false)
    val isActivitySheetOpen: StateFlow<Boolean> = _isActivitySheetOpen.asStateFlow()

    private val _isAdminLoginOpen = MutableStateFlow(false)
    val isAdminLoginOpen: StateFlow<Boolean> = _isAdminLoginOpen.asStateFlow()

    private val _isShareManagementOpen = MutableStateFlow(false)
    val isShareManagementOpen: StateFlow<Boolean> = _isShareManagementOpen.asStateFlow()

    private val defaultRoot = Environment.getExternalStorageDirectory().absolutePath

    private val _config = MutableStateFlow(
        ServerConfig(
            port = prefs.getInt("pref_port", 8080),
            rootPath = prefs.getString("pref_root_path", defaultRoot) ?: defaultRoot,
            basicAuthEnabled = prefs.getBoolean("pref_auth_enabled", false),
            authUsername = prefs.getString("pref_auth_user", "admin") ?: "admin",
            authPassword = prefs.getString("pref_auth_pass", "stark123") ?: "stark123",
            clipboardEnabled = prefs.getBoolean("pref_clipboard_enabled", false),
            readOnly = prefs.getBoolean("pref_read_only", false),
            uploadDisabled = prefs.getBoolean("pref_upload_disabled", false),
            remoteServerModeEnabled = prefs.getBoolean("pref_remote_server_mode", false),
            hostDeviceId = hostDeviceId,
            adminSecret = hostAdminSecret,
            networkPreference = prefs.getString("pref_network_pref", "ANY") ?: "ANY"
        )
    )
    val config: StateFlow<ServerConfig> = _config.asStateFlow()

    private val _uptimeSeconds = MutableStateFlow(0L)
    val uptimeSeconds: StateFlow<Long> = _uptimeSeconds.asStateFlow()

    private val _activeQrData = MutableStateFlow<Pair<String, String>?>(null)
    val activeQrData: StateFlow<Pair<String, String>?> = _activeQrData.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    init {
        ShareRepository.init(application)

        // Track uptime when server is active
        viewModelScope.launch {
            while (isActive) {
                val state = serverState.value
                if (state is ServerState.Running) {
                    val elapsed = (System.currentTimeMillis() - state.startTimeMillis) / 1000
                    _uptimeSeconds.value = if (elapsed > 0) elapsed else 0
                } else {
                    _uptimeSeconds.value = 0L
                }
                delay(1000)
            }
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean("pref_dark_theme", isDark).apply()
    }

    fun setClipboardEnabled(enabled: Boolean) {
        _clipboardEnabled.value = enabled
        prefs.edit().putBoolean("pref_clipboard_enabled", enabled).apply()
        val newConfig = _config.value.copy(clipboardEnabled = enabled)
        _config.value = newConfig
        val app = getApplication<Application>()
        if (serverState.value is ServerState.Running) {
            HttpServerService.startService(app, newConfig)
        }
    }

    fun setRemoteServerMode(enabled: Boolean) {
        _remoteServerMode.value = enabled
        prefs.edit().putBoolean("pref_remote_server_mode", enabled).apply()
        val newConfig = _config.value.copy(remoteServerModeEnabled = enabled)
        _config.value = newConfig
        val app = getApplication<Application>()
        if (serverState.value is ServerState.Running) {
            HttpServerService.startService(app, newConfig)
        }
    }

    fun setNetworkPreference(pref: String) {
        _networkPref.value = pref
        prefs.edit().putString("pref_network_pref", pref).apply()
        val newConfig = _config.value.copy(networkPreference = pref)
        _config.value = newConfig
    }

    fun sendClipboardText(text: String) {
        if (text.isNotBlank()) {
            ClipboardRepository.addFromPhone(text.trim())
        }
    }

    fun markClipboardAsRead() {
        ClipboardRepository.markAsRead()
    }

    fun removeClipboardItem(id: String) {
        ClipboardRepository.removeItem(id)
    }

    fun clearAllClipboard() {
        ClipboardRepository.clearAll()
    }

    fun openClipboard() {
        _isClipboardOpen.value = true
        markClipboardAsRead()
    }

    fun closeClipboard() {
        _isClipboardOpen.value = false
    }

    fun openActivitySheet() {
        _isActivitySheetOpen.value = true
    }

    fun closeActivitySheet() {
        _isActivitySheetOpen.value = false
    }

    fun openAdminLogin() {
        _isAdminLoginOpen.value = true
    }

    fun closeAdminLogin() {
        _isAdminLoginOpen.value = false
    }

    fun openShareManagement() {
        _isShareManagementOpen.value = true
    }

    fun closeShareManagement() {
        _isShareManagementOpen.value = false
    }

    fun toggleServer() {
        val app = getApplication<Application>()
        if (serverState.value is ServerState.Running || serverState.value is ServerState.Starting) {
            HttpServerService.stopService(app)
        } else {
            HttpServerService.startService(app, _config.value)
        }
    }

    fun toggleTunnel(enable: Boolean) {
        val app = getApplication<Application>()
        _config.value = _config.value.copy(enableTunnel = enable)
        HttpServerService.toggleTunnel(app, enable)
    }

    fun updateConfig(newConfig: ServerConfig) {
        _config.value = newConfig
        _clipboardEnabled.value = newConfig.clipboardEnabled
        _readOnly.value = newConfig.readOnly
        _uploadDisabled.value = newConfig.uploadDisabled
        _remoteServerMode.value = newConfig.remoteServerModeEnabled
        _networkPref.value = newConfig.networkPreference

        prefs.edit()
            .putInt("pref_port", newConfig.port)
            .putString("pref_root_path", newConfig.rootPath)
            .putBoolean("pref_auth_enabled", newConfig.basicAuthEnabled)
            .putString("pref_auth_user", newConfig.authUsername)
            .putString("pref_auth_pass", newConfig.authPassword)
            .putBoolean("pref_clipboard_enabled", newConfig.clipboardEnabled)
            .putBoolean("pref_read_only", newConfig.readOnly)
            .putBoolean("pref_upload_disabled", newConfig.uploadDisabled)
            .putBoolean("pref_remote_server_mode", newConfig.remoteServerModeEnabled)
            .putString("pref_network_pref", newConfig.networkPreference)
            .apply()

        val app = getApplication<Application>()
        if (serverState.value is ServerState.Running) {
            HttpServerService.startService(app, newConfig)
        }
    }

    fun setSharedPath(path: String) {
        val newConfig = _config.value.copy(rootPath = path)
        updateConfig(newConfig)
    }

    fun createTokenShare(
        filePath: String,
        name: String,
        password: String?,
        allowDownload: Boolean,
        expiresAtMillis: Long?,
        maxAccessCount: Int?
    ): ShareRecord? {
        val file = File(filePath)
        if (!file.exists()) return null

        val token = SecurityUtils.generateSecureToken(16)
        var saltHex: String? = null
        var hashHex: String? = null
        val isProtected = !password.isNullOrBlank()
        if (isProtected) {
            saltHex = SecurityUtils.generateSalt(16)
            hashHex = SecurityUtils.hashPassword(password!!, saltHex)
        }

        val share = ShareRecord(
            id = UUID.randomUUID().toString(),
            token = token,
            name = name.ifBlank { file.name },
            targetPath = file.absolutePath,
            isDirectory = file.isDirectory,
            isPasswordProtected = isProtected,
            passwordHash = hashHex,
            passwordSalt = saltHex,
            allowDownload = allowDownload,
            expiresAtMillis = expiresAtMillis,
            maxAccessCount = maxAccessCount
        )

        val success = ShareRepository.createShare(share)
        return if (success) share else null
    }

    fun revokeShare(id: String): Boolean {
        return ShareRepository.revokeShare(id)
    }

    fun deleteShare(id: String): Boolean {
        return ShareRepository.deleteShare(id)
    }

    fun showQr(title: String, url: String) {
        _activeQrData.value = title to url
    }

    fun dismissQr() {
        _activeQrData.value = null
    }

    fun clearActivityLogs() {
        ActivityLogger.clear()
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }
}
