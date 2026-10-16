package com.example.schedule.features.schedule.utils

import com.example.schedule.data.models.DaySchedule
import com.example.schedule.data.models.Lesson
import com.example.schedule.data.models.Subgroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class ScheduleUtilsTest {

    private fun createCalendar(hour: Int, minute: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private val sampleDay = DaySchedule(
        dayDate = "16.10.2026",
        lessons = listOf(
            Lesson(
                lessonNumber = "1",
                subgroups = listOf(Subgroup(subject = "Математика", room = "201"))
            ),
            Lesson(
                lessonNumber = "2",
                subgroups = listOf(Subgroup(subject = "Физика", room = "305"))
            )
        )
    )

    // Pair 1: 09:00 - 09:45, 09:55 - 10:40
    // Pair 2: 10:50 - 11:35, 11:55 - 12:40

    @Test
    fun testLiveStatusBeforeClassesFarAway() {
        // 07:30 - more than 60 mins before 09:00
        val cal = createCalendar(7, 30)
        val status = ScheduleUtils.getLiveStatusForToday(sampleDay, nowCalendar = cal)
        assertEquals("Сегодня", status)
    }

    @Test
    fun testLiveStatusBeforeClassesWithin60Mins() {
        // 08:35 - 25 mins before 09:00
        val cal = createCalendar(8, 35)
        val status = ScheduleUtils.getLiveStatusForToday(sampleDay, nowCalendar = cal)
        assertEquals("До пары 25 мин", status)
    }

    @Test
    fun testLiveStatusDuringFirstHalf() {
        // 09:20 - 25 mins before 09:45 (first half ends)
        val cal = createCalendar(9, 20)
        val status = ScheduleUtils.getLiveStatusForToday(sampleDay, nowCalendar = cal)
        assertEquals("До перерыва 25 мин", status)
    }

    @Test
    fun testLiveStatusDuringBreakInsidePair() {
        // 09:48 - 7 mins before 09:55 (second half starts)
        val cal = createCalendar(9, 48)
        val status = ScheduleUtils.getLiveStatusForToday(sampleDay, nowCalendar = cal)
        assertEquals("До звонка 7 мин", status)
    }

    @Test
    fun testLiveStatusDuringSecondHalf() {
        // 10:20 - 20 mins before 10:40 (pair ends)
        val cal = createCalendar(10, 20)
        val status = ScheduleUtils.getLiveStatusForToday(sampleDay, nowCalendar = cal)
        assertEquals("До конца пары 20 мин", status)
    }

    @Test
    fun testLiveStatusDuringBreakBetweenPairs() {
        // 10:42 - 8 mins before 10:50 (pair 2 starts)
        val cal = createCalendar(10, 42)
        val status = ScheduleUtils.getLiveStatusForToday(sampleDay, nowCalendar = cal)
        assertEquals("До пары 8 мин", status)
    }

    @Test
    fun testLiveStatusAfterAllClassesFinished() {
        // 12:45 - after 12:40 (pair 2 second half ends)
        val cal = createCalendar(12, 45)
        val status = ScheduleUtils.getLiveStatusForToday(sampleDay, nowCalendar = cal)
        assertNull(status)
    }
}
