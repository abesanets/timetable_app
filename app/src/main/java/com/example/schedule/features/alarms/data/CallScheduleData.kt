package com.example.schedule.features.alarms.data

import com.example.schedule.core.utils.BuildingUtils
import com.example.schedule.data.models.BuildingId
import com.example.schedule.data.models.CallTime

object CallScheduleData {
    fun getScheduleForBuilding(building: BuildingId): List<CallTime> {
        return BuildingUtils.getCallSchedule(building)
    }

    fun getBuildingName(building: BuildingId): String {
        return BuildingUtils.BUILDINGS[building]?.name ?: ""
    }
}