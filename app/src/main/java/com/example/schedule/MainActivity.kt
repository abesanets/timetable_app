package com.example.schedule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MaterialYouTheme {
                ScheduleApp()
            }
        }
    }
}

@Composable
fun MaterialYouTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Динамические цвета Material You из системы
        if (isDarkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        // Fallback для Android 11 и ниже
        if (isDarkTheme) {
            darkColorScheme(
                primary = Color(0xFFBB86FC),
                secondary = Color(0xFF03DAC6),
                tertiary = Color(0xFF3700B3),
                background = Color(0xFF1C1B1F),
                surface = Color(0xFF1C1B1F),
                onPrimary = Color(0xFF000000),
                onSecondary = Color(0xFF000000),
                onBackground = Color(0xFFE6E1E5),
                onSurface = Color(0xFFE6E1E5)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF6200EE),
                secondary = Color(0xFF03DAC6),
                tertiary = Color(0xFF3700B3),
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFFFFBFE),
                onPrimary = Color(0xFFFFFFFF),
                onSecondary = Color(0xFF000000),
                onBackground = Color(0xFF1C1B1F),
                onSurface = Color(0xFF1C1B1F)
            )
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            displayLarge = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val iconSelected: ImageVector) {
    object Home : Screen("home", "Расписание", Icons.Outlined.Home, Icons.Filled.Home)
    object Buses : Screen("buses", "Автобусы", Icons.Outlined.LocationOn, Icons.Filled.LocationOn)
    object Calls : Screen("calls", "Звонки", Icons.Outlined.Notifications, Icons.Filled.Notifications)
    object Settings : Screen("settings", "Настройки", Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun ScheduleApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    var groupInput by remember { mutableStateOf("88") }
    var loadedGroup by remember { mutableStateOf<String?>(null) }
    var schedule by remember { mutableStateOf<Schedule?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val fetcher = remember { ScheduleFetcher() }
    val parser = remember { ScheduleParser() }
    
    LaunchedEffect(Unit) {
        preferencesManager.lastGroup.collect { savedGroup ->
            if (savedGroup != null && savedGroup.isNotBlank()) {
                groupInput = savedGroup
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
                    loadedGroup = savedGroup
                    
                    // Планируем уведомление после загрузки расписания
                    val notificationsEnabled = preferencesManager.notificationsEnabled.first()
                    if (notificationsEnabled) {
                        val manager = DailyNotificationManager(context)
                        manager.scheduleNotification(parsedSchedule)
                    }
                } catch (e: Exception) {
                    errorMessage = when (e) {
                        is NoInternetException, is GroupNotFoundException, is ServerErrorException -> e.message
                        else -> "Ошибка: ${e.message}"
                    }
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
            try {
                val html = withContext(Dispatchers.IO) {
                    fetcher.fetchScheduleHtml(groupInput)
                }
                val parsedSchedule = withContext(Dispatchers.Default) {
                    parser.parse(html, groupInput)
                }
                schedule = parsedSchedule
                loadedGroup = groupInput
                preferencesManager.saveLastGroup(groupInput)
                
                // Планируем уведомление после загрузки расписания
                val notificationsEnabled = preferencesManager.notificationsEnabled.first()
                if (notificationsEnabled) {
                    val manager = DailyNotificationManager(context)
                    manager.scheduleNotification(parsedSchedule)
                }
            } catch (e: Exception) {
                errorMessage = when (e) {
                    is NoInternetException, is GroupNotFoundException, is ServerErrorException -> e.message
                    else -> "Ошибка: ${e.message}"
                }
                schedule = null
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                listOf(Screen.Home, Screen.Buses, Screen.Calls, Screen.Settings).forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.iconSelected else screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { 
                            Text(
                                screen.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            ) 
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(
                route = Screen.Home.route,
                enterTransition = { fadeIn(tween(400, easing = FastOutSlowInEasing)) },
                exitTransition = { fadeOut(tween(300, easing = FastOutSlowInEasing)) }
            ) {
                HomeScreen(
                    groupInput = groupInput,
                    onGroupInputChange = { groupInput = it },
                    schedule = schedule,
                    errorMessage = errorMessage,
                    isLoading = isLoading,
                    loadSchedule = loadSchedule,
                    loadedGroup = loadedGroup
                )
            }
            composable(
                route = Screen.Buses.route,
                enterTransition = { fadeIn(tween(400, easing = FastOutSlowInEasing)) },
                exitTransition = { fadeOut(tween(300, easing = FastOutSlowInEasing)) }
            ) {
                BusesScreen()
            }
            composable(
                route = Screen.Calls.route,
                enterTransition = { fadeIn(tween(400, easing = FastOutSlowInEasing)) },
                exitTransition = { fadeOut(tween(300, easing = FastOutSlowInEasing)) }
            ) {
                CallsScreen()
            }
            composable(
                route = Screen.Settings.route,
                enterTransition = { fadeIn(tween(400, easing = FastOutSlowInEasing)) },
                exitTransition = { fadeOut(tween(300, easing = FastOutSlowInEasing)) }
            ) {
                SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    groupInput: String,
    onGroupInputChange: (String) -> Unit,
    schedule: Schedule?,
    errorMessage: String?,
    isLoading: Boolean,
    loadSchedule: () -> Unit,
    loadedGroup: String?
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        "Расписание",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.statusBarsPadding()
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = groupInput,
                        onValueChange = onGroupInputChange,
                        placeholder = { Text("Номер группы") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    FilledIconButton(
                        onClick = loadSchedule,
                        enabled = !isLoading && groupInput.isNotBlank(),
                        modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (groupInput != loadedGroup) Icons.Default.Search else Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
            
            AnimatedContent(
                targetState = when {
                    isLoading -> "loading"
                    errorMessage != null -> "error"
                    schedule != null -> "content"
                    else -> "empty"
                },
                transitionSpec = {
                    fadeIn(tween(500, easing = FastOutSlowInEasing)) togetherWith 
                    fadeOut(tween(400, easing = FastOutSlowInEasing))
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
                                    .size(48.dp),
                                strokeWidth = 4.dp
                            )
                        }
                        "error" -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        "content" -> {
                            schedule?.let { currentSchedule ->
                                // Используем key чтобы полностью пересоздать ScheduleList при обновлении
                                key(currentSchedule) {
                                    ScheduleList(schedule = currentSchedule)
                                }
                            }
                        }
                        "empty" -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Введите номер группы",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusesScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        "Автобусы",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.statusBarsPadding()
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Скоро здесь появится расписание автобусов",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Звонки",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            dayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.statusBarsPadding()
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = schedule.size,
                    key = { index -> index },
                    contentType = { "call" }
                ) { index ->
                    CallItem(lessonNumber = index + 1, callTime = schedule[index])
                }
            }
        }
    }
}

@Composable
fun CallItem(lessonNumber: Int, callTime: CallTime) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lessonNumber.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Пара $lessonNumber",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${callTime.firstStart} - ${callTime.firstEnd}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${callTime.secondStart} - ${callTime.secondEnd}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    val notificationsEnabled by preferencesManager.notificationsEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // После получения разрешения на уведомления, проверяем точные будильники
        if (isGranted) {
            scope.launch {
                preferencesManager.setNotificationsEnabled(true)
            }
        }
        }
    }
    
    // Диалог для запроса разрешения на точные будильники

    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        "Настройки",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.statusBarsPadding()
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Уведомления
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                val newValue = !notificationsEnabled
                                if (newValue) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        scope.launch {
                                            preferencesManager.setNotificationsEnabled(true)
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        preferencesManager.setNotificationsEnabled(false)
                                        val manager = DailyNotificationManager(context)
                                        manager.cancelNotification()
                                    }
                                }
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Уведомления",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Получать расписание на следующий день",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        scope.launch {
                                            preferencesManager.setNotificationsEnabled(true)
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        preferencesManager.setNotificationsEnabled(false)
                                        val manager = DailyNotificationManager(context)
                                        manager.cancelNotification()
                                    }
                                }
                            }
                        )
                    }
                }
                
                // Виджет
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                                    val myProvider = android.content.ComponentName(context, ScheduleWidgetReceiver::class.java)
                                    
                                    if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                        appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                    }
                                }
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Виджет на рабочий стол",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Нажмите, чтобы добавить",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Частота обновления виджета
                val widgetUpdateInterval by preferencesManager.widgetUpdateInterval.collectAsState(initial = 60L)
                val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                val componentName = android.content.ComponentName(context, ScheduleWidgetReceiver::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                val hasWidget = widgetIds.isNotEmpty()
                
                var showIntervalDialog by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = hasWidget) {
                                showIntervalDialog = true
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Частота обновления виджета",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasWidget) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Text(
                                text = if (!hasWidget) "Виджет не добавлен" else when (widgetUpdateInterval) {
                                    0L -> "Отключено"
                                    15L -> "Каждые 15 минут"
                                    30L -> "Каждые 30 минут"
                                    60L -> "Каждый час"
                                    180L -> "Каждые 3 часа"
                                    360L -> "Каждые 6 часов"
                                    else -> "$widgetUpdateInterval мин"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasWidget) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
                
                if (showIntervalDialog) {
                    AlertDialog(
                        onDismissRequest = { showIntervalDialog = false },
                        title = { Text(text = "Частота обновления") },
                        text = {
                            Column {
                                val options = listOf(
                                    0L to "Отключено (только вручную)",
                                    15L to "Каждые 15 минут",
                                    30L to "Каждые 30 минут",
                                    60L to "Каждый час",
                                    180L to "Каждые 3 часа",
                                    360L to "Каждые 6 часов"
                                )
                                
                                options.forEach { (value, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    preferencesManager.setWidgetUpdateInterval(value)
                                                    WidgetUpdateScheduler.scheduleUpdate(context, value)
                                                }
                                                showIntervalDialog = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (value == widgetUpdateInterval),
                                            onClick = null // Handled by Row clickable
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = label)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showIntervalDialog = false }) {
                                Text("Отмена")
                            }
                        }
                    )
                }

            }
        }
    }
}

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
    
    val displayIndex = remember(schedule, refreshTrigger) { findTodayIndex(schedule.days) }
    
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
    
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (displayIndex >= 0) displayIndex else 0
    )
    
    LaunchedEffect(schedule, displayIndex) {
        if (displayIndex >= 0) {
            listState.scrollToItem(displayIndex, scrollOffset = 0)
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
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

@Composable
fun DayScheduleItem(
    day: DaySchedule, 
    isToday: Boolean = false, 
    isNext: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isToday || isNext)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    modifier = Modifier.weight(1f)
                )
                
                if (isToday) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Сегодня", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
                if (isNext) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Следующий", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
            
            if (day.lessons.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Нет занятий",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    day.lessons.forEach { lesson ->
                        LessonItem(lesson = lesson, isHighlighted = isToday || isNext)
                    }
                }
            }
        }
    }
}

