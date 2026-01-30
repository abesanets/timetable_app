package com.example.schedule

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ScheduleNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val preferencesManager = PreferencesManager(applicationContext)
            val notificationHelper = NotificationHelper(applicationContext)
            val fetcher = ScheduleFetcher()
            val parser = ScheduleParser()
            
            // Получаем последнюю группу
            var lastGroup: String? = null
            preferencesManager.lastGroup.collect { group ->
                lastGroup = group
            }
            
            if (lastGroup.isNullOrBlank()) {
                return Result.success()
            }
            
            // Загружаем расписание
            val html = withContext(Dispatchers.IO) {
                fetcher.fetchScheduleHtml(lastGroup!!)
            }
            
            val schedule = withContext(Dispatchers.Default) {
                parser.parse(html, lastGroup!!)
            }
            
            // Находим расписание на завтра
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
            }
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
            val tomorrowString = dateFormat.format(tomorrow.time)
            
            val tomorrowSchedule = schedule.days.find { day ->
                val datePart = day.dayDate.substringAfter(", ").trim()
                datePart == tomorrowString
            }
            
            // Отправляем уведомление
            if (tomorrowSchedule != null) {
                notificationHelper.sendScheduleNotification(tomorrowSchedule)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
