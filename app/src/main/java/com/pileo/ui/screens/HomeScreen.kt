package com.pileo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pileo.data.Medication
import com.pileo.ui.PileoViewModel

val DayLabels = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

fun formatTimes(times: List<Int>): String {
    if (times.isEmpty()) return "Aucune heure définie"
    return times.sorted().joinToString(" • ") { m ->
        String.format("%02d:%02d", m / 60, m % 60)
    }
}

fun formatDays(days: Set<Int>): String {
    if (days.size == 7) return "Quotidien"
    if (days.isEmpty()) return "Aucun jour"
    return days.sorted().joinToString(", ") { DayLabels[it - 1] }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: PileoViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit,
    showExactAlarmBanner: Boolean,
    showNotificationBanner: Boolean,
    showBatteryBanner: Boolean,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestBatteryExemption: () -> Unit
) {
    val meds by vm.medications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pileo") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Réglages")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un médicament")
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showNotificationBanner) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Les notifications sont désactivées — vous ne recevrez pas de rappels.",
                            style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onRequestNotifications) { Text("Activer les notifications") }
                    }
                }
            }
            if (showExactAlarmBanner) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Les alarmes exactes sont désactivées — les rappels peuvent être en retard.",
                            style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onRequestExactAlarm) { Text("Autoriser les alarmes exactes") }
                    }
                }
            }
            if (showBatteryBanner) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("L'optimisation de la batterie peut bloquer les rappels quand l'app est fermée ou après un redémarrage.",
                            style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onRequestBatteryExemption) { Text("Autoriser en arrière-plan") }
                    }
                }
            }

            if (meds.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Aucun médicament", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Appuyez sur + pour ajouter votre premier rappel.",
                        style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(meds, key = { it.id }) { med ->
                        MedicationRow(
                            med = med,
                            onClick = { onEdit(med.id) },
                            onToggle = { vm.toggleActive(med, it) },
                            onPauseNext = { vm.pauseNext(med) },
                            onResume = { vm.resume(med) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationRow(
    med: Medication,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onPauseNext: () -> Unit,
    onResume: () -> Unit
) {
    val paused = med.pausedUntil > System.currentTimeMillis()
    var showDisableDialog by remember(med.id) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(med.name, style = MaterialTheme.typography.titleMedium)
                if (med.dosage.isNotBlank()) {
                    Text(med.dosage, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(formatTimes(med.timesMinutes), style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatDays(med.daysOfWeek),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (med.silent) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            "Silencieux (notification)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (paused) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Prochaine sonnerie en pause (jusqu'à ${formatPauseUntil(med.pausedUntil)}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            // En pause : le slider passe en jaune avec une lune à gauche (position OFF).
            // Plus de bouton au-dessus : un appui dessus propose reprendre / tout désactiver.
            val baseSwitchColors = SwitchDefaults.colors()
            Switch(
                checked = med.isActive && !paused,
                onCheckedChange = { checked ->
                    if (!med.isActive) {
                        if (checked) onToggle(true)
                    } else if (paused) {
                        showDisableDialog = true
                    } else if (!checked) {
                        // Par défaut le switch met en pause au lieu de désactiver.
                        onPauseNext()
                    }
                },
                thumbContent = {
                    if (paused) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                            tint = Color(0xFF5C6B73)
                        )
                    }
                },
                colors = if (paused) {
                    baseSwitchColors.copy(
                        uncheckedTrackColor = Color(0xFFF2C14E),
                        uncheckedThumbColor = Color.White,
                        uncheckedBorderColor = Color.Transparent
                    )
                } else {
                    baseSwitchColors
                }
            )
        }
    }

    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            title = { Text("Rappel en pause") },
            text = {
                Text(
                    "« ${med.name} » ne sonnera pas à la prochaine heure, " +
                        "puis reprendra normalement. " +
                        "Vous pouvez aussi le désactiver complètement."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisableDialog = false
                        onResume()
                    }
                ) { Text("Reprendre") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDisableDialog = false }) {
                        Text("Garder la pause")
                    }
                    TextButton(
                        onClick = {
                            showDisableDialog = false
                            onToggle(false)
                        }
                    ) {
                        Text(
                            "Tout désactiver",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }
}

private fun formatPauseUntil(pausedUntil: Long): String {
    return try {
        val sdf = java.text.SimpleDateFormat("EEE HH:mm", java.util.Locale.FRENCH)
        sdf.format(java.util.Date(pausedUntil - 60_000L))
    } catch (_: Exception) {
        ""
    }
}
