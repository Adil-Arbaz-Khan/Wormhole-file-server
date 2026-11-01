package com.jarvis.fileserver.server

import com.jarvis.fileserver.data.ShareRecord
import java.io.File

object ShareHtmlRenderer {

    fun renderPasswordPromptPage(share: ShareRecord, errorMessage: String? = null): String {
        val errorBlock = if (errorMessage != null) {
            """<div style="background: rgba(239, 68, 68, 0.15); border: 1px solid #ef4444; color: #fca5a5; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; font-size: 0.9rem;">$errorMessage</div>"""
        } else ""

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Wormhole — Password Protected Share</title>
    <style>
        :root { --bg: #090d16; --surface: #131b2e; --accent: #38bdf8; --text: #f8fafc; --text-muted: #94a3b8; }
        body { margin: 0; background: var(--bg); color: var(--text); font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; box-sizing: border-box; }
        .card { background: var(--surface); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 16px; padding: 32px 28px; width: 100%; max-width: 420px; box-shadow: 0 20px 40px rgba(0,0,0,0.5); text-align: center; }
        .logo { width: 56px; height: 56px; margin-bottom: 12px; }
        h2 { margin: 0 0 6px 0; font-size: 1.4rem; font-weight: 700; }
        p { color: var(--text-muted); font-size: 0.95rem; margin: 0 0 24px 0; }
        .input-group { margin-bottom: 20px; text-align: left; }
        label { display: block; font-size: 0.85rem; font-weight: 600; color: var(--text-muted); margin-bottom: 6px; }
        input[type="password"] { width: 100%; padding: 12px 14px; border-radius: 8px; border: 1px solid rgba(255, 255, 255, 0.15); background: rgba(0,0,0,0.3); color: #fff; font-size: 1rem; box-sizing: border-box; outline: none; }
        input[type="password"]:focus { border-color: var(--accent); }
        button { width: 100%; padding: 12px; border-radius: 8px; border: none; background: var(--accent); color: #020617; font-size: 1rem; font-weight: 700; cursor: pointer; transition: opacity 0.2s; }
        button:hover { opacity: 0.9; }
    </style>
</head>
<body>
    <div class="card">
        <img src="/logo.png" alt="Wormhole" class="logo" />
        <h2>Protected Content</h2>
        <p>Access to "<strong>${escapeHtml(share.name)}</strong>" requires a password.</p>
        $errorBlock
        <form method="GET" action="/s/${share.token}">
            <div class="input-group">
                <label for="pw">Enter Password</label>
                <input type="password" id="pw" name="pw" placeholder="Share password" required autofocus />
            </div>
            <button type="submit">Unlock & Access</button>
        </form>
    </div>
</body>
</html>
        """.trimIndent()
    }

    fun renderFileSharePage(share: ShareRecord, file: File, pw: String): String {
        val ext = file.extension.lowercase()
        val mime = getMime(ext)
        val isVideo = ext in listOf("mp4", "mkv", "webm", "mov", "avi")
        val isAudio = ext in listOf("mp3", "wav", "flac", "m4a", "ogg", "aac")
        val isImage = ext in listOf("jpg", "jpeg", "png", "gif", "webp", "svg")

        val pwParam = if (pw.isNotBlank()) "?pw=${java.net.URLEncoder.encode(pw, "UTF-8")}" else ""
        val mediaUrl = "/s/${share.token}/download$pwParam"

        val mediaViewer = when {
            isVideo -> """
                <video controls autoplay style="width: 100%; max-height: 520px; border-radius: 12px; background: #000; margin-bottom: 20px;">
                    <source src="$mediaUrl" type="$mime">
                    Your browser does not support HTML5 video streaming.
                </video>
            """.trimIndent()
            isAudio -> """
                <div style="background: rgba(0,0,0,0.3); border-radius: 12px; padding: 24px; margin-bottom: 20px; text-align: center;">
                    <audio controls autoplay style="width: 100%;">
                        <source src="$mediaUrl" type="$mime">
                        Your browser does not support HTML5 audio streaming.
                    </audio>
                </div>
            """.trimIndent()
            isImage -> """
                <div style="text-align: center; margin-bottom: 20px;">
                    <img src="$mediaUrl" alt="${escapeHtml(file.name)}" style="max-width: 100%; max-height: 520px; border-radius: 12px; object-fit: contain; box-shadow: 0 8px 24px rgba(0,0,0,0.4);" />
                </div>
            """.trimIndent()
            else -> """
                <div style="background: rgba(255,255,255,0.03); border: 1px dashed rgba(255,255,255,0.15); border-radius: 12px; padding: 40px; margin-bottom: 20px; text-align: center; color: #94a3b8;">
                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline></svg>
                    <div style="margin-top: 10px; font-size: 0.95rem;">File ready for download</div>
                </div>
            """.trimIndent()
        }

        val downloadButton = if (share.allowDownload) {
            """
            <a href="$mediaUrl" download="${escapeHtml(file.name)}" style="display: inline-flex; align-items: center; justify-content: center; gap: 8px; width: 100%; padding: 14px; border-radius: 8px; background: #38bdf8; color: #020617; font-size: 1rem; font-weight: 700; text-decoration: none; box-sizing: border-box; transition: opacity 0.2s;">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
                Download (${formatBytes(file.length())})
            </a>
            """.trimIndent()
        } else {
            """
            <div style="background: rgba(56, 189, 248, 0.1); border: 1px solid rgba(56, 189, 248, 0.3); color: #38bdf8; padding: 10px 14px; border-radius: 8px; text-align: center; font-size: 0.9rem; font-weight: 600;">
                Stream-Only Permission (Direct download disabled by host)
            </div>
            """.trimIndent()
        }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${escapeHtml(file.name)} — Wormhole Share</title>
    <style>
        :root { --bg: #090d16; --surface: #131b2e; --accent: #38bdf8; --text: #f8fafc; --text-muted: #94a3b8; }
        body { margin: 0; background: var(--bg); color: var(--text); font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; box-sizing: border-box; }
        .card { background: var(--surface); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 16px; padding: 28px; width: 100%; max-width: 720px; box-shadow: 0 20px 40px rgba(0,0,0,0.5); }
        .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; border-bottom: 1px solid rgba(255,255,255,0.06); padding-bottom: 14px; }
        .logo-row { display: flex; align-items: center; gap: 10px; font-weight: 700; font-size: 1.1rem; }
        .logo { width: 32px; height: 32px; }
        .badge { background: rgba(56,189,248,0.15); color: var(--accent); padding: 4px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; }
        .title { font-size: 1.25rem; font-weight: 700; word-break: break-all; margin: 0 0 6px 0; }
        .meta { color: var(--text-muted); font-size: 0.85rem; margin: 0 0 20px 0; }
    </style>
</head>
<body>
    <div class="card">
        <div class="header">
            <div class="logo-row">
                <img src="/logo.png" alt="Wormhole" class="logo" />
                <span>Wormhole</span>
            </div>
            <span class="badge">SECURE SHARE</span>
        </div>
        <h1 class="title">${escapeHtml(file.name)}</h1>
        <p class="meta">Size: ${formatBytes(file.length())} • Direct Stream Gateway</p>
        $mediaViewer
        $downloadButton
    </div>
</body>
</html>
        """.trimIndent()
    }

    fun renderFolderSharePage(share: ShareRecord, pw: String): String {
        val folder = File(share.targetPath)
        val files = folder.listFiles() ?: emptyArray()
        val sorted = files.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })

        val pwParam = if (pw.isNotBlank()) "?pw=${java.net.URLEncoder.encode(pw, "UTF-8")}" else ""

        val rows = sorted.joinToString("\n") { f ->
            val icon = if (f.isDirectory) "📁" else "📄"
            val link = if (f.isDirectory) "#" else "/s/${share.token}/download$pwParam&path=${java.net.URLEncoder.encode(f.name, "UTF-8")}"
            val size = if (f.isDirectory) "Folder" else formatBytes(f.length())
            """
            <tr style="border-bottom: 1px solid rgba(255,255,255,0.04);">
                <td style="padding: 12px 16px;"><span style="margin-right: 8px;">$icon</span> <a href="$link" style="color: #f8fafc; text-decoration: none; font-weight: 500;">${escapeHtml(f.name)}</a></td>
                <td style="padding: 12px 16px; color: #94a3b8; font-size: 0.85rem; text-align: right;">$size</td>
            </tr>
            """.trimIndent()
        }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${escapeHtml(share.name)} — Wormhole Folder Share</title>
    <style>
        :root { --bg: #090d16; --surface: #131b2e; --accent: #38bdf8; --text: #f8fafc; --text-muted: #94a3b8; }
        body { margin: 0; background: var(--bg); color: var(--text); font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding: 30px 20px; }
        .container { max-width: 860px; margin: 0 auto; background: var(--surface); border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; padding: 24px; box-shadow: 0 20px 40px rgba(0,0,0,0.5); }
        .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; border-bottom: 1px solid rgba(255,255,255,0.06); padding-bottom: 14px; }
        .logo-row { display: flex; align-items: center; gap: 10px; font-weight: 700; font-size: 1.1rem; }
        .logo { width: 32px; height: 32px; }
        table { width: 100%; border-collapse: collapse; }
        th { text-align: left; padding: 10px 16px; color: var(--text-muted); font-size: 0.8rem; text-transform: uppercase; border-bottom: 1px solid rgba(255,255,255,0.08); }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="logo-row">
                <img src="/logo.png" alt="Wormhole" class="logo" />
                <span>Wormhole</span>
            </div>
            <span style="color: var(--accent); font-weight: 600; font-size: 0.9rem;">FOLDER SHARE</span>
        </div>
        <h2 style="margin: 0 0 6px 0; font-size: 1.3rem;">${escapeHtml(share.name)}</h2>
        <p style="color: var(--text-muted); font-size: 0.85rem; margin: 0 0 20px 0;">Contains ${files.size} item(s)</p>
        <table>
            <thead>
                <tr><th>Name</th><th style="text-align: right;">Size</th></tr>
            </thead>
            <tbody>
                $rows
            </tbody>
        </table>
    </div>
</body>
</html>
        """.trimIndent()
    }

    fun renderShareErrorPage(title: String, message: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$title — Wormhole</title>
    <style>
        body { margin: 0; background: #090d16; color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; box-sizing: border-box; }
        .card { background: #131b2e; border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; padding: 36px 28px; width: 100%; max-width: 440px; text-align: center; box-shadow: 0 20px 40px rgba(0,0,0,0.5); }
        .icon { width: 52px; height: 52px; margin-bottom: 14px; }
        h2 { margin: 0 0 10px 0; font-size: 1.4rem; color: #f87171; }
        p { color: #94a3b8; font-size: 0.95rem; line-height: 1.5; margin: 0; }
    </style>
</head>
<body>
    <div class="card">
        <img src="/logo.png" alt="Wormhole" class="icon" />
        <h2>${escapeHtml(title)}</h2>
        <p>${escapeHtml(message)}</p>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.2f GB".format(gb)
    }

    private fun getMime(ext: String): String {
        return when (ext) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }
}
