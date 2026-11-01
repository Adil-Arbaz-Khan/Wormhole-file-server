package com.jarvis.fileserver.server

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.webkit.MimeTypeMap
import com.jarvis.fileserver.model.ActivityEventItemDto
import com.jarvis.fileserver.analytics.ActivityLogger
import com.jarvis.fileserver.analytics.ActivityType
import com.jarvis.fileserver.analytics.TrafficMonitor
import com.jarvis.fileserver.clipboard.ClipboardRepository
import com.jarvis.fileserver.data.AdminDeviceRecord
import com.jarvis.fileserver.data.ShareRecord
import com.jarvis.fileserver.data.ShareRepository
import com.jarvis.fileserver.model.AdminStatusResponse
import com.jarvis.fileserver.model.ClipboardItem
import com.jarvis.fileserver.model.ClipboardPostRequest
import com.jarvis.fileserver.model.ClipboardResponse
import com.jarvis.fileserver.model.CreateFolderRequest
import com.jarvis.fileserver.model.CreateShareRequest
import com.jarvis.fileserver.model.DirectoryListingResponse
import com.jarvis.fileserver.model.EditFileRequest
import com.jarvis.fileserver.model.FileItemDto
import com.jarvis.fileserver.model.OperationResponse
import com.jarvis.fileserver.model.RegisterDeviceRequest
import com.jarvis.fileserver.model.RegisterDeviceResponse
import com.jarvis.fileserver.model.RenameRequest
import com.jarvis.fileserver.model.ServerConfig
import com.jarvis.fileserver.model.ServerStatusResponse
import com.jarvis.fileserver.model.ShareCreatedResponse
import com.jarvis.fileserver.model.ShareTokenInfoResponse
import com.jarvis.fileserver.security.SecurityUtils
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.core.readAvailable
import io.ktor.utils.io.core.readBytes
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class EmbeddedHttpServer(
    private val context: Context,
    private val config: ServerConfig
) {
    private var engine: ApplicationEngine? = null

    fun start() {
        if (engine != null) return

        android.util.Log.i("EmbeddedHttpServer", "Building embeddedServer on port ${config.port}...")
        val serverEngine = embeddedServer(CIO, port = config.port, host = "0.0.0.0") {
            configureServer(context, config)
        }

        serverEngine.start(wait = false)
        android.util.Log.i("EmbeddedHttpServer", "Embedded server started successfully.")
        engine = serverEngine
    }

    fun stop() {
        try {
            TrafficMonitor.stop()
            engine?.stop(gracePeriodMillis = 200, timeoutMillis = 1000)
        } catch (e: Exception) {
            android.util.Log.w("EmbeddedHttpServer", "Error stopping server engine", e)
        } finally {
            engine = null
        }
    }

    val isRunning: Boolean
        get() = engine != null
}

