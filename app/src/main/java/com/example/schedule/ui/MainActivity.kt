package com.example.schedule.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.schedule.R
import com.example.schedule.core.utils.TextUtils
import com.example.schedule.features.staff.data.StaffData
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

@OptIn(ExperimentalLayoutApi::class)
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

        val isImeVisible = WindowInsets.isImeVisible
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val dockBottomPadding = maxOf(navBarBottom + 8.dp, imeBottom + 12.dp)

        // Gradient overlay for dock legibility without harsh cutoff
        AnimatedVisibility(
            visible = !isImeVisible,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
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
        }

        // Smart Transforming Bottom Dock Bar
        val screens = remember { listOf(Screen.Home, Screen.Alarms, Screen.Staff, Screen.Settings) }
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val selectedIndex = remember(currentDestination) {
            val idx = screens.indexOfFirst { screen ->
                currentDestination?.hierarchy?.any { it.route == screen.route } == true
            }
            if (idx >= 0) idx else 0
        }
        val isHome = selectedIndex == 0

        val focusManager = LocalFocusManager.current
        var isSearchFocused by remember { mutableStateOf(false) }
        LaunchedEffect(isImeVisible) {
            if (!isImeVisible) {
                focusManager.clearFocus()
            }
        }

        val allStaffNames = remember { 
            (StaffData.administration + StaffData.teachers + StaffData.employees)
                .map { it.fullName }
                .distinct()
                .sorted()
        }
        
        val suggestions = remember(groupInput) {
            if (groupInput.length >= 2 && !groupInput.any { it.isDigit() }) {
                allStaffNames.filter { it.contains(groupInput, ignoreCase = true) }.take(5)
            } else {
                emptyList()
            }
        }
        
        var showSuggestions by remember { mutableStateOf(false) }
        LaunchedEffect(suggestions) {
            showSuggestions = suggestions.isNotEmpty()
        }

        val itemWidth = 50.dp
        val itemHeight = 44.dp
        val itemSpacing = 6.dp

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = dockBottomPadding),
            contentAlignment = Alignment.Center
        ) {
            val maxDockWidth = maxWidth - 32.dp
            val compactDockWidth = (itemWidth * screens.size) + (itemSpacing * (screens.size - 1)) + 16.dp

            val dockWidth by animateDpAsState(
                targetValue = if (isHome) maxDockWidth else compactDockWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "dock_width_spring"
            )

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
                    .width(dockWidth)
                    .height(58.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Animated sliding background pill for screens other than Home
                    AnimatedVisibility(
                        visible = !isHome,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150))
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = pillOffset)
                                .size(width = itemWidth, height = itemHeight)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left section: Search bar on Home, Home icon on other screens
                        Box(
                            modifier = Modifier
                                .height(itemHeight)
                                .then(
                                    if (isHome) Modifier.weight(1f)
                                    else Modifier.width(itemWidth)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = isHome,
                                transitionSpec = {
                                    if (targetState) {
                                        (fadeIn(tween(220, delayMillis = 60)) + scaleIn(tween(220, delayMillis = 60), initialScale = 0.9f))
                                            .togetherWith(fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.9f))
                                    } else {
                                        (fadeIn(tween(180, delayMillis = 60)) + scaleIn(tween(180, delayMillis = 60), initialScale = 0.9f))
                                            .togetherWith(fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.9f))
                                    }
                                },
                                label = "home_search_transition"
                            ) { onHome ->
                                if (onHome) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                CircleShape
                                            )
                                            .padding(start = 16.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            BasicTextField(
                                                value = groupInput,
                                                onValueChange = { groupInput = it },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .onFocusChanged { isSearchFocused = it.isFocused },
                                                enabled = !isLoading,
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                                keyboardActions = KeyboardActions(
                                                    onSearch = {
                                                        focusManager.clearFocus()
                                                        loadSchedule(null)
                                                    }
                                                ),
                                                decorationBox = { innerTextField ->
                                                    if (groupInput.isEmpty()) {
                                                        Text(
                                                            "Группа или преподаватель",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                            maxLines = 1
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            )

                                            MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
                                                DropdownMenu(
                                                    expanded = showSuggestions,
                                                    onDismissRequest = { showSuggestions = false },
                                                    modifier = Modifier.widthIn(min = 220.dp, max = 280.dp),
                                                    properties = PopupProperties(focusable = false)
                                                ) {
                                                    suggestions.forEach { name ->
                                                        DropdownMenuItem(
                                                            text = { Text(name, style = MaterialTheme.typography.bodyMedium) },
                                                            onClick = {
                                                                val shortName = TextUtils.toShortName(name)
                                                                groupInput = shortName
                                                                showSuggestions = false
                                                                focusManager.clearFocus()
                                                                loadSchedule(shortName)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (groupInput.isNotEmpty() && isSearchFocused) {
                                            IconButton(
                                                onClick = { groupInput = "" },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Clear,
                                                    contentDescription = "Очистить",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        val isSearch = groupInput != loadedGroup
                                        val btnInteractionSource = remember { MutableInteractionSource() }
                                        val isBtnPressed by btnInteractionSource.collectIsPressedAsState()
                                        val btnScale by animateFloatAsState(
                                            targetValue = if (isBtnPressed) 0.86f else 1.0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            ),
                                            label = "btn_scale"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .clickable(
                                                    interactionSource = btnInteractionSource,
                                                    indication = null,
                                                    enabled = !isLoading && groupInput.isNotBlank(),
                                                    onClick = {
                                                        focusManager.clearFocus()
                                                        loadSchedule(null)
                                                    }
                                                )
                                                .graphicsLayer {
                                                    scaleX = btnScale
                                                    scaleY = btnScale
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            } else {
                                                Icon(
                                                    painter = painterResource(
                                                        if (isSearch) R.drawable.ic_nav_search else R.drawable.ic_nav_refresh
                                                    ),
                                                    contentDescription = if (isSearch) "Поиск" else "Обновить",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val homeScreen = Screen.Home
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val iconScale by animateFloatAsState(
                                        targetValue = if (isPressed) 0.82f else 1.0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "home_icon_scale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(width = itemWidth, height = itemHeight)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null,
                                                onClick = {
                                                    navController.navigate(homeScreen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(homeScreen.iconRes),
                                            contentDescription = homeScreen.title,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }
                        }

                        // Right section: Alarms, Staff, Settings (hides when search is active to give full width to input)
                        val remainingScreens = remember { listOf(Screen.Alarms, Screen.Staff, Screen.Settings) }
                        val isSearchActive = isHome && (isSearchFocused || isImeVisible)

                        AnimatedVisibility(
                            visible = !isSearchActive,
                            enter = fadeIn(tween(180, delayMillis = 60)) + expandHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                expandFrom = Alignment.End
                            ),
                            exit = fadeOut(tween(120)) + shrinkHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                shrinkTowards = Alignment.End
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                remainingScreens.forEachIndexed { relIndex, screen ->
                                    val absIndex = relIndex + 1
                                    val isSelected = absIndex == selectedIndex
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()

                                    val iconScale by animateFloatAsState(
                                        targetValue = if (isPressed) 0.82f else if (isSelected) 1.05f else 1.0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "nav_icon_scale_$absIndex"
                                    )

                                    val iconColor by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                        },
                                        animationSpec = tween(durationMillis = 180),
                                        label = "nav_icon_tint_$absIndex"
                                    )

                                    Spacer(Modifier.width(itemSpacing))

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
        }
    }
}
