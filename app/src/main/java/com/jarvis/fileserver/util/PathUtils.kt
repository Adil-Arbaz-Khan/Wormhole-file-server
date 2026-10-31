package com.jarvis.fileserver.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object PathUtils {

    fun getPathFromTreeUri(uri: Uri): String? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(uri) ?: return null
            val parts = docId.split(":")
            val type = parts[0]
            val relativePath = if (parts.size > 1) parts[1] else ""

            if ("primary".equals(type, ignoreCase = true)) {
                if (relativePath.isEmpty()) {
                    Environment.getExternalStorageDirectory().absolutePath
                } else {
                    "${Environment.getExternalStorageDirectory().absolutePath}/$relativePath"
                }
            } else {
                "/storage/$type/$relativePath"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPathFromFileUri(context: Context, uri: Uri): String? {
        if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }

        if (DocumentsContract.isDocumentUri(context, uri)) {
            val authority = uri.authority
            val docId = DocumentsContract.getDocumentId(uri)

            if ("com.android.externalstorage.documents" == authority) {
                val split = docId.split(":")
                val type = split[0]
                val path = if (split.size > 1) split[1] else ""
                return if ("primary".equals(type, ignoreCase = true)) {
                    "${Environment.getExternalStorageDirectory().absolutePath}/$path"
                } else {
                    "/storage/$type/$path"
                }
            } else if ("com.android.providers.media.documents" == authority) {
                val split = docId.split(":")
                if (split.size > 1) {
                    val id = split[1]
                    val contentUri = when (split[0]) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> MediaStore.Files.getContentUri("external")
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(id)
                    getDataColumn(context, contentUri, selection, selectionArgs)?.let { return it }
                }
            } else if ("com.android.providers.downloads.documents" == authority) {
                if (docId.startsWith("raw:")) {
                    return docId.removePrefix("raw:")
                }
            }
        }

        if (ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true)) {
            getDataColumn(context, uri, null, null)?.let { return it }
            return copyUriToSharedCache(context, uri)
        }

        return uri.path
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        val column = MediaStore.MediaColumns.DATA
        val projection = arrayOf(column)
        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(column)
                    if (index != -1) {
                        val path = cursor.getString(index)
                        if (!path.isNullOrBlank() && File(path).exists()) {
                            return path
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun copyUriToSharedCache(context: Context, uri: Uri): String? {
        return try {
            val fileName = queryDisplayName(context, uri) ?: "shared_file_${System.currentTimeMillis()}"
            val targetDir = File(context.cacheDir, "shared_stream").apply { mkdirs() }
            val targetFile = File(targetDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun queryDisplayName(context: Context, uri: Uri): String? {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) return cursor.getString(index)
                }
            }
        } catch (_: Exception) {}
        return uri.lastPathSegment
    }
}
