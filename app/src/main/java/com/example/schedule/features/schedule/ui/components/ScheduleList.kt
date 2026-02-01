package com.example.schedule.features.schedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.schedule.data.models.Schedule
import com.example.schedule.features.schedule.utils.ScheduleUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScheduleList(schedule: Schedule) {
    // Добавляем состояние для принудительного обновления каждую минуту
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Обновляем каждую минуту для корректного отображения активного дня
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000) // 60 секунд
            refreshTrigger++
        }
    }
    
    val displayIndex = remember(schedule, refreshTrigger) { ScheduleUtils.findTodayIndex(schedule.days) }
    
    val showingNext = remember(schedule, displayIndex, refreshTrigger) {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        val todayString = dateFormat.format(today.time)
        
        // Проверяем, есть ли сегодняшний день в расписании
        val todayIndex = schedule.days.indexOfFirst { day ->
            val datePart = day.dayDate.substringAfter(", ").trim()
            datePart == todayString
        }
        
        // Если сегодняшний день найден в расписании и displayIndex указывает на другой день
        if (todayIndex >= 0 && displayIndex > todayIndex) {
            return@remember true
        }
        
        // Если сегодняшний день НЕ найден в расписании (например, воскресенье)
        // то выделенный день должен показываться как "следующий"
        if (todayIndex < 0 && displayIndex >= 0) {
            val displayDayDateStr = schedule.days[displayIndex].dayDate.substringAfter(", ").trim()
            try {
                val displayDate = dateFormat.parse(displayDayDateStr)
                val currentDate = today.time
                // Если выделенный день в будущем, показываем как "следующий"
                return@remember displayDate != null && displayDate > currentDate
            } catch (e: Exception) {
                return@remember false
            }
        }
        
        false
    }
    
    // Пересоздаем listState при изменении расписания или displayIndex
    val listState = remember(schedule, displayIndex) {
        LazyListState(
            firstVisibleItemIndex = if (displayIndex >= 0) displayIndex else 0,
            firstVisibleItemScrollOffset = 0
        )
    }
    
    // Прокручиваем к активному дню при изменении расписания или индекса
    LaunchedEffect(schedule, displayIndex) {
        if (displayIndex >= 0) {
            // Добавляем небольшую задержку для корректной прокрутки
            kotlinx.coroutines.delay(100)
            listState.animateScrollToItem(displayIndex, scrollOffset = 0)
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = schedule.days,
            key = { _, day -> day.dayDate },
            contentType = { _, _ -> "day" }
        ) { index, day ->
            // Определяем, является ли этот день сегодняшним
            val today = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
            val todayString = dateFormat.format(today.time)
            val dayDateStr = day.dayDate.substringAfter(", ").trim()
            val isDayToday = dayDateStr == todayString
            
            val isToday = index == displayIndex && isDayToday && !showingNext
            val isNext = index == displayIndex && showingNext
            
            DayScheduleItem(
                day = day, 
                isToday = isToday, 
                isNext = isNext,
                modifier = Modifier.graphicsLayer()
            )
        }
    }
}