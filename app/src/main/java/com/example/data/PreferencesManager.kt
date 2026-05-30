package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    private val LAST_CELEBRATED_MILESTONE = intPreferencesKey("last_celebrated_milestone")

    val lastCelebratedMilestone: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LAST_CELEBRATED_MILESTONE] ?: 0
    }

    suspend fun setLastCelebratedMilestone(milestone: Int) {
        context.dataStore.edit { preferences ->
            preferences[LAST_CELEBRATED_MILESTONE] = milestone
        }
    }
}
