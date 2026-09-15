package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * App Logo displaying the exact white stylized block "N" graphic.
 * Delegates to CustomNexaLogo for native code-based Canvas rendering.
 */
@Composable
fun NexaGeometricLogo(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    shape: Shape = RoundedCornerShape(20.dp),
    hasBorder: Boolean = true,
    borderColor: Color = Color(0x33FFFFFF),
    testTag: String = "nexa_geometric_logo"
) {
    CustomNexaLogo(
        modifier = modifier.size(size),
        shape = shape,
        hasBorder = hasBorder,
        borderColor = borderColor,
        testTag = testTag
    )
}

/**
 * Splash Screen displaying the exact white stylized block "N" graphic
 * with smooth scale animation and fade transitions on pure black background.
 */
@Composable
fun NexaSplashScreen(
    onAnimationFinished: () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        // Smooth fade-in and scale-in
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }

        // Hold display
        delay(1200)

        // Smooth fade-out before entering the app
        alphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )

        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {
        // Centered exact white stylized block "N" logo with scale & fade animation
        CustomNexaLogo(
            modifier = Modifier
                .size(130.dp)
                .graphicsLayer {
                    alpha = alphaAnim.value
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                },
            shape = RoundedCornerShape(28.dp),
            hasBorder = false,
            testTag = "splash_nexa_logo"
        )
    }
}
