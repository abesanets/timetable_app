package com.example.schedule.features.schedule.utils

import com.example.schedule.data.models.DaySchedule
import com.example.schedule.data.models.WEEKDAY_SCHEDULE
import com.example.schedule.data.models.SATURDAY_SCHEDULE
import java.text.SimpleDateFormat
import java.util.*

object ScheduleUtils {
    
    fun areClassesFinishedForToday(day: DaySchedule): Boolean {
        if (day.lessons.isEmpty()) return true
        
        val now = Calendar.getInstance()
        val currentTime = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        
        val isSaturday = now.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
        val schedule = if (isSaturday) SATURDAY_SCHEDULE else WEEKDAY_SCHEDULE
        
        val lastLesson = day.lessons.maxByOrNull { it.lessonNumber.toIntOrNull() ?: 0 } ?: return true
        val lastLessonNumber = lastLesson.lessonNumber.toIntOrNull() ?: return true
        
        if (lastLessonNumber < 1 || lastLessonNumber > schedule.size) return true
        
        val endTime = schedule[lastLessonNumber - 1].secondEnd
        val endParts = endTime.split(":")
        val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
        
        return currentTime >= endMinutes
    }

    fun findTodayIndex(days: List<DaySchedule>): Int {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        val todayString = dateFormat.format(today.time)
        
        // Сначала ищем сегодняшний день в расписании
        val todayIndex = days.indexOfFirst { day ->
            val datePart = day.dayDate.substringAfter(", ").trim()
            datePart == todayString
        }
        
        // Если нашли сегодняшний день в расписании
        if (todayIndex >= 0) {
            val todaySchedule = days[todayIndex]
            // Если занятия на сегодня закончились, ищем следующий учебный день
            if (areClassesFinishedForToday(todaySchedule)) {
                // Ищем следующий день с занятиями, начиная с завтрашнего дня
                for (i in (todayIndex + 1) until days.size) {
                    if (days[i].lessons.isNotEmpty()) {
                        return i
                    }
                }
                // Если не нашли следующий день с занятиями, возвращаем сегодняшний
                return todayIndex
            }
            return todayIndex
        }
        
        // Если сегодняшний день НЕ найден в расписании (например, воскресенье)
        // Ищем ближайший будущий учебный день
        val currentDate = today.time
        
        // Ищем ближайший день в будущем с занятиями
        var nearestFutureIndex = -1
        var nearestFutureDate: Date? = null
        
        for (i in days.indices) {
            val dayDateStr = days[i].dayDate.substringAfter(", ").trim()
            try {
                val dayDate = dateFormat.parse(dayDateStr)
                if (dayDate != null && dayDate > currentDate && days[i].lessons.isNotEmpty()) {
                    if (nearestFutureDate == null || dayDate < nearestFutureDate) {
                        nearestFutureDate = dayDate
                        nearestFutureIndex = i
                    }
                }
            } catch (e: Exception) {
                // Игнорируем ошибки парсинга даты
            }
        }
        
        // Если нашли ближайший будущий день, возвращаем его
        if (nearestFutureIndex >= 0) {
            return nearestFutureIndex
        }
        
        // Если ничего не найдено, возвращаем первый день с занятиями
        return days.indexOfFirst { it.lessons.isNotEmpty() }.takeIf { it >= 0 } ?: 0
    }
}