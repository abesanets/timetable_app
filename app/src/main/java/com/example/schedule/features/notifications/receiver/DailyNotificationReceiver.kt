package com.example.schedule.features.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.schedule.data.models.DaySchedule
import com.example.schedule.data.models.Schedule
import com.example.schedule.data.network.ScheduleFetcher
import com.example.schedule.data.network.ScheduleParser
import com.example.schedule.data.preferences.PreferencesManager
import com.example.schedule.features.notifications.helper.NotificationHelper
import com.example.schedule.features.notifications.manager.DailyNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Получает сигнал от AlarmManager и отправляет уведомление о расписании на завтра
 */
class DailyNotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "DailyNotificationRcvr"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendNotificationForTomorrow(context)
                rescheduleNotification(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error in notification receiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    private suspend fun sendNotificationForTomorrow(context: Context) {
        val preferencesManager = PreferencesManager(context)
        
        // Проверяем, включены ли уведомления
        if (!preferencesManager.notificationsEnabled.first()) return
        
        // Получаем группу
        val group = preferencesManager.lastGroup.first()
        if (group.isNullOrBlank()) return
        
        // Загружаем расписание
        val schedule = fetchSchedule(context, group) ?: return
        
        // Находим расписание на завтра
        val tomorrowSchedule = findTomorrowSchedule(schedule) ?: return
        
        // Отправляем уведомление
        NotificationHelper(context).sendScheduleNotification(tomorrowSchedule)
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
    
    private fun findTomorrowSchedule(schedule: Schedule): DaySchedule? {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        val today = Calendar.getInstance()
        val todayString = dateFormat.format(today.time)
        
        // Ищем расписание на ближайшие 7 дней, начиная с завтра
        for (i in 1..7) {
            val nextDay = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, i)
            }
            val nextDayString = dateFormat.format(nextDay.time)
            
            val foundDay = schedule.days.find { day ->
                day.dayDate.contains(nextDayString)
            }
            
            if (foundDay != null && foundDay.lessons.isNotEmpty()) {
                return foundDay
            }
        }
        
        return null
    }
    
    private suspend fun rescheduleNotification(context: Context) {
        try {
            val preferencesManager = PreferencesManager(context)
            val group = preferencesManager.lastGroup.first()
            
            if (group.isNullOrBlank()) return
            
            val schedule = fetchSchedule(context, group)
            
            DailyNotificationManager(context).scheduleNotification(schedule)
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling", e)
        }
    }
}