package com.jarvis.fileserver.tunnel

import android.content.Context
import android.util.Log
import com.jarvis.fileserver.model.TunnelState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CloudflareTunnelManager(private val context: Context) {

    private val tag = "CloudflareTunnel"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var process: Process? = null
    private var monitorJob: Job? = null
    private val isStopping = AtomicBoolean(false)

    private val _tunnelState = MutableStateFlow<TunnelState>(TunnelState.Disabled)
    val tunnelState: StateFlow<TunnelState> = _tunnelState.asStateFlow()

    private val quickTunnelRegex = Regex("""https://[a-zA-Z0-9-]+\.trycloudflare\.com""")

    fun startTunnel(localPort: Int) {
        if (_tunnelState.value is TunnelState.Connecting || _tunnelState.value is TunnelState.Connected) {
            Log.d(tag, "Tunnel is already running or connecting.")
            return
        }

        isStopping.set(false)
        _tunnelState.value = TunnelState.Connecting

        monitorJob?.cancel()
        monitorJob = scope.launch {
            try {
                val binaryFile = getCloudflaredBinary()
                if (binaryFile == null || !binaryFile.exists()) {
                    val msg = "Binary 'libcloudflared.so' not found in native library directory. Please install the arm64 binary."
                    Log.e(tag, msg)
                    _tunnelState.value = TunnelState.Error(msg)
                    return@launch
                }

                if (!binaryFile.canExecute()) {
                    binaryFile.setExecutable(true, false)
                }

                val command = listOf(
                    binaryFile.absolutePath,
                    "tunnel",
                    "--url", "http://127.0.0.1:$localPort",
                    "--no-autoupdate",
                    "--metrics", "127.0.0.1:0"
                )

                Log.i(tag, "Spawning cloudflared process: ${command.joinToString(" ")}")

                val builder = ProcessBuilder(command)
                builder.directory(context.filesDir)
                val env = builder.environment()
                env["HOME"] = context.filesDir.absolutePath
                val certDirs = listOf(
                    "/apex/com.android.conscrypt/cacerts",
                    "/system/etc/security/cacerts"
                ).filter { File(it).exists() }.joinToString(":")
                if (certDirs.isNotBlank()) {
                    env["SSL_CERT_DIR"] = certDirs
                }
                builder.redirectErrorStream(true) // Merge stderr into stdout for synchronous log capture

                val proc = builder.start()
                process = proc

                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                var urlCaptured = false

                try {
                    while (isActive && !isStopping.get()) {
                        val line = reader.readLine() ?: break
                        Log.d(tag, "[cloudflared] $line")

                        if (!urlCaptured && !isStopping.get()) {
                            val match = quickTunnelRegex.find(line)
                            if (match != null) {
                                val url = match.value
                                Log.i(tag, "Tunnel established: $url")
                                _tunnelState.value = TunnelState.Connected(url)
                                urlCaptured = true
                            }
                        }
                    }
                } finally {
                    try {
                        reader.close()
                    } catch (_: Exception) {}
                }

                val exitCode = proc.waitFor()
                Log.w(tag, "cloudflared process terminated with code $exitCode (isStopping=${isStopping.get()})")

                // If stop was requested or coroutine is no longer active, this exit was deliberate
                if (isStopping.get() || !isActive) {
                    _tunnelState.value = TunnelState.Disabled
                } else if (_tunnelState.value !is TunnelState.Disabled) {
                    // Normal SIGTERM / SIGKILL codes when terminated are 143 and 137
                    if (exitCode != 0 && exitCode != 143 && exitCode != 137 && exitCode != 130) {
                        _tunnelState.value = TunnelState.Error("Process exited with code $exitCode")
                    } else {
                        _tunnelState.value = TunnelState.Disabled
                    }
                }
            } catch (e: CancellationException) {
                Log.d(tag, "Monitor job cancelled cleanly.")
                _tunnelState.value = TunnelState.Disabled
                throw e
            } catch (e: Exception) {
                if (!isStopping.get() && isActive) {
                    Log.e(tag, "Failed to run cloudflared", e)
                    _tunnelState.value = TunnelState.Error(e.localizedMessage ?: "Process execution failed")
                } else {
                    _tunnelState.value = TunnelState.Disabled
                }
            }
        }
    }

    suspend fun stopTunnel() = withContext(Dispatchers.IO) {
        isStopping.set(true)
        _tunnelState.value = TunnelState.Disabled

        monitorJob?.cancel()
        monitorJob = null

        process?.let { proc ->
            Log.i(tag, "Terminating cloudflared gracefully (SIGTERM)...")
            proc.destroy()
            try {
                val exited = proc.waitFor(1, TimeUnit.SECONDS)
                if (!exited) {
                    Log.w(tag, "Process did not terminate within timeout; killing forcefully...")
                    proc.destroyForcibly()
                }
            } catch (e: Exception) {
                proc.destroyForcibly()
            }
        }
        process = null
        _tunnelState.value = TunnelState.Disabled
    }

    private fun getCloudflaredBinary(): File? {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val candidate = File(nativeDir, "libcloudflared.so")
        return if (candidate.exists()) candidate else null
    }
}
