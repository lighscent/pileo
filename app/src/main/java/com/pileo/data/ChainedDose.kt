package com.pileo.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Une prise liée en attente : programmée X minutes après la validation
 * de la prise précédente (prise 1 = heure programmée, prises 2 à 5 = chaîne).
 * takenAt = 0 tant que la prise n'est pas validée.
 */
@Entity(tableName = "chained_doses")
data class ChainedDose(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medId: Long,
    /** 2..5 (1 = prise à heure programmée, jamais stockée ici). */
    val doseNumber: Int,
    /** Échéance en millis (System.currentTimeMillis). */
    val scheduledAt: Long,
    /** Délai en minutes après la validation précédente qui a produit cette prise. */
    val delayMinutes: Int,
    @ColumnInfo(defaultValue = "0") val takenAt: Long = 0L,
    val name: String = "",
    val dosage: String = ""
)

@Dao
interface ChainedDoseDao {
    @Insert
    suspend fun insert(c: ChainedDose): Long

    @Query("SELECT * FROM chained_doses WHERE id = :id")
    suspend fun getById(id: Long): ChainedDose?

    @Query("SELECT * FROM chained_doses WHERE medId = :medId AND takenAt = 0")
    suspend fun pendingForMed(medId: Long): List<ChainedDose>

    @Query("SELECT * FROM chained_doses WHERE takenAt = 0")
    suspend fun pendingAll(): List<ChainedDose>

    @Query("UPDATE chained_doses SET takenAt = :at WHERE id = :id")
    suspend fun markTaken(id: Long, at: Long)

    @Query("DELETE FROM chained_doses WHERE medId = :medId")
    suspend fun deleteForMed(medId: Long)

    @Query("DELETE FROM chained_doses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM chained_doses WHERE takenAt != 0 AND takenAt < :cutoff")
    suspend fun deleteTakenBefore(cutoff: Long)
}
