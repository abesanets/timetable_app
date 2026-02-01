package com.example.schedule.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@Composable
fun MaterialYouTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Динамические цвета Material You из системы
        if (isDarkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        // Fallback для Android 11 и ниже
        if (isDarkTheme) {
            darkColorScheme(
                primary = Color(0xFFBB86FC),
                secondary = Color(0xFF03DAC6),
                tertiary = Color(0xFF3700B3),
                background = Color(0xFF1C1B1F),
                surface = Color(0xFF1C1B1F),
                onPrimary = Color(0xFF000000),
                onSecondary = Color(0xFF000000),
                onBackground = Color(0xFFE6E1E5),
                onSurface = Color(0xFFE6E1E5)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF6200EE),
                secondary = Color(0xFF03DAC6),
                tertiary = Color(0xFF3700B3),
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFFFFBFE),
                onPrimary = Color(0xFFFFFFFF),
                onSecondary = Color(0xFF000000),
                onBackground = Color(0xFF1C1B1F),
                onSurface = Color(0xFF1C1B1F)
            )
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            displayLarge = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}