package com.example.schedule.features.widget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import androidx.compose.ui.graphics.Color
import java.util.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.schedule.data.models.DaySchedule
import com.example.schedule.data.models.Lesson
import com.example.schedule.data.network.ScheduleFetcher
import com.example.schedule.data.network.ScheduleParser
import com.example.schedule.data.preferences.PreferencesManager
import com.example.schedule.features.schedule.utils.ScheduleUtils
import com.example.schedule.features.widget.actions.RefreshWidgetAction
import com.example.schedule.features.widget.utils.WidgetUtils
import com.example.schedule.features.widget.worker.WidgetUpdateScheduler
import com.example.schedule.ui.MainActivity

class ScheduleWidget : GlanceAppWidget() {
    
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val isLoading = prefs[booleanPreferencesKey("is_loading")] ?: false
            val cachedJson = prefs[stringPreferencesKey("cached_data")]
            val errorMessage = prefs[stringPreferencesKey("error_message")]
            val dayLabel = prefs[stringPreferencesKey("day_label")] ?: ""
            
            // Try to load from cache first
            var initialData: WidgetData? = null
            if (cachedJson != null) {
                val schedule = WidgetUtils.dayScheduleFromJson(cachedJson)
                if (schedule != null) {
                    initialData = WidgetData(daySchedule = schedule, dayLabel = dayLabel)
                }
            }
            
            // State to hold the data, initialized with cached data
            var widgetData by remember { mutableStateOf(initialData) }
            
            // Fallback: If no data and not loading (first run), try to load asynchronously
            LaunchedEffect(Unit) {
                if (widgetData == null && !isLoading) {
                     val loadedData = loadWidgetData(context)
                     // Update state to trigger recomposition
                     if (loadedData.daySchedule != null) {
                         widgetData = loadedData
                     } else {
                         widgetData = WidgetData(error = loadedData.error ?: errorMessage ?: "Нет данных")
                     }
                }
            }
            
            // Final data to display
            val finalData = widgetData ?: WidgetData(error = errorMessage ?: "Нет данных")

            GlanceTheme {
                WidgetContent(finalData, isLoading)
            }
        }
    }
}

@Composable
fun WidgetContent(widgetData: WidgetData, isLoading: Boolean) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Основной контент - убираем главный padding для эффекта edge-to-edge при скролле
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .clickable(actionStartActivity<MainActivity>())
        ) {
        when {
            widgetData.error != null -> {
                item {
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠",
                            style = TextStyle(
                                fontSize = 24.sp,
                                color = GlanceTheme.colors.error
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = widgetData.error,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }
            widgetData.daySchedule != null -> {
                item {
                    DayScheduleWidget(widgetData.daySchedule)
                }
                // Добавляем пустой элемент в конце для небольшого отступа снизу
                item {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
            }
            else -> {
                item {
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().padding(32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📅",
                            style = TextStyle(fontSize = 32.sp)
                        )
                        Text(
                            text = "Нет данных",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }

    // Оверлей загрузки
    if (isLoading) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0x66000000)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.surface)
                    .cornerRadius(16.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "...",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
  }
}

@Composable
fun DayScheduleWidget(day: DaySchedule) {
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Заголовок дня - добавляем padding только здесь
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.dayDate,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable(actionRunCallback<RefreshWidgetAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Обновить",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSecondaryContainer
                    )
                )
            }
        }
        
        if (day.lessons.isEmpty()) {
            Box(modifier = GlanceModifier.padding(horizontal = 8.dp)) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(24.dp)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(text = "📅", style = TextStyle(fontSize = 14.sp))
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "Нет занятий",
                        style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                    )
                }
            }
        } else {
            Column(
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                day.lessons.forEach { lesson ->
                    LessonWidgetItem(lesson)
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun LessonWidgetItem(lesson: Lesson) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(24.dp)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        // Уменьшаем размер номера пары
        Box(
            modifier = GlanceModifier
                .size(32.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = lesson.lessonNumber,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSecondaryContainer
                )
            )
        }
        
        Spacer(modifier = GlanceModifier.width(10.dp))
        
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (lesson.subgroups.size == 1) {
                val subgroup = lesson.subgroups[0]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subgroup.subject,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Box(
                        modifier = GlanceModifier
                            .background(GlanceTheme.colors.secondaryContainer)
                            .cornerRadius(12.dp)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = subgroup.room,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSecondaryContainer
                            )
                        )
                    }
                }
            } else {
                lesson.subgroups.forEachIndexed { index, subgroup ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = subgroup.subject,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlanceTheme.colors.onSurface
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Box(
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.secondaryContainer)
                                .cornerRadius(10.dp)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = subgroup.room,
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlanceTheme.colors.onSecondaryContainer
                                )
                            )
                        }
                    }
                    if (index < lesson.subgroups.size - 1) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                    }
                }
            }
        }
    }
}

data class WidgetData(
    val daySchedule: DaySchedule? = null,
    val dayLabel: String = "",
    val error: String? = null
)

suspend fun loadWidgetData(context: Context): WidgetData {
    return try {
        val preferencesManager = PreferencesManager(context)
        val savedGroup = preferencesManager.lastGroup.first()
        
        if (savedGroup.isNullOrBlank()) {
            return WidgetData(error = "Группа не выбрана")
        }
        
        val fetcher = ScheduleFetcher()
        val parser = ScheduleParser()
        
        val html = withContext(Dispatchers.IO) {
            fetcher.fetchScheduleHtml(savedGroup)
        }
        
        val schedule = withContext(Dispatchers.Default) {
            parser.parse(html, savedGroup)
        }
        
        val displayIndex = ScheduleUtils.findTodayIndex(schedule.days)
        
        if (displayIndex < 0 || displayIndex >= schedule.days.size) {
            return WidgetData(error = "Нет расписания")
        }
        
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        val todayString = dateFormat.format(today.time)
        
        val todayIndex = schedule.days.indexOfFirst { day ->
            val datePart = day.dayDate.substringAfter(", ").trim()
            datePart == todayString
        }
        
        val dayLabel = when {
            displayIndex == todayIndex -> "Сегодня"
            displayIndex > todayIndex -> "Следующий"
            else -> ""
        }
        
        WidgetData(
            daySchedule = schedule.days[displayIndex],
            dayLabel = dayLabel
        )
    } catch (e: Exception) {
        WidgetData(error = e.message ?: "Ошибка загрузки")
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Schedule worker with default or saved interval
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
             val prefs = PreferencesManager(context)
             val interval = prefs.widgetUpdateInterval.first()
             WidgetUpdateScheduler.scheduleUpdate(context, interval)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdateScheduler.cancelUpdate(context)
    }
}