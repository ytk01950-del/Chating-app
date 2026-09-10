package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Sophisticated Dark Design System - High Contrast Pure Black Canvas & Dark Charcoal Surfaces
val DarkBg = Color(0xFF000000)             // Pure pitch black canvas
val DarkBgSecondary = Color(0xFF000000)    // Pure pitch black sub-layer
val DarkSurface = Color(0xFF262626)        // Dark charcoal (#262626)
val DarkSurfaceElevated = Color(0xFF262626)// Dialogs, popups, and elevated cards (#262626)
val DarkSurfaceVariant = Color(0xFF262626) // Input backgrounds and chip fills (#262626)
val DarkSurfaceHover = Color(0xFF333333)   // Pressed/hover states

val DarkBorder = Color(0xFF262626)         // Primary structural borders
val DarkBorderSubtle = Color(0xFF363636)   // Higher contrast focus & divider borders
val DarkBorderActive = Color(0xFF2563EB)   // Electric Royal Blue (#2563EB)

val TextPrimary = Color(0xFFFFFFFF)        // Crisp Pure White (#FFFFFF)
val TextSecondary = Color(0xFFE2E8F0)      // Clear secondary details and labels
val TextMuted = Color(0xFFA0AEC0)          // Subdued metadata and timestamps
val TextDisabled = Color(0xFF718096)       // Disabled text

val AccentBlue = Color(0xFF2563EB)         // Electric Royal Blue (#2563EB)
val AccentBlueLight = Color(0xFF3B82F6)    // Soft highlight blue
val AccentBlueDark = Color(0xFF1D4ED8)     // Deep shade blue
val AccentBlueNavy = Color(0xFF0F172A)     // Dark contrast for text on light pills
val AccentIndigo = Color(0xFF6366F1)       // Premium indigo tone
val AccentCyan = Color(0xFF06B6D4)         // Tech cyan accent
val AccentPurple = Color(0xFF8B5CF6)       // Story / highlight purple
val AccentOrange = Color(0xFF2563EB)       // Replaced with Electric Royal Blue (#2563EB)
val AccentOrangeLight = Color(0xFF3B82F6)  // Soft light blue
val AccentOrangeDark = Color(0xFF1D4ED8)   // Deep royal blue tone
val AccentOrangePill = Color(0x332563EB)   // Soft transparent blue for active pill highlights

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

val MessageBubbleOther = Color(0xFF262626)
val MessageBubbleOtherBorder = Color(0xFF363636)
val MessageBubbleOtherText = Color(0xFFFFFFFF)
val MessageBubbleOtherTime = Color(0xFFA8A8A8)

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



