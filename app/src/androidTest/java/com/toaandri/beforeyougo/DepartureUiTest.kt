package com.toaandri.beforeyougo

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.toaandri.beforeyougo.ui.theme.BeforeYouGoTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DepartureUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun editorSavesIndependentSelectedWeekdays() {
        var result: DepartureAlarm? = null
        compose.setContent {
            BeforeYouGoTheme {
                AlarmEditor(DepartureAlarm(91, name = "Sport", days = setOf(1)), emptyList(), {}) { result = it }
            }
        }
        compose.onNodeWithText("Mar", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("Enregistrer l’alarme").performClick()
        compose.runOnIdle { assertEquals(setOf(1, 2), result!!.days); assertEquals("Sport", result!!.name) }
    }

    @Test fun catalogSearchAddsOnceAndShowsSelectedState() {
        val selected = mutableStateOf(emptyList<SavedItem>())
        compose.setContent {
            BeforeYouGoTheme {
                ObjectsScreen(selected.value, androidx.compose.ui.Modifier, {}, {}, { idea ->
                    selected.value = selected.value + SavedItem(1, idea.name, true, null, iconKey = idea.key)
                })
            }
        }
        compose.onNodeWithText("Du quotidien").performClick()
        compose.onNodeWithText("Rechercher un objet").performTextInput("parapluie")
        compose.onNodeWithText("Parapluie").performClick()
        compose.onNodeWithText("✓ Dans mes objets").assertExists()
        compose.runOnIdle { assertEquals("umbrella", selected.value.single().iconKey) }
    }

    @Test fun legacyAlarmMigrationAndCustomFieldsRoundTrip() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = base.getSharedPreferences("test_alarm_store", Context.MODE_PRIVATE)
        prefs.edit().clear().putBoolean("reminder_enabled", true).putString("reminder_hour", "07:45").commit()
        val isolated = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String?, mode: Int) = prefs
        }
        try {
            val store = AppStore(isolated)
            val migrated = store.alarms().single()
            assertEquals(7, migrated.hour); assertEquals(45, migrated.minute); assertEquals((1..7).toSet(), migrated.days)
            val custom = migrated.copy(name = "Sport", days = setOf(2, 4), soundUri = "silent", vibrate = false,
                snoozeMinutes = 15, allItems = false, itemIds = setOf(12, 15), skipUntil = 987654)
            store.saveAlarms(listOf(custom))
            assertEquals(custom, AppStore(isolated).alarms().single())
            store.saveAlarms(emptyList())
            assertTrue(AppStore(isolated).alarms().isEmpty()) // Do not migrate again after deleting the last alarm.
        } finally { prefs.edit().clear().commit() }
    }
}
