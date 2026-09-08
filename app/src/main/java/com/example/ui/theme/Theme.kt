package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ModernDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlueDark,
    onPrimaryContainer = TextPrimary,
    secondary = AccentIndigo,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentCyan,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHighest = DarkSurfaceElevated,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle,
    error = DarkError,
    onError = Color.White,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ModernDarkColorScheme,
        typography = Typography,
        content = content
    )
}


