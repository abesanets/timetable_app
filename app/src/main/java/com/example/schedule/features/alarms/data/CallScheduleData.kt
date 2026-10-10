package com.example.schedule.features.alarms.data

import com.example.schedule.data.models.COMMON_CALLS
import com.example.schedule.data.models.CallTime

object CallScheduleData {
    fun getSchedule(): List<CallTime> = COMMON_CALLS
}