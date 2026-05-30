package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AttemptHistory
import com.example.data.StreakRecord
import com.example.data.StreakRepository
import com.example.data.UrgeLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.Calendar

import kotlinx.coroutines.flow.map

class StreakViewModel(private val repository: StreakRepository) : ViewModel() {

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

    private val cal = Calendar.getInstance()
    
    // Today start (midnight)
    private val todayStartMillis = run {
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    // Week start (7 days ago midnight)
    private val weekStartMillis = todayStartMillis - TimeUnit.DAYS.toMillis(7)

    val todayUrgeCount: StateFlow<Int> = repository.getUrgeCountSince(todayStartMillis)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val weeklyUrgeCount: StateFlow<Int> = repository.getUrgeCountSince(weekStartMillis)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val last7DaysUrges: StateFlow<List<Int>> = repository.getUrgesSince(weekStartMillis)
        .map { urges ->
            val counts = IntArray(7) { 0 }
            val oneDayMillis = TimeUnit.DAYS.toMillis(1)
            for (urge in urges) {
                // Determine which day index this falls in (0 to 6, where 6 is today)
                val diffMillis = urge.timestampMillis - weekStartMillis
                if (diffMillis >= 0) {
                     val index = (diffMillis / oneDayMillis).toInt()
                     if (index in 0..6) {
                         counts[index]++
                     }
                }
            }
            counts.toList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = List(7) { 0 }
        )

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

    fun addUrge() {
        viewModelScope.launch {
            repository.insertUrge(UrgeLog(timestampMillis = System.currentTimeMillis()))
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
                
                // Record attempt history
                repository.insertAttempt(
                    AttemptHistory(
                        startMillis = record.streakStartDateMillis,
                        endMillis = currentMillis,
                        daysAchieved = days
                    )
                )
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

    class Factory(private val repository: StreakRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StreakViewModel(repository) as T
        }
    }
}
