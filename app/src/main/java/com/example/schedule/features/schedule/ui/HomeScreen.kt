package com.example.schedule.features.schedule.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import com.example.schedule.core.utils.TextUtils
import com.example.schedule.data.models.Lesson
import com.example.schedule.data.models.Schedule
import com.example.schedule.features.schedule.ui.components.LessonDetailsSheet
import com.example.schedule.features.schedule.ui.components.ScheduleList
import com.example.schedule.features.staff.data.StaffData
import com.example.schedule.features.staff.data.StaffMember
import com.example.schedule.features.staff.ui.StaffDetailContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    groupInput: String,
    onGroupInputChange: (String) -> Unit,
    schedule: Schedule?,
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
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = groupInput,
                                onValueChange = { 
                                    onGroupInputChange(it)
                                },
                                placeholder = { 
                                    Text(
                                        "Группа или фамилия",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading,
                                singleLine = true,
                                shape = MaterialTheme.shapes.extraLarge,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = { 
                                        focusManager.clearFocus()
                                        loadSchedule(null) 
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                trailingIcon = {
                                    if (groupInput.isNotEmpty()) {
                                        IconButton(onClick = { onGroupInputChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = null)
                                        }
                                    }
                                }
                            )
                            
                            MaterialTheme(
                                shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)
                            ) {
                                DropdownMenu(
                                    expanded = showSuggestions,
                                    onDismissRequest = { showSuggestions = false },
                                    modifier = Modifier.width(IntrinsicSize.Max),
                                    properties = PopupProperties(focusable = false)
                                ) {
                                    suggestions.forEach { name ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    name,
                                                    style = MaterialTheme.typography.bodyMedium
                                                ) 
                                            },
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
                        
                        FilledIconButton(
                            onClick = { 
                                focusManager.clearFocus()
                                loadSchedule(null) 
                            },
                            enabled = !isLoading && groupInput.isNotBlank(),
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Icon(
                                    imageVector = if (groupInput != loadedGroup) Icons.Default.Search else Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                Box(modifier = Modifier.weight(1f)) {
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
                                        key("${currentSchedule.group}_${currentSchedule.days.size}_${currentSchedule.days.firstOrNull()?.dayDate}") {
                                            ScheduleList(
                                                schedule = currentSchedule,
                                                onLessonClick = { lesson ->
                                                    selectedLesson = lesson
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
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isErrorVisible && schedule != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .padding(horizontal = 16.dp)
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
                            imageVector = Icons.Default.Warning,
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
                            imageVector = Icons.Default.Close,
                            contentDescription = "Скрыть",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            if (isLoading && schedule != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = 0.dp)
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
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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