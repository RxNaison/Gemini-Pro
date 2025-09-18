package com.rx.geminipro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val IS_MENU_LEFT = booleanPreferencesKey("is_menu_left")
    }

    val isMenuLeftFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_MENU_LEFT] ?: true
        }

    suspend fun saveMenuPosition(isLeft: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_MENU_LEFT] = isLeft
        }
    }
}