@Composable
fun LessonItem(lesson: Lesson, isHighlighted: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isHighlighted)
            MaterialTheme.colorScheme.surfaceContainerHighest
        else
            MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lesson.lessonNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (lesson.subgroups.size == 1) {
                    val subgroup = lesson.subgroups[0]
                    Text(
                        text = subgroup.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = subgroup.room,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    lesson.subgroups.forEachIndexed { index, subgroup ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = subgroup.subject,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Place,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = subgroup.room,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                        if (index < lesson.subgroups.size - 1) {
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                }
            }
        }
    }
}



fun areClassesFinishedForToday(day: DaySchedule): Boolean {
    if (day.lessons.isEmpty()) return true
    
    val now = Calendar.getInstance()
    val currentTime = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    
    val isSaturday = now.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
    val schedule = if (isSaturday) SATURDAY_SCHEDULE else WEEKDAY_SCHEDULE
    
    val lastLesson = day.lessons.maxByOrNull { it.lessonNumber.toIntOrNull() ?: 0 } ?: return true
    val lastLessonNumber = lastLesson.lessonNumber.toIntOrNull() ?: return true
    
    if (lastLessonNumber < 1 || lastLessonNumber > schedule.size) return true
    
    val endTime = schedule[lastLessonNumber - 1].secondEnd
    val endParts = endTime.split(":")
    val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
    
    return currentTime >= endMinutes
}

