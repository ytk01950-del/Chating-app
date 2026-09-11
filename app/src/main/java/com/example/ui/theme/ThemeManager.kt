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

// Dark Mode: Pure Pitch Black (#000000) canvas with mandatory crisp White (#FFFFFF) for primary text and dark charcoal surfaces (#121212)
val DarkAppColors = CustomAppColors(
    background = Color(0xFF000000),       // Pure pitch black
    backgroundSecondary = Color(0xFF000000),
    surface = Color(0xFF121212),          // Dark charcoal (#121212)
    surfaceElevated = Color(0xFF181818),  // Dialogs & elevated surfaces (#181818)
    surfaceVariant = Color(0xFF1E1E1E),   // Secondary button/chip containers (#1E1E1E)
    surfaceHover = Color(0xFF2A2A2A),
    border = Color(0xFF222222),           // Border matching charcoal surfaces
    borderSubtle = Color(0xFF2E2E2E),
    borderActive = Color(0xFFFFFFFF),     // Crisp White (#FFFFFF)
    textPrimary = Color(0xFFFFFFFF),       // Crisp Pure White (#FFFFFF)
    textSecondary = Color(0xFFCCCCCC),     // Clear crisp white/light gray
    textMuted = Color(0xFF888888),         // Subdued metadata
    textDisabled = Color(0xFF555555),
    accentOrange = Color(0xFFFFFFFF),      // Monochrome Pure White
    accentOrangeLight = Color(0xFFEEEEEE),
    accentOrangeDark = Color(0xFFCCCCCC),
    accentOrangePill = Color(0x33FFFFFF),
    bubbleMe = Color(0xFFFFFFFF),          // Pure White Bubble
    bubbleMeText = Color(0xFF000000),
    bubbleMeTime = Color(0x99000000),
    bubbleOther = Color(0xFF181818),       // Dark charcoal (#181818)
    bubbleOtherBorder = Color(0xFF2E2E2E),
    bubbleOtherText = Color(0xFFFFFFFF),
    bubbleOtherTime = Color(0xFF888888),
    navBarBg = Color(0xF2000000),          // Pure Pitch Black bottom bar
    navBarBorder = Color(0xFF222222),
    cardBackground = Color(0xFF121212),    // Dark charcoal (#121212)
    isDark = true
)

// Light Mode: Pure White (#FFFFFF) canvas with mandatory crisp Black (#111111) for primary text
val LightAppColors = CustomAppColors(
    background = Color(0xFFFFFFFF),
    backgroundSecondary = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFEFEF),
    surfaceHover = Color(0xFFE4E6EB),
    border = Color(0xFFDBDBDB),
    borderSubtle = Color(0xFFE5E5E5),
    borderActive = Color(0xFF111111),     // Crisp Black (#111111)
    textPrimary = Color(0xFF111111),       // Crisp Pure Black (#111111)
    textSecondary = Color(0xFF555555),     // Dark charcoal
    textMuted = Color(0xFF8E8E8E),         // Subdued metadata
    textDisabled = Color(0xFFB0B0B0),
    accentOrange = Color(0xFF111111),      // Crisp Black (#111111)
    accentOrangeLight = Color(0xFF333333),
    accentOrangeDark = Color(0xFF000000),
    accentOrangePill = Color(0x22000000),
    bubbleMe = Color(0xFF111111),          // Crisp Black
    bubbleMeText = Color(0xFFFFFFFF),
    bubbleMeTime = Color(0xCCFFFFFF),
    bubbleOther = Color(0xFFEFEFEF),
    bubbleOtherBorder = Color(0xFFDBDBDB),
    bubbleOtherText = Color(0xFF111111),
    bubbleOtherTime = Color(0xFF737373),
    navBarBg = Color(0xF8FFFFFF),
    navBarBorder = Color(0xFFDBDBDB),
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

