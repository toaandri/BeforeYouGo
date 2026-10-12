package com.toaandri.beforeyougo

import java.time.ZonedDateTime

enum class DeliveryMode(private val labelResource: Int, private val descriptionResource: Int) {
    RING(R.string.ui_with_a_sound, R.string.ui_a_sound_if_you_want_one_stop_or_snooze_it),
    NOTIFICATION(R.string.ui_quiet_nudge, R.string.ui_a_quiet_invitation_to_check_your_bag_open_it_when);
    val label: String get() = uiText(labelResource)
    val description: String get() = uiText(descriptionResource)
}

data class DepartureAlarm(
    val id: Long,
    val name: String = uiText(R.string.ui_my_departure),
    val hour: Int = 8,
    val minute: Int = 0,
    val days: Set<Int> = (1..5).toSet(),
    val enabled: Boolean = true,
    val deliveryMode: DeliveryMode = DeliveryMode.NOTIFICATION,
    val soundUri: String = "default",
    val soundName: String = uiText(R.string.ui_phone_ringtone),
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 5,
    val itemIds: Set<Long> = emptySet(),
    val allItems: Boolean = true,
    val skipUntil: Long = 0
) {
    val timeLabel: String get() = "%02d:%02d".format(hour, minute)
}

/** ISO weekdays: Monday=1. Local wall time is recalculated after each occurrence (DST-safe). */
fun nextOccurrence(alarm: DepartureAlarm, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
    if (!alarm.enabled || alarm.days.isEmpty()) return null
    return (0..14).asSequence().map { offset ->
        now.toLocalDate().plusDays(offset.toLong()).atTime(alarm.hour, alarm.minute).atZone(now.zone)
    }.firstOrNull { candidate ->
        candidate.dayOfWeek.value in alarm.days && candidate.isAfter(now) &&
            candidate.toInstant().toEpochMilli() > alarm.skipUntil
    }
}

fun itemsForAlarm(items: List<SavedItem>, alarm: DepartureAlarm, day: java.time.LocalDate): List<SavedItem> =
    itemsForDay(items, day).filter { alarm.allItems || it.id in alarm.itemIds }

fun daysLabel(days: Set<Int>): String = when (days) {
    (1..7).toSet() -> uiText(R.string.ui_every_day)
    (1..5).toSet() -> uiText(R.string.ui_monday_to_friday)
    setOf(6, 7) -> uiText(R.string.ui_at_the_weekend)
    else -> days.sorted().joinToString(" · ") { listOf(uiText(R.string.ui_mon), uiText(R.string.ui_tue), uiText(R.string.ui_wed), uiText(R.string.ui_thu), uiText(R.string.ui_fri), uiText(R.string.ui_sat), uiText(R.string.ui_sun))[it - 1] }
}

val DepartureAlarm.displaySoundName: String
    get() = when (soundUri) {
        "default" -> uiText(R.string.ui_phone_ringtone)
        "silent" -> uiText(R.string.ui_silent)
        else -> soundName
    }
