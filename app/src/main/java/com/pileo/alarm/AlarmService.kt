package com.pileo.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import com.pileo.notification.NotificationHelper

/**
 * Foreground service that plays the device alarm ringtone on the ALARM stream,
 * loops it and vibrates until the user takes the pill or snoozes.
 */
class AlarmService : Service() {

    companion object {
        const val EXTRA_MED_ID = "medId"
        const val EXTRA_NAME = "name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_TIME_LABEL = "timeLabel"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val ACTION_STOP = "com.pileo.ACTION_STOP_ALARM"
    }

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val i = intent ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val medId = i.getLongExtra(EXTRA_MED_ID, -1L)
        if (medId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }
        val name = i.getStringExtra(EXTRA_NAME) ?: "Médicament"
        val dosage = i.getStringExtra(EXTRA_DOSAGE) ?: ""
        val timeLabel = i.getStringExtra(EXTRA_TIME_LABEL) ?: ""
        val nid = i.getIntExtra(EXTRA_NOTIFICATION_ID, medId.toInt())

        val notif = NotificationHelper.buildAlarmNotification(
            this, nid, medId, name, dosage, timeLabel
        )
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else 0
        ServiceCompat.startForeground(this, nid, notif, type)

        startAlarmSound()
        startVibration()
        return START_STICKY
    }

    private fun startAlarmSound() {
        if (player != null) return
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } catch (_: Exception) {
            player = null
        }
    }

    private fun startVibration() {
        if (vibrator != null) return
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 800, 600)
        try {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } catch (_: Exception) { }
    }

    private fun stopAlarmFeedback() {
        player?.let {
            try { it.stop() } catch (_: Exception) { }
            it.release()
        }
        player = null
        try { vibrator?.cancel() } catch (_: Exception) { }
        vibrator = null
    }

    override fun onDestroy() {
        stopAlarmFeedback()
        super.onDestroy()
    }
}
