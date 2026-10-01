package com.toaandri.beforeyougo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Deliberately small on-device persistence layer for the MVP. */
class AppStore(val context: Context) {
    private val prefs = context.getSharedPreferences("before_you_go", Context.MODE_PRIVATE)
    fun items(): List<SavedItem> = runCatching {
        val array = JSONArray(prefs.getString("items", "[]") ?: "[]"); List(array.length()) { index ->
            val value = array.getJSONObject(index)
            SavedItem(value.getLong("id"), value.getString("title"), value.getBoolean("essential"), value.optString("dueDate").ifBlank { null }, value.optBoolean("archived"), value.optString("iconKey", "object"))
        }
    }.getOrDefault(emptyList())
    fun saveItems(items: List<SavedItem>) { val array = JSONArray(); items.forEach { item -> array.put(JSONObject().put("id", item.id).put("title", item.title).put("essential", item.essential).put("dueDate", item.dueDate ?: "").put("archived", item.archived).put("iconKey", item.iconKey)) }; prefs.edit().putString("items", array.toString()).apply() }
    fun checksForToday(): Map<Long, Boolean> = runCatching {
        val data = JSONObject(prefs.getString("checks", "{}") ?: "{}")
        val ids = data.optJSONArray("ids") ?: JSONArray()
        val completed = (0 until ids.length()).map { ids.getLong(it) }.toSet()
        if (data.optString("day") != LocalDate.now().toString()) {
            // Keep today's one-offs undoable; retire them only when the day changes.
            if (completed.isNotEmpty()) saveItems(items().map { item ->
                if (!item.essential && item.id in completed) item.copy(archived = true) else item
            })
            saveChecksForToday(emptyMap())
            emptyMap()
        } else completed.associateWith { true }
    }.getOrDefault(emptyMap())
    fun saveChecksForToday(checks: Map<Long, Boolean>) { val ids = JSONArray(); checks.filterValues { it }.keys.forEach(ids::put); prefs.edit().putString("checks", JSONObject().put("day", LocalDate.now()).put("ids", ids).toString()).apply() }
    fun reminderHour() = prefs.getString("reminder_hour", "08:00") ?: "08:00"
    fun reminderEnabled() = prefs.getBoolean("reminder_enabled", false)
    fun saveReminder(hour: String, enabled: Boolean) = prefs.edit().putString("reminder_hour", hour).putBoolean("reminder_enabled", enabled).apply()
    fun themePreference() = prefs.getString("theme_preference", ThemePreference.SYSTEM.name)?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() } ?: ThemePreference.SYSTEM
    fun saveThemePreference(preference: ThemePreference) = prefs.edit().putString("theme_preference", preference.name).apply()

    fun alarms(): List<DepartureAlarm> {
        if (!prefs.contains("alarms_v2")) {
            val time = reminderHour().split(":").map { it.toIntOrNull() ?: 0 }
            val migrated = if (reminderEnabled()) listOf(DepartureAlarm(
                id = 1, hour = time.first().coerceIn(0, 23), minute = (time.getOrNull(1) ?: 0).coerceIn(0, 59),
                days = (1..7).toSet()
            )) else emptyList()
            saveAlarms(migrated)
            return migrated
        }
        return runCatching {
            val values = JSONArray(prefs.getString("alarms_v2", "[]"))
            (0 until values.length()).mapNotNull { index -> runCatching {
                val value = values.getJSONObject(index)
                val days = value.getJSONArray("days")
                val ids = value.optJSONArray("items") ?: JSONArray()
                DepartureAlarm(
                    id = value.getLong("id"), name = value.getString("name"),
                    hour = value.getInt("hour").coerceIn(0, 23), minute = value.getInt("minute").coerceIn(0, 59),
                    days = (0 until days.length()).map { days.getInt(it) }.filter { it in 1..7 }.toSet(),
                    enabled = value.optBoolean("enabled", true),
                    deliveryMode = value.optString("deliveryMode", DeliveryMode.RING.name).let { runCatching { DeliveryMode.valueOf(it) }.getOrDefault(DeliveryMode.RING) },
                    soundUri = value.optString("sound", "default"),
                    soundName = value.optString("soundName", "Sonnerie du téléphone"),
                    vibrate = value.optBoolean("vibrate", true), snoozeMinutes = value.optInt("snooze", 5).coerceIn(1, 30),
                    itemIds = (0 until ids.length()).map { ids.getLong(it) }.toSet(),
                    allItems = value.optBoolean("allItems", true), skipUntil = value.optLong("skipUntil", 0)
                )
            }.getOrNull() }
        }.getOrDefault(emptyList())
    }

    fun saveAlarms(alarms: List<DepartureAlarm>) {
        val values = JSONArray()
        alarms.forEach { alarm -> values.put(JSONObject()
            .put("id", alarm.id).put("name", alarm.name).put("hour", alarm.hour).put("minute", alarm.minute)
            .put("days", JSONArray(alarm.days.sorted())).put("enabled", alarm.enabled).put("deliveryMode", alarm.deliveryMode.name)
            .put("sound", alarm.soundUri).put("soundName", alarm.soundName).put("vibrate", alarm.vibrate)
            .put("snooze", alarm.snoozeMinutes).put("items", JSONArray(alarm.itemIds.toList()))
            .put("allItems", alarm.allItems).put("skipUntil", alarm.skipUntil)) }
        prefs.edit().putString("alarms_v2", values.toString()).apply()
    }

    fun snoozedUntil(id: Long) = prefs.getLong("snooze_$id", 0)
    fun setSnoozedUntil(id: Long, millis: Long) = prefs.edit().putLong("snooze_$id", millis).apply()
}

enum class ThemePreference(val label: String) { SYSTEM("Système"), LIGHT("Clair"), DARK("Sombre") }
