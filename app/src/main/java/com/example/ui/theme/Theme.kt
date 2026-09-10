package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val ModernDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6B00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE65100),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF6366F1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1C1C1C),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFF06B6D4),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFF1C1C1C),
    outline = Color(0xFF262626),
    outlineVariant = Color(0xFF383838),
    error = DarkError,
    onError = Color.White,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkError
)

private val ModernLightColorScheme = lightColorScheme(
    primary = Color(0xFFFF6B00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0CC),
    onPrimaryContainer = Color(0xFF4A1B00),
    secondary = Color(0xFF6366F1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F3F5),
    onSecondaryContainer = Color(0xFF111111),
    tertiary = Color(0xFF06B6D4),
    onTertiary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFF1F3F5),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE9ECEF),
    onSurfaceVariant = Color(0xFF2D3748),
    surfaceContainerHighest = Color(0xFFFFFFFF),
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFD0D0D0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemDark
    }

    val customColors = if (isDark) DarkAppColors else LightAppColors
    val colorScheme = if (isDark) ModernDarkColorScheme else ModernLightColorScheme

    CompositionLocalProvider(
        LocalAppColors provides customColors,
        LocalThemeMode provides themeMode,
        LocalThemeUpdater provides onThemeModeChange
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
