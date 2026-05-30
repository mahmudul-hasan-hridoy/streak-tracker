package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "urge_log")
data class UrgeLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestampMillis: Long
)
