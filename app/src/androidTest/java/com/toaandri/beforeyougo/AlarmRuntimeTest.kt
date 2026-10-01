package com.toaandri.beforeyougo

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.ZonedDateTime

/** No Activity launch required: also works on phones blocking background test Activities. */
class AlarmRuntimeTest {
    @Test fun exactAlarmActuallyStartsAudioAndSnoozeStopsIt() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue(ReminderScheduler.canSchedule(context))
        assumeTrue(NotificationManagerCompat.from(context).areNotificationsEnabled())
        val store = AppStore(context)
        val time = ZonedDateTime.now().plusMinutes(1)
        val id = newId(store.alarms().map { it.id })
        val alarm = DepartureAlarm(id, name = "Essai technique", hour = time.hour, minute = time.minute,
            days = (1..7).toSet(), vibrate = false, snoozeMinutes = 1)
        try {
            store.saveAlarms(store.alarms() + alarm)
            assertTrue(ReminderScheduler.schedule(context, alarm))
            val deadline = System.currentTimeMillis() + 75_000
            var playing = false
            while (!playing && System.currentTimeMillis() < deadline) {
                instrumentation.runOnMainSync { playing = RingingState.soundPlaying && RingingState.alarmId == id }
                if (!playing) Thread.sleep(100)
            }
            assertTrue("The scheduled receiver must start real audio playback", playing)
            instrumentation.runOnMainSync { AlarmRingingService.command(context, id, AlarmRingingService.SNOOZE) }
            val stoppedBy = System.currentTimeMillis() + 5_000
            while (playing && System.currentTimeMillis() < stoppedBy) {
                instrumentation.runOnMainSync { playing = RingingState.alarmId == id }
                if (playing) Thread.sleep(100)
            }
            assertFalse(playing)
            assertTrue(store.snoozedUntil(id) > System.currentTimeMillis())
            ReminderScheduler.cancel(context, id)
            assertEquals(0, store.snoozedUntil(id))
        } finally {
            instrumentation.runOnMainSync { AlarmRingingService.command(context, id, AlarmRingingService.STOP) }
            ReminderScheduler.cancel(context, id)
            store.saveAlarms(store.alarms().filterNot { it.id == id })
            ReminderScheduler.restore(context)
        }
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
            val custom = migrated.copy(name = "Sport", days = setOf(2, 4), deliveryMode = DeliveryMode.NOTIFICATION, soundUri = "silent", vibrate = false,
                snoozeMinutes = 15, allItems = false, itemIds = setOf(12, 15), skipUntil = 987654)
            store.saveAlarms(listOf(custom))
            assertEquals(custom, AppStore(isolated).alarms().single())
            store.saveAlarms(emptyList())
            assertTrue(AppStore(isolated).alarms().isEmpty())
        } finally { prefs.edit().clear().commit() }
    }
}
