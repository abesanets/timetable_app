package com.example.schedule

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val LAST_GROUP_KEY = stringPreferencesKey("last_group")
    }
    
    val lastGroup: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_GROUP_KEY]
        }
    
    suspend fun saveLastGroup(group: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_GROUP_KEY] = group
        }
    }
}
