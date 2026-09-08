package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Sophisticated Dark Design System
val DarkBg = Color(0xFF0C0E14)             // Deep slate black canvas
val DarkBgSecondary = Color(0xFF11141D)    // Sub-layer canvas
val DarkSurface = Color(0xFF161A24)        // Cards and elevated panels
val DarkSurfaceElevated = Color(0xFF1E2330)// Dialogs, popups, and elevated cards
val DarkSurfaceVariant = Color(0xFF252B3B) // Input backgrounds and chip fills
val DarkSurfaceHover = Color(0xFF2E3547)   // Pressed/hover states

val DarkBorder = Color(0xFF252B3A)         // Primary structural borders
val DarkBorderSubtle = Color(0xFF333A4E)   // Higher contrast focus & divider borders
val DarkBorderActive = Color(0xFF4F5B7A)   // Focused input borders

val TextPrimary = Color(0xFFF8FAFC)        // Crisp, high-contrast primary text
val TextSecondary = Color(0xFF94A3B8)      // Clear secondary details and labels
val TextMuted = Color(0xFF64748B)          // Subdued metadata and timestamps
val TextDisabled = Color(0xFF475569)       // Disabled text

val AccentBlue = Color(0xFF3B82F6)         // Primary electric blue
val AccentBlueLight = Color(0xFF60A5FA)    // Soft highlight blue
val AccentBlueDark = Color(0xFF1D4ED8)     // Deep shade blue
val AccentBlueNavy = Color(0xFF0F172A)     // Dark contrast for text on light pills
val AccentIndigo = Color(0xFF6366F1)       // Premium indigo tone
val AccentCyan = Color(0xFF06B6D4)         // Tech cyan accent
val AccentPurple = Color(0xFF8B5CF6)       // Story / highlight purple

val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF3B82F6), Color(0xFF6366F1))
)
val StoryActiveGradient = Brush.sweepGradient(
    colors = listOf(
        Color(0xFFF43F5E),
        Color(0xFF8B5CF6),
        Color(0xFF3B82F6),
        Color(0xFF06B6D4),
        Color(0xFFF43F5E)
    )
)
val StoryViewedColor = Color(0xFF475569)

val MessageBubbleMe = Color(0xFF2563EB)
val MessageBubbleMeSecondary = Color(0xFF1D4ED8)
val MessageBubbleMeText = Color(0xFFFFFFFF)
val MessageBubbleMeTime = Color(0xFFBFDBFE)

val MessageBubbleOther = Color(0xFF1E2330)
val MessageBubbleOtherBorder = Color(0xFF2A3144)
val MessageBubbleOtherText = Color(0xFFF1F5F9)
val MessageBubbleOtherTime = Color(0xFF94A3B8)

val OnlineGreen = Color(0xFF10B981)
val OfflineGray = Color(0xFF64748B)
val DarkError = Color(0xFFEF4444)
val DarkErrorContainer = Color(0xFF3F1618)
val DarkSuccess = Color(0xFF10B981)
val DarkSuccessContainer = Color(0xFF0F2E22)
val WarningAmber = Color(0xFFF59E0B)
val WarningContainer = Color(0xFF3B2A0C)
val ButtonActiveDark = Color(0xFF2A3144)
val ShimmerBase = Color(0xFF1A1F2C)
val ShimmerHighlight = Color(0xFF2C3449)



