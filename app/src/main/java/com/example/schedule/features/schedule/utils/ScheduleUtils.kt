package com.example.schedule.features.schedule.utils

import com.example.schedule.core.utils.BuildingUtils
import com.example.schedule.data.models.BuildingId
import com.example.schedule.data.models.DaySchedule
import com.example.schedule.data.models.Lesson
import com.example.schedule.data.models.Schedule
import com.example.schedule.data.models.Subgroup
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
                            isPhysicalEducation(subgroup.subject) || subgroup.number == null || subgroup.number == subgroupNumber 
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

    fun shouldShowAllSubgroupsInDetails(
        lesson: Lesson,
        selectedSubgroup: Int,
        showOtherSubgroupInDetails: Boolean
    ): Boolean {
        if (!showOtherSubgroupInDetails || selectedSubgroup == 0) return false

        val numberedSubgroups = lesson.subgroups.filter { it.number != null }
        if (numberedSubgroups.size < 2) return false
        if (numberedSubgroups.any { isPhysicalEducation(it.subject) }) return false

        val hasSelectedSubgroup = numberedSubgroups.any { it.number == selectedSubgroup && it.hasActualContent() }
        val hasOtherSubgroup = numberedSubgroups.any { it.number != selectedSubgroup && it.hasActualContent() }
        return hasSelectedSubgroup && hasOtherSubgroup
    }

    fun isPhysicalEducation(subject: String): Boolean {
        return subject.contains("физ", ignoreCase = true) &&
                (subject.contains("культ", ignoreCase = true) || subject.contains("к-ра", ignoreCase = true)) ||
                subject.equals("фзк", ignoreCase = true) ||
                subject.contains("фзкиз", ignoreCase = true)
    }

    private fun Subgroup.hasActualContent(): Boolean {
        return subject != "-" && subject != "—" && subject.isNotBlank()
    }

    private fun extractDate(dayDate: String): String {
        val dateRegex = Regex("""\d{2}\.\d{2}\.\d{4}""")
        val match = dateRegex.find(dayDate)
        return match?.value ?: dayDate.substringAfter(",").trim()
    }
    
    fun areClassesFinishedForToday(day: DaySchedule, building: BuildingId = BuildingId.KAZINTSA): Boolean {
        if (day.lessons.isEmpty()) return true
        
        val now = Calendar.getInstance()
        val currentTime = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        
        val schedule = BuildingUtils.getCallSchedule(building)
        
        val lastLesson = day.lessons.maxByOrNull { it.lessonNumber.toIntOrNull() ?: 0 } ?: return true
        val lastLessonNumber = lastLesson.lessonNumber.toIntOrNull() ?: return true
        
        val lastCall = schedule.find { it.pairNumber == lastLessonNumber } ?: return true
        
        val endTime = if (lastCall.isSolid) lastCall.firstEnd else lastCall.secondEnd
        val endParts = endTime.split(":")
        val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
        
        return currentTime >= (endMinutes + 5)
    }

    fun findTodayIndex(days: List<DaySchedule>, building: BuildingId = BuildingId.KAZINTSA): Int {
        if (days.isEmpty()) return -1
        
        val today = Calendar.getInstance()
        val todayString = dateFormat.format(today.time)
        
        val todayIndex = days.indexOfFirst { day ->
            extractDate(day.dayDate) == todayString
        }
        
        if (todayIndex >= 0) {
            val todaySchedule = days[todayIndex]
            if (areClassesFinishedForToday(todaySchedule, building)) {
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

    fun getClassesEndTime(days: List<DaySchedule>, building: BuildingId = BuildingId.KAZINTSA): Pair<Int, Int>? {
        val today = Calendar.getInstance()
        val todayString = dateFormat.format(today.time)

        val todaySchedule = days.find { day ->
            extractDate(day.dayDate) == todayString
        } ?: return null

        if (todaySchedule.lessons.isEmpty()) return null

        val schedule = BuildingUtils.getCallSchedule(building)

        val lastLesson = todaySchedule.lessons.maxByOrNull { it.lessonNumber.toIntOrNull() ?: 0 } ?: return null
        val lastLessonNumber = lastLesson.lessonNumber.toIntOrNull() ?: return null

        val lastCall = schedule.find { it.pairNumber == lastLessonNumber } ?: return null

        val endTime = if (lastCall.isSolid) lastCall.firstEnd else lastCall.secondEnd
        val endParts = endTime.split(":")
        return Pair(endParts[0].toInt(), endParts[1].toInt())
    }
}
