package com.example.schedule.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val iconSelected: ImageVector) {
    object Home : Screen("home", "Расписание", Icons.Rounded.Home, Icons.Rounded.Home)
    object Alarms : Screen("alarms", "Звонки", Icons.Rounded.Notifications, Icons.Rounded.Notifications)
    object Staff : Screen("staff", "Сотрудники", Icons.Rounded.Person, Icons.Rounded.Person)
    object Settings : Screen("settings", "Настройки", Icons.Rounded.Settings, Icons.Rounded.Settings)
}