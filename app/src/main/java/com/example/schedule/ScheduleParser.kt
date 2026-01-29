package com.example.schedule

import android.util.Log
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
        
        // Ищем текст "Группа - XX" (пробуем разные варианты)
        val groupText = doc.getElementsContainingOwnText("Группа - $group").firstOrNull()
            ?: doc.getElementsContainingOwnText("Группа-$group").firstOrNull()
            ?: doc.getElementsContainingOwnText("Группа $group").firstOrNull()
            ?: throw GroupNotFoundException("Группа $group не найдена")
        
        Log.d(TAG, "Найден текст группы: ${groupText.text()}")
        
        // Ищем таблицу: сначала сразу после текста, потом в родителе
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
        
        if (table == null) {
            throw GroupNotFoundException("Расписание для группы $group не найдено")
        }
        
        Log.d(TAG, "Найдена таблица расписания")
        
        // Получаем все строки таблицы
        val rows = table.select("tr")
        if (rows.size < 2) {
            throw Exception("Таблица слишком маленькая")
        }
        
        // Первая строка — заголовки дней
        val headerRow = rows[0]
        val dayHeaders = parseDayHeaders(headerRow)
        
        Log.d(TAG, "Найдено дней: ${dayHeaders.size}")
        dayHeaders.forEachIndexed { index, day ->
            Log.d(TAG, "День $index: $day")
        }
        
        // Инициализируем расписание для каждого дня
        val daysSchedule = dayHeaders.map { dayName ->
            DaySchedule(dayDate = dayName, lessons = mutableListOf())
        }
        
        Log.d(TAG, "Всего строк в таблице: ${rows.size}")
        
        // Парсим строки с занятиями
        // Начинаем с индекса 2, если строка 1 содержит номера пар, иначе с индекса 1
        // Нумерация: первая строка данных = пара 1
        val dataStartIndex = 2 // Пропускаем заголовок (0) и строку с номерами (1)
        
        for (i in dataStartIndex until rows.size) {
            val row = rows[i]
            val lessonNumber = (i - dataStartIndex + 1).toString() // Пара 1, 2, 3...
            parseLessonRow(row, daysSchedule, lessonNumber)
        }
        
        // Логируем результат
        daysSchedule.forEachIndexed { index, day ->
            Log.d(TAG, "День ${day.dayDate}: ${day.lessons.size} занятий")
        }
        
        return Schedule(group = group, days = daysSchedule)
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
        // Ищем все вхождения паттерна: цифры + пробел + скобка + буквы + скобка
        val roomPattern = Regex("""(\d+\s*\([^)]+\))""")
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
