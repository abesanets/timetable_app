package com.example.schedule.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.schedule.data.models.Schedule
import com.example.schedule.data.network.*
import com.example.schedule.data.preferences.PreferencesManager
import com.example.schedule.features.alarms.ui.AlarmsScreen
import com.example.schedule.features.buses.ui.BusesScreen
import com.example.schedule.features.notifications.manager.DailyNotificationManager
import com.example.schedule.features.schedule.ui.HomeScreen
import com.example.schedule.features.schedule.utils.ScheduleUtils
import com.example.schedule.features.settings.ui.SettingsScreen
import com.example.schedule.features.staff.ui.StaffScreen
import com.example.schedule.features.widget.ui.ScheduleWidget
import com.example.schedule.ui.navigation.Screen
import com.example.schedule.ui.theme.MaterialYouTheme
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
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
    
    val selectedSubgroup by preferencesManager.selectedSubgroup.collectAsState(initial = 0)
    
    val filteredSchedule = remember(schedule, selectedSubgroup) {
        schedule?.let { s ->
            ScheduleUtils.filterScheduleBySubgroup(s, selectedSubgroup)
        }
    }

    LaunchedEffect(selectedSubgroup) {
        val currentSchedule = schedule ?: return@LaunchedEffect
        
        // Update notifications
        val notificationsEnabled = preferencesManager.notificationsEnabled.first()
        if (notificationsEnabled) {
            val manager = DailyNotificationManager(context)
            val filteredForNotification = ScheduleUtils.filterScheduleBySubgroup(currentSchedule, selectedSubgroup)
            manager.scheduleNotification(filteredForNotification)
        }
        
        // Update widget
        scope.launch {
            try {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(ScheduleWidget::class.java)
                ids.forEach { glanceId ->
                    androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
                        prefs.remove(androidx.datastore.preferences.core.stringPreferencesKey("cached_data"))
                    }
                    ScheduleWidget().update(context, glanceId)
                }
            } catch (e: Exception) {
                // Ignore widget update errors in UI
            }
        }
    }
    
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
                    val subgroup = preferencesManager.selectedSubgroup.first()
                    if (notificationsEnabled) {
                        val manager = DailyNotificationManager(context)
                        val filteredForNotification = ScheduleUtils.filterScheduleBySubgroup(parsedSchedule, subgroup)
                        manager.scheduleNotification(filteredForNotification)
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
                val subgroup = preferencesManager.selectedSubgroup.first()
                if (notificationsEnabled) {
                    val manager = DailyNotificationManager(context)
                    val filteredForNotification = ScheduleUtils.filterScheduleBySubgroup(parsedSchedule, subgroup)
                    manager.scheduleNotification(filteredForNotification)
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
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                composable(
                    route = Screen.Home.route,
                    enterTransition = { fadeIn(tween(400, easing = FastOutSlowInEasing)) },
                    exitTransition = { fadeOut(tween(300, easing = FastOutSlowInEasing)) }
                ) {
                    HomeScreen(
                        groupInput = groupInput,
                        onGroupInputChange = { groupInput = it },
                        schedule = filteredSchedule,
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
                    BusesScreen(
                        schedule = filteredSchedule,
                        preferencesManager = preferencesManager
                    )
                }
                composable(
                    route = Screen.Alarms.route,
                    enterTransition = { fadeIn(tween(400, easing = FastOutSlowInEasing)) },
                    exitTransition = { fadeOut(tween(300, easing = FastOutSlowInEasing)) }
                ) {
                    AlarmsScreen()
                }
                composable(
                    route = Screen.Staff.route,
                    enterTransition = { fadeIn(tween(400, easing = FastOutSlowInEasing)) },
                    exitTransition = { fadeOut(tween(300, easing = FastOutSlowInEasing)) }
                ) {
                    StaffScreen()
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

        // Gradient overlay for better dock legibility
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Floating Dock Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
                .height(64.dp) // Компактная высота для иконок
                .wrapContentWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                listOf(Screen.Home, Screen.Buses, Screen.Alarms, Screen.Staff, Screen.Settings).forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    
                    Box(
                        modifier = Modifier
                            .size(48.dp) // Квадратная область для симметрии
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(
                                if (selected) MaterialTheme.colorScheme.secondaryContainer 
                                else Color.Transparent
                            )
                            .clickable {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selected) screen.iconSelected else screen.icon,
                            contentDescription = screen.title,
                            modifier = Modifier.size(26.dp), // Чуть крупнее иконки, раз нет текста
                            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}