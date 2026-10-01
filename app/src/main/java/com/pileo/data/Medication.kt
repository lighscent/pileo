package com.pileo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String = "",
    val notes: String = "",
    /** minutes from midnight, e.g. 480 = 08:00. Supports multiple times per day. */
    val timesMinutes: List<Int> = emptyList(),
    /** java.time.DayOfWeek values 1 (Mon) .. 7 (Sun). Empty = never; UI defaults to all 7. */
    val daysOfWeek: Set<Int> = (1..7).toSet(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter
    fun fromIntList(v: List<Int>?): String =
        v?.joinToString(",") ?: ""

    @TypeConverter
    fun toIntList(v: String?): List<Int> {
        if (v.isNullOrBlank()) return emptyList()
        return v.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
    }

    @TypeConverter
    fun fromIntSet(v: Set<Int>?): String =
        v?.joinToString(",") ?: ""

    @TypeConverter
    fun toIntSet(v: String?): Set<Int> {
        if (v.isNullOrBlank()) return emptySet()
        return v.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.toSet()
    }
}
