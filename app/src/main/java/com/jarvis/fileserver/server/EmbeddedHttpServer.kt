package com.jarvis.fileserver.server

import android.content.Context
import android.webkit.MimeTypeMap
import com.jarvis.fileserver.clipboard.ClipboardRepository
import com.jarvis.fileserver.model.ClipboardItem
import com.jarvis.fileserver.model.ClipboardPostRequest
import com.jarvis.fileserver.model.ClipboardResponse
import com.jarvis.fileserver.model.DirectoryListingResponse
import com.jarvis.fileserver.model.FileItemDto
import com.jarvis.fileserver.model.ServerConfig
import com.jarvis.fileserver.model.ServerStatusResponse
import com.jarvis.fileserver.model.RenameRequest
import com.jarvis.fileserver.model.CreateFolderRequest
import com.jarvis.fileserver.model.EditFileRequest
import com.jarvis.fileserver.model.OperationResponse
import io.ktor.utils.io.core.readAvailable
import io.ktor.utils.io.core.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
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
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

class EmbeddedHttpServer(
    private val context: Context,
    private val config: ServerConfig
) {
    private var engine: ApplicationEngine? = null

    fun start() {
        if (engine != null) return

        android.util.Log.i("EmbeddedHttpServer", "Building embeddedServer...")
        val serverEngine = embeddedServer(CIO, port = config.port, host = "0.0.0.0") {
            configureServer(context, config)
        }

        android.util.Log.i("EmbeddedHttpServer", "Invoking serverEngine.start(wait = false)...")
        serverEngine.start(wait = false)
        android.util.Log.i("EmbeddedHttpServer", "serverEngine.start returned!")
        engine = serverEngine
    }

    fun stop() {
        try {
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

    // 5. Basic Authentication if enabled
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
        if (config.basicAuthEnabled && config.authPassword.isNotBlank()) {
            authenticate("auth-basic") {
                registerAppRoutes(context, config)
            }
        } else {
            registerAppRoutes(context, config)
        }
    }
}

private fun Route.registerAppRoutes(context: Context, config: ServerConfig) {
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

    // System / Storage Status API
    get("/api/status") {
        val totalSpace = if (rootDir.isFile) rootDir.parentFile?.totalSpace ?: 0L else rootDir.totalSpace
        val freeSpace = if (rootDir.isFile) rootDir.parentFile?.freeSpace ?: 0L else rootDir.freeSpace
        call.respond(
            ServerStatusResponse(
                status = "ONLINE",
                rootPath = rootDir.absolutePath,
                freeSpaceBytes = freeSpace,
                totalSpaceBytes = totalSpace,
                clipboardEnabled = config.clipboardEnabled
            )
        )
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

        val parentRelative = if (targetDirectory.absolutePath == rootDir.absolutePath) {
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
        val contentType = ContentType.parse(mime)

        // Ensure browser receives inline disposition for media players
        val isMedia = isVideoMime(mime, targetFile.extension) ||
                isAudioMime(mime, targetFile.extension) ||
                isImageMime(mime, targetFile.extension) ||
                isTextMime(mime, targetFile.extension)

        val disposition = if (isMedia) "inline" else "attachment; filename=\"${targetFile.name}\""
        call.response.header(HttpHeaders.ContentDisposition, disposition)
        call.response.header(HttpHeaders.AcceptRanges, "bytes")
        call.response.header(HttpHeaders.ContentType, mime)

        // Ktor's PartialContent feature hooks into respondFile to provide 206 Partial Content
        call.respondFile(targetFile)
    }

    // Delete File or Directory API (supports DELETE and POST)
    suspend fun executeDelete(path: String, call: ApplicationCall) {
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
        val success = if (targetFile.isDirectory) {
            targetFile.deleteRecursively()
        } else {
            targetFile.delete()
        }

        if (success) {
            try {
                android.media.MediaScannerConnection.scanFile(context, arrayOf(deletedAbsPath), null, null)
            } catch (_: Exception) {}
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
            call.respond(HttpStatusCode.OK, OperationResponse(true, "File saved successfully"))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, OperationResponse(false, "Save failed: ${e.localizedMessage}"))
        }
    }

    // Multi-part File Upload API
    post("/api/upload") {
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
                        FileOutputStream(destinationFile).use { outStream ->
                            val input = part.provider()
                            while (!input.endOfInput) {
                                val read = input.readAvailable(buffer, 0, buffer.size)
                                if (read <= 0) break
                                outStream.write(buffer, 0, read)
                            }
                        }

                        uploadedFiles.add(destinationFile.absolutePath)
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
    return mime.startsWith("video/") || ext.lowercase() in setOf("mp4", "mkv", "webm", "avi", "mov", "flv", "m4v")
}

private fun isAudioMime(mime: String, ext: String): Boolean {
    return mime.startsWith("audio/") || ext.lowercase() in setOf("mp3", "wav", "flac", "m4a", "ogg", "aac", "opus")
}

private fun isImageMime(mime: String, ext: String): Boolean {
    return mime.startsWith("image/") || ext.lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp")
}

private fun isTextMime(mime: String, ext: String): Boolean {
    val textExts = setOf(
        "txt", "log", "md", "json", "js", "ts", "html", "htm", "css",
        "xml", "kt", "java", "c", "cpp", "h", "hpp", "py", "sh", "yaml",
        "yml", "ini", "conf", "properties", "gradle", "bat", "cmd", "csv", "sql"
    )
    return mime.startsWith("text/") || mime == "application/json" || mime == "application/xml" || ext.lowercase() in textExts
}
