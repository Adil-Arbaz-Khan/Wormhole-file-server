package com.jarvis.fileserver.model

import com.jarvis.fileserver.data.ShareRecord
import kotlinx.serialization.Serializable

@Serializable
data class AdminStatusResponse(
    val deviceId: String,
    val deviceName: String,
    val status: String,
    val batteryPct: Int,
    val batteryTempCelsius: Float,
    val isCharging: Boolean,
    val uptimeSeconds: Long,
    val storageTotalBytes: Long,
    val storageFreeBytes: Long,
    val activeSharesCount: Int,
    val activeViewersCount: Int,
    val uploadSpeedBps: Long,
    val downloadSpeedBps: Long,
    val totalBytesSent: Long,
    val totalBytesReceived: Long,
    val networkType: String,
    val readOnly: Boolean = false,
    val uploadDisabled: Boolean = false,
    val recentActivities: List<ActivityEventItemDto> = emptyList()
)

@Serializable
data class AdminSecuritySettingsRequest(
    val readOnly: Boolean,
    val uploadDisabled: Boolean
)

@Serializable
data class ActivityEventItemDto(
    val id: String,
    val type: String,
    val title: String,
    val detail: String,
    val timestampMillis: Long
)

@Serializable
data class CreateShareRequest(
    val path: String,
    val name: String,
    val password: String? = null,
    val allowDownload: Boolean = true,
    val expiresAtMillis: Long? = null,
    val maxAccessCount: Int? = null
)

@Serializable
data class ShareCreatedResponse(
    val success: Boolean,
    val message: String,
    val share: ShareRecord? = null,
    val shareUrl: String = ""
)

@Serializable
data class RegisterDeviceRequest(
    val clientDeviceId: String,
    val clientDeviceName: String,
    val adminSecret: String
)

@Serializable
data class RegisterDeviceResponse(
    val success: Boolean,
    val message: String,
    val hostDeviceId: String = "",
    val adminToken: String = ""
)

@Serializable
data class ShareTokenInfoResponse(
    val valid: Boolean,
    val name: String = "",
    val isDirectory: Boolean = false,
    val isPasswordProtected: Boolean = false,
    val allowDownload: Boolean = true,
    val size: Long = 0L,
    val mimeType: String = "",
    val errorMessage: String? = null
)
