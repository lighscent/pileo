package com.pileo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
            // PACKAGE_REPLACED part pour chaque mise à jour d'app : ne réagir
            // qu'au remplacement de notre propre paquet.
            if (a == Intent.ACTION_PACKAGE_REPLACED &&
                intent.data?.schemeSpecificPart != context.packageName
            ) return
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Tout est reconstruit depuis la base locale (rappels + prises
                    // liées), puis le passage quotidien de sécurité est armé.
                    rescheduleAllAlarms(context)
                    WatchdogReceiver.scheduleDaily(context)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
