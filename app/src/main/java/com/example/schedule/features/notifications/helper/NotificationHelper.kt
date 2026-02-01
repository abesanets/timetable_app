package com.example.schedule.features.notifications.helper

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
import com.example.schedule.core.Constants
import com.example.schedule.core.utils.TextUtils
import com.example.schedule.data.models.DaySchedule
import com.example.schedule.ui.MainActivity

class NotificationHelper(private val context: Context) {
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о расписании на следующий день"
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun sendScheduleNotification(daySchedule: DaySchedule) {
        if (!hasNotificationPermission()) return
        
        val pendingIntent = createMainActivityIntent()
        val lessonsText = formatLessonsText(daySchedule)
        val contentText = if (daySchedule.lessons.isEmpty()) {
            "Нет занятий"
        } else {
            "${daySchedule.lessons.size} ${TextUtils.getPairsWord(daySchedule.lessons.size)}"
        }
        
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Расписание на ${daySchedule.dayDate}")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(lessonsText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(Constants.NOTIFICATION_ID, notification)
    }
    
    private fun createMainActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
    
    private fun formatLessonsText(daySchedule: DaySchedule): String {
        return if (daySchedule.lessons.isEmpty()) {
            "Нет занятий"
        } else {
            daySchedule.lessons.joinToString("\n") { lesson ->
                val subjects = lesson.subgroups.joinToString(", ") { it.subject }
                "${lesson.lessonNumber} пара: $subjects"
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
}