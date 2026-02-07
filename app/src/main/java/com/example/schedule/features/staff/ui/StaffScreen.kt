package com.example.schedule.features.staff.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schedule.features.staff.data.StaffData
import com.example.schedule.features.staff.data.StaffMember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen() {
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    var selectedMember by remember { mutableStateOf<StaffMember?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current

    val administration = remember { StaffData.administration.sortedBy { it.fullName } }
    val teachers = remember { StaffData.teachers.sortedBy { it.fullName } }
    val employees = remember { StaffData.employees.sortedBy { it.fullName } }

    val filteredAdministration = remember(searchQuery, administration) {
        administration.filter { 
            it.fullName.contains(searchQuery, ignoreCase = true) || 
            it.position.contains(searchQuery, ignoreCase = true) 
        }
    }
    
    val filteredTeachers = remember(searchQuery, teachers) {
        teachers.filter { 
            it.fullName.contains(searchQuery, ignoreCase = true) || 
            it.position.contains(searchQuery, ignoreCase = true) 
        }
    }
    
    val filteredEmployees = remember(searchQuery, employees) {
        employees.filter { 
            it.fullName.contains(searchQuery, ignoreCase = true) || 
            it.position.contains(searchQuery, ignoreCase = true) 
        }
    }

    // Auto-expand everything when searching, collapse when search cleared
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            expandedCategory = "ALL"
        } else if (expandedCategory == "ALL") {
            expandedCategory = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        "Сотрудники",
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
            
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск сотрудника") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                val noResults = searchQuery.isNotBlank() && 
                               filteredAdministration.isEmpty() && 
                               filteredTeachers.isEmpty() && 
                               filteredEmployees.isEmpty()

                AnimatedContent(
                    targetState = noResults,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "search_results_transition"
                ) { isEmpty ->
                    if (isEmpty) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Text(
                                    "Никто не найден",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Administration Section
                            item(key = "section_admin") {
                                Column {
                                    AnimatedVisibility(
                                        visible = searchQuery.isBlank() || filteredAdministration.isNotEmpty(),
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        StaffCategorySection(
                                            title = "Администрация",
                                            isExpanded = expandedCategory == "Администрация" || expandedCategory == "ALL",
                                            members = filteredAdministration,
                                            onHeaderClick = {
                                                if (searchQuery.isBlank()) {
                                                    expandedCategory = if (expandedCategory == "Администрация") null else "Администрация"
                                                }
                                            },
                                            onMemberClick = {
                                                selectedMember = it
                                                showBottomSheet = true
                                            }
                                        )
                                    }
                                }
                            }

                            // Teachers Section
                            item(key = "section_teachers") {
                                Column {
                                    AnimatedVisibility(
                                        visible = searchQuery.isBlank() || filteredTeachers.isNotEmpty(),
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        StaffCategorySection(
                                            title = "Преподаватели",
                                            isExpanded = expandedCategory == "Преподаватели" || expandedCategory == "ALL",
                                            members = filteredTeachers,
                                            onHeaderClick = {
                                                if (searchQuery.isBlank()) {
                                                    expandedCategory = if (expandedCategory == "Преподаватели") null else "Преподаватели"
                                                }
                                            },
                                            onMemberClick = {
                                                selectedMember = it
                                                showBottomSheet = true
                                            }
                                        )
                                    }
                                }
                            }

                            // Employees Section
                            item(key = "section_employees") {
                                Column {
                                    AnimatedVisibility(
                                        visible = searchQuery.isBlank() || filteredEmployees.isNotEmpty(),
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        StaffCategorySection(
                                            title = "Служащие",
                                            isExpanded = expandedCategory == "Служащие" || expandedCategory == "ALL",
                                            members = filteredEmployees,
                                            onHeaderClick = {
                                                if (searchQuery.isBlank()) {
                                                    expandedCategory = if (expandedCategory == "Служащие") null else "Служащие"
                                                }
                                            },
                                            onMemberClick = {
                                                selectedMember = it
                                                showBottomSheet = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Top gradient for search field overlap
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        if (showBottomSheet && selectedMember != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessHigh))
                ) {
                    StaffDetailContent(selectedMember!!)
                }
            }
        }
    }
}

@Composable
fun StaffCategorySection(
    title: String,
    isExpanded: Boolean,
    members: List<StaffMember>,
    onHeaderClick: () -> Unit,
    onMemberClick: (StaffMember) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CategoryHeader(
            title = title,
            isExpanded = isExpanded,
            onClick = onHeaderClick
        )
        
        // Expansion Animation
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp), 
                modifier = Modifier
                    .padding(top = 12.dp)
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                members.forEach { member ->
                    StaffMemberItem(member) { onMemberClick(member) }
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(
    title: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun StaffMemberItem(member: StaffMember, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = member.fullName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun StaffDetailContent(member: StaffMember) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = member.fullName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailItem(label = "Должность", value = member.position)
            
            member.qualification?.let {
                DetailItem(label = "Квалификация", value = it)
            }
            
            DetailItem(label = "Образование", value = member.education)
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
    }
}