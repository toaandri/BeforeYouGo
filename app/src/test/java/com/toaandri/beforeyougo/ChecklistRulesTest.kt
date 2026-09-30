package com.toaandri.beforeyougo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ChecklistRulesTest {
    @Test fun includes_essentials_and_due_or_overdue_one_off_items() {
        val day = LocalDate.of(2026, 9, 30)
        val result = itemsForDay(listOf(
            SavedItem(1, "Keys", true, null),
            SavedItem(2, "Passport", false, "2026-09-30"),
            SavedItem(3, "Book", false, "2026-09-29"),
            SavedItem(4, "Later", false, "2026-10-01"),
            SavedItem(5, "Archived", true, null, archived = true)
        ), day)
        assertEquals(listOf("Keys", "Passport", "Book"), result.map { it.title })
    }
}
