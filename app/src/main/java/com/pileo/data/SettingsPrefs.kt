package com.pileo.data

import android.content.Context

object SettingsPrefs {
    private const val FILE = "pileo_settings"
    private const val KEY_SNOOZE = "snooze_minutes"
    const val DEFAULT_SNOOZE_MINUTES = 15

    fun snoozeMinutes(ctx: Context): Int =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_SNOOZE, DEFAULT_SNOOZE_MINUTES)

    fun setSnoozeMinutes(ctx: Context, value: Int) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_SNOOZE, value).apply()
    }
}
