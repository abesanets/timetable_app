package com.example.schedule.data.network

import com.example.schedule.data.models.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ScheduleParser {
    
    fun parse(html: String, group: String): Schedule {
        val doc = Jsoup.parse(html)
        
        val groupTexts = mutableListOf<Element>()
        groupTexts.addAll(doc.getElementsContainingOwnText("Группа - $group"))
        if (groupTexts.isEmpty()) {
            groupTexts.addAll(doc.getElementsContainingOwnText("Группа-$group"))
        }
        if (groupTexts.isEmpty()) {
            groupTexts.addAll(doc.getElementsContainingOwnText("Группа $group"))
        }
        
        if (groupTexts.isEmpty()) {
            throw GroupNotFoundException("Группа $group не найдена")
        }
        
        val allDaysSchedule = mutableListOf<DaySchedule>()
        
        groupTexts.forEach { groupText ->
            val table = findTableForGroup(groupText) ?: return@forEach
            
            val rows = table.select("tr")
            if (rows.size < 2) return@forEach
            
            val headerRow = rows[0]
            val dayHeaders = parseDayHeaders(headerRow)
            
            val daysSchedule = dayHeaders.map { DaySchedule(it, mutableListOf()) }
            
            val dataStartIndex = 2 
            for (i in dataStartIndex until rows.size) {
                parseLessonRow(rows[i], daysSchedule, (i - dataStartIndex + 1).toString())
            }
            
            allDaysSchedule.addAll(daysSchedule)
        }
        
        if (allDaysSchedule.isEmpty()) {
            throw GroupNotFoundException("Расписание для группы $group не найдено")
        }
        
        return Schedule(group = group, days = allDaysSchedule)
    }
    
    fun parseTeacherSchedule(html: String, teacherName: String): Schedule {
        val doc = Jsoup.parse(html)
        val headerRegex = Regex("Преподаватель\\s*-\\s*${Regex.escape(teacherName)}", RegexOption.IGNORE_CASE)
        val teacherHeader = doc.getElementsMatchingOwnText(headerRegex.toPattern()).firstOrNull()
            ?: doc.getElementsContainingOwnText(teacherName).firstOrNull()
            
        if (teacherHeader == null) {
            throw TeacherNotFoundException("Преподаватель $teacherName не найден")
        }
        
        val table = findTableForGroup(teacherHeader)
            ?: throw TeacherNotFoundException("Таблица расписания для $teacherName не найдена")
        
        val rows = table.select("tr")
        if (rows.size < 2) {
            throw TeacherNotFoundException("Расписание для $teacherName пустое")
        }
        
        val dayHeaders = parseDayHeaders(rows[0])
        val daysSchedule = dayHeaders.map { DaySchedule(it, mutableListOf()) }
        
        for (i in 2 until rows.size) {
            val row = rows[i]
            val lessonNumber = row.selectFirst("th, td:first-child")?.text()?.trim() ?: (i - 1).toString()
            val cells = row.select("td")
            val expandedCells = expandRow(cells)
            
            var dayIndex = 0
            var cellIndex = 0
            
            while (cellIndex < expandedCells.size - 1 && dayIndex < daysSchedule.size) {
                val subject = expandedCells.getOrNull(cellIndex)?.replace('\u00a0', ' ')?.trim() ?: ""
                val room = expandedCells.getOrNull(cellIndex + 1)?.replace('\u00a0', ' ')?.trim() ?: ""
                
                if (isValidLesson(subject)) {
                    val lesson = Lesson(
                        lessonNumber = lessonNumber,
                        subgroups = listOf(Subgroup(subject = subject, room = room))
                    )
                    (daysSchedule[dayIndex].lessons as MutableList).add(lesson)
                }
                
                cellIndex += 2
                dayIndex++
            }
        }
        
        return Schedule(group = teacherName, days = daysSchedule)
    }

    private fun findTableForGroup(groupText: Element): Element? {
        var nextElement = groupText.nextElementSibling()
        while (nextElement != null) {
            if (nextElement.tagName() == "table") return nextElement
            nextElement = nextElement.nextElementSibling()
        }
        
        groupText.parent()?.selectFirst("table")?.let { return it }
        
        var parentSibling = groupText.parent()?.nextElementSibling()
        while (parentSibling != null) {
            parentSibling.selectFirst("table")?.let { return it }
            parentSibling = parentSibling.nextElementSibling()
        }
        
        return null
    }
    
    private fun parseDayHeaders(headerRow: Element): List<String> {
        val cells = headerRow.select("td, th")
        if (cells.isEmpty()) return emptyList()
        
        val dayNames = mutableListOf<String>()
        for (i in 1 until cells.size) {
            val text = cells[i].text().trim()
            if (text.isNotEmpty()) dayNames.add(text)
        }
        return dayNames
    }
    
    private fun parseLessonRow(row: Element, daysSchedule: List<DaySchedule>, lessonNumber: String) {
        val cells = row.select("td")
        if (cells.isEmpty()) return
        
        val expandedCells = expandRow(cells)
        var dayIndex = 0
        var cellIndex = 0
        
        while (cellIndex < expandedCells.size - 1 && dayIndex < daysSchedule.size) {
            val subject = expandedCells.getOrNull(cellIndex)?.trim() ?: ""
            val room = expandedCells.getOrNull(cellIndex + 1)?.trim() ?: ""
            
            if (isValidLesson(subject) || isValidLesson(room)) {
                val subgroups = parseSubgroups(subject, room)
                val dayLessons = daysSchedule[dayIndex].lessons as MutableList
                val existingLesson = dayLessons.find { it.lessonNumber == lessonNumber }
                
                if (existingLesson != null) {
                    val updatedSubgroups = existingLesson.subgroups.toMutableList()
                    updatedSubgroups.addAll(subgroups)
                    val index = dayLessons.indexOf(existingLesson)
                    dayLessons[index] = existingLesson.copy(subgroups = updatedSubgroups)
                } else {
                    dayLessons.add(Lesson(lessonNumber = lessonNumber, subgroups = subgroups))
                }
            }
            
            cellIndex += 2
            dayIndex++
        }
    }
    
    private fun parseSubgroups(subject: String, room: String): List<Subgroup> {
        val subgroupPattern = Regex("""(\d+)\.\s*""")
        val subjectMatches = subgroupPattern.findAll(subject).toList()
        val roomMatches = subgroupPattern.findAll(room).toList()
        
        if (subjectMatches.isEmpty() && roomMatches.isEmpty()) {
            val cleanedRoom = room.replace(Regex("""\s*\([^)]+\)"""), "").trim()
            val finalRoom = if (cleanedRoom == "-" || cleanedRoom == "—") "" else cleanedRoom
            return listOf(Subgroup(subject = subject, room = finalRoom, number = null))
        }
        
        val subgroups = mutableListOf<Subgroup>()
        
        if (subjectMatches.isNotEmpty()) {
            val subjectParts = splitBySubgroupMarkers(subject, subjectMatches)
            val roomParts = if (roomMatches.isNotEmpty()) {
                splitBySubgroupMarkers(room, roomMatches)
            } else {
                val cleanedRoom = room.replace(Regex("""\s*\([^)]+\)"""), "").trim()
                val isRoomDash = cleanedRoom == "-" || cleanedRoom == "—" || cleanedRoom.isEmpty()
                
                if (isRoomDash) List(subjectParts.size) { "" }
                else if (subjectParts.size == 1) listOf(cleanedRoom)
                else splitRoomsByDelimiters(room, subjectParts.size)
            }
            
            for (i in subjectParts.indices) {
                val sPart = subjectParts.getOrNull(i)?.trim() ?: ""
                val rPart = roomParts.getOrNull(i)?.trim() ?: ""
                val number = subjectMatches.getOrNull(i)?.groupValues?.get(1)?.toIntOrNull()
                val finalRoom = if (rPart == "-" || rPart == "—") "" else rPart
                if (sPart.isNotEmpty()) subgroups.add(Subgroup(sPart, finalRoom, number))
            }
        } else if (roomMatches.isNotEmpty()) {
            val roomParts = splitBySubgroupMarkers(room, roomMatches)
            for (i in roomParts.indices) {
                val rPart = roomParts.getOrNull(i)?.trim() ?: ""
                val number = roomMatches.getOrNull(i)?.groupValues?.get(1)?.toIntOrNull()
                val finalRoom = if (rPart == "-" || rPart == "—") "" else rPart
                if (rPart.isNotEmpty()) subgroups.add(Subgroup(subject, finalRoom, number))
            }
        }
        
        return if (subgroups.isEmpty()) {
            val cleanedRoom = room.replace(Regex("""\s*\([^)]+\)"""), "").trim()
            val finalRoom = if (cleanedRoom == "-" || cleanedRoom == "—") "" else cleanedRoom
            listOf(Subgroup(subject = subject, room = finalRoom, number = null))
        } else {
            val hasNumberedSubgroups = subgroups.any { it.number != null }
            if (hasNumberedSubgroups) {
                if (subgroups.none { it.number == 1 }) subgroups.add(0, Subgroup("", "", 1))
                if (subgroups.none { it.number == 2 }) subgroups.add(Subgroup("", "", 2))
                subgroups.sortBy { it.number }
            }
            subgroups
        }
    }
    
    private fun splitRoomsByDelimiters(rooms: String, expectedCount: Int): List<String> {
        val cleanedRooms = rooms.trim()
        if (cleanedRooms == "-" || cleanedRooms == "—" || cleanedRooms.isEmpty()) {
            return List(expectedCount) { "" }
        }
        
        val roomPattern = Regex("""([^\s(,]*\d+[^\s(,]*(\s*\([^)]+\))?|[-—])""")
        val matches = roomPattern.findAll(rooms).toList()
        
        if (matches.isNotEmpty()) {
            val parts = matches.map { match ->
                val cleaned = match.value.trim().replace(Regex("""\s*\([^)]+\)"""), "").trim()
                if (cleaned == "-" || cleaned == "—") "" else cleaned
            }
            if (parts.size == expectedCount) return parts
            if (parts.size < expectedCount) {
                return parts.toMutableList().apply { while (size < expectedCount) add("") }
            }
            return parts.take(expectedCount)
        }
        
        val commaParts = rooms.split(",").map { it.trim().replace(Regex("""\s*\([^)]+\)"""), "") }.filter { it.isNotEmpty() }
        if (commaParts.size >= expectedCount) return commaParts.take(expectedCount)
        
        if (!rooms.contains("(")) {
            val spaceParts = rooms.split(Regex("""\s+""")).map { it.trim() }.filter { it.isNotEmpty() }
            if (spaceParts.size == expectedCount) return spaceParts
        }
        
        return mutableListOf(rooms.replace(Regex("""\s*\([^)]+\)"""), "").trim()).apply { while (size < expectedCount) add("") }
    }
    
    private fun splitBySubgroupMarkers(text: String, matches: List<MatchResult>): List<String> {
        if (matches.isEmpty()) return listOf(text)
        return matches.indices.map { i ->
            val start = matches[i].range.last + 1
            val end = matches.getOrNull(i + 1)?.range?.first ?: text.length
            text.substring(start, end).trim()
        }
    }
    
    private fun expandRow(cells: List<Element>): List<String> {
        val expanded = mutableListOf<String>()
        for (cell in cells) {
            val text = cell.text().trim()
            val colspan = cell.attr("colspan").toIntOrNull() ?: 1
            repeat(colspan) { expanded.add(text) }
        }
        return expanded
    }
    
    private fun isValidLesson(text: String): Boolean = text.isNotEmpty() && text != "-" && text != "—"
}