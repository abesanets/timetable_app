package com.example.schedule

import android.content.Context
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {
    
    companion object {
        private const val WORK_NAME = "schedule_notification_work"
    }
    
    fun scheduleNotification() {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            // Получаем время окончания последней пары
            val schedule = if (get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                SATURDAY_SCHEDULE
            } else {
                WEEKDAY_SCHEDULE
            }
            
            val lastCallTime = schedule.last()
            val timeParts = lastCallTime.secondEnd.split(":")
            set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            set(Calendar.MINUTE, timeParts[1].toInt())
            set(Calendar.SECOND, 0)
            
            // Если время уже прошло, планируем на завтра
            if (before(currentTime)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        val delay = targetTime.timeInMillis - currentTime.timeInMillis
        
        val workRequest = OneTimeWorkRequestBuilder<ScheduleNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    fun cancelNotification() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
