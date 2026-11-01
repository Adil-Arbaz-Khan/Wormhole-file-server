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
        val downloadUrl = "/s/${share.token}/download$pwParam${if (pwParam.isEmpty()) "?" else "&"}dl=1"

        val mediaViewer = when {
            isVideo -> """
                <video controls autoplay style="width: 100%; max-height: 540px; border-radius: 12px; background: #000; margin-bottom: 20px;">
                    <source src="$mediaUrl" type="$mime">
                    Your browser does not support HTML5 video streaming.
                </video>
            """.trimIndent()
            isAudio -> """
                <div style="background: rgba(0,0,0,0.25); border-radius: 12px; padding: 28px 20px; margin-bottom: 20px; text-align: center;">
                    <div style="font-size: 3rem; margin-bottom: 12px;">🎵</div>
                    <audio controls autoplay style="width: 100%; max-width: 500px;">
                        <source src="$mediaUrl" type="$mime">
                        Your browser does not support HTML5 audio streaming.
                    </audio>
                </div>
            """.trimIndent()
            isImage -> """
                <div style="text-align: center; margin-bottom: 20px;">
                    <img src="$mediaUrl" alt="${escapeHtml(file.name)}" style="max-width: 100%; max-height: 540px; border-radius: 12px; object-fit: contain; box-shadow: 0 8px 24px rgba(0,0,0,0.3);" />
                </div>
            """.trimIndent()
            else -> """
                <div style="background: rgba(255,255,255,0.03); border: 1px dashed rgba(255,255,255,0.15); border-radius: 12px; padding: 40px; margin-bottom: 20px; text-align: center; color: var(--text-muted);">
                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline></svg>
                    <div style="margin-top: 10px; font-size: 0.95rem;">File ready for stream & download</div>
                </div>
            """.trimIndent()
        }

        val downloadButton = if (share.allowDownload) {
            """
            <a href="$downloadUrl" download="${escapeHtml(file.name)}" style="display: inline-flex; align-items: center; justify-content: center; gap: 8px; width: 100%; padding: 14px; border-radius: 10px; background: var(--accent); color: #020617; font-size: 1rem; font-weight: 700; text-decoration: none; box-sizing: border-box; transition: opacity 0.2s;">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
                Download (${formatBytes(file.length())})
            </a>
            """.trimIndent()
        } else {
            """
            <div style="background: rgba(56, 189, 248, 0.1); border: 1px solid rgba(56, 189, 248, 0.3); color: var(--accent); padding: 12px 14px; border-radius: 10px; text-align: center; font-size: 0.9rem; font-weight: 600;">
                Stream-Only Permission (Direct raw download disabled by host)
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
        :root {
            --bg: #090d16;
            --surface: #111827;
            --surface-card: #1f2937;
            --border: rgba(255, 255, 255, 0.08);
            --text: #f8fafc;
            --text-muted: #94a3b8;
            --accent: #38bdf8;
        }
        [data-theme="light"] {
            --bg: #f8fafc;
            --surface: #ffffff;
            --surface-card: #f1f5f9;
            --border: rgba(0, 0, 0, 0.08);
            --text: #0f172a;
            --text-muted: #64748b;
            --accent: #0284c7;
        }
        body { margin: 0; background: var(--bg); color: var(--text); font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; box-sizing: border-box; transition: background 0.2s, color 0.2s; }
        .card { background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 28px; width: 100%; max-width: 760px; box-shadow: 0 20px 40px rgba(0,0,0,0.3); }
        .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; border-bottom: 1px solid var(--border); padding-bottom: 14px; }
        .logo-row { display: flex; align-items: center; gap: 10px; font-weight: 700; font-size: 1.1rem; }
        .logo { width: 32px; height: 32px; }
        .theme-btn { background: none; border: 1px solid var(--border); color: var(--text); border-radius: 8px; padding: 6px 10px; cursor: pointer; font-size: 0.9rem; }
        .badge { background: rgba(56,189,248,0.15); color: var(--accent); padding: 4px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; }
        .title { font-size: 1.25rem; font-weight: 700; word-break: break-word; margin: 0 0 6px 0; }
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
            <div style="display: flex; align-items: center; gap: 8px;">
                <button class="theme-btn" id="theme-btn" onclick="toggleTheme()" title="Toggle Light/Dark Theme">🌓</button>
                <span class="badge">SECURE SHARE</span>
            </div>
        </div>
        <h1 class="title">${escapeHtml(file.name)}</h1>
        <p class="meta">Size: ${formatBytes(file.length())} • Direct Stream Gateway</p>
        $mediaViewer
        $downloadButton
    </div>
    <script>
        function toggleTheme() {
            const current = document.documentElement.getAttribute('data-theme') || 'dark';
            const next = current === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-theme', next);
            localStorage.setItem('wormhole_theme', next);
        }
        (function() {
            const saved = localStorage.getItem('wormhole_theme') || 'dark';
            document.documentElement.setAttribute('data-theme', saved);
        })();
    </script>
</body>
</html>
        """.trimIndent()
    }

    fun renderFolderSharePage(share: ShareRecord, pw: String, subPath: String = ""): String {
        val baseFolder = File(share.targetPath)
        val currentFolder = if (subPath.isNotBlank()) resolveSafeFile(baseFolder, subPath) else baseFolder
        val files = currentFolder.listFiles() ?: emptyArray()

        val pwParam = if (pw.isNotBlank()) "?pw=${java.net.URLEncoder.encode(pw, "UTF-8")}" else ""
        val allowDownload = share.allowDownload

        // Build items data for client-side rendering, instant sorting, grid/list view, audio player & modals
        val itemsJson = StringBuilder("[")
        files.forEachIndexed { index, f ->
            val ext = f.extension.lowercase()
            val isDir = f.isDirectory
            val type = when {
                isDir -> "folder"
                ext in listOf("mp3", "wav", "flac", "m4a", "ogg", "aac") -> "audio"
                ext in listOf("mp4", "mkv", "webm", "mov", "avi") -> "video"
                ext in listOf("jpg", "jpeg", "png", "gif", "webp", "svg") -> "image"
                ext in listOf("pdf", "txt", "md", "doc", "docx", "xls", "xlsx", "zip", "rar", "7z") -> "doc"
                else -> "other"
            }
            val relativeItemPath = if (subPath.isBlank()) f.name else "$subPath/${f.name}"
            val encodedRelative = java.net.URLEncoder.encode(relativeItemPath, "UTF-8")
            val streamUrl = if (isDir) {
                "/s/${share.token}$pwParam${if (pwParam.isEmpty()) "?" else "&"}path=$encodedRelative"
            } else {
                "/s/${share.token}/download$pwParam${if (pwParam.isEmpty()) "?" else "&"}path=$encodedRelative"
            }
            val downloadUrl = if (isDir) "" else {
                "/s/${share.token}/download$pwParam${if (pwParam.isEmpty()) "?" else "&"}path=$encodedRelative&dl=1"
            }

            if (index > 0) itemsJson.append(",")
            itemsJson.append("{")
            itemsJson.append("\"name\":\"${escapeJson(f.name)}\",")
            itemsJson.append("\"isDir\":$isDir,")
            itemsJson.append("\"size\":${if (isDir) 0L else f.length()},")
            itemsJson.append("\"formattedSize\":\"${if (isDir) "Folder" else formatBytes(f.length())}\",")
            itemsJson.append("\"lastModified\":${f.lastModified()},")
            itemsJson.append("\"type\":\"$type\",")
            itemsJson.append("\"streamUrl\":\"${escapeJson(streamUrl)}\",")
            itemsJson.append("\"downloadUrl\":\"${escapeJson(downloadUrl)}\"")
            itemsJson.append("}")
        }
        itemsJson.append("]")

        val parentPath = if (subPath.isNotBlank()) {
            val idx = subPath.lastIndexOfAny(charArrayOf('/', '\\'))
            if (idx > 0) subPath.substring(0, idx) else ""
        } else null

        val parentUrl = if (parentPath != null) {
            val encodedParent = java.net.URLEncoder.encode(parentPath, "UTF-8")
            if (parentPath.isEmpty()) "/s/${share.token}$pwParam" else "/s/${share.token}$pwParam${if (pwParam.isEmpty()) "?" else "&"}path=$encodedParent"
        } else null

        return """
<!DOCTYPE html>
<html lang="en" data-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${escapeHtml(share.name)} — Wormhole</title>
    <style>
        :root {
            --bg: #090d16;
            --surface: #111827;
            --surface-card: #1e293b;
            --surface-hover: #334155;
            --border: rgba(255, 255, 255, 0.08);
            --border-highlight: rgba(56, 189, 248, 0.4);
            --text: #f8fafc;
            --text-muted: #94a3b8;
            --accent: #38bdf8;
            --accent-hover: #0ea5e9;
            --player-bg: rgba(15, 23, 42, 0.95);
            --danger: #ef4444;
            --success: #10b981;
            --shadow: 0 10px 30px rgba(0,0,0,0.4);
        }
        [data-theme="light"] {
            --bg: #f8fafc;
            --surface: #ffffff;
            --surface-card: #f1f5f9;
            --surface-hover: #e2e8f0;
            --border: rgba(0, 0, 0, 0.08);
            --border-highlight: rgba(2, 132, 199, 0.4);
            --text: #0f172a;
            --text-muted: #64748b;
            --accent: #0284c7;
            --accent-hover: #0369a1;
            --player-bg: rgba(255, 255, 255, 0.95);
            --danger: #dc2626;
            --success: #059669;
            --shadow: 0 10px 30px rgba(0,0,0,0.08);
        }
        * { box-sizing: border-box; }
        body {
            margin: 0;
            background: var(--bg);
            color: var(--text);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            min-height: 100vh;
            padding-bottom: 120px;
            transition: background 0.25s ease, color 0.25s ease;
        }

        /* Top Bar */
        .topbar {
            background: var(--surface);
            border-bottom: 1px solid var(--border);
            position: sticky;
            top: 0;
            z-index: 100;
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
        }
        .topbar-content {
            max-width: 1240px;
            margin: 0 auto;
            padding: 12px 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 16px;
        }
        .brand {
            display: flex;
            align-items: center;
            gap: 10px;
            font-weight: 700;
            font-size: 1.15rem;
            color: var(--text);
            text-decoration: none;
        }
        .brand img { width: 32px; height: 32px; border-radius: 8px; }
        .badge-live {
            background: rgba(56, 189, 248, 0.15);
            color: var(--accent);
            padding: 4px 10px;
            border-radius: 9999px;
            font-size: 0.75rem;
            font-weight: 600;
            letter-spacing: 0.5px;
            text-transform: uppercase;
        }

        /* Container */
        .main-container {
            max-width: 1240px;
            margin: 0 auto;
            padding: 24px 20px;
        }

        /* Folder Header & Breadcrumb */
        .folder-header {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 16px;
            padding: 24px;
            margin-bottom: 24px;
            box-shadow: var(--shadow);
        }
        .folder-title-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 12px;
            margin-bottom: 8px;
        }
        .folder-title {
            margin: 0;
            font-size: 1.6rem;
            font-weight: 800;
            letter-spacing: -0.5px;
            word-break: break-word;
        }
        .folder-meta {
            color: var(--text-muted);
            font-size: 0.9rem;
            margin-bottom: 16px;
        }
        .breadcrumb {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 6px 14px;
            background: var(--surface-card);
            border: 1px solid var(--border);
            border-radius: 8px;
            font-size: 0.85rem;
            color: var(--text-muted);
            text-decoration: none;
            margin-bottom: 16px;
            transition: all 0.2s;
        }
        .breadcrumb:hover {
            color: var(--accent);
            border-color: var(--accent);
        }

        /* Controls Bar: Search, Sort, View, Theme */
        .controls-bar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 12px;
            padding-top: 16px;
            border-top: 1px solid var(--border);
        }
        .search-box {
            position: relative;
            flex: 1;
            min-width: 220px;
            max-width: 420px;
        }
        .search-input {
            width: 100%;
            padding: 10px 14px 10px 38px;
            border-radius: 10px;
            border: 1px solid var(--border);
            background: var(--surface-card);
            color: var(--text);
            font-size: 0.9rem;
            outline: none;
            transition: border-color 0.2s;
        }
        .search-input:focus {
            border-color: var(--accent);
        }
        .search-icon {
            position: absolute;
            left: 12px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-muted);
            pointer-events: none;
            font-size: 0.9rem;
        }
        .actions-group {
            display: flex;
            align-items: center;
            gap: 10px;
            flex-wrap: wrap;
        }
        .select-sort {
            padding: 9px 12px;
            border-radius: 10px;
            border: 1px solid var(--border);
            background: var(--surface-card);
            color: var(--text);
            font-size: 0.85rem;
            outline: none;
            cursor: pointer;
        }
        .btn-toggle {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            padding: 8px 14px;
            border-radius: 10px;
            border: 1px solid var(--border);
            background: var(--surface-card);
            color: var(--text);
            font-size: 0.85rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
        }
        .btn-toggle:hover {
            background: var(--surface-hover);
            border-color: var(--accent);
        }
        .btn-toggle.active {
            background: var(--accent);
            color: #020617;
            border-color: var(--accent);
        }

        /* Content Views */
        .content-wrapper {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 16px;
            padding: 20px;
            box-shadow: var(--shadow);
        }

        /* GRID VIEW: Uniform tiles, no tiling breakage, text bounded */
        .grid-container {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
            gap: 16px;
        }
        .grid-card {
            background: var(--surface-card);
            border: 1px solid var(--border);
            border-radius: 14px;
            padding: 16px;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            transition: transform 0.2s, border-color 0.2s, box-shadow 0.2s;
            cursor: pointer;
            position: relative;
            overflow: hidden;
            height: 220px;
        }
        .grid-card:hover {
            transform: translateY(-3px);
            border-color: var(--border-highlight);
            box-shadow: 0 10px 24px rgba(0,0,0,0.25);
        }
        .card-top {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 12px;
        }
        .type-badge {
            font-size: 0.7rem;
            font-weight: 700;
            padding: 3px 8px;
            border-radius: 6px;
            text-transform: uppercase;
        }
        .type-badge.audio { background: rgba(168, 85, 247, 0.2); color: #c084fc; }
        .type-badge.video { background: rgba(59, 130, 246, 0.2); color: #60a5fa; }
        .type-badge.image { background: rgba(16, 185, 129, 0.2); color: #34d399; }
        .type-badge.folder { background: rgba(245, 158, 11, 0.2); color: #fbbf24; }
        .type-badge.doc { background: rgba(236, 72, 153, 0.2); color: #f472b6; }
        .type-badge.other { background: rgba(148, 163, 184, 0.2); color: #cbd5e1; }

        .card-icon-area {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 70px;
            font-size: 2.8rem;
            position: relative;
        }
        .card-play-overlay {
            position: absolute;
            width: 44px;
            height: 44px;
            border-radius: 50%;
            background: var(--accent);
            color: #020617;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.1rem;
            opacity: 0;
            transform: scale(0.8);
            transition: all 0.2s ease;
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        }
        .grid-card:hover .card-play-overlay {
            opacity: 1;
            transform: scale(1);
        }

        /* Fixed text bounds: 2-line clamp, ellipsis, no overflow */
        .card-title {
            font-size: 0.9rem;
            font-weight: 600;
            line-height: 1.35;
            margin: 0;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            text-overflow: ellipsis;
            word-break: break-word;
            min-height: 2.7em;
            color: var(--text);
        }
        .card-footer {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-top: 10px;
            padding-top: 10px;
            border-top: 1px solid var(--border);
        }
        .card-size {
            font-size: 0.78rem;
            color: var(--text-muted);
            font-weight: 500;
        }
        .card-dl-btn {
            background: none;
            border: 1px solid var(--border);
            color: var(--accent);
            padding: 5px 9px;
            border-radius: 6px;
            cursor: pointer;
            font-size: 0.8rem;
            display: flex;
            align-items: center;
            gap: 4px;
            text-decoration: none;
            transition: all 0.2s;
        }
        .card-dl-btn:hover {
            background: var(--accent);
            color: #020617;
            border-color: var(--accent);
        }

        /* LIST VIEW: Clean rows, perfect text boundaries */
        .list-container {
            width: 100%;
            border-collapse: collapse;
        }
        .list-header {
            border-bottom: 2px solid var(--border);
        }
        .list-header th {
            text-align: left;
            padding: 12px 14px;
            color: var(--text-muted);
            font-size: 0.8rem;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .list-row {
            border-bottom: 1px solid var(--border);
            transition: background 0.15s;
            cursor: pointer;
        }
        .list-row:hover {
            background: var(--surface-card);
        }
        .list-row td {
            padding: 14px 14px;
            vertical-align: middle;
        }
        .list-name-col {
            display: flex;
            align-items: center;
            gap: 12px;
            min-width: 0;
            max-width: 600px;
        }
        .list-icon { font-size: 1.4rem; flex-shrink: 0; }
        .list-title {
            font-size: 0.92rem;
            font-weight: 600;
            color: var(--text);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            display: block;
        }
        .list-size-col {
            color: var(--text-muted);
            font-size: 0.85rem;
            white-space: nowrap;
        }
        .list-actions-col {
            text-align: right;
            white-space: nowrap;
        }
        .btn-play-sm {
            background: rgba(56, 189, 248, 0.15);
            border: 1px solid rgba(56, 189, 248, 0.3);
            color: var(--accent);
            padding: 6px 12px;
            border-radius: 8px;
            cursor: pointer;
            font-size: 0.8rem;
            font-weight: 600;
            margin-right: 8px;
            transition: all 0.2s;
        }
        .btn-play-sm:hover {
            background: var(--accent);
            color: #020617;
        }

        /* Empty & Zero Search State */
        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: var(--text-muted);
        }

        /* BOTTOM AUDIO PLAYER (Sleek Spotify-style) */
        .audio-player-bar {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            background: var(--player-bg);
            border-top: 1px solid var(--border-highlight);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            padding: 14px 24px;
            display: none;
            align-items: center;
            justify-content: space-between;
            gap: 20px;
            z-index: 1000;
            box-shadow: 0 -10px 40px rgba(0,0,0,0.5);
            animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
        }
        @keyframes slideUp {
            from { transform: translateY(100%); }
            to { transform: translateY(0); }
        }
        .player-info {
            display: flex;
            align-items: center;
            gap: 14px;
            min-width: 200px;
            max-width: 300px;
        }
        .player-icon {
            width: 44px;
            height: 44px;
            border-radius: 10px;
            background: var(--surface-card);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.5rem;
            flex-shrink: 0;
        }
        .player-text {
            min-width: 0;
        }
        .player-track-name {
            font-size: 0.92rem;
            font-weight: 700;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            color: var(--text);
            margin: 0 0 3px 0;
        }
        .player-track-sub {
            font-size: 0.75rem;
            color: var(--text-muted);
            margin: 0;
        }
        .player-center {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 6px;
            flex: 1;
            max-width: 600px;
        }
        .player-buttons {
            display: flex;
            align-items: center;
            gap: 16px;
        }
        .player-ctrl-btn {
            background: none;
            border: none;
            color: var(--text);
            cursor: pointer;
            font-size: 1.1rem;
            padding: 6px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: color 0.15s;
        }
        .player-ctrl-btn:hover { color: var(--accent); }
        .player-main-play {
            width: 38px;
            height: 38px;
            border-radius: 50%;
            background: var(--accent);
            color: #020617;
            font-size: 1.1rem;
            border: none;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: transform 0.15s;
        }
        .player-main-play:hover { transform: scale(1.08); }
        .player-progress-row {
            display: flex;
            align-items: center;
            gap: 10px;
            width: 100%;
        }
        .player-time {
            font-size: 0.75rem;
            color: var(--text-muted);
            min-width: 38px;
            font-variant-numeric: tabular-nums;
        }
        .progress-bar-wrap {
            flex: 1;
            height: 6px;
            background: var(--surface-card);
            border-radius: 9999px;
            cursor: pointer;
            position: relative;
            overflow: hidden;
        }
        .progress-fill {
            height: 100%;
            background: var(--accent);
            width: 0%;
            border-radius: 9999px;
            transition: width 0.1s linear;
        }
        .player-right {
            display: flex;
            align-items: center;
            gap: 14px;
            min-width: 160px;
            justify-content: flex-end;
        }
        .vol-slider {
            width: 80px;
            accent-color: var(--accent);
            cursor: pointer;
        }
        .btn-close-player {
            background: none;
            border: none;
            color: var(--text-muted);
            cursor: pointer;
            font-size: 1.2rem;
            padding: 4px;
        }
        .btn-close-player:hover { color: var(--danger); }

        /* Video Lightbox Modal */
        .modal-overlay {
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0,0,0,0.85);
            backdrop-filter: blur(8px);
            z-index: 2000;
            display: none;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        .modal-box {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 16px;
            padding: 20px;
            max-width: 900px;
            width: 100%;
            box-shadow: 0 25px 50px rgba(0,0,0,0.5);
            display: flex;
            flex-direction: column;
            gap: 14px;
        }
        .modal-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .modal-title {
            font-weight: 700;
            font-size: 1.1rem;
            margin: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .modal-close {
            background: none;
            border: none;
            color: var(--text);
            font-size: 1.4rem;
            cursor: pointer;
        }
    </style>
</head>
<body>

    <!-- Top Navigation Bar -->
    <header class="topbar">
        <div class="topbar-content">
            <a href="/s/${share.token}$pwParam" class="brand">
                <img src="/logo.png" alt="Wormhole" />
                <span>Wormhole</span>
            </a>
            <div style="display: flex; align-items: center; gap: 12px;">
                <span class="badge-live">Folder Share</span>
                <button class="btn-toggle" onclick="toggleTheme()" id="theme-btn" title="Toggle Light / Dark Mode">🌓</button>
            </div>
        </div>
    </header>

    <!-- Main Container -->
    <main class="main-container">

        <!-- Folder Info Header -->
        <section class="folder-header">
            ${if (parentUrl != null) """
                <a href="$parentUrl" class="breadcrumb">
                    <span>⬅️</span>
                    <span>Back to Parent Folder</span>
                </a>
            """ else ""}
            <div class="folder-title-row">
                <h1 class="folder-title">📂 ${escapeHtml(if (subPath.isBlank()) share.name else currentFolder.name)}</h1>
                <div class="actions-group">
                    <button class="btn-toggle active" id="btn-grid" onclick="setViewMode('grid')" title="Grid View">⊞ Grid</button>
                    <button class="btn-toggle" id="btn-list" onclick="setViewMode('list')" title="List View">☰ List</button>
                </div>
            </div>
            <div class="folder-meta">
                <span id="items-count-label">Loading items...</span> • ${if (allowDownload) "Full Download Permitted" else "Stream-Only Permission"}
            </div>

            <!-- Controls: Search & Sort -->
            <div class="controls-bar">
                <div class="search-box">
                    <span class="search-icon">🔍</span>
                    <input type="text" class="search-input" id="search-input" placeholder="Search in this folder..." oninput="onSearchChange(this.value)" />
                </div>
                <div class="actions-group">
                    <label for="sort-select" style="font-size: 0.85rem; color: var(--text-muted);">Sort by:</label>
                    <select class="select-sort" id="sort-select" onchange="onSortChange(this.value)">
                        <option value="name_asc">Name (A → Z)</option>
                        <option value="name_desc">Name (Z → A)</option>
                        <option value="size_desc">Size (Largest First)</option>
                        <option value="size_asc">Size (Smallest First)</option>
                        <option value="type">Type (Folders First)</option>
                    </select>
                </div>
            </div>
        </section>

        <!-- Dynamic Files Container -->
        <section class="content-wrapper">
            <div id="grid-view" class="grid-container"></div>
            <table id="list-view" class="list-container" style="display: none;">
                <thead class="list-header">
                    <tr>
                        <th style="width: 60%;">Name</th>
                        <th style="width: 20%;">Size</th>
                        <th style="width: 20%; text-align: right;">Action</th>
                    </tr>
                </thead>
                <tbody id="list-body"></tbody>
            </table>
            <div id="empty-state" class="empty-state" style="display: none;">
                <div style="font-size: 3rem; margin-bottom: 12px;">📁</div>
                <h3>No files found</h3>
                <p>No matching files in this folder or search query.</p>
            </div>
        </section>

    </main>

    <!-- Bottom Audio Player -->
    <div id="audio-player-bar" class="audio-player-bar">
        <div class="player-info">
            <div class="player-icon" id="player-art">🎵</div>
            <div class="player-text">
                <p class="player-track-name" id="player-title">Track Name</p>
                <p class="player-track-sub" id="player-sub">${escapeHtml(share.name)}</p>
            </div>
        </div>

        <div class="player-center">
            <div class="player-buttons">
                <button class="player-ctrl-btn" onclick="playPrevAudio()" title="Previous track">⏮️</button>
                <button class="player-main-play" id="audio-play-toggle" onclick="toggleAudioPlay()" title="Play / Pause">▶</button>
                <button class="player-ctrl-btn" onclick="playNextAudio()" title="Next track">⏭️</button>
            </div>
            <div class="player-progress-row">
                <span class="player-time" id="player-cur-time">0:00</span>
                <div class="progress-bar-wrap" id="progress-wrap" onclick="seekAudio(event)">
                    <div class="progress-fill" id="player-progress-fill"></div>
                </div>
                <span class="player-time" id="player-dur-time">0:00</span>
            </div>
        </div>

        <div class="player-right">
            <span style="font-size: 0.9rem;">🔊</span>
            <input type="range" min="0" max="1" step="0.05" value="1" class="vol-slider" oninput="setAudioVolume(this.value)" />
            <button class="btn-close-player" onclick="closeAudioPlayer()" title="Close player">✕</button>
        </div>
    </div>

    <!-- Video Modal -->
    <div id="video-modal" class="modal-overlay" onclick="closeModalOnBg(event)">
        <div class="modal-box">
            <div class="modal-header">
                <h3 class="modal-title" id="video-modal-title">Video Player</h3>
                <button class="modal-close" onclick="closeVideoModal()">✕</button>
            </div>
            <video id="modal-video-elem" controls autoplay style="width: 100%; max-height: 70vh; border-radius: 10px; background: #000;"></video>
        </div>
    </div>

    <!-- Image Lightbox Modal -->
    <div id="image-modal" class="modal-overlay" onclick="closeModalOnBg(event)">
        <div class="modal-box" style="max-width: 800px; text-align: center;">
            <div class="modal-header">
                <h3 class="modal-title" id="image-modal-title">Image Viewer</h3>
                <button class="modal-close" onclick="closeImageModal()">✕</button>
            </div>
            <img id="modal-img-elem" src="" alt="Preview" style="max-width: 100%; max-height: 70vh; object-fit: contain; border-radius: 8px;" />
        </div>
    </div>

    <!-- Core Audio Engine & State -->
    <audio id="global-audio"></audio>

    <script>
        const ALL_ITEMS = $itemsJson;
        const ALLOW_DOWNLOAD = $allowDownload;
        let currentFilter = "";
        let currentSort = "name_asc";
        let currentView = localStorage.getItem("wormhole_view_mode") || "grid";

        // Audio state
        let audioPlaylist = [];
        let currentAudioIndex = -1;
        const audio = document.getElementById("global-audio");

        function init() {
            // Apply saved theme
            const savedTheme = localStorage.getItem("wormhole_theme") || "dark";
            document.documentElement.setAttribute("data-theme", savedTheme);

            // Filter out audio files for playlist
            updateAudioPlaylist();

            // Set initial view
            setViewMode(currentView, false);

            // Render
            renderFiles();
            setupAudioEvents();
        }

        function updateAudioPlaylist() {
            audioPlaylist = ALL_ITEMS.filter(item => item.type === "audio");
        }

        function toggleTheme() {
            const current = document.documentElement.getAttribute("data-theme") || "dark";
            const next = current === "dark" ? "light" : "dark";
            document.documentElement.setAttribute("data-theme", next);
            localStorage.setItem("wormhole_theme", next);
        }

        function setViewMode(mode, save = true) {
            currentView = mode;
            if (save) localStorage.setItem("wormhole_view_mode", mode);
            const btnGrid = document.getElementById("btn-grid");
            const btnList = document.getElementById("btn-list");
            const gridView = document.getElementById("grid-view");
            const listView = document.getElementById("list-view");

            if (mode === "grid") {
                btnGrid.classList.add("active");
                btnList.classList.remove("active");
                gridView.style.display = "grid";
                listView.style.display = "none";
            } else {
                btnList.classList.add("active");
                btnGrid.classList.remove("active");
                gridView.style.display = "none";
                listView.style.display = "table";
            }
        }

        function onSearchChange(val) {
            currentFilter = val.trim().toLowerCase();
            renderFiles();
        }

        function onSortChange(val) {
            currentSort = val;
            renderFiles();
        }

        function getFilteredAndSortedItems() {
            let list = ALL_ITEMS.slice();

            // Filter
            if (currentFilter) {
                list = list.filter(it => it.name.toLowerCase().includes(currentFilter));
            }

            // Sort
            list.sort((a, b) => {
                if (a.isDir && !b.isDir) return -1;
                if (!a.isDir && b.isDir) return 1;

                switch (currentSort) {
                    case "name_asc":
                        return a.name.localeCompare(b.name, undefined, { numeric: true, sensitivity: 'base' });
                    case "name_desc":
                        return b.name.localeCompare(a.name, undefined, { numeric: true, sensitivity: 'base' });
                    case "size_asc":
                        return a.size - b.size;
                    case "size_desc":
                        return b.size - a.size;
                    case "type":
                        return a.type.localeCompare(b.type);
                    default:
                        return 0;
                }
            });

            return list;
        }

        function renderFiles() {
            const items = getFilteredAndSortedItems();
            document.getElementById("items-count-label").textContent = items.length + " item(s)";

            const gridView = document.getElementById("grid-view");
            const listBody = document.getElementById("list-body");
            const emptyState = document.getElementById("empty-state");

            if (items.length === 0) {
                gridView.innerHTML = "";
                listBody.innerHTML = "";
                emptyState.style.display = "block";
                return;
            } else {
                emptyState.style.display = "none";
            }

            // Render Grid
            let gridHtml = "";
            let listHtml = "";

            items.forEach(item => {
                const icon = getItemIcon(item);
                const playOverlay = getPlayOverlay(item);
                const dlBtn = (ALLOW_DOWNLOAD && !item.isDir) ?
                    '<a href="' + item.downloadUrl + '" download="' + escapeHtml(item.name) + '" class="card-dl-btn" onclick="event.stopPropagation();" title="Download file">⬇️ Download</a>' :
                    (item.isDir ? '' : '<span style="font-size:0.75rem; color:var(--text-muted);">Stream</span>');

                // Grid Card
                gridHtml += '<div class="grid-card" onclick="handleItemClick(\'' + escapeJs(item.name) + '\')">';
                gridHtml += '  <div class="card-top">';
                gridHtml += '    <span class="type-badge ' + item.type + '">' + item.type + '</span>';
                gridHtml += '  </div>';
                gridHtml += '  <div class="card-icon-area">';
                gridHtml += '    <span>' + icon + '</span>';
                if (playOverlay) gridHtml += '    <div class="card-play-overlay">' + playOverlay + '</div>';
                gridHtml += '  </div>';
                gridHtml += '  <h3 class="card-title" title="' + escapeHtml(item.name) + '">' + escapeHtml(item.name) + '</h3>';
                gridHtml += '  <div class="card-footer">';
                gridHtml += '    <span class="card-size">' + item.formattedSize + '</span>';
                gridHtml += '    ' + dlBtn;
                gridHtml += '  </div>';
                gridHtml += '</div>';

                // List Row
                const listPlayBtn = (!item.isDir && (item.type === 'audio' || item.type === 'video' || item.type === 'image')) ?
                    '<button class="btn-play-sm" onclick="event.stopPropagation(); handleItemClick(\'' + escapeJs(item.name) + '\');">▶ Play</button>' : '';
                const listDlBtn = (ALLOW_DOWNLOAD && !item.isDir) ?
                    '<a href="' + item.downloadUrl + '" download="' + escapeHtml(item.name) + '" class="card-dl-btn" onclick="event.stopPropagation();" title="Download">⬇️</a>' : '';

                listHtml += '<tr class="list-row" onclick="handleItemClick(\'' + escapeJs(item.name) + '\')">';
                listHtml += '  <td>';
                listHtml += '    <div class="list-name-col">';
                listHtml += '      <span class="list-icon">' + icon + '</span>';
                listHtml += '      <span class="list-title" title="' + escapeHtml(item.name) + '">' + escapeHtml(item.name) + '</span>';
                listHtml += '    </div>';
                listHtml += '  </td>';
                listHtml += '  <td class="list-size-col">' + item.formattedSize + '</td>';
                listHtml += '  <td class="list-actions-col">' + listPlayBtn + listDlBtn + '</td>';
                listHtml += '</tr>';
            });

            gridView.innerHTML = gridHtml;
            listBody.innerHTML = listHtml;
        }

        function getItemIcon(item) {
            if (item.isDir) return "📁";
            switch (item.type) {
                case "audio": return "🎵";
                case "video": return "🎬";
                case "image": return "🖼️";
                case "doc": return "📄";
                default: return "📦";
            }
        }

        function getPlayOverlay(item) {
            if (item.isDir) return "📂";
            if (item.type === "audio" || item.type === "video") return "▶";
            if (item.type === "image") return "🔍";
            return "";
        }

        function handleItemClick(itemName) {
            const item = ALL_ITEMS.find(it => it.name === itemName);
            if (!item) return;

            if (item.isDir) {
                // Navigate into subfolder
                window.location.href = item.streamUrl;
                return;
            }

            if (item.type === "audio") {
                playAudioItem(item);
            } else if (item.type === "video") {
                openVideoModal(item);
            } else if (item.type === "image") {
                openImageModal(item);
            } else {
                if (ALLOW_DOWNLOAD) {
                    window.location.href = item.downloadUrl;
                } else {
                    alert("This file is marked as Stream-Only.");
                }
            }
        }

        /* Audio Engine */
        function playAudioItem(item) {
            currentAudioIndex = audioPlaylist.findIndex(it => it.name === item.name);
            document.getElementById("audio-player-bar").style.display = "flex";
            document.getElementById("player-title").textContent = item.name;

            audio.src = item.streamUrl;
            audio.play().catch(e => console.log("Audio play deferred", e));
            document.getElementById("audio-play-toggle").textContent = "⏸";
        }

        function toggleAudioPlay() {
            if (audio.paused) {
                audio.play();
                document.getElementById("audio-play-toggle").textContent = "⏸";
            } else {
                audio.pause();
                document.getElementById("audio-play-toggle").textContent = "▶";
            }
        }

        function playPrevAudio() {
            if (audioPlaylist.length === 0) return;
            currentAudioIndex = (currentAudioIndex - 1 + audioPlaylist.length) % audioPlaylist.length;
            playAudioItem(audioPlaylist[currentAudioIndex]);
        }

        function playNextAudio() {
            if (audioPlaylist.length === 0) return;
            currentAudioIndex = (currentAudioIndex + 1) % audioPlaylist.length;
            playAudioItem(audioPlaylist[currentAudioIndex]);
        }

        function setAudioVolume(val) {
            audio.volume = parseFloat(val);
        }

        function seekAudio(e) {
            const rect = document.getElementById("progress-wrap").getBoundingClientRect();
            const pos = (e.clientX - rect.left) / rect.width;
            if (audio.duration) {
                audio.currentTime = pos * audio.duration;
            }
        }

        function closeAudioPlayer() {
            audio.pause();
            audio.src = "";
            document.getElementById("audio-player-bar").style.display = "none";
        }

        function setupAudioEvents() {
            audio.ontimeupdate = () => {
                if (audio.duration) {
                    const percent = (audio.currentTime / audio.duration) * 100;
                    document.getElementById("player-progress-fill").style.width = percent + "%";
                    document.getElementById("player-cur-time").textContent = formatTime(audio.currentTime);
                    document.getElementById("player-dur-time").textContent = formatTime(audio.duration);
                }
            };
            audio.onended = () => {
                playNextAudio();
            };
            audio.onplay = () => {
                document.getElementById("audio-play-toggle").textContent = "⏸";
            };
            audio.onpause = () => {
                document.getElementById("audio-play-toggle").textContent = "▶";
            };
        }

        function formatTime(sec) {
            if (isNaN(sec)) return "0:00";
            const m = Math.floor(sec / 60);
            const s = Math.floor(sec % 60);
            return m + ":" + (s < 10 ? "0" : "") + s;
        }

        /* Video Modal */
        function openVideoModal(item) {
            const modal = document.getElementById("video-modal");
            const video = document.getElementById("modal-video-elem");
            document.getElementById("video-modal-title").textContent = item.name;
            video.src = item.streamUrl;
            modal.style.display = "flex";
            video.play().catch(e => console.log("Video auto-play", e));
        }

        function closeVideoModal() {
            const modal = document.getElementById("video-modal");
            const video = document.getElementById("modal-video-elem");
            video.pause();
            video.src = "";
            modal.style.display = "none";
        }

        /* Image Modal */
        function openImageModal(item) {
            const modal = document.getElementById("image-modal");
            const img = document.getElementById("modal-img-elem");
            document.getElementById("image-modal-title").textContent = item.name;
            img.src = item.streamUrl;
            modal.style.display = "flex";
        }

        function closeImageModal() {
            const modal = document.getElementById("image-modal");
            document.getElementById("modal-img-elem").src = "";
            modal.style.display = "none";
        }

        function closeModalOnBg(e) {
            if (e.target.classList.contains("modal-overlay")) {
                closeVideoModal();
                closeImageModal();
            }
        }

        function escapeHtml(str) {
            return String(str)
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#x27;");
        }

        function escapeJs(str) {
            return String(str).replace(/\\/g, "\\\\").replace(/'/g, "\\'");
        }

        // Initialize on DOM load
        window.addEventListener("DOMContentLoaded", init);
    </script>
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

    private fun resolveSafeFile(base: File, subPath: String): File {
        val sanitized = subPath.replace('\\', '/').trimStart('/')
        val target = File(base, sanitized).canonicalFile
        val canonicalBase = base.canonicalFile
        if (!target.path.startsWith(canonicalBase.path)) {
            return base
        }
        return target
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
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
