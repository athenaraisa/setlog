package com.example.quicksnapdiary.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val clipDurationSec: Int = 3,
    val keepAudio: Boolean = true,
    val resolutionP: Int = 1080
)

class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val flow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            clipDurationSec = preferences[DURATION_KEY] ?: 3,
            keepAudio = preferences[KEEP_AUDIO_KEY] ?: true,
            resolutionP = preferences[RESOLUTION_KEY] ?: 1080
        )
    }

    suspend fun setDuration(seconds: Int) {
        dataStore.edit { it[DURATION_KEY] = seconds }
    }

    suspend fun setAudio(enabled: Boolean) {
        dataStore.edit { it[KEEP_AUDIO_KEY] = enabled }
    }

    suspend fun setResolution(resolutionP: Int) {
        dataStore.edit { it[RESOLUTION_KEY] = resolutionP }
    }

    private companion object {
        val DURATION_KEY = intPreferencesKey("clip_duration_sec")
        val KEEP_AUDIO_KEY = booleanPreferencesKey("keep_audio")
        val RESOLUTION_KEY = intPreferencesKey("resolution_p")
    }
}
