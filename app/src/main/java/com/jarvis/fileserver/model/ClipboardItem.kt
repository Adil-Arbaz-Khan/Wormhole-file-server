package com.jarvis.fileserver.model

import kotlinx.serialization.Serializable

@Serializable
data class ClipboardItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val sender: String = "web", // "phone" or "web"
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ClipboardResponse(
    val enabled: Boolean,
    val items: List<ClipboardItem>
)

@Serializable
data class ClipboardPostRequest(
    val text: String,
    val sender: String = "web"
)
