package com.example.schedule.data.network

import android.util.Log
import com.example.schedule.data.models.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Парсер HTML-страницы расписания
 */
class ScheduleParser {
    
    private val TAG = "ScheduleParser"
    
    /**
     * Парсит HTML и возвращает структурированное расписание
     */
    fun parse(html: String, group: String): Schedule {
        val doc = Jsoup.parse(html)
        
        // Ищем ВСЕ вхождения текста "Группа - XX" (для двух недель)
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
        
        Log.d(TAG, "Найдено блоков расписания для группы: ${groupTexts.size}")
        
        // Собираем расписание из всех найденных блоков
        val allDaysSchedule = mutableListOf<DaySchedule>()
        
        groupTexts.forEachIndexed { blockIndex, groupText ->
            Log.d(TAG, "Обработка блока ${blockIndex + 1}: ${groupText.text()}")
            
            // Ищем таблицу для текущего блока
            val table = findTableForGroup(groupText)
            
            if (table == null) {
                Log.w(TAG, "Таблица для блока ${blockIndex + 1} не найдена, пропускаем")
                return@forEachIndexed
            }
            
            Log.d(TAG, "Найдена таблица для блока ${blockIndex + 1}")
            
            // Получаем все строки таблицы
            val rows = table.select("tr")
            if (rows.size < 2) {
                Log.w(TAG, "Таблица блока ${blockIndex + 1} слишком маленькая, пропускаем")
                return@forEachIndexed
            }
            
            // Первая строка — заголовки дней
            val headerRow = rows[0]
            val dayHeaders = parseDayHeaders(headerRow)
            
            Log.d(TAG, "Блок ${blockIndex + 1}: найдено дней: ${dayHeaders.size}")
            
            // Инициализируем расписание для каждого дня
            val daysSchedule = dayHeaders.map { dayName ->
                DaySchedule(dayDate = dayName, lessons = mutableListOf())
            }
            
            // Парсим строки с занятиями
            val dataStartIndex = 2 // Пропускаем заголовок (0) и строку с номерами (1)
            
            for (i in dataStartIndex until rows.size) {
                val row = rows[i]
                val lessonNumber = (i - dataStartIndex + 1).toString()
                parseLessonRow(row, daysSchedule, lessonNumber)
            }
            
            // Добавляем дни из этого блока к общему расписанию
            allDaysSchedule.addAll(daysSchedule)
            
            Log.d(TAG, "Блок ${blockIndex + 1}: добавлено ${daysSchedule.size} дней")
        }
        
        if (allDaysSchedule.isEmpty()) {
            throw GroupNotFoundException("Расписание для группы $group не найдено")
        }
        
        // Логируем итоговый результат
        Log.d(TAG, "Итого дней в расписании: ${allDaysSchedule.size}")
        allDaysSchedule.forEachIndexed { index, day ->
            Log.d(TAG, "День ${index + 1} (${day.dayDate}): ${day.lessons.size} занятий")
        }
        
        return Schedule(group = group, days = allDaysSchedule)
    }
    
    /**
     * Находит таблицу расписания для элемента с текстом группы
     */
    private fun findTableForGroup(groupText: Element): Element? {
        var table: Element? = null
        
        // Вариант 1: таблица - следующий элемент после текста группы
        var nextElement = groupText.nextElementSibling()
        while (nextElement != null && table == null) {
            if (nextElement.tagName() == "table") {
                table = nextElement
                break
            }
            nextElement = nextElement.nextElementSibling()
        }
        
        // Вариант 2: таблица внутри родительского элемента
        if (table == null) {
            val parent = groupText.parent()
            table = parent?.selectFirst("table")
        }
        
        // Вариант 3: ищем таблицу в следующих siblings родителя
        if (table == null) {
            var parentSibling = groupText.parent()?.nextElementSibling()
            while (parentSibling != null && table == null) {
                table = parentSibling.selectFirst("table")
                if (table != null) break
                parentSibling = parentSibling.nextElementSibling()
            }
        }
        
        return table
    }
    
    /**
     * Парсит заголовки дней из первой строки таблицы
     * Возвращает список названий дней (БЕЗ первой ячейки "№")
     */
    private fun parseDayHeaders(headerRow: Element): List<String> {
        val cells = headerRow.select("td, th")
        if (cells.isEmpty()) return emptyList()
        
        val dayNames = mutableListOf<String>()
        
        // ПРОПУСКАЕМ первую ячейку (это "№")
        for (i in 1 until cells.size) {
            val cell = cells[i]
            val text = cell.text().trim()
            
            // Добавляем название дня (без учёта colspan для заголовков)
            if (text.isNotEmpty()) {
                dayNames.add(text)
            }
        }
        
        return dayNames
    }
    
