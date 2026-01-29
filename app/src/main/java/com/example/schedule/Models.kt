package com.example.schedule

/**
 * Модель одного занятия
 */
data class Lesson(
    val lessonNumber: String,  // Номер пары (1, 2, 3...)
    val subject: String,        // Название предмета
    val room: String            // Аудитория
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
