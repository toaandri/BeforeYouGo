package com.toaandri.beforeyougo

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar

object ReminderScheduler {
    private const val REQUEST = 51
    fun schedule(context: Context, hour: String) {
        val (h, m) = hour.split(":").map(String::toInt); val now = Calendar.getInstance(); val next = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (before(now)) add(Calendar.DAY_OF_YEAR, 1) }
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending(context))
    }
    fun cancel(context: Context) = context.getSystemService(AlarmManager::class.java).cancel(pending(context))
    private fun pending(context: Context) = PendingIntent.getBroadcast(context, REQUEST, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("morning_checklist", "Rappel du matin", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = android.app.Notification.Builder(context, "morning_checklist").setSmallIcon(com.toaandri.beforeyougo.R.mipmap.ic_launcher).setContentTitle("Avant de partir").setContentText("Ouvrez votre liste du jour.").setContentIntent(open).setAutoCancel(true).build()
        manager.notify(7, notification)
        val store = AppStore(context); if (store.reminderEnabled()) ReminderScheduler.schedule(context, store.reminderHour())
    }
}
