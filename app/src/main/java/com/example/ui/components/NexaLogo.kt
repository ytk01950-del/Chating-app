package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * App Logo displaying the exact uploaded stylized white 'N' logo asset on pure black background.
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
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF000000))
            .then(if (hasBorder) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.nexa_logo),
            contentDescription = "Nexa Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Premium Splash Screen with smooth zoom-in, soft white glow, and smooth fade-in animation.
 */
@Composable
fun NexaSplashScreen(
    onAnimationFinished: () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.70f) }
    val glowAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "softGlowPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        // Fade in logo & glow smoothly
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))
            )
        }
        // Smooth zoom-in animation
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
            )
        }
        // Soft white glow fade-in
        launch {
            glowAlpha.animateTo(
                targetValue = 0.85f,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
            )
        }
        // Hold briefly for modern premium feel, then navigate to login screen
        delay(1900)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {
        // Soft White Glow Halo
        Box(
            modifier = Modifier
                .size(260.dp)
                .graphicsLayer {
                    alpha = glowAlpha.value * alphaAnim.value
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x3FFFFFFF),
                            Color(0x1AFFFFFF),
                            Color(0x05FFFFFF),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Main Animated Logo Image
        Image(
            painter = painterResource(id = R.drawable.nexa_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(116.dp)
                .graphicsLayer {
                    alpha = alphaAnim.value
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                },
            contentScale = ContentScale.Fit
        )
    }
}
