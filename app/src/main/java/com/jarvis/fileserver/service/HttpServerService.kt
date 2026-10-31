package com.jarvis.fileserver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jarvis.fileserver.R
import com.jarvis.fileserver.model.NetworkInterfaceInfo
import com.jarvis.fileserver.model.ServerConfig
import com.jarvis.fileserver.model.ServerState
import com.jarvis.fileserver.model.TunnelState
import com.jarvis.fileserver.network.NetworkUtils
import com.jarvis.fileserver.server.EmbeddedHttpServer
import com.jarvis.fileserver.tunnel.CloudflareTunnelManager
import com.jarvis.fileserver.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HttpServerService : Service() {

    private val tag = "HttpServerService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var embeddedServer: EmbeddedHttpServer? = null
    private var tunnelManager: CloudflareTunnelManager? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "jarvis_file_server_channel"

        const val ACTION_START = "com.jarvis.fileserver.START"
        const val ACTION_STOP = "com.jarvis.fileserver.STOP"
        const val ACTION_TOGGLE_TUNNEL = "com.jarvis.fileserver.TOGGLE_TUNNEL"

        const val EXTRA_PORT = "EXTRA_PORT"
        const val EXTRA_ROOT_PATH = "EXTRA_ROOT_PATH"
        const val EXTRA_ENABLE_TUNNEL = "EXTRA_ENABLE_TUNNEL"
        const val EXTRA_AUTH_ENABLED = "EXTRA_AUTH_ENABLED"
        const val EXTRA_USERNAME = "EXTRA_USERNAME"
        const val EXTRA_PASSWORD = "EXTRA_PASSWORD"
        const val EXTRA_CLIPBOARD_ENABLED = "EXTRA_CLIPBOARD_ENABLED"

        private val _serverState = MutableStateFlow<ServerState>(ServerState.Idle)
        val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

        private val _tunnelState = MutableStateFlow<TunnelState>(TunnelState.Disabled)
        val tunnelState: StateFlow<TunnelState> = _tunnelState.asStateFlow()

        private val _currentConfig = MutableStateFlow(ServerConfig())
        val currentConfig: StateFlow<ServerConfig> = _currentConfig.asStateFlow()

        fun startService(context: Context, config: ServerConfig) {
            val intent = Intent(context, HttpServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PORT, config.port)
                putExtra(EXTRA_ROOT_PATH, config.rootPath)
                putExtra(EXTRA_ENABLE_TUNNEL, config.enableTunnel)
                putExtra(EXTRA_AUTH_ENABLED, config.basicAuthEnabled)
                putExtra(EXTRA_USERNAME, config.authUsername)
                putExtra(EXTRA_PASSWORD, config.authPassword)
                putExtra(EXTRA_CLIPBOARD_ENABLED, config.clipboardEnabled)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, HttpServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun toggleTunnel(context: Context, enable: Boolean) {
            val intent = Intent(context, HttpServerService::class.java).apply {
                action = ACTION_TOGGLE_TUNNEL
                putExtra(EXTRA_ENABLE_TUNNEL, enable)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
        tunnelManager = CloudflareTunnelManager(applicationContext)

        // Sync tunnel state to companion StateFlow
        serviceScope.launch {
            tunnelManager?.tunnelState?.collectLatest { state ->
                _tunnelState.value = state
                updateNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val config = ServerConfig(
                    port = intent.getIntExtra(EXTRA_PORT, 8080),
                    rootPath = intent.getStringExtra(EXTRA_ROOT_PATH) ?: _currentConfig.value.rootPath,
                    enableTunnel = intent.getBooleanExtra(EXTRA_ENABLE_TUNNEL, false),
                    basicAuthEnabled = intent.getBooleanExtra(EXTRA_AUTH_ENABLED, false),
                    authUsername = intent.getStringExtra(EXTRA_USERNAME) ?: "admin",
                    authPassword = intent.getStringExtra(EXTRA_PASSWORD) ?: "",
                    clipboardEnabled = intent.getBooleanExtra(EXTRA_CLIPBOARD_ENABLED, false)
                )
                _currentConfig.value = config
                startServerAndTunnel(config)
            }
            ACTION_STOP -> {
                stopSelfAndCleanup()
            }
            ACTION_TOGGLE_TUNNEL -> {
                val enable = intent.getBooleanExtra(EXTRA_ENABLE_TUNNEL, false)
                _currentConfig.value = _currentConfig.value.copy(enableTunnel = enable)
                handleTunnelToggle(enable)
            }
        }
        return START_STICKY
    }

    private var startJob: kotlinx.coroutines.Job? = null

    private fun startServerAndTunnel(config: ServerConfig) {
        startJob?.cancel()
        startJob = serviceScope.launch {
            _serverState.value = ServerState.Starting
            startForegroundWithNotification()

            try {
                // 1. Stop any old instance & start Ktor CIO
                embeddedServer?.stop()
                val server = EmbeddedHttpServer(applicationContext, config)
                embeddedServer = server
                server.start()

                // 2. Set Running state IMMEDIATELY (takes < 100ms)
                val startTime = System.currentTimeMillis()
                val fallbackInterfaces = listOf(
                    NetworkInterfaceInfo(
                        interfaceName = "lo",
                        displayName = "Localhost",
                        ipAddress = "127.0.0.1",
                        port = config.port,
                        type = com.jarvis.fileserver.model.NetworkType.LOCALHOST
                    )
                )
                _serverState.value = ServerState.Running(
                    port = config.port,
                    rootPath = config.rootPath,
                    startTimeMillis = startTime,
                    localInterfaces = fallbackInterfaces
                )
                updateNotification()

                // 3. Resolve actual network interfaces asynchronously without blocking server readiness
                serviceScope.launch(Dispatchers.IO) {
                    val resolved = NetworkUtils.getActiveNetworkInterfaces(applicationContext, config.port)
                    val current = _serverState.value
                    if (current is ServerState.Running) {
                        _serverState.value = current.copy(localInterfaces = resolved)
                        updateNotification()
                    }
                }

                // 4. Handle tunnel if requested
                if (config.enableTunnel) {
                    tunnelManager?.startTunnel(config.port)
                } else {
                    tunnelManager?.stopTunnel()
                }
            } catch (t: Throwable) {
                Log.e(tag, "Failed to start server", t)
                _serverState.value = ServerState.Error(t.localizedMessage ?: "Unknown server error")
                stopSelfAndCleanup()
            }
        }
    }

    private fun handleTunnelToggle(enable: Boolean) {
        serviceScope.launch {
            val state = _serverState.value
            if (state is ServerState.Running) {
                if (enable) {
                    tunnelManager?.startTunnel(state.port)
                } else {
                    tunnelManager?.stopTunnel()
                }
            }
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Initializing services...", null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java) ?: return
        val state = _serverState.value
        if (state is ServerState.Running) {
            val primaryIp = state.localInterfaces.firstOrNull { it.ipAddress != "127.0.0.1" }?.ipAddress ?: "127.0.0.1"
            val localUrl = "http://$primaryIp:${state.port}"
            val tunnelUrl = when (val tState = _tunnelState.value) {
                is TunnelState.Connected -> tState.url
                is TunnelState.Connecting -> "Connecting tunnel..."
                else -> null
            }
            val notification = buildNotification(localUrl, tunnelUrl)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(localAddress: String, tunnelAddress: String?): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, HttpServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = buildString {
            append("Local: ").append(localAddress)
            if (!tunnelAddress.isNullOrBlank()) {
                append(" | Cloudflare: ").append(tunnelAddress)
            }
        }

        val largeIcon = try {
            BitmapFactory.decodeResource(resources, R.drawable.ic_wormhole_logo)
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wormhole File Server is Active")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Server", stopPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wormhole Background Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the embedded file server and Cloudflare tunnel responsive."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun acquireLocks() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "JarvisFileServer::CpuWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(12 * 60 * 60 * 1000L) // 12 hours safeguard
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifiLock = wifiManager?.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "JarvisFileServer::WifiHighPerfLock"
        )?.apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (e: Exception) {
            Log.w(tag, "WakeLock release error", e)
        }
        try {
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            Log.w(tag, "WifiLock release error", e)
        }
    }

    private fun stopSelfAndCleanup() {
        serviceScope.launch {
            tunnelManager?.stopTunnel()
            embeddedServer?.stop()
            embeddedServer = null
            _serverState.value = ServerState.Idle
            _tunnelState.value = TunnelState.Disabled
            releaseLocks()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopSelfAndCleanup()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
