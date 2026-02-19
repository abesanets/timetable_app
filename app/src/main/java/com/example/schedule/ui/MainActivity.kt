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
import com.example.schedule.features.schedule.ui.HomeScreen
import com.example.schedule.features.schedule.utils.ScheduleUtils
import com.example.schedule.features.settings.ui.SettingsScreen
import com.example.schedule.features.staff.ui.StaffScreen
import com.example.schedule.features.widget.ui.ScheduleWidget
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
    
    val selectedSubgroup by preferencesManager.selectedSubgroup.collectAsState(initial = 0)
    
    val filteredSchedule = remember(schedule, selectedSubgroup) {
        schedule?.let { s ->
            ScheduleUtils.filterScheduleBySubgroup(s, selectedSubgroup)
        }
    }

    LaunchedEffect(selectedSubgroup) {
        val currentSchedule = schedule ?: return@LaunchedEffect
        
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
    
    val loadSchedule: (String?) -> Unit = { overrideInput ->
        scope.launch {
            val targetInput = overrideInput ?: groupInput
            if (targetInput.isBlank()) return@launch
            
            val isNewGroup = targetInput != loadedGroup
            
            isLoading = true
            errorMessage = null
            
            try {
                val isTeacher = targetInput.contains(".")
                val html = withContext(Dispatchers.IO) {
                    if (isTeacher) fetcher.fetchTeacherScheduleHtml(targetInput)
                    else fetcher.fetchScheduleHtml(targetInput)
                }
                val parsedSchedule = withContext(Dispatchers.Default) {
                    if (isTeacher) parser.parseTeacherSchedule(html, targetInput)
                    else parser.parse(html, targetInput)
                }
                schedule = parsedSchedule
                loadedGroup = targetInput
                preferencesManager.saveLastGroup(targetInput)
                preferencesManager.saveSchedule(parsedSchedule)
            } catch (e: Exception) {
                errorMessage = when (e) {
                    is NoInternetException, is GroupNotFoundException, 
                    is TeacherNotFoundException, is ServerErrorException -> e.message
                    else -> "Ошибка: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        preferencesManager.lastSchedule.first()?.let { cachedSchedule ->
            schedule = cachedSchedule
            loadedGroup = cachedSchedule.group
            groupInput = cachedSchedule.group
        }
        
        preferencesManager.lastGroup.first()?.let { savedGroup ->
            if (savedGroup.isNotBlank()) loadSchedule(savedGroup)
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
                            loadSchedule = { loadSchedule(it) },
                            loadedGroup = loadedGroup
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
                    StaffScreen(
                        onViewScheduleClick = { teacherName ->
                            groupInput = teacherName
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            // Trigger loading after state update
                            loadSchedule(teacherName)
                        }
                    )
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
                
                listOf(Screen.Home, Screen.Alarms, Screen.Staff, Screen.Settings).forEach { screen ->
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