    /**
     * Парсит одну строку с занятиями
     * Разворачивает colspan и добавляет занятия в соответствующие дни
     */
    private fun parseLessonRow(row: Element, daysSchedule: List<DaySchedule>, lessonNumber: String) {
        val cells = row.select("td")
        if (cells.isEmpty()) return
        
        // Разворачиваем строку с учётом colspan
        val expandedCells = expandRow(cells)
        
        Log.d(TAG, "Пара №$lessonNumber: ${expandedCells.size} ячеек")
        
        // ВСЕ ячейки - это данные (нет номера пары в начале!)
        // Структура: [предмет_ПН, аудитория_ПН, предмет_ВТ, аудитория_ВТ, ...]
        
        // Проходим по дням с шагом 2 (предмет, аудитория)
        var dayIndex = 0
        var cellIndex = 0
        
        while (cellIndex < expandedCells.size - 1 && dayIndex < daysSchedule.size) {
            val subject = expandedCells.getOrNull(cellIndex)?.trim() ?: ""
            val room = expandedCells.getOrNull(cellIndex + 1)?.trim() ?: ""
            
            Log.d(TAG, "  День $dayIndex: предмет='$subject', аудитория='$room'")
            
            // Пропускаем пустые или прочерки
            if (isValidLesson(subject) || isValidLesson(room)) {
                // Проверяем, есть ли подгруппы в тексте
                val subgroups = parseSubgroups(subject, room)
                
                // Ищем существующую пару с таким номером
                val dayLessons = daysSchedule[dayIndex].lessons as MutableList
                val existingLesson = dayLessons.find { it.lessonNumber == lessonNumber }
                
                if (existingLesson != null) {
                    // Добавляем подгруппы к существующей паре
                    val updatedSubgroups = existingLesson.subgroups.toMutableList()
                    updatedSubgroups.addAll(subgroups)
                    val updatedLesson = existingLesson.copy(subgroups = updatedSubgroups)
                    
                    val index = dayLessons.indexOf(existingLesson)
                    dayLessons[index] = updatedLesson
                    Log.d(TAG, "    Добавлены подгруппы к паре $lessonNumber: ${subgroups.size} шт.")
                } else {
                    // Создаем новую пару
                    val lesson = Lesson(
                        lessonNumber = lessonNumber,
                        subgroups = subgroups
                    )
                    dayLessons.add(lesson)
                    Log.d(TAG, "    Создана новая пара с ${subgroups.size} подгруппами")
                }
            } else {
                Log.d(TAG, "    Пропущено (пустое или прочерк)")
            }
            
            cellIndex += 2
            dayIndex++
        }
    }
    
    /**
     * Распознает подгруппы в тексте предмета и аудитории
     * Ищет паттерны типа "1.Предмет1 2.Предмет2" или "1. Предмет1 2. Предмет2"
     */
    private fun parseSubgroups(subject: String, room: String): List<Subgroup> {
        // Регулярное выражение для поиска подгрупп: "1.", "2.", "3." и т.д.
        val subgroupPattern = Regex("""(\d+)\.\s*""")
        
        // Проверяем, есть ли маркеры подгрупп в тексте предмета
        val subjectMatches = subgroupPattern.findAll(subject).toList()
        val roomMatches = subgroupPattern.findAll(room).toList()
        
        // Если нет маркеров подгрупп, возвращаем одну подгруппу (с очисткой скобок)
        if (subjectMatches.isEmpty() && roomMatches.isEmpty()) {
            val cleanedRoom = room.replace(Regex("""\s*\([^)]+\)"""), "").trim()
            return listOf(Subgroup(subject = subject, room = cleanedRoom))
        }
        
        val subgroups = mutableListOf<Subgroup>()
        
        // Если есть маркеры в предмете
        if (subjectMatches.isNotEmpty()) {
            // Разбиваем текст предмета на части
            val subjectParts = splitBySubgroupMarkers(subject, subjectMatches)
            
            // Разбиваем текст аудитории на части
            val roomParts = if (roomMatches.isNotEmpty()) {
                // Если в аудитории есть маркеры, разбиваем по ним
                splitBySubgroupMarkers(room, roomMatches)
            } else {
                // Если в аудитории нет маркеров, пробуем разделить по пробелам/запятым
                splitRoomsByDelimiters(room, subjectParts.size)
            }
            
            // Создаем подгруппы
            for (i in subjectParts.indices) {
                val subjectPart = subjectParts.getOrNull(i)?.trim() ?: ""
                val roomPart = roomParts.getOrNull(i)?.trim() ?: ""
                
                if (subjectPart.isNotEmpty()) {
                    subgroups.add(Subgroup(subject = subjectPart, room = roomPart))
                }
            }
        } else if (roomMatches.isNotEmpty()) {
            // Если маркеры только в аудитории
            val roomParts = splitBySubgroupMarkers(room, roomMatches)
            
            for (i in roomParts.indices) {
                val roomPart = roomParts.getOrNull(i)?.trim() ?: ""
                
                if (roomPart.isNotEmpty()) {
                    subgroups.add(Subgroup(subject = subject, room = roomPart))
                }
            }
        }
        
        return if (subgroups.isEmpty()) {
            val cleanedRoom = room.replace(Regex("""\s*\([^)]+\)"""), "").trim()
            listOf(Subgroup(subject = subject, room = cleanedRoom))
        } else {
            subgroups
        }
    }
    
