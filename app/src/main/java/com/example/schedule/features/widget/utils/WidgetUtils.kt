package com.example.schedule.features.widget.utils

import com.example.schedule.data.models.DaySchedule
import com.example.schedule.data.models.Lesson
import com.example.schedule.data.models.Subgroup
import org.json.JSONArray
import org.json.JSONObject

object WidgetUtils {

    fun dayScheduleToJson(daySchedule: DaySchedule): String {
        val json = JSONObject()
        json.put("dayDate", daySchedule.dayDate)
        
        val lessonsArray = JSONArray()
        daySchedule.lessons.forEach { lesson ->
            val lessonJson = JSONObject()
            lessonJson.put("lessonNumber", lesson.lessonNumber)
            
            val subgroupsArray = JSONArray()
            lesson.subgroups.forEach { subgroup ->
                val sgJson = JSONObject()
                sgJson.put("subject", subgroup.subject)
                sgJson.put("room", subgroup.room)
                if (subgroup.number != null) {
                    sgJson.put("number", subgroup.number)
                }
                subgroupsArray.put(sgJson)
            }
            lessonJson.put("subgroups", subgroupsArray)
            lessonsArray.put(lessonJson)
        }
        json.put("lessons", lessonsArray)
        return json.toString()
    }

    fun dayScheduleFromJson(jsonString: String): DaySchedule? {
        if (jsonString.isBlank()) return null
        
        return try {
            val json = JSONObject(jsonString)
            val dayDate = json.getString("dayDate")
            val lessonsArray = json.getJSONArray("lessons")
            
            val lessons = mutableListOf<Lesson>()
            for (i in 0 until lessonsArray.length()) {
                val lessonJson = lessonsArray.getJSONObject(i)
                val lessonNumber = lessonJson.getString("lessonNumber")
                
                val subgroupsArray = lessonJson.getJSONArray("subgroups")
                val subgroups = mutableListOf<Subgroup>()
                for (j in 0 until subgroupsArray.length()) {
                    val sgJson = subgroupsArray.getJSONObject(j)
                    val subject = sgJson.getString("subject")
                    val room = sgJson.getString("room")
                    val number = if (sgJson.has("number")) sgJson.getInt("number") else null
                    subgroups.add(Subgroup(subject, room, number))
                }
                lessons.add(Lesson(lessonNumber, subgroups))
            }
            DaySchedule(dayDate, lessons)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}