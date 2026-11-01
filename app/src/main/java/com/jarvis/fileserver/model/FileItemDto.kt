package com.jarvis.fileserver.model

import kotlinx.serialization.Serializable

@Serializable
data class FileItemDto(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String,
    val isVideo: Boolean = false,
    val isAudio: Boolean = false,
    val isImage: Boolean = false,
    val isText: Boolean = false
)

@Serializable
data class DirectoryListingResponse(
    val currentPath: String,
    val parentPath: String?,
    val items: List<FileItemDto>,
    val totalCount: Int
)

@Serializable
data class ServerStatusResponse(
    val status: String,
    val rootPath: String,
    val freeSpaceBytes: Long,
    val totalSpaceBytes: Long,
    val clipboardEnabled: Boolean = false,
    val readOnly: Boolean = false,
    val uploadDisabled: Boolean = false,
    val uploadSpeedBps: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val totalBytesSent: Long = 0L,
    val totalBytesReceived: Long = 0L,
    val activeConnections: Int = 0,
    val activeVisitorsCount: Int = 0,
    val uptimeSeconds: Long = 0L,
    val remoteServerMode: Boolean = false
)

@Serializable
data class RenameRequest(
    val path: String,
    val newName: String
)

@Serializable
data class CreateFolderRequest(
    val path: String,
    val name: String
)

@Serializable
data class EditFileRequest(
    val path: String,
    val content: String
)

@Serializable
data class OperationResponse(
    val success: Boolean,
    val message: String
)
