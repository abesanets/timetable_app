package com.example.schedule.features.schedule.ui.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.schedule.data.models.DaySchedule
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
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val topPadding = statusBarTop + 12.dp
        val bottomPadding = navBarBottom + 80.dp
        val availableHeight = maxHeight - topPadding - bottomPadding

        val totalContentHeight = remember(schedule) {
            if (schedule.days.isEmpty()) 0.dp
            else {
                var total = 0.dp
                schedule.days.forEachIndexed { index, day ->
                    total += estimateDayHeight(day)
                    if (index > 0) total += 16.dp
                }
                total
            }
        }

        val extraSpace = remember(availableHeight, totalContentHeight) {
            maxOf(0.dp, availableHeight - totalContentHeight)
        }

        // Если все расписание меньше высоты экрана, центрируем его по вертикали через паддинги
        val dynamicTopPadding = topPadding + (extraSpace / 2)
        val dynamicBottomPadding = bottomPadding + (extraSpace / 2)

        // Нижний спейсер для возможности прокрутки последнего дня в центр экрана
        val bottomSpacerHeight = remember(availableHeight, extraSpace) {
            if (extraSpace > 0.dp) 0.dp else availableHeight / 2
        }

        // Прокрутка к активному дню с центрированием по вертикали
        LaunchedEffect(schedule, displayIndex, availableHeight, extraSpace) {
            if (displayIndex in schedule.days.indices && extraSpace <= 0.dp) {
                val targetCenter = availableHeight / 2
                val activeDayHeight = estimateDayHeight(schedule.days[displayIndex])
                val targetTop = maxOf(0.dp, targetCenter - (activeDayHeight / 2))
                
                var distanceToItem = 0.dp
                for (i in 0 until displayIndex) {
                    distanceToItem += estimateDayHeight(schedule.days[i]) + 16.dp
                }
                
                val targetScrollDp = maxOf(0.dp, distanceToItem - targetTop)
                
                var accumulated = 0.dp
                var scrollIndex = 0
                var scrollOffsetDp = 0.dp
                
                for (i in schedule.days.indices) {
                    val itemH = estimateDayHeight(schedule.days[i]) + 16.dp
                    if (accumulated + itemH > targetScrollDp) {
                        scrollIndex = i
                        scrollOffsetDp = targetScrollDp - accumulated
                        break
                    }
                    accumulated += itemH
                }
                
                val scrollOffsetPx = with(density) { scrollOffsetDp.roundToPx() }
                
                if (!hasInitialScrolled) {
                    listState.scrollToItem(scrollIndex, scrollOffsetPx)
                    hasInitialScrolled = true
                } else {
                    listState.animateScrollToItem(scrollIndex, scrollOffsetPx)
                }
                
                // Точная подгонка по пикселям после рендера элементов
                kotlinx.coroutines.delay(80)
                val visibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == displayIndex }
                if (visibleItem != null) {
                    val viewportHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                    val itemCenter = visibleItem.offset + visibleItem.size / 2
                    val viewportCenter = listState.layoutInfo.viewportStartOffset + viewportHeight / 2
                    val delta = itemCenter - viewportCenter
                    if (kotlin.math.abs(delta) > 6) {
                        listState.scrollBy(delta.toFloat())
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = dynamicTopPadding,
                bottom = dynamicBottomPadding
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

            if (bottomSpacerHeight > 0.dp) {
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(bottomSpacerHeight))
                }
            }
        }
    }
}

private fun estimateDayHeight(day: DaySchedule): Dp {
    var dayHeight = 36.dp
    if (day.lessons.isNotEmpty()) {
        dayHeight += 8.dp
        day.lessons.forEachIndexed { index, lesson ->
            val itemHeight = if (lesson.subgroups.size > 1) 78.dp else 66.dp
            dayHeight += itemHeight
            if (index > 0) dayHeight += 6.dp
        }
    }
    return dayHeight
}
