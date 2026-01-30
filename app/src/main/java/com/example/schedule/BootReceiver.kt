package com.example.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Receiver для перезапуска уведомлений после перезагрузки устройства
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in listOf(Intent.ACTION_BOOT_COMPLETED, "android.intent.action.QUICKBOOT_POWERON")) {
            return
        }
        
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager(context)
                
                if (!preferencesManager.notificationsEnabled.first()) return@launch
                
                val group = preferencesManager.lastGroup.first()
                if (group.isNullOrBlank()) return@launch
                
                // Загружаем расписание
                val schedule = fetchSchedule(context, group)
                
                // Планируем уведомление
                DailyNotificationManager(context).scheduleNotification(schedule)
                
                Log.d(TAG, "Notifications rescheduled after boot")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    private suspend fun fetchSchedule(context: Context, group: String): Schedule? {
        return try {
            val fetcher = ScheduleFetcher()
            val parser = ScheduleParser()
            
            val html = withContext(Dispatchers.IO) {
                fetcher.fetchScheduleHtml(group)
            }
            
            withContext(Dispatchers.Default) {
                parser.parse(html, group)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching schedule", e)
            null
        }
    }
}
