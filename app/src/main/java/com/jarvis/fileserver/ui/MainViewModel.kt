package com.jarvis.fileserver.ui

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.fileserver.clipboard.ClipboardRepository
import com.jarvis.fileserver.model.ClipboardItem
import com.jarvis.fileserver.model.ServerConfig
import com.jarvis.fileserver.model.ServerState
import com.jarvis.fileserver.model.TunnelState
import com.jarvis.fileserver.service.HttpServerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("wormhole_prefs", Context.MODE_PRIVATE)

    val serverState: StateFlow<ServerState> = HttpServerService.serverState
    val tunnelState: StateFlow<TunnelState> = HttpServerService.tunnelState

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("pref_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _clipboardEnabled = MutableStateFlow(prefs.getBoolean("pref_clipboard_enabled", false))
    val clipboardEnabled: StateFlow<Boolean> = _clipboardEnabled.asStateFlow()

    val clipboardItems: StateFlow<List<ClipboardItem>> = ClipboardRepository.items
    val unreadClipboardCount: StateFlow<Int> = ClipboardRepository.unreadCount

    private val _isClipboardOpen = MutableStateFlow(false)
    val isClipboardOpen: StateFlow<Boolean> = _isClipboardOpen.asStateFlow()

    private val defaultRoot = Environment.getExternalStorageDirectory().absolutePath

    private val _config = MutableStateFlow(
        ServerConfig(
            port = prefs.getInt("pref_port", 8080),
            rootPath = prefs.getString("pref_root_path", defaultRoot) ?: defaultRoot,
            basicAuthEnabled = prefs.getBoolean("pref_auth_enabled", false),
            authUsername = prefs.getString("pref_auth_user", "admin") ?: "admin",
            authPassword = prefs.getString("pref_auth_pass", "stark123") ?: "stark123",
            clipboardEnabled = prefs.getBoolean("pref_clipboard_enabled", false)
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
        prefs.edit()
            .putInt("pref_port", newConfig.port)
            .putString("pref_root_path", newConfig.rootPath)
            .putBoolean("pref_auth_enabled", newConfig.basicAuthEnabled)
            .putString("pref_auth_user", newConfig.authUsername)
            .putString("pref_auth_pass", newConfig.authPassword)
            .putBoolean("pref_clipboard_enabled", newConfig.clipboardEnabled)
            .apply()
    }

    fun setSharedPath(path: String) {
        val newConfig = _config.value.copy(rootPath = path)
        updateConfig(newConfig)
        val app = getApplication<Application>()
        if (serverState.value is ServerState.Running) {
            HttpServerService.startService(app, newConfig)
        }
    }

    fun showQr(title: String, url: String) {
        _activeQrData.value = title to url
    }

    fun dismissQr() {
        _activeQrData.value = null
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }
}
