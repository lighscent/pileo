package com.pileo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pileo.data.ChainedDose
import com.pileo.data.PileoDatabase
import com.pileo.data.SettingsPrefs
import com.pileo.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the "Pris" and "Rappeler plus tard" actions from the alarm notification.
 * Validating a dose starts the next linked dose (X min after validation, max 4 linked).
 */
class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(AlarmScheduler.EXTRA_MED_ID, -1L)
        val nid = intent.getIntExtra(AlarmScheduler.EXTRA_NOTIFICATION_ID, 0)
        val name = intent.getStringExtra(AlarmScheduler.EXTRA_NAME) ?: "Médicament"
        val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_DOSAGE) ?: ""
        val timeLabel = intent.getStringExtra(AlarmScheduler.EXTRA_TIME_LABEL) ?: ""
        val chainId = intent.getLongExtra(AlarmScheduler.EXTRA_CHAIN_ID, 0L)
        val doseNumber = intent.getIntExtra(AlarmScheduler.EXTRA_DOSE_NUMBER, 1)

        stopAlarm(context)
        if (nid != 0) NotificationHelper.cancel(context, nid)

        when (intent.action) {
            NotificationHelper.ACTION_MARK_TAKEN -> {
                if (medId == -1L) return
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        onDoseTaken(context, medId, chainId)
                    } finally {
                        pending.finish()
                    }
                }
            }
            NotificationHelper.ACTION_SNOOZE -> {
                if (medId == -1L) return
                val delay = SettingsPrefs.snoozeMinutes(context)
                AlarmScheduler(context).scheduleSnooze(
                    medId, name, dosage, timeLabel, delay, if (nid != 0) nid else medId.toInt(),
                    chainId = chainId, doseNumber = doseNumber
                )
            }
        }
    }

    /**
     * Une prise validée (à l'heure, en avance ou reportée) déclenche la prise liée
     * suivante : delays[dose-1] minutes après maintenant (la prise 1 utilise delays[0]).
     */
    private suspend fun onDoseTaken(context: Context, medId: Long, chainId: Long) {
        val db = PileoDatabase.getInstance(context)
        val now = System.currentTimeMillis()
        val scheduler = AlarmScheduler(context)
        val doseNumber: Int
        if (chainId != 0L) {
            val chain = db.chainedDoseDao().getById(chainId) ?: return
            if (chain.takenAt != 0L) return
            db.chainedDoseDao().markTaken(chainId, now)
            doseNumber = chain.doseNumber
        } else {
            doseNumber = 1
        }
        val med = db.medicationDao().getById(medId)
        if (med == null || !med.isActive) return
        val nextDelay = med.chainDelaysMinutes.getOrNull(doseNumber - 1) ?: return
        val next = ChainedDose(
            medId = medId,
            doseNumber = doseNumber + 1,
            scheduledAt = now + nextDelay * 60_000L,
            delayMinutes = nextDelay,
            name = med.name,
            dosage = med.dosage
        )
        val id = db.chainedDoseDao().insert(next)
        scheduler.scheduleChain(next.copy(id = id))
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
