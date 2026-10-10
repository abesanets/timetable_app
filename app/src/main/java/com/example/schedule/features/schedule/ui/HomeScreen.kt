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
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Не удалось загрузить",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { loadSchedule(null) },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Повторить", style = MaterialTheme.typography.labelLarge)
                                }
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

            // Subtle top fade overlay behind status bar
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(statusBarTop + 8.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                Color.Transparent
                            )
                        )
                    )
            )

            AnimatedVisibility(
                visible = isErrorVisible && schedule != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    onClick = { isErrorVisible = false }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            val errorTitle = if (groupInput != loadedGroup) "Расписание не найдено" else "Не удалось обновить"
                            Text(
                                text = errorTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = (errorMessage ?: "Проверьте подключение к интернету") + 
                                       (if (loadedGroup != null) " • Показано: $loadedGroup" else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { isErrorVisible = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Скрыть",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                windowInsets = WindowInsets(0, 0, 0, 0)
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
