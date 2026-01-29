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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

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
 * Навигационные маршруты
 */
sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Главная")
    object Calls : Screen("calls", "Звонки")
    object Settings : Screen("settings", "Настройки")
}

/**
 * Главный экран приложения с навигацией
 */
@Composable
fun ScheduleApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    // Поднимаем состояние на уровень ScheduleApp, чтобы оно сохранялось при переключении вкладок
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
                    val html = withContext(Dispatchers.IO) {
                        fetcher.fetchScheduleHtml(savedGroup)
                    }
                    val parsedSchedule = withContext(Dispatchers.Default) {
                        parser.parse(html, savedGroup)
                    }
                    schedule = parsedSchedule
                } catch (e: NoInternetException) {
                    errorMessage = e.message
                } catch (e: GroupNotFoundException) {
                    errorMessage = e.message
                } catch (e: ServerErrorException) {
                    errorMessage = e.message
                } catch (e: Exception) {
                    errorMessage = "Неизвестная ошибка: ${e.message}"
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
                val html = withContext(Dispatchers.IO) {
                    fetcher.fetchScheduleHtml(groupInput)
                }
                val parsedSchedule = withContext(Dispatchers.Default) {
                    parser.parse(html, groupInput)
                }
                schedule = parsedSchedule
                preferencesManager.saveLastGroup(groupInput)
            } catch (e: NoInternetException) {
                errorMessage = e.message
            } catch (e: GroupNotFoundException) {
                errorMessage = e.message
            } catch (e: ServerErrorException) {
                errorMessage = e.message
            } catch (e: Exception) {
                errorMessage = "Неизвестная ошибка: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                NavigationBarItem(
                    icon = { 
                        Icon(
                            if (currentDestination?.route == Screen.Calls.route) 
                                Icons.Filled.Notifications 
                            else 
                                Icons.Outlined.Notifications,
                            contentDescription = Screen.Calls.title
                        ) 
                    },
                    label = { Text(Screen.Calls.title, fontFamily = CustomFont) },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Calls.route } == true,
                    onClick = {
                        navController.navigate(Screen.Calls.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                NavigationBarItem(
                    icon = { 
                        Icon(
                            if (currentDestination?.route == Screen.Home.route) 
                                Icons.Filled.Home 
                            else 
                                Icons.Outlined.Home,
                            contentDescription = Screen.Home.title
                        ) 
                    },
                    label = { Text(Screen.Home.title, fontFamily = CustomFont) },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true,
                    onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                NavigationBarItem(
                    icon = { 
                        Icon(
                            if (currentDestination?.route == Screen.Settings.route) 
                                Icons.Filled.Settings 
                            else 
                                Icons.Outlined.Settings,
                            contentDescription = Screen.Settings.title
                        ) 
                    },
                    label = { Text(Screen.Settings.title, fontFamily = CustomFont) },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Settings.route } == true,
                    onClick = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(
                route = Screen.Calls.route,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            ) {
                CallsScreen()
            }
            composable(
                route = Screen.Home.route,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { 
                            when (initialState.destination.route) {
                                Screen.Calls.route -> it  // Звонки -> Главная: приезжает справа
                                Screen.Settings.route -> -it  // Настройки -> Главная: приезжает слева
                                else -> 0
                            }
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { 
                            when (targetState.destination.route) {
                                Screen.Calls.route -> it  // Главная -> Звонки: уезжает вправо
                                Screen.Settings.route -> -it  // Главная -> Настройки: уезжает влево
                                else -> 0
                            }
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            ) {
                HomeScreen(
                    groupInput = groupInput,
                    onGroupInputChange = { groupInput = it },
                    schedule = schedule,
                    errorMessage = errorMessage,
                    isLoading = isLoading,
                    loadSchedule = loadSchedule
                )
            }
            composable(
                route = Screen.Settings.route,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            ) {
                SettingsScreen()
            }
        }
    }
}

/**
 * Экран главной (текущее расписание)
 */
@Composable
fun HomeScreen(
    groupInput: String,
    onGroupInputChange: (String) -> Unit,
    schedule: Schedule?,
    errorMessage: String?,
    isLoading: Boolean,
    loadSchedule: () -> Unit
) {
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
                        onValueChange = onGroupInputChange,
                        placeholder = { 
                            Text(
                                "Группа", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = CustomFont,
                                fontSize = 19.sp
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
                            fontSize = 19.sp
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
                                imageVector = if (schedule != null) Icons.Default.Refresh else Icons.Default.Search,
                                contentDescription = if (schedule != null) "Обновить" else "Поиск",
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
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp,
                                    fontFamily = CustomFont,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(24.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
 * Экран звонков
 */
@Composable
fun CallsScreen() {
    val calendar = Calendar.getInstance()
    val isSaturday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
    val schedule = if (isSaturday) SATURDAY_SCHEDULE else WEEKDAY_SCHEDULE
    val dayName = if (isSaturday) "Суббота" else "Будни"
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Шапка
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Расписание звонков",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = CustomFont,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = dayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = CustomFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            
            // Список звонков
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(schedule.size) { index ->
                    CallItem(
                        lessonNumber = index + 1,
                        callTime = schedule[index]
                    )
                }
            }
        }
    }
}

/**
 * Элемент звонка
 */
@Composable
fun CallItem(lessonNumber: Int, callTime: CallTime) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Номер пары
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = lessonNumber.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = CustomFont,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        // Время
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${callTime.firstStart} - ${callTime.firstEnd}  |  ${callTime.secondStart} - ${callTime.secondEnd}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = CustomFont,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Пара $lessonNumber",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = CustomFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Экран настроек (заглушка)
 */
@Composable
fun SettingsScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Шапка
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Настройки",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = CustomFont,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            
            // Контент
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Скоро здесь появятся настройки",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = CustomFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}

/**
 * Список расписания (scrollable)
 */
@Composable
fun ScheduleList(schedule: Schedule) {
    // Находим индекс текущего дня
    val displayIndex = remember(schedule) {
        findTodayIndex(schedule.days)
    }
    
    // Определяем, показываем ли мы завтра
    val showingTomorrow = remember(schedule, displayIndex) {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        val todayString = dateFormat.format(today.time)
        
        val todayIndex = schedule.days.indexOfFirst { day ->
            val datePart = day.dayDate.substringAfter(", ").trim()
            datePart == todayString
        }
        
        // Если displayIndex != todayIndex, значит показываем завтра
        todayIndex >= 0 && displayIndex > todayIndex
    }
    
    // Создаем listState с начальной позицией на текущем дне
    val listState = rememberLazyListState()
    
    // Прокручиваем к текущему дню после загрузки
    LaunchedEffect(displayIndex) {
        if (displayIndex >= 0) {
            // Используем offset для небольшого отступа от навбара
            listState.scrollToItem(displayIndex, scrollOffset = 30)
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(schedule.days.size) { index ->
            val day = schedule.days[index]
            val isToday = index == displayIndex && !showingTomorrow
            val isTomorrow = index == displayIndex && showingTomorrow
            
            // Добавляем отступ сверху для текущего дня, чтобы предыдущий не был виден
            if (isToday || isTomorrow) {
                Spacer(modifier = Modifier.height(0.dp))
            }
            
            DayScheduleItem(day = day, isToday = isToday, isTomorrow = isTomorrow)
        }
    }
}

/**
 * Расписание звонков для будних дней (с делением на части)
 */
val WEEKDAY_SCHEDULE = listOf(
    CallTime("09:00", "09:45", "09:55", "10:40"),
    CallTime("10:50", "11:35", "11:55", "12:40"),
    CallTime("13:00", "13:45", "13:55", "14:40"),
    CallTime("14:50", "15:35", "15:45", "16:30"),
    CallTime("16:40", "17:25", "17:35", "18:20"),
    CallTime("18:30", "19:15", "19:25", "20:10")
)

/**
 * Расписание звонков для субботы (с делением на части)
 */
val SATURDAY_SCHEDULE = listOf(
    CallTime("09:00", "09:45", "09:55", "10:40"),
    CallTime("10:50", "11:35", "11:55", "12:40"),
    CallTime("12:50", "13:35", "13:45", "14:30"),
    CallTime("14:40", "15:25", "15:35", "16:20"),
    CallTime("16:30", "17:15", "17:25", "18:10"),
    CallTime("18:20", "19:05", "19:15", "20:00")
)

/**
 * Модель времени звонка
 */
data class CallTime(
    val firstStart: String,
    val firstEnd: String,
    val secondStart: String,
    val secondEnd: String
)

/**
 * Проверяет, закончились ли все пары на сегодня
 */
fun areClassesFinishedForToday(day: DaySchedule): Boolean {
    if (day.lessons.isEmpty()) return true
    
    val now = Calendar.getInstance()
    val currentTime = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    
    // Определяем, суббота ли сегодня
    val isSaturday = now.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
    val schedule = if (isSaturday) SATURDAY_SCHEDULE else WEEKDAY_SCHEDULE
    
    // Находим последнюю пару
    val lastLesson = day.lessons.maxByOrNull { it.lessonNumber.toIntOrNull() ?: 0 } ?: return true
    val lastLessonNumber = lastLesson.lessonNumber.toIntOrNull() ?: return true
    
    // Проверяем индекс (номер пары - 1)
    if (lastLessonNumber < 1 || lastLessonNumber > schedule.size) return true
    
    // Получаем время окончания последней пары (вторая часть)
    val endTime = schedule[lastLessonNumber - 1].secondEnd
    val endParts = endTime.split(":")
    val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
    
    return currentTime >= endMinutes
}

/**
 * Находит индекс текущего дня в списке расписания
 * Если пары на сегодня закончились, возвращает индекс завтрашнего дня
 */
fun findTodayIndex(days: List<DaySchedule>): Int {
    val today = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
    val todayString = dateFormat.format(today.time)
    
    // Находим индекс сегодняшнего дня
    val todayIndex = days.indexOfFirst { day ->
        val datePart = day.dayDate.substringAfter(", ").trim()
        datePart == todayString
    }
    
    // Если сегодняшний день найден, проверяем, закончились ли пары
    if (todayIndex >= 0) {
        val todaySchedule = days[todayIndex]
        if (areClassesFinishedForToday(todaySchedule)) {
            // Пары закончились, возвращаем следующий день
            return if (todayIndex + 1 < days.size) todayIndex + 1 else todayIndex
        }
    }
    
    return todayIndex
}

/**
 * Расписание на один день
 */
@Composable
fun DayScheduleItem(day: DaySchedule, isToday: Boolean = false, isTomorrow: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isToday || isTomorrow) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else 
                    MaterialTheme.colorScheme.surface
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = day.dayDate,
                fontSize = 21.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = CustomFont,
                color = if (isToday || isTomorrow) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            if (isToday) {
                Text(
                    text = "Сегодня",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = CustomFont,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            if (isTomorrow) {
                Text(
                    text = "Завтра",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = CustomFont,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
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
