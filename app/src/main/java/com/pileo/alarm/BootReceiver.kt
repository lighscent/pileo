package com.pileo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pileo.data.PileoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val a = intent.action
        if (a == Intent.ACTION_BOOT_COMPLETED ||
            a == Intent.ACTION_MY_PACKAGE_REPLACED ||
            a == Intent.ACTION_PACKAGE_REPLACED ||
            a == "android.intent.action.QUICKBOOT_POWERON" ||
            a == Intent.ACTION_TIME_CHANGED ||
            a == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = PileoDatabase.getInstance(context).medicationDao()
                    val active = dao.getActiveOnce()
                    AlarmScheduler(context).scheduleAll(active)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
