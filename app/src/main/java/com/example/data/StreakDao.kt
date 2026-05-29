package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak_record WHERE id = 1")
    fun getRecord(): Flow<StreakRecord?>

    @Query("SELECT * FROM streak_record WHERE id = 1")
    suspend fun getRecordOnce(): StreakRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: StreakRecord)
}
