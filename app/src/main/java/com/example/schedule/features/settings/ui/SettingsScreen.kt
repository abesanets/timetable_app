package com.example.schedule.features.settings.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.schedule.data.preferences.PreferencesManager
import com.example.schedule.features.notifications.manager.DailyNotificationManager
import com.example.schedule.features.widget.ui.ScheduleWidgetReceiver
import com.example.schedule.features.widget.worker.WidgetUpdateScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    val notificationsEnabled by preferencesManager.notificationsEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scope.launch {
                preferencesManager.setNotificationsEnabled(true)
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        "Настройки",
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
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Уведомления
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                val newValue = !notificationsEnabled
                                if (newValue) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        scope.launch {
                                            preferencesManager.setNotificationsEnabled(true)
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        preferencesManager.setNotificationsEnabled(false)
                                        val manager = DailyNotificationManager(context)
                                        manager.cancelNotification()
                                    }
                                }
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Уведомления",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Получать расписание на следующий день",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        scope.launch {
                                            preferencesManager.setNotificationsEnabled(true)
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        preferencesManager.setNotificationsEnabled(false)
                                        val manager = DailyNotificationManager(context)
                                        manager.cancelNotification()
                                    }
                                }
                            }
                        )
                    }
                }
                
                // Виджет
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                                    val myProvider = android.content.ComponentName(context, ScheduleWidgetReceiver::class.java)
                                    
                                    if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                        appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                    }
                                }
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Виджет на рабочий стол",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Нажмите, чтобы добавить",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Частота обновления виджета
                val widgetUpdateInterval by preferencesManager.widgetUpdateInterval.collectAsState(initial = 60L)
                val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                val componentName = android.content.ComponentName(context, ScheduleWidgetReceiver::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                val hasWidget = widgetIds.isNotEmpty()
                
                var showIntervalDialog by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = hasWidget) {
                                showIntervalDialog = true
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Частота обновления виджета",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasWidget) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Text(
                                text = if (!hasWidget) "Виджет не добавлен" else when (widgetUpdateInterval) {
                                    0L -> "Отключено"
                                    15L -> "Каждые 15 минут"
                                    30L -> "Каждые 30 минут"
                                    60L -> "Каждый час"
                                    180L -> "Каждые 3 часа"
                                    360L -> "Каждые 6 часов"
                                    else -> "$widgetUpdateInterval мин"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasWidget) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
                
                if (showIntervalDialog) {
                    AlertDialog(
                        onDismissRequest = { showIntervalDialog = false },
                        title = { Text(text = "Частота обновления") },
                        text = {
                            Column {
                                val options = listOf(
                                    0L to "Отключено (только вручную)",
                                    15L to "Каждые 15 минут",
                                    30L to "Каждые 30 минут",
                                    60L to "Каждый час",
                                    180L to "Каждые 3 часа",
                                    360L to "Каждые 6 часов"
                                )
                                
                                options.forEach { (value, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    preferencesManager.setWidgetUpdateInterval(value)
                                                    WidgetUpdateScheduler.scheduleUpdate(context, value)
                                                }
                                                showIntervalDialog = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (value == widgetUpdateInterval),
                                            onClick = null // Handled by Row clickable
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = label)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showIntervalDialog = false }) {
                                Text("Отмена")
                            }
                        }
                    )
                }
            }
        }
    }
}