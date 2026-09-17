package com.example.sensy.data

import android.content.Context
import android.content.SharedPreferences

data class ActiveStatus(
    val statusText: String,
    val startTimeMillis: Long,
    val durationMinutes: Int
)

class StatusRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun saveStatus(statusText: String, durationMinutes: Int, startTimeMillis: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putString(KEY_STATUS_TEXT, statusText)
            .putLong(KEY_START_TIME, startTimeMillis)
            .putInt(KEY_DURATION, durationMinutes)
            .apply()
    }

    fun getActiveStatus(): ActiveStatus? {
        val statusText = prefs.getString(KEY_STATUS_TEXT, null) ?: return null
        val startTimeMillis = prefs.getLong(KEY_START_TIME, -1L)
        val durationMinutes = prefs.getInt(KEY_DURATION, -1)

        if (startTimeMillis == -1L) return null

        return ActiveStatus(statusText, startTimeMillis, durationMinutes)
    }

    fun clearStatus() {
        prefs.edit()
            .remove(KEY_STATUS_TEXT)
            .remove(KEY_START_TIME)
            .remove(KEY_DURATION)
            .apply()
    }

    fun saveRecentStatus(status: RecentStatus) {
        val recents = getRecentStatuses().toMutableList()
        // Remove if exists to move to top
        recents.removeAll { it.text == status.text && it.durationMinutes == status.durationMinutes }
        recents.add(0, status)
        if (recents.size > 10) {
            recents.removeAt(recents.size - 1)
        }

        val jsonArray = org.json.JSONArray()
        recents.forEach {
            val obj = org.json.JSONObject()
            obj.put("text", it.text)
            obj.put("duration", it.durationMinutes)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_RECENT_STATUSES, jsonArray.toString()).apply()
    }

    fun getRecentStatuses(): List<RecentStatus> {
        val jsonString = prefs.getString(KEY_RECENT_STATUSES, "[]")
        val list = mutableListOf<RecentStatus>()
        try {
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(RecentStatus(obj.getString("text"), obj.getInt("duration")))
            }
        } catch (e: Exception) {
            // ignore
        }
        return list
    }

    fun removeRecentStatus(status: RecentStatus) {
        val recents = getRecentStatuses().toMutableList()
        recents.removeAll { it.text == status.text && it.durationMinutes == status.durationMinutes }
        saveRecentStatusesList(recents)
    }

    private fun saveRecentStatusesList(recents: List<RecentStatus>) {
        val jsonArray = org.json.JSONArray()
        recents.forEach {
            val obj = org.json.JSONObject()
            obj.put("text", it.text)
            obj.put("duration", it.durationMinutes)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_RECENT_STATUSES, jsonArray.toString()).apply()
    }

    fun getPresets(): List<PresetStatus> {
        val jsonString = prefs.getString(KEY_PRESETS, null)
        if (jsonString == null) {
            val defaultPresets = listOf(
                PresetStatus("Working", "doing some work"),
                PresetStatus("Cricket", "is playing"),
                PresetStatus("Busy", "is busy right now"),
                PresetStatus("Bath", "went for bath"),
                PresetStatus("Walking", "went for walk"),
                PresetStatus("Sleeping", "is sleeping")
            )
            savePresets(defaultPresets)
            return defaultPresets
        }
        val list = mutableListOf<PresetStatus>()
        try {
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(PresetStatus(obj.getString("heading"), obj.getString("content")))
            }
        } catch (e: Exception) {
            // Old format detected or corrupt data. Clear and return defaults.
            prefs.edit().remove(KEY_PRESETS).apply()
            return getPresets() // recursive call will now hit jsonString == null and generate defaults
        }
        return list
    }

    fun savePresets(presets: List<PresetStatus>) {
        val jsonArray = org.json.JSONArray()
        presets.forEach { 
            val obj = org.json.JSONObject()
            obj.put("heading", it.heading)
            obj.put("content", it.content)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PRESETS, jsonArray.toString()).apply()
    }

    fun addPreset(preset: PresetStatus) {
        val presets = getPresets().toMutableList()
        if (presets.none { it.heading == preset.heading }) {
            presets.add(preset)
            savePresets(presets)
        }
    }

    fun removePreset(preset: PresetStatus) {
        val presets = getPresets().toMutableList()
        presets.removeAll { it.heading == preset.heading && it.content == preset.content }
        savePresets(presets)
    }

    companion object {
        private const val PREFS_NAME = "sensy_status_prefs"
        private const val KEY_STATUS_TEXT = "status_text"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_DURATION = "duration_minutes"
        private const val KEY_RECENT_STATUSES = "recent_statuses"
        private const val KEY_PRESETS = "presets"
    }
}

data class RecentStatus(val text: String, val durationMinutes: Int)
data class PresetStatus(val heading: String, val content: String)