fun findTodayIndex(days: List<DaySchedule>): Int {
    val today = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
    val todayString = dateFormat.format(today.time)
    
    // Сначала ищем сегодняшний день в расписании
    val todayIndex = days.indexOfFirst { day ->
        val datePart = day.dayDate.substringAfter(", ").trim()
        datePart == todayString
    }
    
    // Если нашли сегодняшний день в расписании
    if (todayIndex >= 0) {
        val todaySchedule = days[todayIndex]
        // Если занятия на сегодня закончились, ищем следующий учебный день
        if (areClassesFinishedForToday(todaySchedule)) {
            // Ищем следующий день с занятиями, начиная с завтрашнего дня
            for (i in (todayIndex + 1) until days.size) {
                if (days[i].lessons.isNotEmpty()) {
                    return i
                }
            }
            // Если не нашли следующий день с занятиями, возвращаем сегодняшний
            return todayIndex
        }
        return todayIndex
    }
    
    // Если сегодняшний день НЕ найден в расписании (например, воскресенье)
    // Ищем ближайший будущий учебный день
    val currentDate = today.time
    
    // Ищем ближайший день в будущем с занятиями
    var nearestFutureIndex = -1
    var nearestFutureDate: Date? = null
    
    for (i in days.indices) {
        val dayDateStr = days[i].dayDate.substringAfter(", ").trim()
        try {
            val dayDate = dateFormat.parse(dayDateStr)
            if (dayDate != null && dayDate > currentDate && days[i].lessons.isNotEmpty()) {
                if (nearestFutureDate == null || dayDate < nearestFutureDate) {
                    nearestFutureDate = dayDate
                    nearestFutureIndex = i
                }
            }
        } catch (e: Exception) {
            // Игнорируем ошибки парсинга даты
        }
    }
    
    // Если нашли ближайший будущий день, возвращаем его
    if (nearestFutureIndex >= 0) {
        return nearestFutureIndex
    }
    
    // Если ничего не найдено, возвращаем первый день с занятиями
    return days.indexOfFirst { it.lessons.isNotEmpty() }.takeIf { it >= 0 } ?: 0
}
