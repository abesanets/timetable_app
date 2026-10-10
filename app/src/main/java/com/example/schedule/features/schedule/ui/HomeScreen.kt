package com.example.schedule.features.schedule.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.schedule.R
import com.example.schedule.core.utils.TextUtils
import com.example.schedule.data.models.Lesson
import com.example.schedule.data.models.Schedule
import com.example.schedule.features.schedule.ui.components.LessonDetailsSheet
import com.example.schedule.features.schedule.ui.components.ScheduleList
import com.example.schedule.features.schedule.utils.ScheduleUtils
import com.example.schedule.features.staff.data.StaffData
import com.example.schedule.features.staff.data.StaffMember
import com.example.schedule.features.staff.ui.StaffDetailContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    groupInput: String,
    onGroupInputChange: (String) -> Unit,
    schedule: Schedule?,
    fullSchedule: Schedule?,
    selectedSubgroup: Int,
    showOtherSubgroupInDetails: Boolean,
    errorMessage: String?,
    isLoading: Boolean,
    loadSchedule: (String?) -> Unit,
    loadedGroup: String?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    var selectedStaffMember by remember { mutableStateOf<StaffMember?>(null) }
    var sheetContentState by remember { mutableStateOf<SheetContent>(SheetContent.LessonDetails) }
    
    var isErrorVisible by remember(errorMessage) { mutableStateOf(errorMessage != null) }

    val focusManager = LocalFocusManager.current

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fullscreen content area (Schedule list / Loading / Error / Empty)
            AnimatedContent(
                targetState = when {
                    isLoading && schedule == null -> "loading"
                    schedule != null -> "content"
                    errorMessage != null -> "error"
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
                                    imageVector = Icons.Rounded.Close,
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
                                key("${currentSchedule.group}_${currentSchedule.days.size}_${currentSchedule.days.firstOrNull()?.dayDate}") {
                                    ScheduleList(
                                        schedule = currentSchedule,
                                        onLessonClick = { dayDate, lesson ->
                                            val originalLesson = fullSchedule
                                                ?.days
                                                ?.firstOrNull { it.dayDate == dayDate }
                                                ?.lessons
                                                ?.firstOrNull { it.lessonNumber == lesson.lessonNumber }

                                            selectedLesson = if (
                                                originalLesson != null &&
                                                ScheduleUtils.shouldShowAllSubgroupsInDetails(
                                                    lesson = originalLesson,
                                                    selectedSubgroup = selectedSubgroup,
                                                    showOtherSubgroupInDetails = showOtherSubgroupInDetails
                                                )
                                            ) {
                                                originalLesson
                                            } else {
                                                lesson
                                            }
                                            sheetContentState = SheetContent.LessonDetails
                                            showBottomSheet = true
                                        }
                                    )
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
                                    painter = painterResource(id = R.drawable.ic_nav_search),
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

            // Mirrored top gradient overlay behind floating search island
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(statusBarTop + 76.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )

            var isSearchFocused by remember { mutableStateOf(false) }

            // Floating Top Search Island (exact match to bottom dock bar dimensions)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
                    .width(242.dp)
                    .height(58.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 18.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        BasicTextField(
                            value = groupInput,
                            onValueChange = onGroupInputChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isSearchFocused = it.isFocused },
                            enabled = !isLoading,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
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
                                        "Группа или фио",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
                            DropdownMenu(
                                expanded = showSuggestions,
                                onDismissRequest = { showSuggestions = false },
                                modifier = Modifier.width(220.dp),
                                properties = PopupProperties(focusable = false)
                            ) {
                                suggestions.forEach { name ->
                                    DropdownMenuItem(
                                        text = { Text(name, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            val shortName = TextUtils.toShortName(name)
                                            onGroupInputChange(shortName)
                                            showSuggestions = false
                                            focusManager.clearFocus()
                                            loadSchedule(shortName)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (isSearchFocused && groupInput.isNotEmpty()) {
                        IconButton(
                            onClick = { onGroupInputChange("") },
                            modifier = Modifier.size(32.dp)
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
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val btnScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.86f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "refresh_btn_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable(
                                interactionSource = interactionSource,
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
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    if (isSearch) R.drawable.ic_nav_search else R.drawable.ic_nav_refresh
                                ),
                                contentDescription = if (isSearch) "Поиск" else "Обновить",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isErrorVisible && schedule != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 70.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    onClick = { isErrorVisible = false }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            val errorTitle = if (groupInput != loadedGroup) "Не удалось найти расписание" else "Ошибка обновления"
                            Text(
                                text = errorTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = (errorMessage ?: "Не удалось обновить данные") + 
                                       (if (loadedGroup != null) "\nПоказано последнее загруженное: $loadedGroup" else ""),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Скрыть",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isLoading && schedule != null) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 66.dp, start = 32.dp, end = 32.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                        .clip(CircleShape)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "loading_line")
                    val offset by infiniteTransition.animateFloat(
                        initialValue = -0.5f,
                        targetValue = 1.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "offset"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                            .graphicsLayer { 
                                this.translationX = offset * this.size.width 
                            }
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showBottomSheet = false 
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessHigh))
                ) {
                    AnimatedContent(
                        targetState = sheetContentState,
                        transitionSpec = {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                        },
                        label = "sheet_content"
                    ) { state ->
                        when (state) {
                            SheetContent.LessonDetails -> {
                                selectedLesson?.let { lesson ->
                                    LessonDetailsSheet(
                                        lesson = lesson,
                                        groupName = loadedGroup ?: groupInput,
                                        onTeacherClick = { staffMember ->
                                            selectedStaffMember = staffMember
                                            sheetContentState = SheetContent.StaffDetails
                                        }
                                    )
                                }
                            }
                            SheetContent.StaffDetails -> {
                                selectedStaffMember?.let { member ->
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { sheetContentState = SheetContent.LessonDetails }) {
                                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                                            }
                                            Text(
                                                text = "Назад к занятию",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        StaffDetailContent(
                                            member = member,
                                            onViewScheduleClick = { teacherName ->
                                                onGroupInputChange(teacherName)
                                                showBottomSheet = false
                                                loadSchedule(teacherName)
                                            }
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

enum class SheetContent {
    LessonDetails,
    StaffDetails
}
