package com.toaandri.beforeyougo

import java.time.LocalDate

/** Small, platform-independent rules for the list shown on a given day. */
data class SavedItem(
    val id: Long,
    val title: String,
    val essential: Boolean,
    val dueDate: String?,
    val archived: Boolean = false
)

fun itemsForDay(items: List<SavedItem>, day: LocalDate): List<SavedItem> =
    items.filter { item ->
        !item.archived && (item.essential || item.dueDate?.let { LocalDate.parse(it) <= day } == true)
    }
