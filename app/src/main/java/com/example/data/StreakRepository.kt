package com.example.data

import kotlinx.coroutines.flow.Flow

class StreakRepository(
    private val streakDao: StreakDao,
    private val attemptDao: AttemptDao,
    private val urgeDao: UrgeDao
) {
    val record: Flow<StreakRecord?> = streakDao.getRecord()
    val history: Flow<List<AttemptHistory>> = attemptDao.getHistory()

    fun getUrgeCountSince(since: Long): Flow<Int> = urgeDao.getUrgeCountSince(since)
    
    fun getUrgesSince(since: Long): Flow<List<UrgeLog>> = urgeDao.getUrgesSince(since)

    suspend fun insertUrge(urge: UrgeLog) {
        urgeDao.insert(urge)
    }

    suspend fun getRecordOnce(): StreakRecord? {
        return streakDao.getRecordOnce()
    }

    suspend fun insertOrUpdate(record: StreakRecord) {
        streakDao.insertOrUpdate(record)
    }

    suspend fun insertAttempt(attempt: AttemptHistory) {
        attemptDao.insert(attempt)
    }
}
