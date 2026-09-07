package com.example.schedule.features.alarms.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schedule.core.utils.BuildingUtils
import com.example.schedule.core.utils.CourseGroupItem
import com.example.schedule.data.models.BuildingId
import com.example.schedule.features.alarms.ui.components.CallItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    initialBuilding: BuildingId = BuildingId.KAZINTSA
) {
    var selectedBuilding by remember { mutableStateOf(initialBuilding) }
    var showGroups by remember { mutableStateOf(false) }

    val buildingInfo = BuildingUtils.BUILDINGS[selectedBuilding]
    val schedule = BuildingUtils.getCallSchedule(selectedBuilding)
    val courseGroups = if (selectedBuilding == BuildingId.KAZINTSA) {
        BuildingUtils.KAZINTSA_COURSE_GROUPS
    } else {
        BuildingUtils.KNORINA_COURSE_GROUPS
    }
    val totalGroupsCount = if (selectedBuilding == BuildingId.KAZINTSA) {
        BuildingUtils.KAZINTSA_GROUPS.size
    } else {
        BuildingUtils.KNORINA_GROUPS.size
    }

    val shift1Calls = schedule.filter { it.shift == 1 }
    val shift2Calls = schedule.filter { it.shift == 2 }

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
                            buildingInfo?.name ?: "",
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

            // Building Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BuildingPill(
                    label = "Казинца, 91",
                    selected = selectedBuilding == BuildingId.KAZINTSA,
                    onClick = { selectedBuilding = BuildingId.KAZINTSA }
                )
                BuildingPill(
                    label = "Кнорина, 14",
                    selected = selectedBuilding == BuildingId.KNORINA,
                    onClick = { selectedBuilding = BuildingId.KNORINA }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedBuilding == BuildingId.KAZINTSA) {
                    // Shift 1
                    item(key = "shift_1_header") {
                        ShiftHeader(shiftNumber = 1, title = "1–3 пары")
                    }
                    items(
                        items = shift1Calls,
                        key = { "shift1_${it.pairNumber}" }
                    ) { callTime ->
                        CallItem(lessonNumber = callTime.pairNumber, callTime = callTime)
                    }

                    // Shift 2
                    item(key = "shift_2_header") {
                        Spacer(modifier = Modifier.height(6.dp))
                        ShiftHeader(shiftNumber = 2, title = "4–7 пары")
                    }
                    items(
                        items = shift2Calls,
                        key = { "shift2_${it.pairNumber}" }
                    ) { callTime ->
                        CallItem(lessonNumber = callTime.pairNumber, callTime = callTime)
                    }
                } else {
                    items(
                        items = schedule,
                        key = { "knorina_${it.pairNumber}" }
                    ) { callTime ->
                        CallItem(lessonNumber = callTime.pairNumber, callTime = callTime)
                    }
                }

                // Expandable Groups Section
                item(key = "groups_section") {
                    Spacer(modifier = Modifier.height(12.dp))
                    BuildingGroupsCard(
                        count = totalGroupsCount,
                        courseGroups = courseGroups,
                        isExpanded = showGroups,
                        onToggle = { showGroups = !showGroups }
                    )
                }
            }
        }
    }
}

@Composable
fun BuildingPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(42.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ShiftHeader(shiftNumber: Int, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "$shiftNumber смена",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BuildingGroupsCard(
    count: Int,
    courseGroups: List<CourseGroupItem>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Группы корпуса ($count)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Скрыть" else "Показать",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    courseGroups.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = item.year,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.groups,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}