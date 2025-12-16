package com.studymate.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = PrimaryBlueLight,

    secondary = AccentPurple,
    onSecondary = Color.White,
    secondaryContainer = AccentPurpleDark,
    onSecondaryContainer = AccentPurpleLight,

    tertiary = SuccessGreen,
    onTertiary = Color.White,

    error = ErrorRed,
    onError = Color.White,

    background = DarkBackground,
    onBackground = TextWhite,

    surface = DarkSurface,
    onSurface = TextWhite,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextGray,

    outline = DarkBorder,
    outlineVariant = DarkDivider,

    inverseSurface = LightSurface,
    inverseOnSurface = LightTextPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,

    secondary = AccentPurple,
    onSecondary = Color.White,
    secondaryContainer = AccentPurpleLight,
    onSecondaryContainer = AccentPurpleDark,

    tertiary = SuccessGreen,
    onTertiary = Color.White,

    error = ErrorRed,
    onError = Color.White,

    background = LightBackground,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,

    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
)

@Composable
fun StudyMateTheme(
    darkTheme: Boolean = true, // Default to dark theme to match StudyBuddy design
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
