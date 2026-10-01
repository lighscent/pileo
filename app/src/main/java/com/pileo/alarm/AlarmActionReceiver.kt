package com.pileo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pileo.data.SettingsPrefs
import com.pileo.notification.NotificationHelper

/** Handles the "Pris" and "Rappeler plus tard" actions from the alarm notification. */
class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(AlarmScheduler.EXTRA_MED_ID, -1L)
        val nid = intent.getIntExtra(AlarmScheduler.EXTRA_NOTIFICATION_ID, 0)
        val name = intent.getStringExtra(AlarmScheduler.EXTRA_NAME) ?: "Médicament"
        val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_DOSAGE) ?: ""
        val timeLabel = intent.getStringExtra(AlarmScheduler.EXTRA_TIME_LABEL) ?: ""

        stopAlarm(context)
        if (nid != 0) NotificationHelper.cancel(context, nid)

        when (intent.action) {
            NotificationHelper.ACTION_MARK_TAKEN -> { /* nothing else to do */ }
            NotificationHelper.ACTION_SNOOZE -> {
                if (medId == -1L) return
                val delay = SettingsPrefs.snoozeMinutes(context)
                AlarmScheduler(context).scheduleSnooze(
                    medId, name, dosage, timeLabel, delay, if (nid != 0) nid else medId.toInt()
                )
            }
        }
    }

    private fun stopAlarm(context: Context) {
        try {
            context.startService(
                Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_STOP
                }
            )
        } catch (_: Exception) { }
        // Ferme aussi l'écran d'alarme s'il est affiché.
        context.sendBroadcast(
            Intent(AlarmActivity.ACTION_DISMISS_UI).setPackage(context.packageName)
        )
    }
}
