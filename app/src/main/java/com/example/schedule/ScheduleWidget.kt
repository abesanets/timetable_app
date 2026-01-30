package com.example.schedule

import android.content.Context
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.material3.ColorProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ScheduleWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = loadWidgetData(context)
        
        provideContent {
            // Используем динамические цвета Material You как в приложении
            val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                GlanceTheme.colors // Динамические цвета из системы (автоматически светлые/темные)
            } else {
                // Fallback для старых версий Android
                ColorProviders(
                    light = androidx.compose.material3.lightColorScheme(
                        primary = androidx.compose.ui.graphics.Color(0xFF6200EE),
                        secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
                        tertiary = androidx.compose.ui.graphics.Color(0xFF3700B3),
                        background = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
                        surface = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
                        onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                        onSecondary = androidx.compose.ui.graphics.Color(0xFF000000),
                        onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
                        onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F)
                    ),
                    dark = androidx.compose.material3.darkColorScheme(
                        primary = androidx.compose.ui.graphics.Color(0xFFBB86FC),
                        secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
                        tertiary = androidx.compose.ui.graphics.Color(0xFF3700B3),
                        background = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
                        surface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
                        onPrimary = androidx.compose.ui.graphics.Color(0xFF000000),
                        onSecondary = androidx.compose.ui.graphics.Color(0xFF000000),
                        onBackground = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
                        onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5)
                    )
                )
            }
            
            GlanceTheme(colors = colors) {
                WidgetContent(widgetData)
            }
        }
    }
}

// Удаляем старый объект WidgetColorScheme, он больше не нужен


@Composable
fun WidgetContent(widgetData: WidgetData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Заголовок с кнопкой обновления
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Расписание",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            
            // Кнопка обновления
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(16.dp)
                    .clickable(actionRunCallback<RefreshWidgetAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⟳",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = GlanceTheme.colors.onPrimaryContainer
                    )
                )
            }
        }
        
        when {
            widgetData.error != null -> {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠",
                        style = TextStyle(
                            fontSize = 36.sp,
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
            widgetData.daySchedule != null -> {
                DayScheduleWidget(widgetData.daySchedule, widgetData.dayLabel)
            }
            else -> {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📅",
                        style = TextStyle(fontSize = 36.sp)
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
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

@Composable
fun DayScheduleWidget(day: DaySchedule, dayLabel: String) {
    // Карточка дня как в приложении
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(
                if (dayLabel.isNotEmpty()) 
                    GlanceTheme.colors.secondaryContainer 
                else 
                    GlanceTheme.colors.surfaceVariant
            )
            .cornerRadius(16.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Заголовок дня
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.dayDate,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            
            if (dayLabel.isNotEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dayLabel,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onTertiaryContainer
                        )
                    )
                }
            }
        }
        
        if (day.lessons.isEmpty()) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "📅",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "Нет занятий",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        } else {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                day.lessons.take(3).forEach { lesson ->
                    LessonWidgetItem(lesson, dayLabel.isNotEmpty())
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
                
                if (day.lessons.size > 3) {
                    Text(
                        text = "Еще ${day.lessons.size - 3} пар...",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        modifier = GlanceModifier.padding(top = 2.dp, start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LessonWidgetItem(lesson: Lesson, isHighlighted: Boolean) {
    // Карточка пары как в приложении
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(
                if (isHighlighted)
                    GlanceTheme.colors.surface
                else
                    GlanceTheme.colors.surfaceVariant
            )
            .cornerRadius(12.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Номер пары
        Box(
            modifier = GlanceModifier
                .size(32.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = lesson.lessonNumber,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onPrimaryContainer
                )
            )
        }
        
        Spacer(modifier = GlanceModifier.width(8.dp))
        
        // Информация о паре
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Top
        ) {
            if (lesson.subgroups.size == 1) {
                val subgroup = lesson.subgroups[0]
                Text(
                    text = subgroup.subject,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "📍",
                        style = TextStyle(fontSize = 12.sp)
                    )
                    Spacer(modifier = GlanceModifier.width(3.dp))
                    Text(
                        text = subgroup.room,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.primary
                        )
                    )
                }
            } else {
                lesson.subgroups.take(2).forEachIndexed { index, subgroup ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "${index + 1}. ",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = subgroup.subject,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GlanceTheme.colors.onSurface
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "📍",
                                    style = TextStyle(fontSize = 10.sp)
                                )
                                Spacer(modifier = GlanceModifier.width(2.dp))
                                Text(
                                    text = subgroup.room,
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlanceTheme.colors.primary
                                    )
                                )
                            }
                        }
                    }
                    if (index < lesson.subgroups.size - 1 && index < 1) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }
                if (lesson.subgroups.size > 2) {
                    Text(
                        text = "...",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
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
        
        val displayIndex = findTodayIndex(schedule.days)
        
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
            displayIndex == todayIndex + 1 -> "Завтра"
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
}
