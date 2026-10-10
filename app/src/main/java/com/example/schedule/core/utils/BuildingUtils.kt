package com.example.schedule.core.utils

import com.example.schedule.data.models.BuildingId
import com.example.schedule.data.models.CallTime
import com.example.schedule.data.models.COMMON_CALLS

object BuildingUtils {

    fun getCallSchedule(building: BuildingId? = null): List<CallTime> {
        return COMMON_CALLS
    }

    fun getCallTime(pairNumber: String?, building: BuildingId? = null): CallTime? {
        val num = pairNumber?.toIntOrNull() ?: return null
        return COMMON_CALLS.find { it.pairNumber == num }
    }

    fun formatCallTimeInterval(call: CallTime): String {
        return if (call.isSolid) {
            "${call.firstStart} - ${call.firstEnd}"
        } else {
            "${call.firstStart} - ${call.secondEnd}"
        }
    }

    fun getBuildingForGroup(groupName: String?): BuildingId {
        return BuildingId.KAZINTSA
    }
}
