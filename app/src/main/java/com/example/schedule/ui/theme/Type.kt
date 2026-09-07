package com.example.schedule.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.schedule.R

val GoogleSans = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_medium, FontWeight.SemiBold),
    Font(R.font.google_sans_bold, FontWeight.Bold)
)

private val defaultTypography = Typography()

val AppTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = GoogleSans),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = GoogleSans),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = GoogleSans),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = GoogleSans),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = GoogleSans),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = GoogleSans),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Normal),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = GoogleSans),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = GoogleSans),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = GoogleSans),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = GoogleSans),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = GoogleSans)
)
