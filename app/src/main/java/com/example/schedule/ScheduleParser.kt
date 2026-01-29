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
            ?: throw Exception("Группа $group не найдена на странице")
        
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
            throw Exception("Таблица расписания для группы $group не найдена")
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
                val subgroup = Subgroup(
                    subject = subject,
                    room = room
                )
                
                // Ищем существующую пару с таким номером
                val dayLessons = daysSchedule[dayIndex].lessons as MutableList
                val existingLesson = dayLessons.find { it.lessonNumber == lessonNumber }
                
                if (existingLesson != null) {
                    // Добавляем подгруппу к существующей паре
                    val updatedSubgroups = existingLesson.subgroups.toMutableList()
                    updatedSubgroups.add(subgroup)
                    val updatedLesson = existingLesson.copy(subgroups = updatedSubgroups)
                    
                    val index = dayLessons.indexOf(existingLesson)
                    dayLessons[index] = updatedLesson
                    Log.d(TAG, "    Добавлена подгруппа к паре $lessonNumber")
                } else {
                    // Создаем новую пару
                    val lesson = Lesson(
                        lessonNumber = lessonNumber,
                        subgroups = listOf(subgroup)
                    )
                    dayLessons.add(lesson)
                    Log.d(TAG, "    Создана новая пара: $lesson")
                }
            } else {
                Log.d(TAG, "    Пропущено (пустое или прочерк)")
            }
            
            cellIndex += 2
            dayIndex++
        }
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
