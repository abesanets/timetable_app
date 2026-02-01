package com.example.schedule.features.alarms.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.schedule.features.alarms.data.CallScheduleData
import com.example.schedule.features.alarms.ui.components.CallItem
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen() {
    val calendar = Calendar.getInstance()
    val isSaturday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
    val schedule = CallScheduleData.getScheduleForDay(isSaturday)
    val dayName = CallScheduleData.getDayName(isSaturday)
    
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
                            dayName,
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
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = schedule.size,
                    key = { index -> index },
                    contentType = { "call" }
                ) { index ->
                    CallItem(lessonNumber = index + 1, callTime = schedule[index])
                }
            }
        }
    }
}