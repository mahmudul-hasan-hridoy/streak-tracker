package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {
    @Query("SELECT * FROM attempt_history ORDER BY id DESC")
    fun getHistory(): Flow<List<AttemptHistory>>

    @Insert
    suspend fun insert(attempt: AttemptHistory)
}
