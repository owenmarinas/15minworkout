package com.workout.timer

import android.content.Context
import android.content.SharedPreferences

/**
 * Thin wrapper around SharedPreferences for all workout settings.
 */
class Prefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)

    var reps: Int
        get() = prefs.getInt("reps", 12)
        set(v) = prefs.edit().putInt("reps", v).apply()

    var activeSeconds: Int
        get() = prefs.getInt("active_seconds", 30)
        set(v) = prefs.edit().putInt("active_seconds", v).apply()

    var restSeconds: Int
        get() = prefs.getInt("rest_seconds", 10)
        set(v) = prefs.edit().putInt("rest_seconds", v).apply()

    var announceStart: Boolean
        get() = prefs.getBoolean("announce_start", true)
        set(v) = prefs.edit().putBoolean("announce_start", v).apply()

    var announceRest: Boolean
        get() = prefs.getBoolean("announce_rest", true)
        set(v) = prefs.edit().putBoolean("announce_rest", v).apply()

    var announceMilestones: Boolean
        get() = prefs.getBoolean("announce_milestones", false)
        set(v) = prefs.edit().putBoolean("announce_milestones", v).apply()

    var warmupEnabled: Boolean
        get() = prefs.getBoolean("warmup_enabled", true)
        set(v) = prefs.edit().putBoolean("warmup_enabled", v).apply()

    var warmupSeconds: Int
        get() = prefs.getInt("warmup_seconds", 10)
        set(v) = prefs.edit().putInt("warmup_seconds", v).apply()

    /** Total workout duration in seconds */
    fun totalSeconds(): Int = (reps * activeSeconds) + ((reps - 1) * restSeconds)
}
