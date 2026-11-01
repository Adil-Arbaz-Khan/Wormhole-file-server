package com.jarvis.fileserver.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class TrafficSnapshot(
    val uploadSpeedBps: Long = 0L,      // Host upload speed (bytes/sec sent to visitors)
    val downloadSpeedBps: Long = 0L,    // Host download speed (bytes/sec received from visitors)
    val totalBytesSent: Long = 0L,       // Total bytes sent to clients
    val totalBytesReceived: Long = 0L,   // Total bytes received from clients
    val activeConnections: Int = 0,      // In-flight HTTP connections
    val activeVisitorsCount: Int = 0     // Unique visitors active in last 5 minutes
)

object TrafficMonitor {
    private val totalSent = AtomicLong(0L)
    private val totalReceived = AtomicLong(0L)
    private val activeConnections = AtomicInteger(0)

    private val windowSent = AtomicLong(0L)
    private val windowReceived = AtomicLong(0L)

    // Key = Client Identifier (IP or hash), Value = timestamp
    private val visitorSessions = ConcurrentHashMap<String, Long>()

    private val _snapshot = MutableStateFlow(TrafficSnapshot())
    val snapshot: StateFlow<TrafficSnapshot> = _snapshot.asStateFlow()

    val currentSnapshot: TrafficSnapshot
        get() = _snapshot.value.copy(
            totalBytesSent = totalSent.get(),
            totalBytesReceived = totalReceived.get(),
            activeConnections = activeConnections.get().coerceAtLeast(0)
        )

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        if (monitorJob != null) return
        monitorJob = scope.launch {
            while (isActive) {
                delay(1000)
                val sentInSec = windowSent.getAndSet(0L)
                val recvInSec = windowReceived.getAndSet(0L)

                // Clean up sessions older than 5 minutes
                val now = System.currentTimeMillis()
                val it = visitorSessions.entries.iterator()
                while (it.hasNext()) {
                    val entry = it.next()
                    if (now - entry.value > 5 * 60 * 1000L) {
                        it.remove()
                    }
                }

                _snapshot.value = TrafficSnapshot(
                    uploadSpeedBps = sentInSec,
                    downloadSpeedBps = recvInSec,
                    totalBytesSent = totalSent.get(),
                    totalBytesReceived = totalReceived.get(),
                    activeConnections = activeConnections.get().coerceAtLeast(0),
                    activeVisitorsCount = visitorSessions.size
                )
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        activeConnections.set(0)
        windowSent.set(0L)
        windowReceived.set(0L)
        visitorSessions.clear()
        _snapshot.value = TrafficSnapshot(
            totalBytesSent = totalSent.get(),
            totalBytesReceived = totalReceived.get()
        )
    }

    fun recordSent(bytes: Long) {
        if (bytes <= 0) return
        totalSent.addAndGet(bytes)
        windowSent.addAndGet(bytes)
    }

    fun recordReceived(bytes: Long) {
        if (bytes <= 0) return
        totalReceived.addAndGet(bytes)
        windowReceived.addAndGet(bytes)
    }

    fun onConnectionOpened() {
        activeConnections.incrementAndGet()
    }

    fun onConnectionClosed() {
        activeConnections.decrementAndGet()
    }

    fun registerVisitor(clientKey: String) {
        if (clientKey.isNotBlank()) {
            visitorSessions[clientKey] = System.currentTimeMillis()
        }
    }

    fun reset() {
        totalSent.set(0L)
        totalReceived.set(0L)
        windowSent.set(0L)
        windowReceived.set(0L)
        activeConnections.set(0)
        visitorSessions.clear()
        _snapshot.value = TrafficSnapshot()
    }
}
