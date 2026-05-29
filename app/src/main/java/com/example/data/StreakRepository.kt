package com.example.data

import kotlinx.coroutines.flow.Flow

class StreakRepository(private val streakDao: StreakDao) {
    val record: Flow<StreakRecord?> = streakDao.getRecord()

    suspend fun getRecordOnce(): StreakRecord? {
        return streakDao.getRecordOnce()
    }

    suspend fun insertOrUpdate(record: StreakRecord) {
        streakDao.insertOrUpdate(record)
    }
}
