package com.toaandri.beforeyougo

import java.time.LocalDate

/** Small, platform-independent rules for the list shown on a given day. */
data class SavedItem(
    val id: Long,
    val title: String,
    val essential: Boolean,
    val dueDate: String?,
    val archived: Boolean = false,
    val iconKey: String = "object"
)

fun itemsForDay(items: List<SavedItem>, day: LocalDate): List<SavedItem> =
    items.filter { item ->
        !item.archived && (item.essential || item.dueDate?.let { runCatching { LocalDate.parse(it) <= day }.getOrDefault(false) } == true)
    }

fun completionFor(items: List<SavedItem>, checks: Map<Long, Boolean>): Float =
    if (items.isEmpty()) 0f else items.count { checks[it.id] == true }.toFloat() / items.size
