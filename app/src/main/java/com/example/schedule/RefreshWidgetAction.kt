package com.example.schedule

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.updateAll

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // 1. Устанавливаем состояние загрузки
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[booleanPreferencesKey("is_loading")] = true
            prefs.remove(stringPreferencesKey("error_message"))
        }
        ScheduleWidget().update(context, glanceId)

        // 2. Загружаем данные
        try {
            // Используем ту же функцию загрузки, что и виджет
            val widgetData = loadWidgetData(context)
            
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[booleanPreferencesKey("is_loading")] = false
                
                if (widgetData.error != null) {
                    prefs[stringPreferencesKey("error_message")] = widgetData.error
                } else if (widgetData.daySchedule != null) {
                    val json = WidgetUtils.dayScheduleToJson(widgetData.daySchedule)
                    prefs[stringPreferencesKey("cached_data")] = json
                    prefs[stringPreferencesKey("day_label")] = widgetData.dayLabel
                    prefs.remove(stringPreferencesKey("error_message"))
                }
            }
        } catch (e: Exception) {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[booleanPreferencesKey("is_loading")] = false
                prefs[stringPreferencesKey("error_message")] = "Ошибка: ${e.message}"
            }
        }

        // 3. Обновляем виджет с новыми данными
        ScheduleWidget().update(context, glanceId)
    }
}
