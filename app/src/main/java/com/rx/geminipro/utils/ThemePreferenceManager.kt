package com.rx.geminipro.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemePreferenceManager(private val context: Context) {
    private val THEME_KEY = stringPreferencesKey("theme_preference")

    suspend fun saveThemePreference(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = if (isDarkMode) "dark" else "light"
        }
    }

    val themePreferenceFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            when (preferences[THEME_KEY] ?: "light") {
                "dark" -> true
                "light" -> false
                else -> false
            }
        }
}
