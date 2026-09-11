package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Premium Monochrome Black & White Design System - Pure Pitch Black (#000000) Canvas
val DarkBg = Color(0xFF000000)             // Pure pitch black canvas (#000000)
val DarkBgSecondary = Color(0xFF000000)    // Pure pitch black sub-layer (#000000)
val DarkSurface = Color(0xFF121212)        // Dark charcoal surface (#121212)
val DarkSurfaceElevated = Color(0xFF181818)// Dialogs, popups, and elevated cards (#181818)
val DarkSurfaceVariant = Color(0xFF1E1E1E) // Input backgrounds and chip fills (#1E1E1E)
val DarkSurfaceHover = Color(0xFF2A2A2A)   // Pressed/hover states

val DarkBorder = Color(0xFF222222)         // Primary structural borders
val DarkBorderSubtle = Color(0xFF2E2E2E)   // Higher contrast focus & divider borders
val DarkBorderActive = Color(0xFFFFFFFF)   // Crisp White active border (#FFFFFF)

val TextPrimary = Color(0xFFFFFFFF)        // Crisp Pure White (#FFFFFF)
val TextSecondary = Color(0xFFCCCCCC)      // Clear secondary details and labels (#CCCCCC)
val TextMuted = Color(0xFF888888)          // Subdued metadata and timestamps (#888888)
val TextDisabled = Color(0xFF555555)       // Disabled text

val AccentBlue = Color(0xFFFFFFFF)         // Monochrome Pure White accent
val AccentBlueLight = Color(0xFFEEEEEE)    // Soft white highlight
val AccentBlueDark = Color(0xFFCCCCCC)     // Subtle off-white shade
val AccentBlueNavy = Color(0xFF000000)     // Dark contrast for text on white pills
val AccentIndigo = Color(0xFFFFFFFF)       // Monochrome White
val AccentCyan = Color(0xFFFFFFFF)         // Monochrome White
val AccentPurple = Color(0xFFFFFFFF)       // Monochrome White
val AccentOrange = Color(0xFFFFFFFF)       // Monochrome Pure White
val AccentOrangeLight = Color(0xFFEEEEEE)  // Soft light white
val AccentOrangeDark = Color(0xFFCCCCCC)   // Crisp off-white tone
val AccentOrangePill = Color(0x33FFFFFF)   // Soft transparent white pill highlights

val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFE5E5E5))
)
val StoryActiveGradient = Brush.sweepGradient(
    colors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFAAAAAA),
        Color(0xFFFFFFFF),
        Color(0xFFAAAAAA),
        Color(0xFFFFFFFF)
    )
)
val StoryViewedColor = Color(0xFF444444)

val MessageBubbleMe = Color(0xFFFFFFFF)
val MessageBubbleMeSecondary = Color(0xFFEEEEEE)
val MessageBubbleMeText = Color(0xFF000000)
val MessageBubbleMeTime = Color(0xFF666666)

val MessageBubbleOther = Color(0xFF181818)
val MessageBubbleOtherBorder = Color(0xFF2A2A2A)
val MessageBubbleOtherText = Color(0xFFFFFFFF)
val MessageBubbleOtherTime = Color(0xFF888888)

val OnlineGreen = Color(0xFFFFFFFF)
val OfflineGray = Color(0xFF555555)
val DarkError = Color(0xFFEF4444)
val DarkErrorContainer = Color(0xFF2A1414)
val DarkSuccess = Color(0xFFFFFFFF)
val DarkSuccessContainer = Color(0xFF1A1A1A)
val WarningAmber = Color(0xFFE5E5E5)
val WarningContainer = Color(0xFF222222)
val ButtonActiveDark = Color(0xFF262626)
val ShimmerBase = Color(0xFF141414)
val ShimmerHighlight = Color(0xFF262626)




