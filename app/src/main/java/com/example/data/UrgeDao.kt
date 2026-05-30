package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UrgeDao {
    @Query("SELECT COUNT(*) FROM urge_log WHERE timestampMillis >= :since")
    fun getUrgeCountSince(since: Long): Flow<Int>

    @Query("SELECT * FROM urge_log WHERE timestampMillis >= :since ORDER BY timestampMillis ASC")
    fun getUrgesSince(since: Long): Flow<List<UrgeLog>>

    @Insert
    suspend fun insert(urge: UrgeLog)
}
