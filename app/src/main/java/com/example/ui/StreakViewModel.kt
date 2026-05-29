package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.StreakRecord
import com.example.data.StreakRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class StreakViewModel(private val repository: StreakRepository) : ViewModel() {

    val streakRecord: StateFlow<StreakRecord?> = repository.record
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
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

    fun resetStreak() {
        viewModelScope.launch {
            val record = repository.getRecordOnce()
            val currentMillis = System.currentTimeMillis()
            var currentLongest = 0
            if (record != null) {
                val diff = currentMillis - record.streakStartDateMillis
                val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
                currentLongest = maxOf(record.longestStreakDays, days)
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
