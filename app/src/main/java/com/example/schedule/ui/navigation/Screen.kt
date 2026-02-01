package com.example.schedule.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val iconSelected: ImageVector) {
    object Home : Screen("home", "Расписание", Icons.Outlined.Home, Icons.Filled.Home)
    object Buses : Screen("buses", "Автобусы", Icons.Outlined.DirectionsBus, Icons.Filled.DirectionsBus)
    object Alarms : Screen("alarms", "Звонки", Icons.Outlined.Notifications, Icons.Filled.Notifications)
    object Settings : Screen("settings", "Настройки", Icons.Outlined.Settings, Icons.Filled.Settings)
}