package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AttemptHistory
import com.example.data.PreferencesManager
import com.example.data.StreakRecord
import com.example.data.StreakRepository
import com.example.data.UrgeLog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class ElapsedTime(val days: Int, val hours: Int, val minutes: Int, val seconds: Int)

@OptIn(ExperimentalCoroutinesApi::class)
class StreakViewModel(
    private val repository: StreakRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val streakRecord: StateFlow<StreakRecord?> = repository.record
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val history: StateFlow<List<AttemptHistory>> = repository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val elapsedTime: StateFlow<ElapsedTime> = combine(streakRecord, flow {
        while(true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }) { record, now ->
        if (record == null) ElapsedTime(0, 0, 0, 0)
        else {
            val diff = maxOf(0L, now - record.streakStartDateMillis)
            val d = TimeUnit.MILLISECONDS.toDays(diff).toInt()
            val h = TimeUnit.MILLISECONDS.toHours(diff).toInt() % 24
            val m = TimeUnit.MILLISECONDS.toMinutes(diff).toInt() % 60
            val s = TimeUnit.MILLISECONDS.toSeconds(diff).toInt() % 60
            ElapsedTime(d, h, m, s)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ElapsedTime(0, 0, 0, 0))

    val displayedRecord: StateFlow<Int> = combine(streakRecord, elapsedTime) { record, elapsed ->
        maxOf(record?.longestStreakDays ?: 0, elapsed.days)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun todayStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun weekStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todayUrgeCount: StateFlow<Int> = flow {
        while (true) {
            emit(todayStartMillis())
            delay(60_000)
        }
    }.distinctUntilChanged().flatMapLatest { startMillis ->
        repository.getUrgeCountSince(startMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weeklyUrgeCount: StateFlow<Int> = flow {
        while (true) {
            emit(weekStartMillis())
            delay(60_000)
        }
    }.distinctUntilChanged().flatMapLatest { startMillis ->
        repository.getUrgeCountSince(startMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val last7DaysUrges: StateFlow<List<Int>> = flow {
        while (true) {
            emit(weekStartMillis())
            delay(60_000)
        }
    }.distinctUntilChanged().flatMapLatest { weekStart ->
        repository.getUrgesSince(weekStart).map { urges ->
            val counts = IntArray(7) { 0 }
            val oneDayMillis = TimeUnit.DAYS.toMillis(1)
            for (urge in urges) {
                val diffMillis = urge.timestampMillis - weekStart
                if (diffMillis >= 0) {
                     val index = (diffMillis / oneDayMillis).toInt()
                     if (index in 0..6) {
                         counts[index]++
                     }
                }
            }
            counts.toList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { 0 })

    init {
        viewModelScope.launch {
            val record = repository.getRecordOnce()
            if (record == null) {
                repository.insertOrUpdate(
                    StreakRecord(
                        id = 1,
                        streakStartDateMillis = System.currentTimeMillis(),
                        longestStreakDays = 0
                    )
                )
            }
        }
    }

    val lastCelebratedMilestone = preferencesManager.lastCelebratedMilestone.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    fun setLastCelebratedMilestone(milestone: Int) {
        viewModelScope.launch {
            preferencesManager.setLastCelebratedMilestone(milestone)
        }
    }

    private var lastUrgeTap = 0L

    fun addUrge() {
        val now = System.currentTimeMillis()
        if (now - lastUrgeTap < 1000L) return
        lastUrgeTap = now
        viewModelScope.launch {
            repository.insertUrge(UrgeLog(timestampMillis = now))
        }
    }

    fun resetStreak() {
        viewModelScope.launch {
            val record = repository.getRecordOnce()
            val currentMillis = System.currentTimeMillis()
            var currentLongest = 0
            if (record != null) {
                val diff = currentMillis - record.streakStartDateMillis
                val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
                currentLongest = maxOf(record.longestStreakDays, days)
                
                if (days > 0) {
                    repository.insertAttempt(
                        AttemptHistory(
                            startMillis = record.streakStartDateMillis,
                            endMillis = currentMillis,
                            daysAchieved = days
                        )
                    )
                }
            }
            repository.insertOrUpdate(
                StreakRecord(
                    id = 1,
                    streakStartDateMillis = currentMillis,
                    longestStreakDays = currentLongest
                )
            )
        }
    }

    class Factory(
        private val repository: StreakRepository,
        private val preferencesManager: PreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StreakViewModel(repository, preferencesManager) as T
        }
    }
}
