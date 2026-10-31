package com.jarvis.fileserver.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import com.jarvis.fileserver.model.NetworkInterfaceInfo
import com.jarvis.fileserver.model.NetworkType
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    private const val TAG = "NetworkUtils"

    /**
     * Resolves local network addresses quickly.
     * Uses Android ConnectivityManager & WifiManager first for instant, non-blocking resolution,
     * followed by a fast interface scan.
     */
    fun getActiveNetworkInterfaces(context: Context, port: Int): List<NetworkInterfaceInfo> {
        val interfaces = mutableListOf<NetworkInterfaceInfo>()

        // 1. Instant resolution via ConnectivityManager (System-cached, zero ioctl delay)
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                for (network in cm.allNetworks) {
                    val caps = cm.getNetworkCapabilities(network) ?: continue
                    val lp = cm.getLinkProperties(network) ?: continue

                    val (displayName, netType) = when {
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Local Wi-Fi" to NetworkType.WIFI
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet" to NetworkType.ETHERNET
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_USB) -> "USB Tethering" to NetworkType.USB
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data" to NetworkType.CELLULAR
                        else -> (lp.interfaceName ?: "Network") to NetworkType.OTHER
                    }

                    for (linkAddr in lp.linkAddresses) {
                        val addr = linkAddr.address
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val ip = addr.hostAddress ?: continue
                            if (interfaces.none { it.ipAddress == ip }) {
                                interfaces.add(
                                    NetworkInterfaceInfo(
                                        interfaceName = lp.interfaceName ?: "net",
                                        displayName = displayName,
                                        ipAddress = ip,
                                        port = port,
                                        type = netType
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ConnectivityManager query failed", e)
        }

        // 2. Wi-Fi Hotspot & USB Tethering fallback scan
        try {
            val netInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (netIf in netInterfaces) {
                val lowerName = netIf.name.lowercase()

                // Fast skip for carrier and virtual devices to avoid modem ioctl timeouts
                if (lowerName.startsWith("dummy") ||
                    lowerName.startsWith("sit") ||
                    lowerName.startsWith("ip6tnl") ||
                    lowerName.startsWith("tun") ||
                    lowerName.startsWith("tap") ||
                    lowerName.startsWith("bonding") ||
                    lowerName.startsWith("rmnet") ||
                    lowerName.startsWith("ccmni")
                ) {
                    continue
                }

                val inetAddresses = runCatching { Collections.list(netIf.inetAddresses) }.getOrDefault(emptyList())
                for (inetAddress in inetAddresses) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        val hostAddress = inetAddress.hostAddress ?: continue
                        if (interfaces.none { it.ipAddress == hostAddress }) {
                            val (displayName, networkType) = resolveInterfaceType(netIf.name)
                            interfaces.add(
                                NetworkInterfaceInfo(
                                    interfaceName = netIf.name,
                                    displayName = displayName,
                                    ipAddress = hostAddress,
                                    port = port,
                                    type = networkType
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "NetworkInterface scan failed", e)
        }

        // 3. Always include Localhost fallback
        if (interfaces.none { it.type == NetworkType.LOCALHOST }) {
            interfaces.add(
                NetworkInterfaceInfo(
                    interfaceName = "lo",
                    displayName = "Localhost",
                    ipAddress = "127.0.0.1",
                    port = port,
                    type = NetworkType.LOCALHOST
                )
            )
        }

        return interfaces.sortedBy { getSortPriority(it.type) }
    }

    private fun resolveInterfaceType(name: String): Pair<String, NetworkType> {
        val lower = name.lowercase()
        return when {
            lower.startsWith("wlan") || lower.startsWith("tiwlan") -> {
                "Local Wi-Fi" to NetworkType.WIFI
            }
            lower.startsWith("ap") || lower.startsWith("softap") || lower.startsWith("wigig") -> {
                "Wi-Fi Hotspot" to NetworkType.HOTSPOT
            }
            lower.startsWith("rndis") || lower.startsWith("usb") -> {
                "USB Tethering" to NetworkType.USB
            }
            lower.startsWith("eth") -> {
                "Ethernet LAN" to NetworkType.ETHERNET
            }
            lower.startsWith("lo") -> {
                "Localhost" to NetworkType.LOCALHOST
            }
            else -> {
                "Network ($name)" to NetworkType.OTHER
            }
        }
    }

    private fun getSortPriority(type: NetworkType): Int {
        return when (type) {
            NetworkType.WIFI -> 1
            NetworkType.HOTSPOT -> 2
            NetworkType.USB -> 3
            NetworkType.ETHERNET -> 4
            NetworkType.CELLULAR -> 5
            NetworkType.OTHER -> 6
            NetworkType.LOCALHOST -> 7
        }
    }
}
