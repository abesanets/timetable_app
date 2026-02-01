package com.example.schedule.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val LAST_GROUP_KEY = stringPreferencesKey("last_group")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val WIDGET_UPDATE_INTERVAL_KEY = androidx.datastore.preferences.core.longPreferencesKey("widget_update_interval")
        private val HOME_ADDRESS_KEY = stringPreferencesKey("home_address")
        private val DEPARTURE_LOCATION_KEY = stringPreferencesKey("departure_location")
    }
    
    val lastGroup: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_GROUP_KEY]
        }
    
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] ?: false
        }
    
    /**
     * Интервал обновления виджета в минутах. 0 - отключено.
     */
    val widgetUpdateInterval: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[WIDGET_UPDATE_INTERVAL_KEY] ?: 60L // Default 60 minutes
        }

    val homeAddress: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[HOME_ADDRESS_KEY] ?: ""
        }

    val departureLocation: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DEPARTURE_LOCATION_KEY] ?: "Казинца 91"
        }
    
    suspend fun saveLastGroup(group: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_GROUP_KEY] = group
        }
    }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setWidgetUpdateInterval(interval: Long) {
        context.dataStore.edit { preferences ->
            preferences[WIDGET_UPDATE_INTERVAL_KEY] = interval
        }
    }

    suspend fun setHomeAddress(address: String) {
        context.dataStore.edit { preferences ->
            preferences[HOME_ADDRESS_KEY] = address
        }
    }

    suspend fun setDepartureLocation(location: String) {
        context.dataStore.edit { preferences ->
            preferences[DEPARTURE_LOCATION_KEY] = location
        }
    }
}