package com.pileo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Medication::class, ChainedDose::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PileoDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun chainedDoseDao(): ChainedDoseDao

    companion object {
        @Volatile private var INSTANCE: PileoDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE medications ADD COLUMN pausedUntil INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE medications ADD COLUMN silent INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Le silencieux est devenu un réglage global : la colonne par-rappel disparaît.
         * (DROP COLUMN indisponible sur le SQLite des anciennes API : recréation de table.)
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `medications_new` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `dosage` TEXT NOT NULL, `notes` TEXT NOT NULL, " +
                        "`timesMinutes` TEXT NOT NULL, `daysOfWeek` TEXT NOT NULL, " +
                        "`isActive` INTEGER NOT NULL, `pausedUntil` INTEGER NOT NULL DEFAULT 0, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO medications_new " +
                        "(id, name, dosage, notes, timesMinutes, daysOfWeek, isActive, pausedUntil, createdAt) " +
                        "SELECT id, name, dosage, notes, timesMinutes, daysOfWeek, isActive, pausedUntil, createdAt " +
                        "FROM medications"
                )
                db.execSQL("DROP TABLE medications")
                db.execSQL("ALTER TABLE medications_new RENAME TO medications")
            }
        }

        /**
         * Prises liées : nouvelle colonne + nouvelle table des prises en attente.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE medications ADD COLUMN chainDelaysMinutes TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chained_doses` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`medId` INTEGER NOT NULL, `doseNumber` INTEGER NOT NULL, " +
                        "`scheduledAt` INTEGER NOT NULL, `delayMinutes` INTEGER NOT NULL, " +
                        "`takenAt` INTEGER NOT NULL DEFAULT 0, " +
                        "`name` TEXT NOT NULL, `dosage` TEXT NOT NULL)"
                )
            }
        }

        fun getInstance(ctx: Context): PileoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    PileoDatabase::class.java,
                    "pileo.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { INSTANCE = it }
            }
    }
}
