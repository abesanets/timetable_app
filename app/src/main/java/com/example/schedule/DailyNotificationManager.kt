package com.example.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.*

/**
 * Управляет ежедневными уведомлениями о расписании на завтра
 */
class DailyNotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DailyNotificationMgr"
        private const val REQUEST_CODE = 2001
        private const val DEFAULT_NOTIFICATION_HOUR = 21
        private const val DEFAULT_NOTIFICATION_MINUTE = 0
    }
    
    /**
     * Планирует уведомление после окончания последней пары сегодня
     * @param schedule - расписание группы для определения времени последней пары
     */
    fun scheduleNotification(schedule: Schedule?) {
        cancelNotification()
        
        val targetTime = calculateNotificationTime(schedule) ?: run {
            scheduleForTomorrow()
            return
        }
        
        val intent = Intent(context, DailyNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetTime.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                targetTime.timeInMillis,
                pendingIntent
            )
        }
        
        Log.d(TAG, "Notification scheduled for: ${targetTime.time}")
    }
    
    /**
     * Отменяет запланированное уведомление
     */
    fun cancelNotification() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
    
    /**
     * Вычисляет время для уведомления - после окончания последней пары сегодня
     */
    private fun calculateNotificationTime(schedule: Schedule?): Calendar? {
        if (schedule == null) return null
        
        val now = Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        val todayString = dateFormat.format(now.time)
        
        // Находим расписание на сегодня
        val todaySchedule = schedule.days.find { day ->
            day.dayDate.substringAfter(", ").trim() == todayString
        } ?: return null
        
        if (todaySchedule.lessons.isEmpty()) return null
        
        // Находим номер последней пары
        val lastLessonNumber = todaySchedule.lessons.maxOfOrNull { 
            it.lessonNumber.toIntOrNull() ?: 0 
        } ?: return null
        
        if (lastLessonNumber < 1) return null
        
        // Определяем расписание звонков
        val isSaturday = now.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
        val callSchedule = if (isSaturday) SATURDAY_SCHEDULE else WEEKDAY_SCHEDULE
        
        if (lastLessonNumber > callSchedule.size) return null
        
        // Берем время окончания последней пары
        val endTime = callSchedule[lastLessonNumber - 1].secondEnd
        val timeParts = endTime.split(":")
        
        val notificationTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            set(Calendar.MINUTE, timeParts[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Если время уже прошло, планируем на завтра
        return if (notificationTime.before(now)) null else notificationTime
    }
    
    /**
     * Планирует уведомление на завтра в 21:00 (если не удалось вычислить по расписанию)
     */
    private fun scheduleForTomorrow() {
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, DEFAULT_NOTIFICATION_HOUR)
            set(Calendar.MINUTE, DEFAULT_NOTIFICATION_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                tomorrow.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                tomorrow.timeInMillis,
                pendingIntent
            )
        }
        
        Log.d(TAG, "Scheduled for tomorrow at $DEFAULT_NOTIFICATION_HOUR:00")
    }
}
