package com.pileo.data

import kotlinx.coroutines.flow.Flow

class MedicationRepository(private val dao: MedicationDao) {
    fun observeAll(): Flow<List<Medication>> = dao.observeAll()
    suspend fun getActiveOnce(): List<Medication> = dao.getActiveOnce()
    suspend fun getById(id: Long): Medication? = dao.getById(id)
    suspend fun upsert(m: Medication): Long = dao.upsert(m)
    suspend fun update(m: Medication) = dao.update(m)
    suspend fun delete(m: Medication) = dao.delete(m)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
