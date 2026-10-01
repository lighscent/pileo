package com.pileo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Medication::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PileoDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile private var INSTANCE: PileoDatabase? = null

        fun getInstance(ctx: Context): PileoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    PileoDatabase::class.java,
                    "pileo.db"
                ).build().also { INSTANCE = it }
            }
    }
}
