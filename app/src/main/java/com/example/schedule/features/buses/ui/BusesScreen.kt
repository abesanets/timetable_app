package com.example.schedule.features.buses.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.schedule.data.models.Schedule
import com.example.schedule.data.preferences.PreferencesManager
import com.example.schedule.features.schedule.utils.ScheduleUtils
import com.example.schedule.features.buses.data.RouteRepository
import com.example.schedule.features.buses.data.RouteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusesScreen(
    schedule: Schedule?,
    preferencesManager: PreferencesManager
) {
    val scope = rememberCoroutineScope()
    val repository = remember { RouteRepository() }
    
    // State for UI fields
    var homeAddress by remember { mutableStateOf("") }
    var departureLocation by remember { mutableStateOf("Казинца 91") }
    
    // Data state
    var routes by remember { mutableStateOf<List<RouteResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Time state
    val calendar = Calendar.getInstance()
    var departureHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var departureMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }
    var isTimeSetByUser by remember { mutableStateOf(false) }

    // Dropdown state
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val locationOptions = listOf("Казинца 91", "Кнорина 14")

    // Time picker dialog state
    var showTimePicker by remember { mutableStateOf(false) }

    // Load preferences
    LaunchedEffect(Unit) {
        preferencesManager.homeAddress.collect { savedAddress ->
            homeAddress = savedAddress
        }
    }

    LaunchedEffect(Unit) {
        preferencesManager.departureLocation.collect { savedLocation ->
            if (savedLocation.isNotBlank()) {
                departureLocation = savedLocation
            }
        }
    }

    // Set default time from schedule
    LaunchedEffect(schedule) {
        if (!isTimeSetByUser && schedule != null) {
            val endTime = ScheduleUtils.getClassesEndTime(schedule.days)
            if (endTime != null) {
                departureHour = endTime.first
                departureMinute = endTime.second
            }
        }
    }
    
    fun saveAddress(address: String) {
        homeAddress = address
        scope.launch { preferencesManager.setHomeAddress(address) }
    }

    fun saveLocation(location: String) {
        departureLocation = location
        scope.launch { preferencesManager.setDepartureLocation(location) }
    }
    
    fun searchRoutes() {
        if (homeAddress.isBlank()) {
            errorMessage = "Введите домашний адрес"
            return
        }
        isLoading = true
        errorMessage = null
        routes = emptyList()
        scope.launch {
            try {
                val results = repository.getRoutes(homeAddress, departureLocation, departureHour, departureMinute)
                routes = results
                if (results.isEmpty()) errorMessage = "Маршруты не найдены"
            } catch (e: Exception) {
                errorMessage = "Ошибка: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = departureHour, initialMinute = departureMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    departureHour = timePickerState.hour
                    departureMinute = timePickerState.minute
                    isTimeSetByUser = true
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Отмена") } },
            text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Автобусы", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.statusBarsPadding()
            )
            
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                OutlinedTextField(
                    value = homeAddress,
                    onValueChange = { saveAddress(it) },
                    label = { Text("Домашний адрес") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = departureLocation,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Откуда") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
                        locationOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { saveLocation(option); isDropdownExpanded = false })
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = String.format("%02d:%02d", departureHour, departureMinute),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Время отправления") },
                        leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showTimePicker = true })
                }
                
                Button(onClick = { searchRoutes() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = MaterialTheme.shapes.large) {
                    if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Text("Найти маршрут")
                }
                
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                
                if (routes.isNotEmpty()) {
                    Text("Маршруты:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    routes.forEach { route ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("${route.departureTime} - ${route.arrivalTime}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        Text(text = if (route.transfers > 0) "Пересадки: ${route.transfers}" else "Без пересадок", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                                        Text(route.duration, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val steps = route.description.split("\n↓\n")
                                steps.forEach { step ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Icon(
                                            imageVector = if (step.startsWith("Пешком")) Icons.Default.DirectionsWalk else Icons.Default.DirectionsBus,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(step, style = MaterialTheme.typography.bodyMedium)
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
