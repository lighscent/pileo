package com.pileo.data

import kotlinx.coroutines.flow.Flow

class MedicationRepository(private val dao: MedicationDao, private val chainDao: ChainedDoseDao) {
    fun observeAll(): Flow<List<Medication>> = dao.observeAll()
    suspend fun getActiveOnce(): List<Medication> = dao.getActiveOnce()
    suspend fun getById(id: Long): Medication? = dao.getById(id)
    suspend fun upsert(m: Medication): Long = dao.upsert(m)
    suspend fun update(m: Medication) = dao.update(m)
    suspend fun delete(m: Medication) = dao.delete(m)
    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun insertChain(c: ChainedDose): Long = chainDao.insert(c)
    suspend fun getChainById(id: Long): ChainedDose? = chainDao.getById(id)
    suspend fun pendingChainsForMed(medId: Long): List<ChainedDose> = chainDao.pendingForMed(medId)
    suspend fun pendingChainsAll(): List<ChainedDose> = chainDao.pendingAll()
    suspend fun markChainTaken(id: Long, at: Long) = chainDao.markTaken(id, at)
    suspend fun deleteChainsForMed(medId: Long) = chainDao.deleteForMed(medId)
    suspend fun deleteChainById(id: Long) = chainDao.deleteById(id)
    suspend fun deleteTakenChainsBefore(cutoff: Long) = chainDao.deleteTakenBefore(cutoff)
}
