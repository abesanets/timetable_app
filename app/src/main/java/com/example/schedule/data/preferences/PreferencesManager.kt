package com.example.schedule.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.schedule.data.models.Schedule
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    private val gson = Gson()
    
    companion object {
        private val LAST_GROUP_KEY = stringPreferencesKey("last_group")
        private val LAST_SCHEDULE_KEY = stringPreferencesKey("last_schedule")
        private val SELECTED_SUBGROUP_KEY = androidx.datastore.preferences.core.intPreferencesKey("selected_subgroup")
        private val WIDGET_UPDATE_INTERVAL_KEY = androidx.datastore.preferences.core.longPreferencesKey("widget_update_interval")
    }
    
    val lastGroup: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_GROUP_KEY]
        }

    val lastSchedule: Flow<Schedule?> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_SCHEDULE_KEY]?.let { json ->
                try {
                    gson.fromJson(json, Schedule::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }

    val selectedSubgroup: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_SUBGROUP_KEY] ?: 0
        }
    
    val widgetUpdateInterval: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[WIDGET_UPDATE_INTERVAL_KEY] ?: 60L
        }
    
    suspend fun saveLastGroup(group: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_GROUP_KEY] = group
        }
    }

    suspend fun saveSchedule(schedule: Schedule) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SCHEDULE_KEY] = gson.toJson(schedule)
        }
    }

    suspend fun setSelectedSubgroup(subgroup: Int) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_SUBGROUP_KEY] = subgroup
        }
    }
    
    suspend fun setWidgetUpdateInterval(interval: Long) {
        context.dataStore.edit { preferences ->
            preferences[WIDGET_UPDATE_INTERVAL_KEY] = interval
        }
    }
}