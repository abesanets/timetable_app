package com.example.schedule.features.schedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schedule.data.models.DaySchedule
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DayScheduleItem(
    day: DaySchedule, 
    isToday: Boolean = false, 
    isNext: Boolean = false,
    modifier: Modifier = Modifier
) {
    val statusLabel = remember(day.dayDate, isToday, isNext) {
        when {
            isToday -> "Сегодня"
            isNext -> {
                try {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
                    val dateStr = day.dayDate.substringAfter(", ").trim()
                    val dayDate = dateFormat.parse(dateStr)
                    
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    val target = Calendar.getInstance().apply {
                        time = dayDate ?: return@remember "Следующий"
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    val diffInDays = ((target.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                    
                    when (diffInDays) {
                        1 -> "Завтра"
                        2 -> "Послезавтра"
                        else -> "Следующий"
                    }
                } catch (e: Exception) {
                    "Следующий"
                }
            }
            else -> null
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isToday || isNext)
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = day.dayDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                )
                
                statusLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            
            if (day.lessons.isEmpty()) {
                // ... (no changes here, keeping compact)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { // Уменьшили с 12.dp
                    day.lessons.forEach { lesson ->
                        LessonItem(lesson = lesson, isHighlighted = isToday || isNext)
                    }
                }
            }
        }
    }
}