    /**
     * Разделяет строку с аудиториями по разделителям (пробелы, запятые)
     * Возвращает список из expectedCount элементов
     */
    private fun splitRoomsByDelimiters(rooms: String, expectedCount: Int): List<String> {
        // Сначала пробуем разделить по паттерну "номер (буква)" - например "503 (к)501 (к)"
        // Обновленный паттерн: ищем слова содержащие цифры, опционально с уточнением в скобках
        // Это позволяет находить "2-105", "703", "2-105 (к)" и т.д.
        val roomPattern = Regex("""([^\s(,]*\d+[^\s(,]*(\s*\([^)]+\))?)""")
        val matches = roomPattern.findAll(rooms).toList()
        
        if (matches.isNotEmpty()) {
            // Убираем скобки с буквами из каждой аудитории
            val parts = matches.map { 
                it.value.trim().replace(Regex("""\s*\([^)]+\)"""), "")
            }
            
            // Если количество совпадает, возвращаем как есть
            if (parts.size == expectedCount) {
                return parts
            }
            
            // Если частей меньше чем нужно, дублируем последнюю
            if (parts.size < expectedCount) {
                val result = parts.toMutableList()
                val lastRoom = parts.lastOrNull() ?: rooms.replace(Regex("""\s*\([^)]+\)"""), "")
                while (result.size < expectedCount) {
                    result.add(lastRoom)
                }
                return result
            }
            
            // Если частей больше, берем первые expectedCount
            return parts.take(expectedCount)
        }
        
        // Если паттерн не найден, пробуем разделить по запятым
        val commaParts = rooms.split(",")
            .map { it.trim().replace(Regex("""\s*\([^)]+\)"""), "") }
            .filter { it.isNotEmpty() }
        
        if (commaParts.size >= expectedCount) {
            return commaParts.take(expectedCount)
        }
        
        // Если запятых нет, пробуем по пробелам (но только если нет скобок)
        if (!rooms.contains("(")) {
            val spaceParts = rooms.split(Regex("""\s+"""))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            if (spaceParts.size == expectedCount) {
                return spaceParts
            }
        }
        
        // Если ничего не подошло, убираем скобки и дублируем всю строку
        val cleanedRoom = rooms.replace(Regex("""\s*\([^)]+\)"""), "")
        return List(expectedCount) { cleanedRoom }
    }
    
    /**
     * Разбивает текст на части по маркерам подгрупп
     */
    private fun splitBySubgroupMarkers(text: String, matches: List<MatchResult>): List<String> {
        if (matches.isEmpty()) return listOf(text)
        
        val parts = mutableListOf<String>()
        
        for (i in matches.indices) {
            val currentMatch = matches[i]
            val nextMatch = matches.getOrNull(i + 1)
            
            // Начало текущей подгруппы (после маркера "1.", "2." и т.д.)
            val start = currentMatch.range.last + 1
            
            // Конец текущей подгруппы (до следующего маркера или до конца строки)
            val end = nextMatch?.range?.first ?: text.length
            
            // Извлекаем текст подгруппы
            val part = text.substring(start, end).trim()
            parts.add(part)
        }
        
        return parts
    }
    
    /**
     * Разворачивает строку таблицы с учётом colspan
     * Аналог expand_row из Python-кода
     */
    private fun expandRow(cells: List<Element>): List<String> {
        val expanded = mutableListOf<String>()
        
        for (cell in cells) {
            val text = cell.text().trim()
            val colspan = cell.attr("colspan").toIntOrNull() ?: 1
            
            // Добавляем текст ячейки colspan раз
            repeat(colspan) {
                expanded.add(text)
            }
        }
        
        return expanded
    }
    
    /**
     * Проверяет, является ли текст валидным занятием
     * (не пустой, не прочерк)
     */
    private fun isValidLesson(text: String): Boolean {
        if (text.isEmpty()) return false
        if (text == "-" || text == "—") return false
        return true
    }
}