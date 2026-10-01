package com.pileo.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pileo.alarm.AlarmActivity
import com.pileo.alarm.AlarmScheduler
import com.pileo.alarm.AlarmActionReceiver
import com.pileo.alarm.AlarmService
import com.pileo.data.SettingsPrefs

object NotificationHelper {
    const val CHANNEL_ID = "pileo_alarms"
    const val ACTION_MARK_TAKEN = "com.pileo.ACTION_MARK_TAKEN"
    const val ACTION_SNOOZE = "com.pileo.ACTION_SNOOZE"

    /** Ancien canal créé par une version anglaise : immuable, on le remplace. */
    private const val LEGACY_CHANNEL_ID = "pileo_reminders"

    fun createChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Alarmes de médicaments",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alarmes pour prendre vos médicaments"
                    // Le son est joué par AlarmService sur la voie ALARME :
                    // le canal doit rester muet pour éviter un double son.
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(true)
                }
            )
            nm.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
    }

    fun buildAlarmNotification(
        ctx: Context,
        notificationId: Int,
        medId: Long,
        name: String,
        dosage: String,
        timeLabel: String
    ): Notification {
        createChannel(ctx)

        val fullIntent = Intent(ctx, AlarmActivity::class.java).apply {
            action = "com.pileo.ACTION_ALARM_UI"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(AlarmService.EXTRA_MED_ID, medId)
            putExtra(AlarmService.EXTRA_NAME, name)
            putExtra(AlarmService.EXTRA_DOSAGE, dosage)
            putExtra(AlarmService.EXTRA_TIME_LABEL, timeLabel)
            putExtra(AlarmService.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val fullPi = PendingIntent.getActivity(
            ctx, notificationId,
            fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeMinutes = SettingsPrefs.snoozeMinutes(ctx)

        val takenPi = actionPi(ctx, notificationId, medId, name, dosage, timeLabel, ACTION_MARK_TAKEN)
        val snoozePi = actionPi(ctx, notificationId, medId, name, dosage, timeLabel, ACTION_SNOOZE)

        val detail = buildString {
            append(name)
            if (dosage.isNotBlank()) append(" — ").append(dosage)
        }

        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("C'est l'heure de votre médicament")
            .setContentText(if (timeLabel.isNotBlank()) "$detail • $timeLabel" else detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$detail\nPrévu : $timeLabel"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullPi)
            .setFullScreenIntent(fullPi, true)
            .addAction(android.R.drawable.checkbox_on_background, "J'ai pris", takenPi)
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                "Rappeler dans $snoozeMinutes min",
                snoozePi
            )
            .build()
    }

    private fun actionPi(
        ctx: Context,
        notificationId: Int,
        medId: Long,
        name: String,
        dosage: String,
        timeLabel: String,
        action: String
    ): PendingIntent {
        val i = Intent(ctx, AlarmActionReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_MED_ID, medId)
            putExtra(AlarmScheduler.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AlarmScheduler.EXTRA_NAME, name)
            putExtra(AlarmScheduler.EXTRA_DOSAGE, dosage)
            putExtra(AlarmScheduler.EXTRA_TIME_LABEL, timeLabel)
        }
        val offset = if (action == ACTION_MARK_TAKEN) 1_000_000 else 2_000_000
        return PendingIntent.getBroadcast(
            ctx, notificationId + offset, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancel(ctx: Context, notificationId: Int) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)
    }
}
