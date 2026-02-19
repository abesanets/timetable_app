package com.example.schedule.features.schedule.utils

import com.example.schedule.data.models.DaySchedule
import com.example.schedule.data.models.Lesson
import com.example.schedule.data.models.Schedule
import com.example.schedule.data.models.Subgroup
import com.example.schedule.data.models.WEEKDAY_SCHEDULE
import com.example.schedule.data.models.SATURDAY_SCHEDULE
import java.text.SimpleDateFormat
import java.util.*

object ScheduleUtils {
    
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))

    fun filterScheduleBySubgroup(schedule: Schedule, subgroupNumber: Int): Schedule {
        if (subgroupNumber == 0) return schedule
        
        return schedule.copy(
            days = schedule.days.map { day ->
                day.copy(
                    lessons = day.lessons.mapNotNull { lesson ->
                        val filteredSubgroups = lesson.subgroups.filter { subgroup ->
                            val isPhysicalEducation = subgroup.subject.contains("физ", ignoreCase = true) && 
                                    (subgroup.subject.contains("культ", ignoreCase = true) || subgroup.subject.contains("к-ра", ignoreCase = true)) ||
                                    subgroup.subject.equals("фзк", ignoreCase = true) ||
                                    subgroup.subject.contains("фзкиз", ignoreCase = true)

                            isPhysicalEducation || subgroup.number == null || subgroup.number == subgroupNumber 
                        }
                        
                        val hasActualContent = filteredSubgroups.any { 
                            it.subject != "-" && it.subject != "—" && it.subject.isNotBlank() 
                        }
                        
                        if (!hasActualContent) null
                        else lesson.copy(subgroups = filteredSubgroups)
                    }
                )
            }
        )
    }

    private fun extractDate(dayDate: String): String {
        val dateRegex = Regex("""\d{2}\.\d{2}\.\d{4}""")
        val match = dateRegex.find(dayDate)
        return match?.value ?: dayDate.substringAfter(",").trim()
    }
    
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
        
        return currentTime >= (endMinutes + 5)
    }

    fun findTodayIndex(days: List<DaySchedule>): Int {
        if (days.isEmpty()) return -1
        
        val today = Calendar.getInstance()
        val todayString = dateFormat.format(today.time)
        
        val todayIndex = days.indexOfFirst { day ->
            extractDate(day.dayDate) == todayString
        }
        
        if (todayIndex >= 0) {
            val todaySchedule = days[todayIndex]
            if (areClassesFinishedForToday(todaySchedule)) {
                for (i in (todayIndex + 1) until days.size) {
                    if (days[i].lessons.isNotEmpty()) {
                        return i
                    }
                }
                return todayIndex
            }
            return todayIndex
        }
        
        val currentDate = today.time
        
        var nearestFutureIndex = -1
        var nearestFutureDate: Date? = null
        
        for (i in days.indices) {
            val dateStr = extractDate(days[i].dayDate)
            try {
                val dayDate = dateFormat.parse(dateStr)
                if (dayDate != null && dayDate > currentDate && days[i].lessons.isNotEmpty()) {
                    if (nearestFutureDate == null || dayDate < nearestFutureDate) {
                        nearestFutureDate = dayDate
                        nearestFutureIndex = i
                    }
                }
            } catch (e: Exception) {
            }
        }
        
        if (nearestFutureIndex >= 0) {
            return nearestFutureIndex
        }
        
        return days.indexOfFirst { it.lessons.isNotEmpty() }.takeIf { it >= 0 } ?: 0
    }

    fun isShowingNextDay(days: List<DaySchedule>, displayIndex: Int): Boolean {
        if (displayIndex < 0 || displayIndex >= days.size) return false
        
        val today = Calendar.getInstance()
        val todayString = dateFormat.format(today.time)
        
        val todayIndex = days.indexOfFirst { day ->
            extractDate(day.dayDate) == todayString
        }
        
        if (todayIndex >= 0 && displayIndex > todayIndex) {
            return true
        }
        
        if (todayIndex < 0) {
            val displayDateStr = extractDate(days[displayIndex].dayDate)
            try {
                val displayDate = dateFormat.parse(displayDateStr)
                val currentDate = today.time
                return displayDate != null && displayDate > currentDate
            } catch (e: Exception) {
                return false
            }
        }
        
        return false
    }

    fun getClassesEndTime(days: List<DaySchedule>): Pair<Int, Int>? {
        val today = Calendar.getInstance()
        val todayString = dateFormat.format(today.time)

        val todaySchedule = days.find { day ->
            extractDate(day.dayDate) == todayString
        } ?: return null

        if (todaySchedule.lessons.isEmpty()) return null

        val isSaturday = today.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
        val schedule = if (isSaturday) SATURDAY_SCHEDULE else WEEKDAY_SCHEDULE

        val lastLesson = todaySchedule.lessons.maxByOrNull { it.lessonNumber.toIntOrNull() ?: 0 } ?: return null
        val lastLessonNumber = lastLesson.lessonNumber.toIntOrNull() ?: return null

        if (lastLessonNumber < 1 || lastLessonNumber > schedule.size) return null

        val endTime = schedule[lastLessonNumber - 1].secondEnd
        val endParts = endTime.split(":")
        return Pair(endParts[0].toInt(), endParts[1].toInt())
    }
}