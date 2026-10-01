package com.toaandri.beforeyougo

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat

object ReminderScheduler {
    const val EXTRA_ID = "alarm_id"
    private const val SNOOZE = "snoozed"
    const val ACTION_NOTIFICATION_SNOOZE = "notification_snooze"
    fun canSchedule(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun openIntent(context: Context, id: Long): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java).setData(Uri.parse("beforeyougo://alarm/$id"))
            .putExtra(EXTRA_ID, id).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun notificationSnoozeIntent(context: Context, id: Long): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java)
            .setData(Uri.parse("beforeyougo://notification/$id/snooze"))
            .putExtra(EXTRA_ID, id).putExtra("action", ACTION_NOTIFICATION_SNOOZE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun pending(context: Context, id: Long, snooze: Boolean = false): PendingIntent =
        PendingIntent.getBroadcast(context, 0, Intent(context, ReminderReceiver::class.java)
            .setData(Uri.parse("beforeyougo://schedule/$id/$snooze"))
            .putExtra(EXTRA_ID, id).putExtra(SNOOZE, snooze),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun schedule(context: Context, alarm: DepartureAlarm): Boolean {
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(pending(context, alarm.id))
        val next = nextOccurrence(alarm) ?: return true
        if (!canSchedule(context)) return false
        return runCatching {
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(next.toInstant().toEpochMilli(), openIntent(context, alarm.id)), pending(context, alarm.id))
        }.isSuccess
    }

    fun cancel(context: Context, id: Long) {
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(pending(context, id)); manager.cancel(pending(context, id, true))
        AppStore(context).setSnoozedUntil(id, 0)
    }

    fun snooze(context: Context, alarm: DepartureAlarm): Boolean {
        val time = System.currentTimeMillis() + alarm.snoozeMinutes * 60_000L
        if (!scheduleSnooze(context, alarm.id, time)) return false
        AppStore(context).setSnoozedUntil(alarm.id, time)
        return true
    }

    private fun scheduleSnooze(context: Context, id: Long, time: Long): Boolean {
        if (!canSchedule(context)) return false
        return runCatching { context.getSystemService(AlarmManager::class.java).setAlarmClock(
            AlarmManager.AlarmClockInfo(time, openIntent(context, id)), pending(context, id, true)) }.isSuccess
    }

    fun restore(context: Context) {
        // Retire the old single-alarm PendingIntent once the store has migrated.
        val store = AppStore(context)
        val alarms = store.alarms()
        context.getSystemService(AlarmManager::class.java).cancel(PendingIntent.getBroadcast(
            context, 51, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        alarms.forEach { alarm ->
            if (!alarm.enabled) cancel(context, alarm.id) else {
                schedule(context, alarm)
                val snooze = store.snoozedUntil(alarm.id)
                if (snooze > System.currentTimeMillis()) scheduleSnooze(context, alarm.id, snooze)
                else store.setSnoozedUntil(alarm.id, 0)
            }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1)
        val store = AppStore(context)
        val alarm = store.alarms().find { it.id == id && it.enabled } ?: return
        if (intent.getStringExtra("action") == ReminderScheduler.ACTION_NOTIFICATION_SNOOZE) {
            ReminderScheduler.snooze(context, alarm)
            context.getSystemService(NotificationManager::class.java).cancel(id.toInt())
            return
        }
        if (intent.getBooleanExtra("snoozed", false)) store.setSnoozedUntil(id, 0)
        else ReminderScheduler.schedule(context, alarm)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (alarm.deliveryMode == DeliveryMode.NOTIFICATION) {
            showQuietNotification(context, alarm)
            return
        }
        // This foreground service is started only by an exact, user-created alarm (or a visible preview).
        runCatching { context.startForegroundService(Intent(context, AlarmRingingService::class.java)
            .putExtra(ReminderScheduler.EXTRA_ID, id)) }
    }

    private fun showQuietNotification(context: Context, alarm: DepartureAlarm) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = "departure_reminder_v1"
        manager.createNotificationChannel(NotificationChannel(channel, "Rappels BeforeYouGo", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Rappels discrets de vos routines de départ"
            setSound(null, null)
            enableVibration(false)
        })
        val notification = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(alarm.name)
            .setContentText("Votre checklist est prête à être vérifiée.")
            .setContentIntent(ReminderScheduler.openIntent(context, alarm.id))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .addAction(Notification.Action.Builder(null, "Ouvrir", ReminderScheduler.openIntent(context, alarm.id)).build())
            .addAction(Notification.Action.Builder(null, "Reporter ${alarm.snoozeMinutes} min", ReminderScheduler.notificationSnoozeIntent(context, alarm.id)).build())
            .build()
        manager.notify(alarm.id.toInt(), notification)
    }
}

class AlarmRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) ReminderScheduler.restore(context)
    }
}
