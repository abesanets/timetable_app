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

val KAZINTSA_CALLS = listOf(
    CallTime(pairNumber = 1, shift = 1, firstStart = "08:00", firstEnd = "09:20", secondStart = "08:00", secondEnd = "09:20", isSolid = true),
    CallTime(pairNumber = 2, shift = 1, firstStart = "09:30", firstEnd = "10:50", secondStart = "09:30", secondEnd = "10:50", isSolid = true),
    CallTime(pairNumber = 3, shift = 1, firstStart = "11:10", firstEnd = "12:30", secondStart = "11:10", secondEnd = "12:30", isSolid = true),
    CallTime(pairNumber = 4, shift = 2, firstStart = "12:40", firstEnd = "14:00", secondStart = "12:40", secondEnd = "14:00", isSolid = true),
    CallTime(pairNumber = 5, shift = 2, firstStart = "14:20", firstEnd = "15:40", secondStart = "14:20", secondEnd = "15:40", isSolid = true),
    CallTime(pairNumber = 6, shift = 2, firstStart = "15:50", firstEnd = "17:10", secondStart = "15:50", secondEnd = "17:10", isSolid = true),
    CallTime(pairNumber = 7, shift = 2, firstStart = "17:20", firstEnd = "18:40", secondStart = "17:20", secondEnd = "18:40", isSolid = true)
)

val KNORINA_CALLS = listOf(
    CallTime(pairNumber = 1, firstStart = "09:00", firstEnd = "09:45", secondStart = "09:55", secondEnd = "10:40"),
    CallTime(pairNumber = 2, firstStart = "10:50", firstEnd = "11:35", secondStart = "11:55", secondEnd = "12:40"),
    CallTime(pairNumber = 3, firstStart = "13:00", firstEnd = "13:45", secondStart = "13:55", secondEnd = "14:40"),
    CallTime(pairNumber = 4, firstStart = "14:50", firstEnd = "15:35", secondStart = "15:45", secondEnd = "16:30"),
    CallTime(pairNumber = 5, firstStart = "16:40", firstEnd = "17:25", secondStart = "17:35", secondEnd = "18:20"),
    CallTime(pairNumber = 6, firstStart = "18:30", firstEnd = "19:15", secondStart = "19:25", secondEnd = "20:10")
)

val WEEKDAY_SCHEDULE = KNORINA_CALLS
val SATURDAY_SCHEDULE = KNORINA_CALLS