package com.example.schedule.data.models

data class Subgroup(
    val subject: String,
    val room: String,
    val number: Int? = null
)

data class Lesson(
    val lessonNumber: String,
    val subgroups: List<Subgroup>
)

data class DaySchedule(
    val dayDate: String,
    val lessons: List<Lesson>
)

data class Schedule(
    val group: String,
    val days: List<DaySchedule>
)

enum class BuildingId(val id: String) {
    KAZINTSA("kazintsa"),
    KNORINA("knorina")
}

data class CallTime(
    val pairNumber: Int,
    val shift: Int? = null,
    val firstStart: String,
    val firstEnd: String,
    val secondStart: String,
    val secondEnd: String,
    val isSolid: Boolean = false
)

val COMMON_CALLS = listOf(
    CallTime(pairNumber = 1, firstStart = "09:00", firstEnd = "09:45", secondStart = "09:55", secondEnd = "10:40"),
    CallTime(pairNumber = 2, firstStart = "10:50", firstEnd = "11:35", secondStart = "11:55", secondEnd = "12:40"),
    CallTime(pairNumber = 3, firstStart = "13:00", firstEnd = "13:45", secondStart = "13:55", secondEnd = "14:40"),
    CallTime(pairNumber = 4, firstStart = "14:50", firstEnd = "15:35", secondStart = "15:45", secondEnd = "16:30"),
    CallTime(pairNumber = 5, firstStart = "16:40", firstEnd = "17:25", secondStart = "17:35", secondEnd = "18:20"),
    CallTime(pairNumber = 6, firstStart = "18:30", firstEnd = "19:15", secondStart = "19:25", secondEnd = "20:10")
)

val KAZINTSA_CALLS = COMMON_CALLS
val KNORINA_CALLS = COMMON_CALLS

val WEEKDAY_SCHEDULE = COMMON_CALLS
val SATURDAY_SCHEDULE = COMMON_CALLS