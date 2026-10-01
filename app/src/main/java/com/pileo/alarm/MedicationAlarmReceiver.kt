package com.pileo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.pileo.data.PileoDatabase
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

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = PileoDatabase.getInstance(context).medicationDao()
                val med = dao.getById(medId)
                if (med != null && med.isActive) {
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
}
