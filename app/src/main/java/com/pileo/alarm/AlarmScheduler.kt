package com.pileo.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pileo.MainActivity
import com.pileo.data.Medication
import java.util.Calendar

class AlarmScheduler(private val ctx: Context) {

    companion object {
        const val ACTION_REMIND = "com.pileo.ACTION_REMIND"
        const val EXTRA_MED_ID = "medId"
        const val EXTRA_NAME = "medName"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_TIME_LABEL = "timeLabel"
        const val EXTRA_NOTIFICATION_ID = "notificationId"

        fun requestCode(medId: Long, day: Int, timeIndex: Int): Int {
            return ((medId % 100000L) * 1000L + day * 20L + timeIndex).toInt()
        }

        fun notificationId(medId: Long, day: Int, timeIndex: Int): Int {
            return ((medId % 100000L) * 1000L + 500000L + day * 20L + timeIndex).toInt()
        }

        fun formatTime(minutes: Int): String {
            val h = minutes / 60
            val m = minutes % 60
            return String.format("%02d:%02d", h, m)
        }
    }

    private fun alarmManager(): AlarmManager =
        ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(med: Medication) {
        if (!med.isActive || med.timesMinutes.isEmpty() || med.daysOfWeek.isEmpty()) {
            cancel(med.id)
            return
        }
        val am = alarmManager()
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true

        med.timesMinutes.sorted().forEachIndexed { timeIndex, minutes ->
            med.daysOfWeek.forEach { day ->
                val triggerAt = nextTriggerMillis(day, minutes)
                val nid = notificationId(med.id, day, timeIndex)
                val intent = Intent(ctx, MedicationAlarmReceiver::class.java).apply {
                    action = ACTION_REMIND
                    putExtra(EXTRA_MED_ID, med.id)
                    putExtra(EXTRA_NAME, med.name)
                    putExtra(EXTRA_DOSAGE, med.dosage)
                    putExtra(EXTRA_TIME_LABEL, formatTime(minutes))
                    putExtra(EXTRA_NOTIFICATION_ID, nid)
                }
                val pi = PendingIntent.getBroadcast(
                    ctx,
                    requestCode(med.id, day, timeIndex),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    if (canExact) {
                        am.setAlarmClock(alarmClockInfo(triggerAt), pi)
                    } else {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    }
                } catch (_: SecurityException) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            }
        }
    }

    fun cancel(medId: Long) {
        val am = alarmManager()
        // Cancel all weekday/time slots (up to 12 times per day, 7 days)
        for (day in 1..7) {
            for (idx in 0 until 12) {
                val intent = Intent(ctx, MedicationAlarmReceiver::class.java).apply {
                    action = ACTION_REMIND
                }
                val pi = PendingIntent.getBroadcast(
                    ctx,
                    requestCode(medId, day, idx),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.cancel(pi)
                pi.cancel()
            }
        }
    }

    fun scheduleAll(meds: List<Medication>) {
        meds.forEach { schedule(it) }
    }

    /** One-shot alarm fired `delayMinutes` from now, used by "Rappeler plus tard". */
    fun scheduleSnooze(
        medId: Long,
        name: String,
        dosage: String,
        timeLabel: String,
        delayMinutes: Int,
        notificationId: Int
    ) {
        val fireAt = System.currentTimeMillis() + delayMinutes * 60_000L
        val intent = Intent(ctx, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_MED_ID, medId)
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_TIME_LABEL, if (timeLabel.isBlank()) "reporté +$delayMinutes min" else "$timeLabel • reporté +$delayMinutes min")
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pi = PendingIntent.getBroadcast(
            ctx,
            notificationId + 777,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = alarmManager()
        try {
            am.setAlarmClock(alarmClockInfo(fireAt), pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        }
    }

    /** Next wall-clock trigger for a given weekday (1=Mon..7=Sun) + minutes-of-day. Weekly cadence. */
    private fun nextTriggerMillis(dayOfWeek1to7: Int, minutes: Int): Long {
        val calDay = when (dayOfWeek1to7) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            else -> Calendar.SUNDAY
        }
        val now = Calendar.getInstance()
        val t = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, calDay)
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Calendar week starts Sunday; ensure we land in the future.
        // Only a slot already past is pushed a week; a slot a few seconds ahead must still
        // fire today (sinon un rappel réglé juste avant son heure était reporté d'une semaine).
        var guard = 0
        while (t.timeInMillis <= now.timeInMillis && guard < 3) {
            t.add(Calendar.DAY_OF_YEAR, 7)
            guard++
        }
        while (t.timeInMillis <= now.timeInMillis) {
            t.add(Calendar.DAY_OF_YEAR, 7)
        }
        // Créneau imminent : le garder aujourd'hui avec une petite marge de sécurité.
        if (t.timeInMillis - now.timeInMillis < 5_000) {
            return now.timeInMillis + 5_000
        }
        return t.timeInMillis
    }

    /** Le système traite ces alarmes comme de vraies alarmes (icône, réveil, hors Doze). */
    private fun alarmClockInfo(triggerAt: Long): AlarmManager.AlarmClockInfo =
        AlarmManager.AlarmClockInfo(
            triggerAt,
            PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
}
