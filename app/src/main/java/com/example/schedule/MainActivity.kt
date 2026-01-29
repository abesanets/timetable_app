package com.example.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun Dp.toPx(): Float {
    return with(LocalDensity.current) { this@toPx.toPx() }
}

val CustomFont = FontFamily.Serif

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PremiumDarkTheme {
                ScheduleApp()
            }
        }
    }
}

@Composable
fun PremiumDarkTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFFFFFFFF),
        onPrimary = Color(0xFF0A0A0A),
        secondary = Color(0xFFE8E8E8),
        onSecondary = Color(0xFF0A0A0A),
        background = Color(0xFF0A0A0A),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF141414),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF1E1E1E),
        onSurfaceVariant = Color(0xFFB8B8B8),
        outline = Color(0xFF2A2A2A),
        error = Color(0xFFFF6B6B)
    )
    
    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = Typography(
            displayLarge = MaterialTheme.typography.displayLarge.copy(
                fontFamily = CustomFont,
                fontWeight = FontWeight.Normal
            ),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = CustomFont,
                fontWeight = FontWeight.Normal
            ),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = CustomFont,
                fontWeight = FontWeight.Normal
            ),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = CustomFont,
                fontWeight = FontWeight.Normal
            )
        ),
        content = content
    )
}

/**
 * Главный экран приложения
 */
@Composable
fun ScheduleApp() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    var groupInput by remember { mutableStateOf("88") }
    var schedule by remember { mutableStateOf<Schedule?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val fetcher = remember { ScheduleFetcher() }
    val parser = remember { ScheduleParser() }
    
    // Загружаем последнюю группу при запуске
    LaunchedEffect(Unit) {
        preferencesManager.lastGroup.collect { savedGroup ->
            if (savedGroup != null && savedGroup.isNotBlank()) {
                groupInput = savedGroup
                // Автоматически загружаем расписание
                isLoading = true
                errorMessage = null
                try {
                    // Выполняем загрузку в IO потоке
                    val html = withContext(Dispatchers.IO) {
                        fetcher.fetchScheduleHtml(savedGroup)
                    }
                    val parsedSchedule = withContext(Dispatchers.Default) {
                        parser.parse(html, savedGroup)
                    }
                    schedule = parsedSchedule
                } catch (e: Exception) {
                    errorMessage = "Ошибка: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }
    
    val loadSchedule: () -> Unit = {
        scope.launch {
            isLoading = true
            errorMessage = null
            schedule = null
            try {
                // Выполняем загрузку в IO потоке
                val html = withContext(Dispatchers.IO) {
                    fetcher.fetchScheduleHtml(groupInput)
                }
                val parsedSchedule = withContext(Dispatchers.Default) {
                    parser.parse(html, groupInput)
                }
                schedule = parsedSchedule
                // Сохраняем группу
                preferencesManager.saveLastGroup(groupInput)
            } catch (e: Exception) {
                errorMessage = "Ошибка: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Компактная шапка с поиском
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Расписание",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = CustomFont,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = groupInput,
                        onValueChange = { groupInput = it },
                        placeholder = { 
                            Text(
                                "Группа", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = CustomFont,
                                fontSize = 13.sp
                            ) 
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = CustomFont,
                            fontSize = 13.sp
                        )
                    )
                    
                    Button(
                        onClick = loadSchedule,
                        enabled = !isLoading && groupInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .width(56.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Поиск",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            // Контент
            Box(modifier = Modifier.fillMaxSize()) {
                // Анимация перехода между состояниями
                AnimatedContent(
                    targetState = when {
                        isLoading -> "loading"
                        errorMessage != null -> "error"
                        schedule != null -> "content"
                        else -> "empty"
                    },
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "content_transition",
                    modifier = Modifier.fillMaxSize()
                ) { state ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (state) {
                            "loading" -> {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            }
                            "error" -> {
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    fontFamily = CustomFont,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(24.dp)
                                )
                            }
                            "content" -> {
                                schedule?.let { ScheduleList(schedule = it) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Список расписания (scrollable)
 */
@Composable
fun ScheduleList(schedule: Schedule) {
    val animationPlayed = remember { mutableStateOf(false) }
    
    LaunchedEffect(schedule) {
        animationPlayed.value = false
        // Небольшая задержка перед началом анимации
        kotlinx.coroutines.delay(50)
        animationPlayed.value = true
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(schedule.days.size) { index ->
            val day = schedule.days[index]
            AnimatedDayScheduleItem(
                day = day,
                index = index,
                animationPlayed = animationPlayed.value
            )
        }
    }
}

/**
 * Анимированное расписание на один день
 */
@Composable
fun AnimatedDayScheduleItem(day: DaySchedule, index: Int, animationPlayed: Boolean) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = index * 100,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )
    
    val animatedOffset by animateDpAsState(
        targetValue = if (animationPlayed) 0.dp else 50.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
            visibilityThreshold = 1.dp
        ),
        label = "offset"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                alpha = animatedAlpha,
                translationY = animatedOffset.toPx(),
                scaleX = animatedScale,
                scaleY = animatedScale
            )
    ) {
        DayScheduleItem(day = day)
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
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = day.dayDate,
            fontSize = 21.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = CustomFont,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (day.lessons.isEmpty()) {
            Text(
                text = "Нет занятий",
                fontSize = 14.sp,
                fontFamily = CustomFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                day.lessons.forEach { lesson ->
                    LessonItem(lesson = lesson)
                }
            }
        }
    }
}

/**
 * Одно занятие (пара с подгруппами)
 */
@Composable
fun LessonItem(lesson: Lesson) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Номер пары
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = lesson.lessonNumber,
                fontSize = 23.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = CustomFont,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        // Информация о паре
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (lesson.subgroups.size == 1) {
                val subgroup = lesson.subgroups[0]
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = subgroup.subject,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = CustomFont,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subgroup.room,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Normal,
                        fontFamily = CustomFont
                    )
                }
            } else {
                lesson.subgroups.forEachIndexed { index, subgroup ->
                    Column(
                        modifier = Modifier.padding(bottom = if (index < lesson.subgroups.size - 1) 4.dp else 0.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Normal,
                                fontFamily = CustomFont,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                Text(
                                    text = subgroup.subject,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = CustomFont,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = subgroup.room,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = CustomFont
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
