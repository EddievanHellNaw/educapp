package com.example.educapp.commons // Or your preferred package

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object UserPreferencesRepository {
    private const val USER_PREFERENCES_NAME = "user_preferences"
    private const val DATA_STORE_FILE_NAME = "user_prefs.pb"
    private const val ROLE_KEY = "role"

    private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
        name = USER_PREFERENCES_NAME
    )

    suspend fun saveRole(context: Context, role: UserRole) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[stringPreferencesKey(ROLE_KEY)] = role.name
        }
    }

    fun getRole(context: Context): Flow<UserRole> {
        return context.userPreferencesDataStore.data
            .map { preferences ->
                val roleName = preferences[stringPreferencesKey(ROLE_KEY)] ?: ""
                try {
                    UserRole.valueOf(roleName)
                } catch (e: IllegalArgumentException) {
                    UserRole.STUDENT // Default role if not found or invalid
                }
            }
    }

    // In UserPreferencesRepository
    fun saveUnverifiedRole(context: Context, role: UserRole) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("unverified_role", role.name).apply()
    }

    fun getUnverifiedRole(context: Context): UserRole? {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return prefs.getString("unverified_role", null)?.let { UserRole.valueOf(it) }
    }

    fun clearUnverifiedRole(context: Context) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("unverified_role").apply()
    }
}