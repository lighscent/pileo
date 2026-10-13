package com.pileo.data

import android.content.Context

object SettingsPrefs {
    private const val FILE = "pileo_settings"
    private const val KEY_SNOOZE = "snooze_minutes"
    const val DEFAULT_SNOOZE_MINUTES = 15
    private const val KEY_SILENT_MODE = "silent_mode"

    fun snoozeMinutes(ctx: Context): Int =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_SNOOZE, DEFAULT_SNOOZE_MINUTES)

    fun setSnoozeMinutes(ctx: Context, value: Int) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_SNOOZE, value).apply()
    }

    /** Mode silencieux global : notifications persistantes, sans son ni écran d'alarme. */
    fun isSilentMode(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_SILENT_MODE, false)

    fun setSilentMode(ctx: Context, value: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SILENT_MODE, value).apply()
    }
}
