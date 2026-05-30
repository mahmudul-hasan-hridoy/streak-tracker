package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.StreakRepository
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.work.ReminderWorker
import java.util.concurrent.TimeUnit
import com.example.widget.updateStreakWidget
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.DelicateCoroutinesApi

class ZeroApplication : Application() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        try {
            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            // WorkManager not initialized in tests
        }

        GlobalScope.launch {
            repository.record.collectLatest {
                updateStreakWidget(this@ZeroApplication)
            }
        }
    }

    val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "streak-database"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .build()
    }

    val repository by lazy {
        StreakRepository(database.streakDao(), database.attemptDao(), database.urgeDao())
    }

    val preferencesManager by lazy {
        PreferencesManager(this)
    }
}
