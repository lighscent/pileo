package com.pileo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pileo.data.SettingsPrefs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var snooze by remember { mutableStateOf(SettingsPrefs.snoozeMinutes(ctx)) }
    var silentMode by remember { mutableStateOf(SettingsPrefs.isSilentMode(ctx)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Rappel ultérieur", style = MaterialTheme.typography.titleMedium)
            Text(
                "Durée par défaut avant de re-déclencher l'alarme « Rappeler plus tard » : $snooze min.",
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (v in listOf(5, 10, 15, 20, 30, 60)) {
                    FilterChip(
                        selected = snooze == v,
                        onClick = {
                            snooze = v
                            SettingsPrefs.setSnoozeMinutes(ctx, v)
                        },
                        label = { Text("$v min") }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Text("Mode silencieux", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Notifications persistantes, sans son ni écran d'alarme.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = silentMode,
                    onCheckedChange = {
                        silentMode = it
                        SettingsPrefs.setSilentMode(ctx, it)
                    }
                )
            }
            Spacer(Modifier.height(8.dp))

            Text("Démarrage automatique", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pileo relance seul ses rappels après un redémarrage, sans ouvrir l'app. " +
                    "Sur certains téléphones (Xiaomi…), autorisez aussi le « Démarrage auto ».",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = { openAutoStart(ctx) }) {
                Text("Autoriser le démarrage auto")
            }
        }
    }
}

/** Ouvre si possible l'écran « Démarrage auto » du fabricant, sinon les réglages de l'app. */
private fun openAutoStart(ctx: android.content.Context) {
    val candidates = listOf(
        Intent("miui.intent.action.OP_AUTO_START"),
        Intent().apply {
            component = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        }
    )
    for (i in candidates) {
        try {
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) { }
    }
    try {
        ctx.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${ctx.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) { }
}
