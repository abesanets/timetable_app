package com.example.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScheduleApp()
        }
    }
}

/**
 * Главный экран приложения
 */
@Composable
fun ScheduleApp() {
    // Состояния UI
    var groupInput by remember { mutableStateOf("88") }
    var schedule by remember { mutableStateOf<Schedule?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Для запуска корутин
    val scope = rememberCoroutineScope()
    
    // Экземпляры классов
    val fetcher = remember { ScheduleFetcher() }
    val parser = remember { ScheduleParser() }
    
    // UI
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Заголовок
            Text(
                text = "Расписание МГКЦТ",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Поле ввода группы
            OutlinedTextField(
                value = groupInput,
                onValueChange = { groupInput = it },
                label = { Text("Номер группы") },
                placeholder = { Text("Например: 88") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Кнопка загрузки
            Button(
                onClick = {
                    // Запускаем загрузку в корутине
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        schedule = null
                        
                        try {
                            // 1. Загружаем HTML
                            val html = fetcher.fetchScheduleHtml(groupInput)
                            
                            // 2. Парсим расписание
                            val parsedSchedule = parser.parse(html, groupInput)
                            
                            // 3. Сохраняем результат
                            schedule = parsedSchedule
                            
                        } catch (e: Exception) {
                            // Показываем ошибку
                            errorMessage = "Ошибка: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && groupInput.isNotBlank()
            ) {
                Text(if (isLoading) "Загрузка..." else "Загрузить расписание")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Индикатор загрузки
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
            
            // Сообщение об ошибке
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            // Результат расписания
            schedule?.let { scheduleData ->
                ScheduleList(schedule = scheduleData)
            }
        }
    }
}

/**
 * Список расписания (scrollable)
 */
@Composable
fun ScheduleList(schedule: Schedule) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Заголовок группы
        item {
            Text(
                text = "Группа ${schedule.group}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Расписание по дням
        items(schedule.days) { day ->
            DayScheduleItem(day = day)
        }
    }
}

/**
 * Расписание на один день
 */
@Composable
fun DayScheduleItem(day: DaySchedule) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Название дня (жирным)
        Text(
            text = day.dayDate,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        // Занятия в этот день
        if (day.lessons.isEmpty()) {
            Text(
                text = "  Нет занятий",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            day.lessons.forEach { lesson ->
                LessonItem(lesson = lesson)
            }
        }
        
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}

/**
 * Одно занятие
 */
@Composable
fun LessonItem(lesson: Lesson) {
    Text(
        text = "  Пара ${lesson.lessonNumber}: ${lesson.subject} — ${lesson.room}",
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
