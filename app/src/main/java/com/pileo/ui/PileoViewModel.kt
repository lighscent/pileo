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
            val id = repo.upsert(med)
            val saved = (if (med.id == 0L) med.copy(id = id) else med)
            if (saved.isActive) scheduler.schedule(saved) else scheduler.cancel(saved.id)
            onDone(id)
        }
    }

    fun toggleActive(med: Medication, active: Boolean) {
        viewModelScope.launch {
            val updated = med.copy(isActive = active)
            repo.update(updated)
            if (active) scheduler.schedule(updated) else scheduler.cancel(updated.id)
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
