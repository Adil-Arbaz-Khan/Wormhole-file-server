package com.jarvis.fileserver.model

import android.os.Environment
import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val port: Int = 8080,
    val rootPath: String = Environment.getExternalStorageDirectory().absolutePath,
    val enableTunnel: Boolean = false,
    val basicAuthEnabled: Boolean = false,
    val authUsername: String = "admin",
    val authPassword: String = "stark123",
    val clipboardEnabled: Boolean = false,
    val readOnly: Boolean = false,
    val uploadDisabled: Boolean = false,
    val remoteServerModeEnabled: Boolean = false,
    val hostDeviceId: String = "wormhole-host",
    val adminSecret: String = "stark_admin_2026",
    val networkPreference: String = "ANY"
)
