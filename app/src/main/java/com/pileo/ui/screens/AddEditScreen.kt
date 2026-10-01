package com.pileo.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pileo.data.Medication
import com.pileo.ui.PileoViewModel
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditScreen(
    vm: PileoViewModel,
    medId: Long?,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(medId == null || medId == 0L) }
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var dayPreset by remember { mutableStateOf("Quotidien") }
    var confirmDelete by remember { mutableStateOf(false) }
    val times = remember { mutableStateListOf<Int>() }
    val days = remember { mutableStateListOf<Int>() }

    fun presetFor(d: List<Int>): String {
        val s = d.toSet()
        return when {
            s.size == 7 -> "Quotidien"
            s == setOf(1, 2, 3, 4, 5) -> "Semaine"
            s == setOf(6, 7) -> "Week-end"
            else -> "Personnalisé"
        }
    }

    fun selectPreset(p: String) {
        dayPreset = p
        when (p) {
            "Quotidien" -> { days.clear(); days.addAll((1..7).toList()) }
            "Semaine" -> { days.clear(); days.addAll(listOf(1, 2, 3, 4, 5)) }
            "Week-end" -> { days.clear(); days.addAll(listOf(6, 7)) }
            else -> if (days.isEmpty()) days.addAll((1..7).toList())
        }
    }

    LaunchedEffect(medId) {
        if (medId != null && medId != 0L && !loaded) {
            val m = vm.getById(medId)
            if (m != null) {
                name = m.name
                dosage = m.dosage
                notes = m.notes
                isActive = m.isActive
                times.clear(); times.addAll(m.timesMinutes.sorted())
                days.clear(); days.addAll(m.daysOfWeek)
            } else {
                days.clear(); days.addAll((1..7).toList())
            }
            dayPreset = presetFor(days)
            loaded = true
        } else if (medId == null || medId == 0L) {
            if (days.isEmpty()) days.addAll((1..7).toList())
            dayPreset = presetFor(days)
            loaded = true
        }
    }

    // Default to daily when creating
    LaunchedEffect(loaded) {
        if (loaded && (medId == null || medId == 0L) && days.isEmpty()) {
            days.addAll((1..7).toList())
            dayPreset = presetFor(days)
        }
    }

    val isEditing = medId != null && medId != 0L
    val valid = name.isNotBlank() && times.isNotEmpty() && days.isNotEmpty()

    fun showTimePicker(initial: Int? = null) {
        val cal = Calendar.getInstance()
        val h = initial?.div(60) ?: cal.get(Calendar.HOUR_OF_DAY)
        val m = initial?.rem(60) ?: cal.get(Calendar.MINUTE)
        TimePickerDialog(ctx, { _, hour, minute ->
            val v = hour * 60 + minute
            if (initial != null && initial != v) times.remove(initial)
            if (!times.contains(v)) {
                times.add(v)
                times.sort()
            }
        }, h, m, true).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Modifier le médicament" else "Nouveau médicament") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { pad ->
        if (!loaded) {
            Column(
                Modifier.fillMaxSize().padding(pad),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { Text("Chargement…") }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(pad)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nom du médicament *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = dosage, onValueChange = { dosage = it },
                label = { Text("Dosage (ex. 500mg, 1 comprimé)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes (facultatif)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2
            )

            Text("Heures de prise *")
            if (times.isEmpty()) {
                Text("Ajoutez au moins une heure (ex. 08:00).")
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                times.sorted().forEach { t ->
                    TimeChip(
                        time = t,
                        onEdit = { showTimePicker(t) },
                        onRemove = { times.remove(t) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showTimePicker() }) { Text("Ajouter une heure") }
            }

            Text("Jours de prise *")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (preset in listOf("Quotidien", "Semaine", "Week-end", "Personnalisé")) {
                    FilterChip(
                        selected = dayPreset == preset,
                        onClick = { selectPreset(preset) },
                        label = { Text(preset) }
                    )
                }
            }
            if (dayPreset == "Personnalisé") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (d in 1..7) {
                        FilterChip(
                            selected = days.contains(d),
                            onClick = {
                                if (days.contains(d)) {
                                    if (days.size > 1) days.remove(d)
                                } else days.add(d)
                            },
                            label = { Text(DayLabels[d - 1]) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF26C6DA),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Rappel activé")
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        val med = Medication(
                            id = medId ?: 0L,
                            name = name.trim(),
                            dosage = dosage.trim(),
                            notes = notes.trim(),
                            timesMinutes = times.sorted(),
                            daysOfWeek = days.toSet(),
                            isActive = isActive
                        )
                        vm.save(med) { onDone() }
                    }
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E9E6B),
                    contentColor = Color.White
                )
            ) { Text("Enregistrer le rappel") }

            if (isEditing) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFC62828)
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text("  Supprimer ce rappel")
                }
            }

            if (!valid) {
                Text("Saisissez un nom, au moins une heure et un jour pour enregistrer.")
            }
        }
    }

    if (confirmDelete) {
        val label = name.ifBlank { "ce rappel" }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer le rappel ?") },
            text = { Text("Supprimer « $label » ? Ses alarmes seront annulées.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        scope.launch {
                            val m = vm.getById(medId!!)
                            if (m != null) vm.delete(m)
                            onDone()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFC62828)
                    )
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun TimeChip(
    time: Int,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(onClick = onEdit)
                .padding(start = 18.dp, end = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                String.format("%02d:%02d", time / 60, time % 60),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(onClick = onRemove)
                .padding(end = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Retirer",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
