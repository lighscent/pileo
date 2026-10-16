package com.pileo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.pileo.data.PileoDatabase
import com.pileo.data.SettingsPrefs
import com.pileo.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(AlarmScheduler.EXTRA_MED_ID, -1L)
        if (medId == -1L) return

        val name = intent.getStringExtra(AlarmScheduler.EXTRA_NAME) ?: "Médicament"
        val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_DOSAGE) ?: ""
        val timeLabel = intent.getStringExtra(AlarmScheduler.EXTRA_TIME_LABEL) ?: ""
        val nid = intent.getIntExtra(AlarmScheduler.EXTRA_NOTIFICATION_ID, medId.toInt())
        val chainId = intent.getLongExtra(AlarmScheduler.EXTRA_CHAIN_ID, 0L)
        val doseNumber = intent.getIntExtra(AlarmScheduler.EXTRA_DOSE_NUMBER, 1)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PileoDatabase.getInstance(context)
                val dao = db.medicationDao()
                if (chainId != 0L) {
                    ringChainedDose(context, db, medId, chainId, doseNumber, name, dosage, nid)
                    return@launch
                }
                val med = dao.getById(medId)
                if (med != null && med.isActive) {
                    val now = System.currentTimeMillis()
                    if (now < med.pausedUntil) {
                        // Occurrence mise en pause : on ne sonne pas, le cycle suivant reste programmé.
                        AlarmScheduler(context).schedule(med)
                        return@launch
                    }
                    // Nettoyage : une pause expirée ne doit plus s'afficher.
                    if (med.pausedUntil != 0L) {
                        dao.update(med.copy(pausedUntil = 0L))
                    }
                    if (SettingsPrefs.isSilentMode(context)) {
                        // Mode silencieux global : notification persistante, sans sonnerie ni écran d'alarme.
                        NotificationHelper.showSilentReminder(context, nid, medId, name, dosage, timeLabel)
                        AlarmScheduler(context).schedule(med)
                        return@launch
                    }
                    val serviceIntent = Intent(context, AlarmService::class.java).apply {
                        putExtra(AlarmService.EXTRA_MED_ID, medId)
                        putExtra(AlarmService.EXTRA_NAME, name)
                        putExtra(AlarmService.EXTRA_DOSAGE, dosage)
                        putExtra(AlarmService.EXTRA_TIME_LABEL, timeLabel)
                        putExtra(AlarmService.EXTRA_NOTIFICATION_ID, nid)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                    // Re-schedule this medication so the fired weekly slot moves to next week.
                    AlarmScheduler(context).schedule(med)
                }
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * Sonnerie d'une prise liée : pas de cycle hebdo à reprogrammer
     * (la suite de la chaîne avance à chaque validation).
     * La pause ne s'y applique pas (elle ne vise que la prochaine heure programmée),
     * mais un rappel désactivé annule la chaîne en attente.
     */
    private suspend fun ringChainedDose(
        context: Context,
        db: PileoDatabase,
        medId: Long,
        chainId: Long,
        doseNumber: Int,
        name: String,
        dosage: String,
        nid: Int
    ) {
        val chainDao = db.chainedDoseDao()
        val chain = chainDao.getById(chainId)
        if (chain == null || chain.takenAt != 0L) return
        val med = db.medicationDao().getById(medId)
        if (med == null || !med.isActive) {
            chainDao.deleteById(chainId)
            return
        }
        if (SettingsPrefs.isSilentMode(context)) {
            NotificationHelper.showSilentReminder(
                context, nid, medId, name, dosage, "",
                chainId = chainId, doseNumber = doseNumber
            )
            return
        }
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmService.EXTRA_MED_ID, medId)
            putExtra(AlarmService.EXTRA_NAME, name)
            putExtra(AlarmService.EXTRA_DOSAGE, dosage)
            putExtra(AlarmService.EXTRA_TIME_LABEL, "")
            putExtra(AlarmService.EXTRA_NOTIFICATION_ID, nid)
            putExtra(AlarmScheduler.EXTRA_CHAIN_ID, chainId)
            putExtra(AlarmScheduler.EXTRA_DOSE_NUMBER, doseNumber)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
