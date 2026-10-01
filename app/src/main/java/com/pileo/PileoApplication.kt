package com.pileo

import android.app.Application
import com.pileo.data.PileoDatabase
import com.pileo.notification.NotificationHelper

class PileoApplication : Application() {
    val database by lazy { PileoDatabase.getInstance(this) }
    val repository by lazy { com.pileo.data.MedicationRepository(database.medicationDao()) }
    val alarmScheduler by lazy { com.pileo.alarm.AlarmScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