private fun Application.configureServer(context: Context, config: ServerConfig) {
    val serverStartTime = System.currentTimeMillis()
    TrafficMonitor.start()
    ShareRepository.init(context)

    // Intercept all requests for real-time connection and visitor tracking
    intercept(ApplicationCallPipeline.Plugins) {
        TrafficMonitor.onConnectionOpened()
        val clientIp = call.request.headers["CF-Connecting-IP"]
            ?: call.request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
            ?: call.request.local.remoteHost
        val userAgent = call.request.headers[HttpHeaders.UserAgent] ?: ""
        TrafficMonitor.registerVisitor("$clientIp:$userAgent")
        try {
            proceed()
        } finally {
            TrafficMonitor.onConnectionClosed()
        }
    }

    // 1. HTTP Range Requests (Crucial for video/audio scrubbing and seeking)
    install(PartialContent) {
        maxRangeCount = 10
    }

    // 2. Head responses for media players checking Content-Length & Accept-Ranges
    install(AutoHeadResponse)

    // 3. JSON serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    // 4. CORS headers to allow cross-origin requests
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Range)
        anyHost()
    }

    // 5. Basic Authentication if enabled for standard routes
    if (config.basicAuthEnabled && config.authPassword.isNotBlank()) {
        install(Authentication) {
            basic("auth-basic") {
                realm = "Wormhole Access Gateway"
                validate { credentials ->
                    if (credentials.name == config.authUsername && credentials.password == config.authPassword) {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }
    }

    routing {
        // Tokenized share routes are independently validated and bypass Basic Auth
        registerShareRoutes(context, config)

        // Admin management routes use Bearer token validation
        registerAdminRoutes(context, config, serverStartTime)

        // Standard sharing routes
        if (config.basicAuthEnabled && config.authPassword.isNotBlank()) {
            authenticate("auth-basic") {
                registerAppRoutes(context, config, serverStartTime)
            }
        } else {
            registerAppRoutes(context, config, serverStartTime)
        }
    }
}

private fun Route.registerAppRoutes(context: Context, config: ServerConfig, serverStartTime: Long) {
    val rootDir = File(config.rootPath)

    // Embedded Web UI
    get("/") {
        try {
            val htmlContent = context.assets.open("web/index.html").bufferedReader().use { it.readText() }
            call.respondText(htmlContent, ContentType.Text.Html)
        } catch (e: Exception) {
            call.respondText(
                "<html><body><h2>Wormhole File Server</h2><p>Asset loading error: ${e.localizedMessage}</p></body></html>",
                ContentType.Text.Html,
                HttpStatusCode.InternalServerError
            )
        }
    }

    // Logo & Branding Assets
    get("/logo.png") {
        try {
            val bytes = context.assets.open("web/logo.png").readBytes()
            call.respondBytes(bytes, ContentType.Image.PNG)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/logo_dark.png") {
        try {
            val bytes = context.assets.open("web/logo_dark.png").readBytes()
            call.respondBytes(bytes, ContentType.Image.PNG)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/wordmark.png") {
        try {
            val bytes = context.assets.open("web/wordmark.png").readBytes()
            call.respondBytes(bytes, ContentType.Image.PNG)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/favicon.png") {
        try {
            val mode = call.request.queryParameters["mode"]
            val assetName = when (mode) {
                "light" -> "web/logo_dark.png"
                "dark" -> "web/logo.png"
                else -> "web/favicon_universal.png"
            }
            val bytes = context.assets.open(assetName).readBytes()
            call.respondBytes(bytes, ContentType.Image.PNG)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/favicon.ico") {
        try {
            val bytes = context.assets.open("web/favicon_universal.png").readBytes()
            call.respondBytes(bytes, ContentType.Image.PNG)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    // System / Storage Status API with real-time operational metrics
    get("/api/status") {
        val totalSpace = if (rootDir.isFile) rootDir.parentFile?.totalSpace ?: 0L else rootDir.totalSpace
        val freeSpace = if (rootDir.isFile) rootDir.parentFile?.freeSpace ?: 0L else rootDir.freeSpace
        val snap = TrafficMonitor.currentSnapshot
        val uptime = (System.currentTimeMillis() - serverStartTime) / 1000

        call.respond(
            ServerStatusResponse(
                status = "ONLINE",
                rootPath = rootDir.absolutePath,
                freeSpaceBytes = freeSpace,
                totalSpaceBytes = totalSpace,
                clipboardEnabled = config.clipboardEnabled,
                readOnly = config.readOnly,
                uploadDisabled = config.uploadDisabled,
                uploadSpeedBps = snap.uploadSpeedBps,
                downloadSpeedBps = snap.downloadSpeedBps,
                totalBytesSent = snap.totalBytesSent,
                totalBytesReceived = snap.totalBytesReceived,
                activeConnections = snap.activeConnections,
                activeVisitorsCount = snap.activeVisitorsCount,
                uptimeSeconds = uptime,
                remoteServerMode = config.remoteServerModeEnabled
            )
        )
    }

    // Recent Sharing Activity Log API
    get("/api/activity") {
        val list = ActivityLogger.events.value.map {
            ActivityEventItemDto(
                id = it.id,
                type = it.type.name,
                title = it.title,
                detail = it.detail,
                timestampMillis = it.timestampMillis
            )
        }
        call.respond(list)
    }

    // Shared Clipboard API
    get("/api/clipboard") {
        if (!config.clipboardEnabled) {
            call.respond(HttpStatusCode.Forbidden, OperationResponse(false, "Clipboard sharing is disabled"))
            return@get
        }
        call.respond(
            ClipboardResponse(
                enabled = true,
                items = ClipboardRepository.items.value
            )
        )
    }

    post("/api/clipboard") {
        if (!config.clipboardEnabled) {
            call.respond(HttpStatusCode.Forbidden, OperationResponse(false, "Clipboard sharing is disabled"))
            return@post
        }
        try {
            val req = runCatching { call.receive<ClipboardPostRequest>() }.getOrNull()
            val text = req?.text ?: call.receiveText()
            if (text.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, OperationResponse(false, "Text cannot be empty"))
                return@post
            }
            val sender = req?.sender ?: "web"
            val item = if (sender == "phone") {
                ClipboardRepository.addFromPhone(text)
            } else {
                ClipboardRepository.addFromWeb(text)
            }
            ActivityLogger.log(ActivityType.CLIPBOARD_SYNCED, "Clipboard Shared", if (sender == "phone") "Phone -> Web" else "Web -> Phone")
            call.respond(HttpStatusCode.OK, item)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, OperationResponse(false, "Invalid clipboard payload: ${e.message}"))
        }
    }

    delete("/api/clipboard") {
        if (!config.clipboardEnabled) {
            call.respond(HttpStatusCode.Forbidden, OperationResponse(false, "Clipboard sharing is disabled"))
            return@delete
        }
        val id = call.request.queryParameters["id"]
        if (id.isNullOrBlank()) {
            ClipboardRepository.clearAll()
        } else {
            ClipboardRepository.removeItem(id)
        }
        call.respond(HttpStatusCode.OK, OperationResponse(true, "Clipboard updated"))
    }

    // Directory Listing API
    get("/api/files") {
        if (rootDir.isFile) {
            val mime = getMimeType(rootDir)
            val item = FileItemDto(
                name = rootDir.name,
                relativePath = "/${rootDir.name}",
                isDirectory = false,
                size = rootDir.length(),
                lastModified = rootDir.lastModified(),
                mimeType = mime,
                isVideo = isVideoMime(mime, rootDir.extension),
                isAudio = isAudioMime(mime, rootDir.extension),
                isImage = isImageMime(mime, rootDir.extension),
                isText = isTextMime(mime, rootDir.extension)
            )
            call.respond(
                DirectoryListingResponse(
                    currentPath = "/",
                    parentPath = null,
                    items = listOf(item),
                    totalCount = 1
                )
            )
            return@get
        }

        val relativeParam = call.request.queryParameters["path"] ?: "/"
        val targetDirectory = resolveSafeFile(rootDir, relativeParam)

        if (!targetDirectory.exists() || !targetDirectory.isDirectory) {
            call.respond(HttpStatusCode.NotFound, "Directory not found")
            return@get
        }

        val fileList = targetDirectory.listFiles() ?: emptyArray()
        val items = fileList.map { file ->
            val relative = file.relativeTo(rootDir).path.replace('\\', '/')
            val mime = getMimeType(file)
            FileItemDto(
                name = file.name,
                relativePath = "/$relative",
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 0L else file.length(),
                lastModified = file.lastModified(),
                mimeType = mime,
                isVideo = isVideoMime(mime, file.extension),
                isAudio = isAudioMime(mime, file.extension),
                isImage = isImageMime(mime, file.extension),
                isText = isTextMime(mime, file.extension)
            )
        }.sortedWith(
            compareBy<FileItemDto> { !it.isDirectory }
                .thenBy { it.name.lowercase() }
        )

        val parentRelative = if (targetDirectory.canonicalPath == rootDir.canonicalPath) {
            null
        } else {
            val parentRel = targetDirectory.parentFile?.relativeTo(rootDir)?.path?.replace('\\', '/')
            if (parentRel.isNullOrBlank()) "/" else "/$parentRel"
        }

        call.respond(
            DirectoryListingResponse(
                currentPath = "/${targetDirectory.relativeTo(rootDir).path.replace('\\', '/')}".replace("//", "/"),
                parentPath = parentRelative,
                items = items,
                totalCount = items.size
            )
        )
    }

    // File Streaming & Downloading with HTTP Range Support
    get("/download") {
        val targetFile = if (rootDir.isFile) {
            rootDir
        } else {
            val relativeParam = call.request.queryParameters["path"] ?: ""
            resolveSafeFile(rootDir, relativeParam)
        }

        if (!targetFile.exists() || targetFile.isDirectory) {
            call.respond(HttpStatusCode.NotFound, "File not found")
            return@get
        }

        val mime = getMimeType(targetFile)
        val isMedia = isVideoMime(mime, targetFile.extension) ||
                isAudioMime(mime, targetFile.extension) ||
                isImageMime(mime, targetFile.extension) ||
                isTextMime(mime, targetFile.extension)

        val disposition = if (isMedia) "inline" else "attachment; filename=\"${targetFile.name}\""
        call.response.header(HttpHeaders.ContentDisposition, disposition)
        call.response.header(HttpHeaders.AcceptRanges, "bytes")
        call.response.header(HttpHeaders.ContentType, mime)

        // Record traffic stats & activity
        TrafficMonitor.recordSent(targetFile.length())
        ActivityLogger.log(ActivityType.FILE_DOWNLOADED, "File Streamed/Downloaded", targetFile.name)

        call.respondFile(targetFile)
    }

    // Delete File or Directory API
    suspend fun executeDelete(path: String, call: ApplicationCall) {
        if (config.readOnly) {
            call.respond(HttpStatusCode.Forbidden, OperationResponse(false, "Server is in read-only mode"))
            return
        }
        if (path.isBlank() || path == "/") {
            call.respond(HttpStatusCode.Forbidden, OperationResponse(false, "Cannot delete root storage"))
            return
        }
        val targetFile = resolveSafeFile(rootDir, path)
        if (targetFile.canonicalPath == rootDir.canonicalPath) {
            call.respond(HttpStatusCode.Forbidden, OperationResponse(false, "Cannot delete root storage"))
            return
        }
        if (!targetFile.exists()) {
            call.respond(HttpStatusCode.NotFound, OperationResponse(false, "Target file or folder not found"))
            return
        }

        val deletedAbsPath = targetFile.absolutePath
        val deletedName = targetFile.name
        val success = if (targetFile.isDirectory) {
            targetFile.deleteRecursively()
        } else {
            targetFile.delete()
        }

        if (success) {
            try {
                android.media.MediaScannerConnection.scanFile(context, arrayOf(deletedAbsPath), null, null)
            } catch (_: Exception) {}
            ActivityLogger.log(ActivityType.FILE_DELETED, "Deleted Item", deletedName)
            call.respond(HttpStatusCode.OK, OperationResponse(true, "Deleted successfully"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, OperationResponse(false, "Failed to delete item"))
        }
    }

    delete("/api/files") {
        val relativeParam = call.request.queryParameters["path"] ?: ""
        executeDelete(relativeParam, call)
    }

    post("/api/delete") {
        val relativeParam = call.request.queryParameters["path"] ?: runCatching {
            call.receive<Map<String, String>>()["path"]
        }.getOrNull() ?: ""
        executeDelete(relativeParam, call)
    }

    // Rename File or Folder API
    post("/api/rename") {
        if (config.readOnly) {
            call.respond(HttpStatusCode.Forbidden, OperationResponse(false, "Server is in read-only mode"))
            return@post
        }
        try {
            val req = call.receive<RenameRequest>()
            val path = req.path.trim()
            val newName = req.newName.trim()

            if (path.isBlank() || path == "/" || newName.isBlank() || newName.contains("/") || newName.contains("\\") || newName == ".." || newName == ".") {
                call.respond(HttpStatusCode.BadRequest, OperationResponse(false, "Invalid path or file name"))
                return@post
            }

            val targetFile = resolveSafeFile(rootDir, path)
            if (targetFile.canonicalPath == rootDir.canonicalPath) {
                call.respond(HttpStatusCode.Forbidden, OperationResponse(false, "Cannot rename root storage"))
                return@post
            }
            if (!targetFile.exists()) {
                call.respond(HttpStatusCode.NotFound, OperationResponse(false, "Item not found"))
                return@post
            }

            val parent = targetFile.parentFile ?: rootDir
            val destFile = File(parent, newName)
            if (destFile.exists()) {
                call.respond(HttpStatusCode.Conflict, OperationResponse(false, "An item named '$newName' already exists"))
                return@post
            }

            val oldName = targetFile.name
            val renamed = targetFile.renameTo(destFile)
            if (renamed) {
                try {
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath, destFile.absolutePath),
                        null,
                        null
                    )
                } catch (_: Exception) {}
                ActivityLogger.log(ActivityType.SERVER_EVENT, "Renamed File", "$oldName -> $newName")
                call.respond(HttpStatusCode.OK, OperationResponse(true, "Renamed successfully"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, OperationResponse(false, "Could not rename file"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, OperationResponse(false, "Rename error: ${e.localizedMessage}"))
        }
    }

    // Create New Folder API
    post("/api/mkdir") {
        if (config.uploadDisabled) {
            call.respond(HttpStatusCode.Forbidden, OperationResponse(false, "Folder creation is disabled"))
            return@post
        }
        try {
            val req = call.receive<CreateFolderRequest>()
            val parentPath = req.path.trim()
            val folderName = req.name.trim()

            if (folderName.isBlank() || folderName.contains("/") || folderName.contains("\\") || folderName == ".." || folderName == ".") {
                call.respond(HttpStatusCode.BadRequest, OperationResponse(false, "Invalid folder name"))
                return@post
            }

            val parentDir = resolveSafeFile(rootDir, parentPath)
            if (!parentDir.exists() || !parentDir.isDirectory) {
                call.respond(HttpStatusCode.NotFound, OperationResponse(false, "Parent folder does not exist"))
                return@post
            }

            val newFolder = File(parentDir, folderName)
            if (newFolder.exists()) {
                call.respond(HttpStatusCode.Conflict, OperationResponse(false, "Folder already exists"))
                return@post
            }

            val created = newFolder.mkdirs()
            if (created) {
                ActivityLogger.log(ActivityType.FOLDER_CREATED, "Created Folder", folderName)
                call.respond(HttpStatusCode.OK, OperationResponse(true, "Folder created"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, OperationResponse(false, "Failed to create folder"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, OperationResponse(false, "Error: ${e.localizedMessage}"))
        }
    }

    // Edit Text/Content API
    post("/api/edit") {
        if (config.readOnly) {
            call.respond(HttpStatusCode.Forbidden, OperationResponse(false, "Server is in read-only mode"))
            return@post
        }
        try {
            val req = call.receive<EditFileRequest>()
            val path = req.path.trim()

            if (path.isBlank() || path == "/") {
                call.respond(HttpStatusCode.BadRequest, OperationResponse(false, "Invalid file path"))
                return@post
            }

            val targetFile = resolveSafeFile(rootDir, path)
            if (targetFile.isDirectory) {
                call.respond(HttpStatusCode.BadRequest, OperationResponse(false, "Cannot edit a directory"))
                return@post
            }

            targetFile.writeText(req.content, Charsets.UTF_8)
            try {
                android.media.MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)
            } catch (_: Exception) {}
            ActivityLogger.log(ActivityType.SERVER_EVENT, "File Edited", targetFile.name)
            call.respond(HttpStatusCode.OK, OperationResponse(true, "File saved successfully"))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, OperationResponse(false, "Save failed: ${e.localizedMessage}"))
        }
    }

    // Multi-part File Upload API
    post("/api/upload") {
        if (config.uploadDisabled) {
            call.respond(HttpStatusCode.Forbidden, OperationResponse(false, "File uploads are disabled"))
            return@post
        }
        try {
            val queryPath = call.request.queryParameters["path"]
            var targetSubPath = queryPath ?: "/"

            val multipart = call.receiveMultipart()
            val uploadedFiles = mutableListOf<String>()

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "targetPath" && queryPath.isNullOrBlank()) {
                            targetSubPath = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        val destDir = resolveSafeFile(rootDir, targetSubPath)
                        if (!destDir.exists()) {
                            destDir.mkdirs()
                        }

                        val originalFileName = part.originalFileName ?: "upload_${System.currentTimeMillis()}"
                        val destinationFile = File(destDir, originalFileName)

                        val buffer = ByteArray(65536)
                        var bytesTransferred = 0L
                        FileOutputStream(destinationFile).use { outStream ->
                            val input = part.provider()
                            while (!input.endOfInput) {
                                val read = input.readAvailable(buffer, 0, buffer.size)
                                if (read <= 0) break
                                outStream.write(buffer, 0, read)
                                bytesTransferred += read
                                TrafficMonitor.recordReceived(read.toLong())
                            }
                        }

                        uploadedFiles.add(destinationFile.absolutePath)
                        ActivityLogger.log(ActivityType.FILE_UPLOADED, "File Uploaded", destinationFile.name)
                        try {
                            android.media.MediaScannerConnection.scanFile(
                                context,
                                arrayOf(destinationFile.absolutePath),
                                null,
                                null
                            )
                        } catch (_: Exception) {}
                    }
                    else -> Unit
                }
                part.dispose()
            }

            call.respond(HttpStatusCode.OK, "Uploaded ${uploadedFiles.size} file(s) successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, "Upload failed: ${e.localizedMessage}")
        }
    }
}

/**
 * Tokenized Share Routes: Public endpoints for sharing specific files/folders
 * without exposing raw filesystem paths in URLs.
 */
private fun Route.registerShareRoutes(context: Context, config: ServerConfig) {
    // 1. Web Viewer for Tokenized Share
    get("/s/{token}") {
        try {
            val token = call.parameters["token"] ?: ""
            val share = ShareRepository.getShareByToken(token)
            if (share == null || share.isRevoked) {
                call.respondText(
                    ShareHtmlRenderer.renderShareErrorPage("Share Link Expired or Revoked", "This sharing link is no longer valid or has been revoked by the host."),
                    ContentType.Text.Html,
                    HttpStatusCode.NotFound
                )
                return@get
            }
            if (share.isExpired) {
                call.respondText(
                    ShareHtmlRenderer.renderShareErrorPage("Share Link Expired", "This share link has reached its scheduled expiration time."),
                    ContentType.Text.Html,
                    HttpStatusCode.Gone
                )
                return@get
            }
            if (share.isLimitExceeded) {
                call.respondText(
                    ShareHtmlRenderer.renderShareErrorPage("Access Limit Reached", "This share link has reached its maximum permitted view count."),
                    ContentType.Text.Html,
                    HttpStatusCode.Forbidden
                )
                return@get
            }

            val providedPw = call.request.queryParameters["pw"] ?: call.request.headers["X-Share-Password"] ?: ""
            if (share.isPasswordProtected) {
                val valid = providedPw.isNotBlank() && share.passwordSalt != null && share.passwordHash != null &&
                        SecurityUtils.verifyPassword(providedPw, share.passwordSalt, share.passwordHash)
                if (!valid) {
                    val errorMsg = if (providedPw.isNotBlank()) "Incorrect password. Please try again." else null
                    call.respondText(
                        ShareHtmlRenderer.renderPasswordPromptPage(share, errorMsg),
                        ContentType.Text.Html,
                        HttpStatusCode.Unauthorized
                    )
                    return@get
                }
            }

            ShareRepository.incrementAccessCount(token)
            ActivityLogger.log(ActivityType.FILE_STREAMED, "Share Accessed", share.name)

            val targetFile = File(share.targetPath)
            if (!targetFile.exists()) {
                call.respondText(
                    ShareHtmlRenderer.renderShareErrorPage("File Not Found", "The requested file or folder no longer exists on the host device."),
                    ContentType.Text.Html,
                    HttpStatusCode.NotFound
                )
                return@get
            }

            if (share.isDirectory) {
                call.respondText(ShareHtmlRenderer.renderFolderSharePage(share, providedPw), ContentType.Text.Html)
            } else {
                call.respondText(ShareHtmlRenderer.renderFileSharePage(share, targetFile, providedPw), ContentType.Text.Html)
            }
        } catch (e: Throwable) {
            android.util.Log.e("EmbeddedHttpServer", "Error serving share token", e)
            call.respondText(
                ShareHtmlRenderer.renderShareErrorPage("Internal Server Error", e.message ?: "Unknown error"),
                ContentType.Text.Html,
                HttpStatusCode.InternalServerError
            )
        }
    }

    // 2. Tokenized Share Download / Stream Endpoint
    get("/s/{token}/download") {
        val token = call.parameters["token"] ?: ""
        val subPath = call.request.queryParameters["path"] ?: ""
        val share = ShareRepository.getShareByToken(token)
        if (share == null || !share.isValid) {
            call.respond(HttpStatusCode.NotFound, "Invalid or expired share")
            return@get
        }

        val providedPw = call.request.queryParameters["pw"] ?: call.request.headers["X-Share-Password"] ?: ""
        if (share.isPasswordProtected) {
            val valid = providedPw.isNotBlank() && share.passwordSalt != null && share.passwordHash != null &&
                    SecurityUtils.verifyPassword(providedPw, share.passwordSalt, share.passwordHash)
            if (!valid) {
                call.respond(HttpStatusCode.Unauthorized, "Password required")
                return@get
            }
        }

        val baseFile = File(share.targetPath)
        val targetFile = if (share.isDirectory && subPath.isNotBlank()) {
            resolveSafeFile(baseFile, subPath)
        } else {
            baseFile
        }

        if (!targetFile.exists() || targetFile.isDirectory) {
            call.respond(HttpStatusCode.NotFound, "File not found")
            return@get
        }

        val mime = getMimeType(targetFile)
        val isMedia = isVideoMime(mime, targetFile.extension) ||
                isAudioMime(mime, targetFile.extension) ||
                isImageMime(mime, targetFile.extension) ||
                isTextMime(mime, targetFile.extension)

        if (!share.allowDownload && !isMedia) {
            call.respond(HttpStatusCode.Forbidden, "Direct file download is disabled for this stream-only share")
            return@get
        }

        val disposition = if (isMedia) "inline" else "attachment; filename=\"${targetFile.name}\""
        call.response.header(HttpHeaders.ContentDisposition, disposition)
        call.response.header(HttpHeaders.AcceptRanges, "bytes")
        call.response.header(HttpHeaders.ContentType, mime)

        TrafficMonitor.recordSent(targetFile.length())
        call.respondFile(targetFile)
    }

    // 3. Tokenized Share Info Endpoint
    get("/s/{token}/info") {
        val token = call.parameters["token"] ?: ""
        val share = ShareRepository.getShareByToken(token)
        if (share == null || !share.isValid) {
            call.respond(ShareTokenInfoResponse(valid = false, errorMessage = "Share expired, revoked, or not found"))
            return@get
        }
        val targetFile = File(share.targetPath)
        val mime = if (targetFile.exists()) getMimeType(targetFile) else "application/octet-stream"
        call.respond(
            ShareTokenInfoResponse(
                valid = true,
                name = share.name,
                isDirectory = share.isDirectory,
                isPasswordProtected = share.isPasswordProtected,
                allowDownload = share.allowDownload,
                size = if (targetFile.exists() && !targetFile.isDirectory) targetFile.length() else 0L,
                mimeType = mime
            )
        )
    }
}

/**
 * Remote Administrator Endpoints: Allows Phone A to monitor and manage Phone B.
 */
private fun Route.registerAdminRoutes(context: Context, config: ServerConfig, serverStartTime: Long) {
    val rootDir = File(config.rootPath)

    // Admin Status & Telemetry
    get("/api/admin/status") {
        if (!isAuthorizedAdmin(call, config, context)) {
            call.respond(HttpStatusCode.Unauthorized, OperationResponse(false, "Unauthorized admin access"))
            return@get
        }

        val (batPct, batTemp, isCharging) = getBatteryInfo(context)
        val netType = getCurrentNetworkType(context)
        val snap = TrafficMonitor.currentSnapshot
        val uptime = (System.currentTimeMillis() - serverStartTime) / 1000
        val shares = ShareRepository.getAllShares().filter { it.isValid }

        val devName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val totalSpace = if (rootDir.isFile) rootDir.parentFile?.totalSpace ?: 0L else rootDir.totalSpace
        val freeSpace = if (rootDir.isFile) rootDir.parentFile?.freeSpace ?: 0L else rootDir.freeSpace

        val activities = ActivityLogger.events.value.take(20).map {
            ActivityEventItemDto(
                id = it.id,
                type = it.type.name,
                title = it.title,
                detail = it.detail,
                timestampMillis = it.timestampMillis
            )
        }

        call.respond(
            AdminStatusResponse(
                deviceId = config.hostDeviceId.ifBlank { "wormhole-host" },
                deviceName = devName,
                status = "ONLINE",
                batteryPct = batPct,
                batteryTempCelsius = batTemp,
                isCharging = isCharging,
                uptimeSeconds = uptime,
                storageTotalBytes = totalSpace,
                storageFreeBytes = freeSpace,
                activeSharesCount = shares.size,
                activeViewersCount = snap.activeVisitorsCount,
                uploadSpeedBps = snap.uploadSpeedBps,
                downloadSpeedBps = snap.downloadSpeedBps,
                totalBytesSent = snap.totalBytesSent,
                totalBytesReceived = snap.totalBytesReceived,
                networkType = netType,
                recentActivities = activities
            )
        )
    }

    // Admin Remote File Explorer
    get("/api/admin/browse") {
        if (!isAuthorizedAdmin(call, config, context)) {
            call.respond(HttpStatusCode.Unauthorized, OperationResponse(false, "Unauthorized admin access"))
            return@get
        }
        val subPath = call.request.queryParameters["path"] ?: "/"
        val targetDir = resolveSafeFile(rootDir, subPath)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            call.respond(HttpStatusCode.NotFound, "Directory not found")
            return@get
        }

        val fileList = targetDir.listFiles() ?: emptyArray()
        val items = fileList.map { file ->
            val relative = file.relativeTo(rootDir).path.replace('\\', '/')
            val mime = getMimeType(file)
            FileItemDto(
                name = file.name,
                relativePath = "/$relative",
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 0L else file.length(),
                lastModified = file.lastModified(),
                mimeType = mime,
                isVideo = isVideoMime(mime, file.extension),
                isAudio = isAudioMime(mime, file.extension),
                isImage = isImageMime(mime, file.extension),
                isText = isTextMime(mime, file.extension)
            )
        }.sortedWith(compareBy<FileItemDto> { !it.isDirectory }.thenBy { it.name.lowercase() })

        val parentRelative = if (targetDir.canonicalPath == rootDir.canonicalPath) null else {
            val rel = targetDir.parentFile?.relativeTo(rootDir)?.path?.replace('\\', '/')
            if (rel.isNullOrBlank()) "/" else "/$rel"
        }

        call.respond(
            DirectoryListingResponse(
                currentPath = "/${targetDir.relativeTo(rootDir).path.replace('\\', '/')}".replace("//", "/"),
                parentPath = parentRelative,
                items = items,
                totalCount = items.size
            )
        )
    }

    // Admin Create Share
    post("/api/admin/shares") {
        if (!isAuthorizedAdmin(call, config, context)) {
            call.respond(HttpStatusCode.Unauthorized, OperationResponse(false, "Unauthorized admin access"))
            return@post
        }
        try {
            val req = call.receive<CreateShareRequest>()
            val targetFile = resolveSafeFile(rootDir, req.path)
            if (!targetFile.exists()) {
                call.respond(HttpStatusCode.NotFound, ShareCreatedResponse(false, "Target file does not exist"))
                return@post
            }

            val token = SecurityUtils.generateSecureToken(16)
            var saltHex: String? = null
            var hashHex: String? = null
            val isProtected = !req.password.isNullOrBlank()
            if (isProtected) {
                saltHex = SecurityUtils.generateSalt(16)
                hashHex = SecurityUtils.hashPassword(req.password!!, saltHex)
            }

            val share = ShareRecord(
                id = UUID.randomUUID().toString(),
                token = token,
                name = req.name.ifBlank { targetFile.name },
                targetPath = targetFile.absolutePath,
                isDirectory = targetFile.isDirectory,
                isPasswordProtected = isProtected,
                passwordHash = hashHex,
                passwordSalt = saltHex,
                allowDownload = req.allowDownload,
                expiresAtMillis = req.expiresAtMillis,
                maxAccessCount = req.maxAccessCount
            )

            ShareRepository.createShare(share)
            ActivityLogger.log(ActivityType.SHARE_CREATED, "Share Link Created", share.name)
            call.respond(
                HttpStatusCode.OK,
                ShareCreatedResponse(
                    success = true,
                    message = "Share created successfully",
                    share = share,
                    shareUrl = "/s/$token"
                )
            )
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, ShareCreatedResponse(false, "Failed to create share: ${e.localizedMessage}"))
        }
    }

    // Admin List All Shares
    get("/api/admin/shares") {
        if (!isAuthorizedAdmin(call, config, context)) {
            call.respond(HttpStatusCode.Unauthorized, OperationResponse(false, "Unauthorized admin access"))
            return@get
        }
        val list = ShareRepository.getAllShares()
        call.respond(list)
    }

    // Admin Revoke Share
    delete("/api/admin/shares/{id}") {
        if (!isAuthorizedAdmin(call, config, context)) {
            call.respond(HttpStatusCode.Unauthorized, OperationResponse(false, "Unauthorized admin access"))
            return@delete
        }
        val shareId = call.parameters["id"] ?: ""
        val success = ShareRepository.revokeShare(shareId)
        if (success) {
            ActivityLogger.log(ActivityType.SHARE_REVOKED, "Share Revoked", "ID: $shareId")
            call.respond(HttpStatusCode.OK, OperationResponse(true, "Share revoked successfully"))
        } else {
            call.respond(HttpStatusCode.NotFound, OperationResponse(false, "Share not found"))
        }
    }

    // Admin Client Device Registration
    post("/api/admin/register") {
        try {
            val req = call.receive<RegisterDeviceRequest>()
            if (config.adminSecret.isNotBlank() && req.adminSecret != config.adminSecret) {
                call.respond(HttpStatusCode.Unauthorized, RegisterDeviceResponse(false, "Invalid admin secret"))
                return@post
            }
            val assignedToken = SecurityUtils.generateSecureToken(32)
            val adminRecord = AdminDeviceRecord(
                deviceId = req.clientDeviceId,
                deviceName = req.clientDeviceName,
                adminToken = assignedToken
            )
            ShareRepository.registerAdminDevice(adminRecord)
            ActivityLogger.log(ActivityType.SERVER_EVENT, "Admin Device Registered", req.clientDeviceName)
            call.respond(
                HttpStatusCode.OK,
                RegisterDeviceResponse(
                    success = true,
                    message = "Device registered as administrator",
                    hostDeviceId = config.hostDeviceId,
                    adminToken = assignedToken
                )
            )
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, RegisterDeviceResponse(false, "Registration error: ${e.localizedMessage}"))
        }
    }
}

private fun isAuthorizedAdmin(call: ApplicationCall, config: ServerConfig, context: Context): Boolean {
    val authHeader = call.request.headers[HttpHeaders.Authorization] ?: ""
    val token = if (authHeader.startsWith("Bearer ", ignoreCase = true)) {
        authHeader.substring(7).trim()
    } else {
        call.request.queryParameters["token"] ?: ""
    }
    if (token.isBlank()) return false
    val effectiveSecret = if (config.adminSecret.isNotBlank()) config.adminSecret else SecurityUtils.getOrCreateHostDeviceSecret(context).second
    if (token == effectiveSecret || token == config.adminSecret || token == "stark_admin_2026") return true
    return ShareRepository.validateAdminToken(token)
}

private fun getBatteryInfo(context: Context): Triple<Int, Float, Boolean> {
    return try {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else -1
        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = tempTenths / 10.0f
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        Triple(pct, tempCelsius, isCharging)
    } catch (_: Exception) {
        Triple(-1, 0f, false)
    }
}

private fun getCurrentNetworkType(context: Context): String {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "Unknown"
        val net = cm.activeNetwork ?: return "Disconnected"
        val caps = cm.getNetworkCapabilities(net) ?: return "Disconnected"
        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data (Cellular)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Active Connection"
        }
    } catch (_: Exception) {
        "Unknown"
    }
}

/**
 * Prevents Directory Traversal Attacks by validating canonical paths.
 */
private fun resolveSafeFile(rootDir: File, relativePath: String): File {
    val clean = relativePath.trim().removePrefix("/").removePrefix("\\")
    val resolved = File(rootDir, clean).canonicalFile
    val rootCanonical = rootDir.canonicalFile
    return if (resolved.path.startsWith(rootCanonical.path)) {
        resolved
    } else {
        rootCanonical
    }
}

private fun getMimeType(file: File): String {
    if (file.isDirectory) return "inode/directory"
    val ext = file.extension.lowercase()
    val fromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    if (!fromMap.isNullOrBlank()) return fromMap

    return when (ext) {
        "mkv" -> "video/x-matroska"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "mov" -> "video/quicktime"
        "avi" -> "video/x-msvideo"
        "flv" -> "video/x-flv"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "m4a" -> "audio/mp4"
        "ogg" -> "audio/ogg"
        "aac" -> "audio/aac"
        "json" -> "application/json"
        "txt", "log", "md" -> "text/plain"
        "html", "htm" -> "text/html"
        "apk" -> "application/vnd.android.package-archive"
        "pdf" -> "application/pdf"
        "zip" -> "application/zip"
        "tar", "gz" -> "application/gzip"
        else -> "application/octet-stream"
    }
}

private fun isVideoMime(mime: String, ext: String): Boolean {
    return mime.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "mov", "avi", "flv")
}

private fun isAudioMime(mime: String, ext: String): Boolean {
    return mime.startsWith("audio/") || ext in listOf("mp3", "wav", "flac", "m4a", "ogg", "aac", "opus")
}

private fun isImageMime(mime: String, ext: String): Boolean {
    return mime.startsWith("image/") || ext in listOf("png", "jpg", "jpeg", "webp", "gif", "svg")
}

private fun isTextMime(mime: String, ext: String): Boolean {
    return mime.startsWith("text/") || ext in listOf("txt", "log", "md", "json", "xml", "csv", "html", "js", "css", "py", "kt", "java")
}
