package com.toaandri.beforeyougo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Deliberately small on-device persistence layer for the MVP. */
class AppStore(val context: Context) {
    private val prefs = context.getSharedPreferences("before_you_go", Context.MODE_PRIVATE)
    fun items(): List<SavedItem> = runCatching {
        val array = JSONArray(prefs.getString("items", "[]")); List(array.length()) { index ->
            val value = array.getJSONObject(index)
            SavedItem(value.getLong("id"), value.getString("title"), value.getBoolean("essential"), value.optString("dueDate").ifBlank { null }, value.optBoolean("archived"))
        }
    }.getOrDefault(emptyList())
    fun saveItems(items: List<SavedItem>) { val array = JSONArray(); items.forEach { item -> array.put(JSONObject().put("id", item.id).put("title", item.title).put("essential", item.essential).put("dueDate", item.dueDate ?: "").put("archived", item.archived)) }; prefs.edit().putString("items", array.toString()).apply() }
    fun checksForToday(): Map<Long, Boolean> = runCatching { val data = JSONObject(prefs.getString("checks", "{}")); if (data.optString("day") != LocalDate.now().toString()) emptyMap() else data.getJSONArray("ids").let { ids -> List(ids.length()) { ids.getLong(it) }.associateWith { true } } }.getOrDefault(emptyMap())
    fun saveChecksForToday(checks: Map<Long, Boolean>) { val ids = JSONArray(); checks.filterValues { it }.keys.forEach(ids::put); prefs.edit().putString("checks", JSONObject().put("day", LocalDate.now()).put("ids", ids).toString()).apply() }
    fun reminderHour() = prefs.getString("reminder_hour", "08:00") ?: "08:00"
    fun reminderEnabled() = prefs.getBoolean("reminder_enabled", false)
    fun saveReminder(hour: String, enabled: Boolean) = prefs.edit().putString("reminder_hour", hour).putBoolean("reminder_enabled", enabled).apply()
}
