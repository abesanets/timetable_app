package com.example.schedule

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "schedule_notifications"
        private const val CHANNEL_NAME = "Уведомления о расписании"
        private const val NOTIFICATION_ID = 1001
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Уведомления о расписании на следующий день"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun sendScheduleNotification(daySchedule: DaySchedule) {
        if (!hasNotificationPermission()) {
            return
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Формируем текст уведомления
        val isSaturday = daySchedule.dayDate.contains("суббота", ignoreCase = true) || 
                         daySchedule.dayDate.contains("сб", ignoreCase = true)
        val schedule = if (isSaturday) SATURDAY_SCHEDULE else WEEKDAY_SCHEDULE
        
        val lessonsText = if (daySchedule.lessons.isEmpty()) {
            "Нет занятий"
        } else {
            daySchedule.lessons.joinToString("\n") { lesson ->
                val lessonNum = lesson.lessonNumber.toIntOrNull() ?: 0
                val timeInfo = if (lessonNum in 1..schedule.size) {
                    val time = schedule[lessonNum - 1]
                    "${time.firstStart}-${time.secondEnd}"
                } else ""
                
                if (lesson.subgroups.size == 1) {
                    val sub = lesson.subgroups[0]
                    // Компактный формат: "1. Математика (305) 09:00-10:40"
                    "${lesson.lessonNumber}. ${sub.subject} (${sub.room}) $timeInfo"
                } else {
                    // Для подгрупп: "1. 1.Sub1(R) / 2.Sub2(R) Time"
                    val subgroups = lesson.subgroups.mapIndexed { index, sub ->
                        "${index + 1}.${sub.subject}(${sub.room})"
                    }.joinToString(" / ")
                    
                    "${lesson.lessonNumber}. $subgroups $timeInfo"
                }
            }
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Расписание на ${daySchedule.dayDate}")
            .setContentText(if (daySchedule.lessons.isEmpty()) "Нет занятий" else "${daySchedule.lessons.size} ${getPairsWord(daySchedule.lessons.size)}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(lessonsText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                notify(NOTIFICATION_ID, notification)
            }
        }
    }
    
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    private fun getPairsWord(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "пара"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "пары"
            else -> "пар"
        }
    }
}
