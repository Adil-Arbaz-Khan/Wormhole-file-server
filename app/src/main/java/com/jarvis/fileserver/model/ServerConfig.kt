package com.jarvis.fileserver.model

import android.os.Environment

data class ServerConfig(
    val port: Int = 8080,
    val rootPath: String = Environment.getExternalStorageDirectory().absolutePath,
    val enableTunnel: Boolean = false,
    val basicAuthEnabled: Boolean = false,
    val authUsername: String = "admin",
    val authPassword: String = "stark123",
    val clipboardEnabled: Boolean = false
)
