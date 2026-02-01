package com.example.schedule.features.alarms.data

import com.example.schedule.data.models.CallTime
import com.example.schedule.data.models.WEEKDAY_SCHEDULE
import com.example.schedule.data.models.SATURDAY_SCHEDULE

object CallScheduleData {
    fun getScheduleForDay(isSaturday: Boolean): List<CallTime> {
        return if (isSaturday) SATURDAY_SCHEDULE else WEEKDAY_SCHEDULE
    }
    
    fun getDayName(isSaturday: Boolean): String {
        return if (isSaturday) "Суббота" else "Будни"
    }
}