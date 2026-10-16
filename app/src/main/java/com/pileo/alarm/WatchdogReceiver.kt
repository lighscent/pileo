package com.pileo.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pileo.data.PileoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reprogramme tout depuis la base locale : rappels hebdo actifs + prises liées
 * en attente, et purge les prises validées de plus de 7 jours.
 * La base Room survit aux coupures ; seules les alarmes système sont effacées,
 * d'où cette reconstruction (boot + passage quotidien de sécurité).
 */
suspend fun rescheduleAllAlarms(context: Context) {
    val db = PileoDatabase.getInstance(context)
    val scheduler = AlarmScheduler(context)
    scheduler.scheduleAll(db.medicationDao().getActiveOnce())
    val now = System.currentTimeMillis()
    db.chainedDoseDao().deleteTakenBefore(now - 7 * 24 * 60 * 60_000L)
    for (chain in db.chainedDoseDao().pendingAll()) {
        scheduler.scheduleChain(chain)
    }
}

/**
 * Passage quotidien de sécurité : si le système ou le fabricant a annulé des
 * alarmes (optimisations batterie agressives…), tout est reconstruit depuis
 * la base locale sans ouvrir l'app.
 */
class WatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAllAlarms(context)
                scheduleDaily(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 999_888

        fun scheduleDaily(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, WatchdogReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + AlarmManager.INTERVAL_DAY,
                AlarmManager.INTERVAL_DAY,
                pi
            )
        }
    }
}
