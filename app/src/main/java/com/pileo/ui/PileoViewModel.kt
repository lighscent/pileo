package com.pileo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pileo.PileoApplication
import com.pileo.data.Medication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PileoViewModel(app: Application) : AndroidViewModel(app) {
    private val pileoApp = app as PileoApplication
    private val repo = pileoApp.repository
    private val scheduler = pileoApp.alarmScheduler

    val medications = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(med: Medication, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            // Si les horaires/jours ont changé, une pause posée sur l'ancien planning n'a plus de sens.
            val toSave = if (med.id != 0L) {
                val existing = repo.getById(med.id)
                if (existing != null &&
                    (existing.timesMinutes != med.timesMinutes || existing.daysOfWeek != med.daysOfWeek)
                ) med.copy(pausedUntil = 0L) else med
            } else med
            val id = repo.upsert(toSave)
            val saved = (if (toSave.id == 0L) toSave.copy(id = id) else toSave)
            // cancel d'abord : les anciens créneaux (heures/jours modifiés) ne doivent plus sonner.
            scheduler.cancel(saved.id)
            if (saved.isActive) scheduler.schedule(saved)
            onDone(id)
        }
    }

    fun toggleActive(med: Medication, active: Boolean) {
        viewModelScope.launch {
            // Désactiver efface aussi une éventuelle pause (sans objet une fois tout coupé).
            val updated = med.copy(isActive = active, pausedUntil = if (active) med.pausedUntil else 0L)
            repo.update(updated)
            if (active) scheduler.schedule(updated) else scheduler.cancel(updated.id)
        }
    }

    /** Met en pause la prochaine sonnerie uniquement : le cycle suivant reprend normalement. */
    fun pauseNext(med: Medication) {
        viewModelScope.launch {
            val next = scheduler.nextUpcomingTrigger(med) ?: return@launch
            // Marge de 60 s pour couvrir l'heure exacte même si elle sonne avec un léger retard.
            val updated = med.copy(pausedUntil = next + 60_000L)
            repo.update(updated)
            scheduler.cancel(med.id)
            scheduler.schedule(updated)
        }
    }

    /** Annule la pause : la prochaine sonnerie est reprogrammée. */
    fun resume(med: Medication) {
        viewModelScope.launch {
            if (med.pausedUntil == 0L) return@launch
            val updated = med.copy(pausedUntil = 0L)
            repo.update(updated)
            scheduler.cancel(med.id)
            scheduler.schedule(updated)
        }
    }

    fun delete(med: Medication) {
        viewModelScope.launch {
            repo.delete(med)
            scheduler.cancel(med.id)
        }
    }

    /** Reprogramme toutes les alarmes actives (ouverture de l'app, retour d'autorisation…). */
    fun refreshAlarms() {
        viewModelScope.launch {
            scheduler.scheduleAll(repo.getActiveOnce())
        }
    }

    suspend fun getById(id: Long): Medication? = repo.getById(id)
}

class PileoViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PileoViewModel(app) as T
    }
}
