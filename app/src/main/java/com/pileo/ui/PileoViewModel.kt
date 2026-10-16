package com.pileo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pileo.PileoApplication
import com.pileo.alarm.WatchdogReceiver
import com.pileo.alarm.rescheduleAllAlarms
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
            // Si les horaires/jours/prises liées ont changé, pause et chaînes en cours
            // (construites sur l'ancien planning) n'ont plus de sens.
            val toSave = if (med.id != 0L) {
                val existing = repo.getById(med.id)
                if (existing != null &&
                    (existing.timesMinutes != med.timesMinutes || existing.daysOfWeek != med.daysOfWeek ||
                        existing.chainDelaysMinutes != med.chainDelaysMinutes)
                ) {
                    clearPendingChains(med.id)
                    med.copy(pausedUntil = 0L)
                } else med
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
            if (active) scheduler.schedule(updated) else {
                clearPendingChains(updated.id)
                scheduler.cancel(updated.id)
            }
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
            clearPendingChains(med.id)
            repo.delete(med)
            scheduler.cancel(med.id)
        }
    }

    /**
     * Valide la prochaine prise AVANT l'heure prévue : l'occurrence programmée est
     * sautée (comme une pause) et la chaîne démarre maintenant (prise 2 = now + 1er délai).
     * Sans prises liées configurées, ce bouton n'est pas proposé.
     */
    fun validateEarly(med: Medication) {
        viewModelScope.launch {
            if (!med.isActive || med.chainDelaysMinutes.isEmpty()) return@launch
            val next = scheduler.nextUpcomingTrigger(med) ?: return@launch
            val now = System.currentTimeMillis()
            val firstDelay = med.chainDelaysMinutes.first()
            val chain = com.pileo.data.ChainedDose(
                medId = med.id,
                doseNumber = 2,
                scheduledAt = now + firstDelay * 60_000L,
                delayMinutes = firstDelay,
                name = med.name,
                dosage = med.dosage
            )
            val chainId = repo.insertChain(chain)
            val updated = med.copy(pausedUntil = next + 60_000L)
            repo.update(updated)
            scheduler.cancel(med.id)
            scheduler.schedule(updated)
            scheduler.scheduleChain(chain.copy(id = chainId))
        }
    }

    /** Supprime les prises liées en attente et annule leurs alarmes. */
    private suspend fun clearPendingChains(medId: Long) {
        val pending = repo.pendingChainsForMed(medId)
        pending.forEach { scheduler.cancelChain(it.id, it.doseNumber) }
        repo.deleteChainsForMed(medId)
    }

    /** Reprogramme tout depuis la base locale + arme le passage quotidien (ouverture de l'app…). */
    fun refreshAlarms() {
        viewModelScope.launch {
            rescheduleAllAlarms(getApplication())
            WatchdogReceiver.scheduleDaily(getApplication())
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
