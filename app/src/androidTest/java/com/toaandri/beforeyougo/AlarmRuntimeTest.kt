package com.toaandri.beforeyougo

import android.app.NotificationManager
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
    private fun awaitNotification(manager: NotificationManager, id: Int, visible: Boolean): Boolean {
        val deadline = System.currentTimeMillis() + 3_000
        while (System.currentTimeMillis() < deadline) {
            if (manager.activeNotifications.any { it.id == id } == visible) return true
            Thread.sleep(50)
        }
        return false
    }

    @Test fun exactAlarmActuallyStartsAudioAndSnoozeStopsIt() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue(ReminderScheduler.canSchedule(context))
        assumeTrue(NotificationManagerCompat.from(context).areNotificationsEnabled())
        val store = AppStore(context)
        val time = ZonedDateTime.now().plusMinutes(1)
        val id = newId(store.alarms().map { it.id })
        val alarm = DepartureAlarm(id, name = "Essai technique", hour = time.hour, minute = time.minute,
            days = (1..7).toSet(), deliveryMode = DeliveryMode.RING, vibrate = false, snoozeMinutes = 1)
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
            assertEquals(DeliveryMode.RING, migrated.deliveryMode)
            val custom = migrated.copy(name = "Sport", days = setOf(2, 4), deliveryMode = DeliveryMode.NOTIFICATION, soundUri = "silent", vibrate = false,
                snoozeMinutes = 15, allItems = false, itemIds = setOf(12, 15), skipUntil = 987654)
            store.saveAlarms(listOf(custom))
            assertEquals(custom, AppStore(isolated).alarms().single())
            store.saveAlarms(emptyList())
            assertTrue(AppStore(isolated).alarms().isEmpty())
        } finally { prefs.edit().clear().commit() }
    }

    @Test fun savedRingingRoutineWithoutDeliveryModeStillRings() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = base.getSharedPreferences("test_legacy_alarm_mode", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("alarms_v2", """[{"id":1,"name":"Travail","hour":8,"minute":0,"days":[1,2,3,4,5]}]""").commit()
        val isolated = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String?, mode: Int) = prefs
        }
        try {
            assertEquals(DeliveryMode.RING, AppStore(isolated).alarms().single().deliveryMode)
        } finally { prefs.edit().clear().commit() }
    }

    @Test fun decliningQuietInvitationLeavesObjectsAndRoutineUntouched() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        assumeTrue(NotificationManagerCompat.from(base).areNotificationsEnabled())
        val prefs = base.getSharedPreferences("test_quiet_invitation", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val isolated = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String?, mode: Int) = prefs
        }
        val manager = base.getSystemService(NotificationManager::class.java)
        val id = 1_900_000_123L
        val store = AppStore(isolated)
        val alarm = DepartureAlarm(id, name = "Mon tour", deliveryMode = DeliveryMode.NOTIFICATION)
        val items = listOf(SavedItem(12, "Clés", true, null), SavedItem(15, "Gourde", true, null))
        val checks = mapOf(12L to true, 15L to true)
        val until = System.currentTimeMillis() + 300_000
        try {
            store.saveAlarms(listOf(alarm))
            store.saveItems(items)
            store.saveChecksForToday(checks)
            // A delivered snooze avoids scheduling any real future event in this test.
            instrumentation.runOnMainSync {
                ReminderReceiver().onReceive(isolated, Intent()
                    .putExtra(ReminderScheduler.EXTRA_ID, id).putExtra("snoozed", true))
            }
            assumeTrue(manager.getNotificationChannel("departure_reminder_v1").importance != NotificationManager.IMPORTANCE_NONE)
            assertTrue(awaitNotification(manager, id.toInt(), visible = true))
            store.setSnoozedUntil(id, until)
            instrumentation.runOnMainSync {
                ReminderReceiver().onReceive(isolated, Intent()
                    .putExtra(ReminderScheduler.EXTRA_ID, id)
                    .putExtra("action", ReminderScheduler.ACTION_NOTIFICATION_DISMISS))
            }
            assertTrue(awaitNotification(manager, id.toInt(), visible = false))
            assertEquals(listOf(alarm), store.alarms())
            assertEquals(items, store.items())
            assertEquals(checks, store.checksForToday())
            assertEquals(until, store.snoozedUntil(id))
        } finally {
            manager.cancel(id.toInt())
            prefs.edit().clear().commit()
        }
    }
}
