# J.A.R.V.I.S. Core Storage Hub (Android)

An open-source, zero-ad Android application integrating:
1. **High-Performance Embedded HTTP File Server** (Ktor CIO) with full byte-range support for seamless video/audio seeking and remote browser access.
2. **Cloudflare Quick Tunnel Integration** (`cloudflared`) providing one-tap, public, zero-configuration HTTPS tunnels without opening router ports or registering domain accounts.

---

## 1. Cloudflare ARM64 Binary Setup (Bypassing W^X / SELinux)

Android 10+ (API 29+) enforces strict W^X (Write XOR Execute) memory policies and SELinux file restrictions (`noexec` mounts on app data directories such as `/data/data/<pkg>/files/`). Consequently, running dynamically downloaded binaries via `ProcessBuilder` triggers an immediate permission fault or `exec format error`.

### The Solution: Packaging via `jniLibs`
Android permits executable execution from `context.applicationInfo.nativeLibraryDir` (`/data/app/<pkg>/lib/arm64`), where shared libraries (`.so`) are installed with `rx` (read and execute) permissions.

By naming the static `cloudflared` binary with a `lib` prefix and `.so` extension (e.g. `libcloudflared.so`), the Android package manager treats it as a native library and deploys it with native execution privileges.

### How to Install the Binary:

1. Download the official pre-compiled Linux ARM64 binary from Cloudflare Releases:
   ```bash
   # Download cloudflared for linux arm64
   curl -L -o cloudflared-linux-arm64 https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64
   ```

2. Rename the binary to `libcloudflared.so`:
   ```bash
   mv cloudflared-linux-arm64 libcloudflared.so
   ```

3. Place it into the project directory:
   ```
   app/src/main/jniLibs/arm64-v8a/libcloudflared.so
   ```

4. Verify `app/build.gradle.kts` configuration:
   ```kotlin
   packaging {
       jniLibs {
           // CRITICAL: Extracts .so files uncompressed into nativeLibraryDir
           useLegacyPackaging = true
       }
   }
   ```
   *Note: `useLegacyPackaging = true` prevents Android from reading the .so file directly from within the compressed APK zip, ensuring it exists as a real file path on the filesystem accessible by `ProcessBuilder`.*

---

## 2. Architectural Highlights

- **Ktor Server (CIO Engine):**
  - Uses `PartialContent` for `206 Partial Content` & `Accept-Ranges: bytes`. Video players (VLC, Safari, Chrome, ExoPlayer) can scrub and seek large 4K / 1080p files instantaneously without buffering the whole file.
  - Implements directory traversal protection via `resolveSafeFile` canonical path validation.
- **Embedded Web Client:**
  - Modern dark-mode SPA embedded directly into `app/src/main/assets/web/index.html`.
  - Zero external CDN dependencies; operates 100% offline over Wi-Fi Hotspot or USB tethering.
  - Features breadcrumbs, instant file filtering, inline HTML5 media player modal, drag-and-drop multi-file upload, and direct downloads.
- **Background Reliability:**
  - Sticky `ForegroundService` with `PARTIAL_WAKE_LOCK` and `WifiLock` (high-performance mode) preventing CPU sleep and socket drops.
  - Persistent rich notification with live local/tunnel URLs and one-tap Stop action.
- **Dynamic Interface Detection:**
  - Real-time enumeration of `wlan0` (Local Wi-Fi), `ap0` (Hotspot), `rndis0`/`usb0` (USB Tethering), and `lo` (Localhost).
