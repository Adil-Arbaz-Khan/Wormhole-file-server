package com.jarvis.fileserver.model

sealed interface ServerState {
    data object Idle : ServerState
    data object Starting : ServerState
    data class Running(
        val port: Int,
        val rootPath: String,
        val startTimeMillis: Long,
        val localInterfaces: List<NetworkInterfaceInfo>
    ) : ServerState
    data class Error(val message: String) : ServerState
}

sealed interface TunnelState {
    data object Disabled : TunnelState
    data object Connecting : TunnelState
    data class Connected(val url: String) : TunnelState
    data class Error(val message: String) : TunnelState
}
