package com.pileo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE isActive = 1")
    suspend fun getActiveOnce(): List<Medication>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): Medication?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(m: Medication): Long

    @Update
    suspend fun update(m: Medication)

    @Delete
    suspend fun delete(m: Medication)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteById(id: Long)
}
