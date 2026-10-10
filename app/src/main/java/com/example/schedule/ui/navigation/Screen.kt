package com.example.schedule.ui.navigation

import androidx.annotation.DrawableRes
import com.example.schedule.R

sealed class Screen(
    val route: String,
    val title: String,
    @field:DrawableRes val iconRes: Int
) {
    object Home : Screen("home", "Расписание", R.drawable.ic_nav_calendar)
    object Alarms : Screen("alarms", "Звонки", R.drawable.ic_nav_bell)
    object Staff : Screen("staff", "Сотрудники", R.drawable.ic_nav_users)
    object Settings : Screen("settings", "Настройки", R.drawable.ic_nav_settings)
}