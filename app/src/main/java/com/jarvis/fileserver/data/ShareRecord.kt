package com.jarvis.fileserver.data

import kotlinx.serialization.Serializable

@Serializable
data class ShareRecord(
    val id: String,
    val token: String,
    val name: String,
    val targetPath: String,
    val isDirectory: Boolean,
    val isPasswordProtected: Boolean = false,
    val passwordHash: String? = null,
    val passwordSalt: String? = null,
    val allowDownload: Boolean = true,
    val expiresAtMillis: Long? = null,
    val maxAccessCount: Int? = null,
    val accessCount: Int = 0,
    val isRevoked: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() = expiresAtMillis != null && System.currentTimeMillis() > expiresAtMillis

    val isLimitExceeded: Boolean
        get() = maxAccessCount != null && accessCount >= maxAccessCount

    val isValid: Boolean
        get() = !isRevoked && !isExpired && !isLimitExceeded
}

@Serializable
data class AdminDeviceRecord(
    val deviceId: String,
    val deviceName: String,
    val adminToken: String,
    val registeredAtMillis: Long = System.currentTimeMillis(),
    val lastActiveMillis: Long = System.currentTimeMillis()
)
