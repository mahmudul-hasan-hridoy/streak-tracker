package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [StreakRecord::class, AttemptHistory::class, UrgeLog::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun streakDao(): StreakDao
    abstract fun attemptDao(): AttemptDao
    abstract fun urgeDao(): UrgeDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `attempt_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startMillis` INTEGER NOT NULL, `endMillis` INTEGER NOT NULL, `daysAchieved` INTEGER NOT NULL)")
            }
        }
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `urge_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestampMillis` INTEGER NOT NULL)")
            }
        }
    }
}
