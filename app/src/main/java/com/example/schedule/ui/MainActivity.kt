package com.example.schedule.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    val showOtherSubgroupInDetails by preferencesManager.showOtherSubgroupInDetails.collectAsState(initial = false)
    
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
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                            fullSchedule = schedule,
                            selectedSubgroup = selectedSubgroup,
                            showOtherSubgroupInDetails = showOtherSubgroupInDetails,
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
                    val defaultBuilding = remember(loadedGroup, groupInput) {
                        com.example.schedule.core.utils.BuildingUtils.getBuildingForGroup(loadedGroup ?: groupInput)
                    }
                    AlarmsScreen(initialBuilding = defaultBuilding)
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

        // Gradient overlay for dock legibility without harsh cutoff
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Floating Dock Bar with sliding pill & Lucide outline icons
        val screens = remember { listOf(Screen.Home, Screen.Alarms, Screen.Staff, Screen.Settings) }
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val selectedIndex = remember(currentDestination) {
            val idx = screens.indexOfFirst { screen ->
                currentDestination?.hierarchy?.any { it.route == screen.route } == true
            }
            if (idx >= 0) idx else 0
        }

        val itemWidth = 52.dp
        val itemHeight = 44.dp
        val itemSpacing = 6.dp

        val pillOffset by animateDpAsState(
            targetValue = (itemWidth + itemSpacing) * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "dock_pill_slide"
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .navigationBarsPadding()
                .height(58.dp)
                .wrapContentWidth(),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 7.dp)
                    .wrapContentSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                // Animated sliding background pill
                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .size(width = itemWidth, height = itemHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )

                // Navigation items
                Row(
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    screens.forEachIndexed { index, screen ->
                        val isSelected = index == selectedIndex
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        val iconScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.82f else if (isSelected) 1.05f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "nav_icon_scale"
                        )

                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            },
                            animationSpec = tween(durationMillis = 180),
                            label = "nav_icon_tint"
                        )

                        Box(
                            modifier = Modifier
                                .size(width = itemWidth, height = itemHeight)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = {
                                        if (!isSelected) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(screen.iconRes),
                                contentDescription = screen.title,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                                tint = iconColor
                            )
                        }
                    }
                }
            }
        }
    }
}
