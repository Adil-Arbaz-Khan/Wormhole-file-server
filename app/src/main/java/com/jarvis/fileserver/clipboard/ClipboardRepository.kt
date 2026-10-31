package com.jarvis.fileserver.clipboard

import com.jarvis.fileserver.model.ClipboardItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ClipboardRepository {
    private val _items = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val items: StateFlow<List<ClipboardItem>> = _items.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun addFromPhone(text: String): ClipboardItem {
        val item = ClipboardItem(
            text = text,
            sender = "phone",
            timestamp = System.currentTimeMillis()
        )
        _items.value = listOf(item) + _items.value
        return item
    }

    fun addFromWeb(text: String): ClipboardItem {
        val item = ClipboardItem(
            text = text,
            sender = "web",
            timestamp = System.currentTimeMillis()
        )
        _items.value = listOf(item) + _items.value
        _unreadCount.value = _unreadCount.value + 1
        return item
    }

    fun markAsRead() {
        _unreadCount.value = 0
    }

    fun removeItem(id: String) {
        _items.value = _items.value.filter { it.id != id }
    }

    fun clearAll() {
        _items.value = emptyList()
        _unreadCount.value = 0
    }
}
