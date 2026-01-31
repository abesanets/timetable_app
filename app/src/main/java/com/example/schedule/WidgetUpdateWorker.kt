package com.example.schedule

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting periodic widget update")
            
            val prefsManager = PreferencesManager(context)
            val group = prefsManager.lastGroup.first()
            
            if (group.isNullOrBlank()) {
                return@withContext Result.success()
            }

            // 1. Fetch data
            val fetcher = ScheduleFetcher()
            val parser = ScheduleParser()
            val html = fetcher.fetchScheduleHtml(group)
            val schedule = parser.parse(html, group)

            // 2. Prepare display data
            val displayIndex = findTodayIndex(schedule.days)
            var dayLabel = ""
            var daySchedule: DaySchedule? = null
            
            if (displayIndex >= 0 && displayIndex < schedule.days.size) {
                 daySchedule = schedule.days[displayIndex]
                 
                 // Calculate label logic (simplified reuse)
                 // You might want to extract this logic to a shared Utils class to avoid duplication
            }

            // 3. Update Widget State
            if (daySchedule != null) {
                // We need to update state for ALL instances of ScheduleWidget
                // In Glance, we typically iterate over all GlanceIds for this provider
                // But updateAll() simplifies this if we just update the specific state 
                // associated with the widget class.
                // However, updateAppWidgetState requires a glanceId.
                // We will use ScheduleWidget().updateAll(context) but we need to set the state first.
                // Glance state is stored in DataStore keyed by GlanceId.
                // Since this is a background worker, updating "Visual" state is tricky without a GlanceId.
                
                // Correction: When using PreferencesGlanceStateDefinition, the state is stored 
                // in a file named after the specific widget ID.
                // To update ALL widgets, we need to iterate them.
                
                androidx.glance.appwidget.GlanceAppWidgetManager(context)
                    .getGlanceIds(ScheduleWidget::class.java)
                    .forEach { glanceId ->
                        updateAppWidgetState(context, glanceId) { prefs ->
                            val json = WidgetUtils.dayScheduleToJson(daySchedule)
                            prefs[stringPreferencesKey("cached_data")] = json
                            prefs.remove(stringPreferencesKey("error_message"))
                            prefs[booleanPreferencesKey("is_loading")] = false
                            // Day label logic would ideally be re-calculated here
                        }
                        ScheduleWidget().update(context, glanceId)
                    }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WidgetUpdateWorker"
    }
}
