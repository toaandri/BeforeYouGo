package com.toaandri.beforeyougo

import java.time.ZonedDateTime

enum class DeliveryMode(val label: String, val description: String) {
    RING("Sonnerie", "Une alarme audible avec commandes Arrêter et Reporter."),
    NOTIFICATION("Notification", "Un rappel discret dans la barre de notifications, sans sonnerie.")
}

data class DepartureAlarm(
    val id: Long,
    val name: String = "Mon départ",
    val hour: Int = 8,
    val minute: Int = 0,
    val days: Set<Int> = (1..5).toSet(),
    val enabled: Boolean = true,
    val deliveryMode: DeliveryMode = DeliveryMode.RING,
    val soundUri: String = "default",
    val soundName: String = "Sonnerie du téléphone",
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
    (1..7).toSet() -> "Tous les jours"
    (1..5).toSet() -> "Du lundi au vendredi"
    setOf(6, 7) -> "Le week-end"
    else -> days.sorted().joinToString(" · ") { listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")[it - 1] }
}
