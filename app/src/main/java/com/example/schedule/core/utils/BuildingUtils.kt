package com.example.schedule.core.utils

import com.example.schedule.data.models.BuildingId
import com.example.schedule.data.models.CallTime
import com.example.schedule.data.models.KAZINTSA_CALLS
import com.example.schedule.data.models.KNORINA_CALLS

data class BuildingInfo(
    val id: BuildingId,
    val name: String,
    val shortName: String,
    val address: String
)

data class CourseGroupItem(
    val year: String,
    val groups: String
)

object BuildingUtils {

    val KAZINTSA_GROUPS = listOf(
        "104", "105", "106",
        "96", "97", "98", "99", "100",
        "86", "87", "88", "89", "90", "91", "92",
        "80", "81", "82", "83", "84"
    )

    val KNORINA_GROUPS = listOf(
        "102", "103", "107", "108", "109",
        "95", "101",
        "85", "9", "93", "94", "169",
        "79", "167", "168"
    )

    val BUILDINGS = mapOf(
        BuildingId.KAZINTSA to BuildingInfo(
            id = BuildingId.KAZINTSA,
            name = "Корпус по ул. Казинца",
            shortName = "Казинца, 91",
            address = "ул. Казинца, 91"
        ),
        BuildingId.KNORINA to BuildingInfo(
            id = BuildingId.KNORINA,
            name = "Корпус по ул. Кнорина",
            shortName = "Кнорина, 14",
            address = "ул. Кнорина, 14"
        )
    )

    val KAZINTSA_COURSE_GROUPS = listOf(
        CourseGroupItem("1 курс", "104РТК, 105ТП, 106МНЭ"),
        CourseGroupItem("2 курс", "96РТК, 97РТК, 98ТП, 99ТП, 100МНЭ"),
        CourseGroupItem("3 курс", "86РТК, 87ТП, 88ТП, 89ТП, 90МНЭ, 91МНЭ, 92МНЭ"),
        CourseGroupItem("4 курс", "80РКТ, 81ТП, 82ТП, 83МНЭ, 84МНЭ")
    )

    val KNORINA_COURSE_GROUPS = listOf(
        CourseGroupItem("1 курс", "102М, 103М, 107ТЭ, 108ТЭ, 109ТП"),
        CourseGroupItem("2 курс", "95М, 101ТП"),
        CourseGroupItem("3 курс", "85М, 9СР, 93МНЭ, 94ТП, 169ТП"),
        CourseGroupItem("4 курс", "79ТЭ, 167ТП, 168МНЭ")
    )

    fun getBuildingForGroup(groupName: String?): BuildingId {
        if (groupName.isNullOrBlank()) return BuildingId.KAZINTSA

        val match = Regex("""\d+""").find(groupName) ?: return BuildingId.KAZINTSA
        val groupNum = match.value
        return if (KNORINA_GROUPS.contains(groupNum)) {
            BuildingId.KNORINA
        } else {
            BuildingId.KAZINTSA
        }
    }

    fun getCallSchedule(building: BuildingId): List<CallTime> {
        return if (building == BuildingId.KNORINA) KNORINA_CALLS else KAZINTSA_CALLS
    }

    fun getCallTime(pairNumber: String?, building: BuildingId): CallTime? {
        val num = pairNumber?.toIntOrNull() ?: return null
        return getCallSchedule(building).find { it.pairNumber == num }
    }

    fun formatCallTimeInterval(call: CallTime): String {
        return if (call.isSolid) {
            "${call.firstStart} - ${call.firstEnd}"
        } else {
            "${call.firstStart} - ${call.secondEnd}"
        }
    }
}
