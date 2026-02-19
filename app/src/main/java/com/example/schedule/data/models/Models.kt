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

data class CallTime(
    val firstStart: String,
    val firstEnd: String,
    val secondStart: String,
    val secondEnd: String
)

val WEEKDAY_SCHEDULE = listOf(
    CallTime("09:00", "09:45", "09:55", "10:40"),
    CallTime("10:50", "11:35", "11:55", "12:40"),
    CallTime("13:00", "13:45", "13:55", "14:40"),
    CallTime("14:50", "15:35", "15:45", "16:30"),
    CallTime("16:40", "17:25", "17:35", "18:20"),
    CallTime("18:30", "19:15", "19:25", "20:10")
)

val SATURDAY_SCHEDULE = listOf(
    CallTime("09:00", "09:45", "09:55", "10:40"),
    CallTime("10:50", "11:35", "11:55", "12:40"),
    CallTime("12:50", "13:35", "13:45", "14:30"),
    CallTime("14:40", "15:25", "15:35", "16:20"),
    CallTime("16:30", "17:15", "17:25", "18:10"),
    CallTime("18:20", "19:05", "19:15", "20:00")
)