package com.example.schedule.features.schedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.schedule.core.utils.BuildingUtils
import com.example.schedule.data.models.Lesson
import com.example.schedule.data.models.Schedule
import com.example.schedule.features.schedule.utils.ScheduleUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScheduleList(
    schedule: Schedule,
    onLessonClick: (String, Lesson) -> Unit = { _, _ -> }
) {
    // Состояние для принудительного обновления каждую минуту
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Обновляем каждую минуту для корректного отображения активного дня
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000)
            refreshTrigger++
        }
    }
    
    // Вычисляем индекс активного дня
    val displayIndex = remember(schedule, refreshTrigger) { 
        ScheduleUtils.findTodayIndex(schedule.days) 
    }
    
    // Вычисляем, показываем ли мы "следующий" день
    val showingNext = remember(schedule, displayIndex, refreshTrigger) {
        ScheduleUtils.isShowingNextDay(schedule.days, displayIndex)
    }
    
    // Используем стандартный rememberLazyListState, чтобы не терять состояние при рекомпозиции
    val listState = rememberLazyListState()
    
    // Флаг для первоначальной прокрутки
    var hasInitialScrolled by remember(schedule.group) { mutableStateOf(false) }
    
    // Прокрутка к активному дню
    LaunchedEffect(schedule, displayIndex) {
        if (displayIndex >= 0 && displayIndex < schedule.days.size) {
            // Если это первая загрузка для этой группы или индекс изменился существенно
            if (!hasInitialScrolled || listState.firstVisibleItemIndex == 0) {
                // Даем время на отрисовку списка
                kotlinx.coroutines.delay(50)
                listState.scrollToItem(displayIndex)
                hasInitialScrolled = true
            } else {
                // Если индекс изменился (например, наступил следующий день), плавно прокручиваем
                listState.animateScrollToItem(displayIndex)
            }
        }
    }
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val topPadding = statusBarTop + 12.dp
        val bottomPadding = navBarBottom + 80.dp
        val availableHeight = maxHeight - topPadding - bottomPadding

        val topSpacerHeight = remember(schedule, displayIndex, availableHeight) {
            if (displayIndex < 0 || displayIndex >= schedule.days.size) {
                0.dp
            } else {
                var contentHeight = 0.dp
                for (i in displayIndex until schedule.days.size) {
                    val day = schedule.days[i]
                    var dayHeight = 36.dp
                    if (day.lessons.isNotEmpty()) {
                        dayHeight += 6.dp
                        day.lessons.forEachIndexed { index, lesson ->
                            val itemHeight = if (lesson.subgroups.size > 1) 76.dp else 64.dp
                            dayHeight += itemHeight
                            if (index > 0) dayHeight += 6.dp
                        }
                    }
                    contentHeight += dayHeight
                    if (i > displayIndex) contentHeight += 16.dp
                }
                maxOf(0.dp, availableHeight - contentHeight)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = topPadding,
                bottom = bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = schedule.days,
                key = { _, day -> day.dayDate },
                contentType = { _, _ -> "day" }
            ) { index, day ->
                // Определяем, является ли этот день сегодняшним для подсветки
                val today = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
                val todayString = dateFormat.format(today.time)
                
                // Извлекаем чистую дату для сравнения
                val dateRegex = Regex("""\d{2}\.\d{2}\.\d{4}""")
                val dayDateStr = dateRegex.find(day.dayDate)?.value ?: day.dayDate.substringAfter(",").trim()
                
                val isDayToday = dayDateStr == todayString
                
                val isToday = index == displayIndex && isDayToday && !showingNext
                val isNext = index == displayIndex && showingNext
                
                DayScheduleItem(
                    day = day, 
                    isToday = isToday, 
                    isNext = isNext,
                    modifier = Modifier.graphicsLayer(),
                    onLessonClick = { lesson -> onLessonClick(day.dayDate, lesson) }
                )
            }

            if (topSpacerHeight > 0.dp) {
                item(key = "top_spacer") {
                    Spacer(modifier = Modifier.height(topSpacerHeight))
                }
            }
        }
    }
}
