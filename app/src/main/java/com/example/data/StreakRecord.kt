package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak_record")
data class StreakRecord(
    @PrimaryKey val id: Int = 1,
    val streakStartDateMillis: Long = System.currentTimeMillis(),
    val longestStreakDays: Int = 0
)
