package com.workout.timer

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class Prefs(private val context: Context) {

    private val master: SharedPreferences =
        context.getSharedPreferences("workout_master", Context.MODE_PRIVATE)

    // ── Profile list ─────────────────────────────────────────────────────────

    var profileNames: List<String>
        get() {
            val json = master.getString("profile_list", null)
                ?: return listOf("Default").also { profileNames = it }
            return try {
                val arr = JSONArray(json)
                List(arr.length()) { arr.getString(it) }.ifEmpty { listOf("Default") }
            } catch (e: Exception) { listOf("Default") }
        }
        private set(names) {
            val arr = JSONArray()
            names.forEach { arr.put(it) }
            master.edit().putString("profile_list", arr.toString()).apply()
        }

    var activeProfile: String
        get() = master.getString("active_profile", "Default") ?: "Default"
        set(v) { master.edit().putString("active_profile", v).apply() }

    private fun profilePrefs(name: String): SharedPreferences =
        context.getSharedPreferences("profile_$name", Context.MODE_PRIVATE)

    private val p: SharedPreferences get() = profilePrefs(activeProfile)

    // ── Settings (read/write on active profile) ───────────────────────────

    var reps: Int
        get() = p.getInt("reps", 12)
        set(v) { p.edit().putInt("reps", v).apply() }

    var activeSeconds: Int
        get() = p.getInt("active_seconds", 30)
        set(v) { p.edit().putInt("active_seconds", v).apply() }

    var restSeconds: Int
        get() = p.getInt("rest_seconds", 10)
        set(v) { p.edit().putInt("rest_seconds", v).apply() }

    var announceStart: Boolean
        get() = p.getBoolean("announce_start", true)
        set(v) { p.edit().putBoolean("announce_start", v).apply() }

    var announceRest: Boolean
        get() = p.getBoolean("announce_rest", true)
        set(v) { p.edit().putBoolean("announce_rest", v).apply() }

    var announceMilestones: Boolean
        get() = p.getBoolean("announce_milestones", false)
        set(v) { p.edit().putBoolean("announce_milestones", v).apply() }

    var warmupEnabled: Boolean
        get() = p.getBoolean("warmup_enabled", true)
        set(v) { p.edit().putBoolean("warmup_enabled", v).apply() }

    var warmupSeconds: Int
        get() = p.getInt("warmup_seconds", 10)
        set(v) { p.edit().putInt("warmup_seconds", v).apply() }

    fun totalSeconds(): Int = (reps * activeSeconds) + ((reps - 1) * restSeconds)

    // ── Profile management ────────────────────────────────────────────────

    fun createProfile(name: String) {
        val list = profileNames.toMutableList()
        if (!list.contains(name)) {
            list.add(name)
            profileNames = list
        }
    }

    fun renameProfile(oldName: String, newName: String) {
        if (oldName == newName || newName.isBlank()) return
        val oldP = profilePrefs(oldName)
        val newP = profilePrefs(newName)
        val editor = newP.edit()
        for ((key, value) in oldP.all) {
            when (value) {
                is Int     -> editor.putInt(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is String  -> editor.putString(key, value)
                is Float   -> editor.putFloat(key, value)
                is Long    -> editor.putLong(key, value)
            }
        }
        editor.apply()
        val list = profileNames.toMutableList()
        val idx = list.indexOf(oldName)
        if (idx >= 0) list[idx] = newName
        profileNames = list
        if (activeProfile == oldName) activeProfile = newName
        oldP.edit().clear().apply()
    }

    fun deleteProfile(name: String) {
        val list = profileNames.toMutableList()
        list.remove(name)
        if (list.isEmpty()) list.add("Default")
        profileNames = list
        if (activeProfile == name) activeProfile = list[0]
        profilePrefs(name).edit().clear().apply()
    }
}
