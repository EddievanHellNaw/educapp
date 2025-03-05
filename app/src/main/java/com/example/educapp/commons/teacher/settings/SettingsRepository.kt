package com.example.educapp.commons.teacher.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.educapp.commons.ui.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")


class SettingsRepository(context: Context) {

    // Use the extension property defined above
    private val dataStore = context.dataStore

    val defaultTheme: AppTheme = runBlocking {
        dataStore.data.first()[PreferencesKeys.THEME]?.let { AppTheme.valueOf(it) } ?: AppTheme.Ghost
    }
    object PreferencesKeys {
        val THEME = stringPreferencesKey("theme_key")
        val TIMER = intPreferencesKey("timer_key")
    }

    // Provide flows from DataStore
    val themeFlow: Flow<AppTheme> = dataStore.data
        .map { prefs ->
            prefs[PreferencesKeys.THEME]?.let { AppTheme.valueOf(it) } ?: AppTheme.Ghost        }
        .distinctUntilChanged()

    val timerDurationFlow: Flow<Int> = dataStore.data
        .map { prefs -> prefs[PreferencesKeys.TIMER] ?: 60 }
        .distinctUntilChanged()

    suspend fun saveTheme(newTheme: AppTheme) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME] = newTheme.name
        }
    }

    suspend fun saveTimerDuration(seconds: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.TIMER] = seconds
        }
    }
}
