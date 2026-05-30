package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attempt_history")
data class AttemptHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startMillis: Long,
    val endMillis: Long,
    val daysAchieved: Int
)
