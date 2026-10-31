package com.jarvis.fileserver.model

enum class NetworkType {
    WIFI,
    HOTSPOT,
    USB,
    ETHERNET,
    CELLULAR,
    LOCALHOST,
    OTHER
}

data class NetworkInterfaceInfo(
    val interfaceName: String,
    val displayName: String,
    val ipAddress: String,
    val port: Int,
    val type: NetworkType
) {
    val fullUrl: String
        get() = "http://$ipAddress:$port"
}
