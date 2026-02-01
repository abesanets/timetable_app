package com.example.schedule.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.example.schedule.features.settings.ui.SettingsScreen
import com.example.schedule.ui.navigation.Screen
import com.example.schedule.ui.theme.MaterialYouTheme
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
                
                listOf(Screen.Home, Screen.Buses, Screen.Alarms, Screen.Settings).forEach { screen ->
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
                route = Screen.Alarms.route,
                enterTransition = { fadeIn(tween(400, easing = FastOutSlowInEasing)) },
                exitTransition = { fadeOut(tween(300, easing = FastOutSlowInEasing)) }
            ) {
                AlarmsScreen()
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