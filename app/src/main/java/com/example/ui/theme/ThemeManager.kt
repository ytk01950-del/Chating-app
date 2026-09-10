package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val title: String, val key: String) {
    LIGHT("Light Mode", "light"),
    DARK("Dark Mode", "dark"),
    SYSTEM("System Default", "system");

    companion object {
        fun fromKey(key: String?): AppThemeMode {
            return entries.firstOrNull { it.key == key } ?: SYSTEM
        }
    }
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wp_chat_theme_prefs", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(AppThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.key)))
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.key).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val KEY_THEME_MODE = "app_theme_mode"
    }
}

data class CustomAppColors(
    val background: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val surfaceHover: Color,
    val border: Color,
    val borderSubtle: Color,
    val borderActive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val accentOrange: Color,
    val accentOrangeLight: Color,
    val accentOrangeDark: Color,
    val accentOrangePill: Color,
    val bubbleMe: Color,
    val bubbleMeText: Color,
    val bubbleMeTime: Color,
    val bubbleOther: Color,
    val bubbleOtherBorder: Color,
    val bubbleOtherText: Color,
    val bubbleOtherTime: Color,
    val navBarBg: Color,
    val navBarBorder: Color,
    val cardBackground: Color,
    val isDark: Boolean
)

// Dark Mode: Pure Black (#000000) canvas with mandatory crisp White (#FFFFFF) for primary text
val DarkAppColors = CustomAppColors(
    background = Color(0xFF000000),
    backgroundSecondary = Color(0xFF0B0B0B),
    surface = Color(0xFF141414),
    surfaceElevated = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFF222222),
    surfaceHover = Color(0xFF2C2C2C),
    border = Color(0xFF262626),
    borderSubtle = Color(0xFF383838),
    borderActive = Color(0xFFFF6B00),
    textPrimary = Color(0xFFFFFFFF),       // Mandatory crisp pure white
    textSecondary = Color(0xFFE2E8F0),     // High-contrast bright light grey
    textMuted = Color(0xFFA0AEC0),         // High-visibility muted text
    textDisabled = Color(0xFF718096),
    accentOrange = Color(0xFFFF6B00),      // Dedicated orange accent
    accentOrangeLight = Color(0xFFFF8533),
    accentOrangeDark = Color(0xFFE65100),
    accentOrangePill = Color(0x33FF6B00),
    bubbleMe = Color(0xFFFF6B00),
    bubbleMeText = Color(0xFFFFFFFF),
    bubbleMeTime = Color(0xFFFFE0CC),
    bubbleOther = Color(0xFF1C1C1C),
    bubbleOtherBorder = Color(0xFF2A2A2A),
    bubbleOtherText = Color(0xFFFFFFFF),
    bubbleOtherTime = Color(0xFFB0B0B0),
    navBarBg = Color(0xF2101010),
    navBarBorder = Color(0x33FFFFFF),
    cardBackground = Color(0xFF141414),
    isDark = true
)

// Light Mode: Pure White (#FFFFFF) canvas with mandatory crisp Black (#111111) for primary text
val LightAppColors = CustomAppColors(
    background = Color(0xFFFFFFFF),
    backgroundSecondary = Color(0xFFF8F9FA),
    surface = Color(0xFFF1F3F5),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9ECEF),
    surfaceHover = Color(0xFFDEE2E6),
    border = Color(0xFFE0E0E0),
    borderSubtle = Color(0xFFD0D0D0),
    borderActive = Color(0xFFFF6B00),
    textPrimary = Color(0xFF111111),       // Mandatory crisp pure black
    textSecondary = Color(0xFF2D3748),     // High-contrast dark charcoal
    textMuted = Color(0xFF4A5568),         // High-visibility muted text
    textDisabled = Color(0xFF718096),
    accentOrange = Color(0xFFFF6B00),      // Dedicated orange accent
    accentOrangeLight = Color(0xFFFF8533),
    accentOrangeDark = Color(0xFFE65100),
    accentOrangePill = Color(0x22FF6B00),
    bubbleMe = Color(0xFFFF6B00),
    bubbleMeText = Color(0xFFFFFFFF),
    bubbleMeTime = Color(0xFFFFE0CC),
    bubbleOther = Color(0xFFF1F3F5),
    bubbleOtherBorder = Color(0xFFE0E0E0),
    bubbleOtherText = Color(0xFF111111),
    bubbleOtherTime = Color(0xFF4A5568),
    navBarBg = Color(0xF8FFFFFF),
    navBarBorder = Color(0x22000000),
    cardBackground = Color(0xFFFFFFFF),
    isDark = false
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }
val LocalThemeMode = staticCompositionLocalOf { AppThemeMode.SYSTEM }
val LocalThemeUpdater = staticCompositionLocalOf<(AppThemeMode) -> Unit> { {} }

object AppTheme {
    val colors: CustomAppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val mode: AppThemeMode
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeMode.current
}

