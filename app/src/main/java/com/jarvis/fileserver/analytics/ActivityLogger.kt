package com.jarvis.fileserver.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class ActivityType {
    VISITOR_CONNECTED,
    FILE_STREAMED,
    FILE_DOWNLOADED,
    FILE_UPLOADED,
    FILE_DELETED,
    FOLDER_CREATED,
    CLIPBOARD_SYNCED,
    SERVER_EVENT,
    SHARE_CREATED,
    SHARE_REVOKED
}

data class ActivityEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: ActivityType,
    val title: String,
    val detail: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

object ActivityLogger {
    private const val MAX_LOGS = 100
    private val logs = mutableListOf<ActivityEvent>()
    private val _events = MutableStateFlow<List<ActivityEvent>>(emptyList())
    val events: StateFlow<List<ActivityEvent>> = _events.asStateFlow()

    @Synchronized
    fun log(type: ActivityType, title: String, detail: String = "") {
        val event = ActivityEvent(type = type, title = title, detail = detail)
        logs.add(0, event)
        if (logs.size > MAX_LOGS) {
            logs.removeAt(logs.lastIndex)
        }
        _events.value = logs.toList()
    }

    @Synchronized
    fun clear() {
        logs.clear()
        _events.value = emptyList()
    }
}
