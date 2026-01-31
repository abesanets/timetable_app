package com.example.schedule

/**
 * Модель одной подгруппы в паре
 */
data class Subgroup(
    val subject: String,        // Название предмета
    val room: String            // Аудитория
)

/**
 * Модель одной пары (может содержать несколько подгрупп)
 */
data class Lesson(
    val lessonNumber: String,   // Номер пары (1, 2, 3...)
    val subgroups: List<Subgroup> // Список подгрупп
)

/**
 * Расписание на один день
 */
data class DaySchedule(
    val dayDate: String,        // Например: "Понедельник, 20.01.2025"
    val lessons: List<Lesson>   // Список занятий в этот день
)

/**
 * Полное расписание группы
 */
data class Schedule(
    val group: String,          // Номер группы
    val days: List<DaySchedule> // Расписание по дням
)

/**
 * Время звонков
 */
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
