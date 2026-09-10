package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val ModernDarkColorScheme = darkColorScheme(
    primary = Color(0xFF2563EB),        // Electric Royal Blue (#2563EB)
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1D4ED8),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF6366F1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFF06B6D4),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),     // Pure pitch black (#000000)
    onBackground = Color(0xFFFFFFFF),   // Crisp White (#FFFFFF)
    surface = Color(0xFF262626),        // Dark charcoal (#262626)
    onSurface = Color(0xFFFFFFFF),      // Crisp White (#FFFFFF)
    surfaceVariant = Color(0xFF262626), // Dark charcoal (#262626)
    onSurfaceVariant = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFF262626),
    outline = Color(0xFF262626),
    outlineVariant = Color(0xFF363636),
    error = DarkError,
    onError = Color.White,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkError
)

private val ModernLightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),        // Electric Royal Blue (#2563EB)
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = Color(0xFF6366F1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F3F5),
    onSecondaryContainer = Color(0xFF111111),
    tertiary = Color(0xFF06B6D4),
    onTertiary = Color.White,
    background = Color(0xFFFFFFFF),     // Pure white (#FFFFFF)
    onBackground = Color(0xFF111111),   // Dark charcoal (#111111)
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEFEFEF),
    onSurfaceVariant = Color(0xFF2D3748),
    surfaceContainerHighest = Color(0xFFFFFFFF),
    outline = Color(0xFFDBDBDB),
    outlineVariant = Color(0xFFE5E5E5),
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
