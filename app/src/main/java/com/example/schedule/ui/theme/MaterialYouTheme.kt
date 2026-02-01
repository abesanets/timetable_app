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
        // Fallback для Android 11 и ниже - используем более спокойную сине-серую палитру
        if (isDarkTheme) {
            darkColorScheme(
                primary = Color(0xFFD1E4FF),
                onPrimary = Color(0xFF003258),
                primaryContainer = Color(0xFF00497D),
                onPrimaryContainer = Color(0xFFD1E4FF),
                secondary = Color(0xFFBBC7DB),
                onSecondary = Color(0xFF253140),
                secondaryContainer = Color(0xFF3B4858),
                onSecondaryContainer = Color(0xFFD7E3F7),
                tertiary = Color(0xFFD6BEE4),
                onTertiary = Color(0xFF3B2948),
                background = Color(0xFF1A1C1E),
                onBackground = Color(0xFFE2E2E6),
                surface = Color(0xFF1A1C1E),
                onSurface = Color(0xFFE2E2E6),
                surfaceVariant = Color(0xFF43474E),
                onSurfaceVariant = Color(0xFFC3C7CF),
                outline = Color(0xFF8D9199)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF0061A4),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD1E4FF),
                onPrimaryContainer = Color(0xFF001D36),
                secondary = Color(0xFF535F70),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFD7E3F7),
                onSecondaryContainer = Color(0xFF101C2B),
                tertiary = Color(0xFF6B5778),
                onTertiary = Color(0xFFFFFFFF),
                background = Color(0xFFFDFCFF),
                onBackground = Color(0xFF1A1C1E),
                surface = Color(0xFFFDFCFF),
                onSurface = Color(0xFF1A1C1E),
                surfaceVariant = Color(0xFFDEE3EB),
                onSurfaceVariant = Color(0xFF43474E),
                outline = Color(0xFF73777F)
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
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}