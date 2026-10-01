package com.toaandri.beforeyougo

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.*
import android.net.Uri
import android.os.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object RingingState {
    var alarmId by mutableStateOf<Long?>(null)
    var preview by mutableStateOf(false)
    var soundPlaying by mutableStateOf(false)
}

/** A bounded alarm session, independent of the Activity. Never starts at boot. */
class AlarmRingingService : Service() {
    private var player: MediaPlayer? = null
    private var current: DepartureAlarm? = null
    private val queue = mutableListOf<Long>()
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { finishCurrent() }
    private val vibrator by lazy { getSystemService(Vibrator::class.java) }
    private var focus: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()

    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getLongExtra(ReminderScheduler.EXTRA_ID, -1) ?: -1
        if (intent?.action == STOP || intent?.action == SNOOZE) {
            if (current?.id == id) {
                if (intent.action == SNOOZE && !RingingState.preview) current?.let { ReminderScheduler.snooze(this, it) }
                finishCurrent()
            } else queue.remove(id)
            return START_NOT_STICKY
        }
        val alarm = AppStore(this).alarms().find { it.id == id } ?: run { stopSelf(); return START_NOT_STICKY }
        if (current != null && !RingingState.preview) {
            if (current?.id != id && id !in queue) queue.add(id)
            return START_NOT_STICKY
        }
        begin(alarm, intent?.getBooleanExtra(PREVIEW, false) == true)
        return START_NOT_STICKY
    }

    private fun begin(alarm: DepartureAlarm, preview: Boolean) {
        releaseSound()
        current = alarm; RingingState.alarmId = alarm.id; RingingState.preview = preview
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Sonneries de départ", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarmes programmées avec commandes d’arrêt et de report"
            setSound(null, null); enableVibration(false)
        })
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_alarm_notification).setContentTitle(if (preview) "Essai · ${alarm.name}" else alarm.name)
            .setContentText("C’est le moment de préparer vos objets.")
            .setContentIntent(ReminderScheduler.openIntent(this, alarm.id)).setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC).setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Arrêter", actionIntent(alarm.id, STOP)).build())
            .apply { if (!preview) addAction(Notification.Action.Builder(null, "Reporter ${alarm.snoozeMinutes} min", actionIntent(alarm.id, SNOOZE)).build()) }
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(NOTIFICATION, notification)
        wakeLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BeforeYouGo:ring").apply {
            acquire(if (preview) 15_000L else 125_000L)
        }
        if (alarm.soundUri != "silent") {
            focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes).setOnAudioFocusChangeListener { change ->
                    if (change == AudioManager.AUDIOFOCUS_LOSS) finishCurrent()
                }.build().also { getSystemService(AudioManager::class.java).requestAudioFocus(it) }
            val sound = if (alarm.soundUri == "default") defaultSound() else Uri.parse(alarm.soundUri)
            if (sound != null) play(sound, fallback = alarm.soundUri != "default")
        }
        if (alarm.vibrate) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 350, 500, 1000), 0), attributes)
        // Preview is deliberately short. A missed alarm never rings indefinitely.
        handler.postDelayed(timeout, if (preview) 10_000L else 120_000L)
    }

    private fun defaultSound(): Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun play(uri: Uri, fallback: Boolean) {
        val media = MediaPlayer()
        player = media
        runCatching {
            media.setAudioAttributes(attributes); media.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            media.setDataSource(this, uri); media.isLooping = true
            media.setOnPreparedListener { if (player === it) { it.start(); RingingState.soundPlaying = true } }
            media.setOnErrorListener { failed, _, _ ->
                failed.release(); if (player === failed) player = null
                if (fallback) defaultSound()?.let { play(it, false) }
                true
            }
            media.prepareAsync()
        }.onFailure {
            media.release(); player = null
            if (fallback) defaultSound()?.let { play(it, false) }
        }
    }

    private fun actionIntent(id: Long, action: String) = PendingIntent.getService(this, 0,
        Intent(this, AlarmRingingService::class.java).setAction(action).setData(Uri.parse("beforeyougo://ring/$id/$action"))
            .putExtra(ReminderScheduler.EXTRA_ID, id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun finishCurrent() {
        releaseSound(); current = null; RingingState.alarmId = null; RingingState.preview = false
        while (queue.isNotEmpty()) {
            val id = queue.removeAt(0)
            val next = AppStore(this).alarms().find { it.id == id && it.enabled }
            if (next != null) { begin(next, false); return }
        }
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    private fun releaseSound() {
        handler.removeCallbacks(timeout); player?.release(); player = null; vibrator.cancel(); RingingState.soundPlaying = false
        wakeLock?.let { if (it.isHeld) it.release() }; wakeLock = null
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }; focus = null
    }
    override fun onDestroy() {
        releaseSound(); RingingState.alarmId = null; RingingState.preview = false; super.onDestroy()
    }

    companion object {
        const val STOP = "stop"
        const val SNOOZE = "snooze"
        const val PREVIEW = "preview"
        const val CHANNEL = "departure_ring_v3"
        private const val NOTIFICATION = 700
        fun command(context: Context, id: Long, action: String) {
            // Never create an idle service merely to stop an already-finished session.
            if (RingingState.alarmId != null) context.startService(Intent(context, AlarmRingingService::class.java)
                .setAction(action).putExtra(ReminderScheduler.EXTRA_ID, id))
        }
    }
}
