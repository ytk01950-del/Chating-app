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
 * Splash Screen displaying only the uploaded black-and-white N logo on a clean black background
 * with a smooth fade-in, gentle scale, and fade-out animation.
 */
@Composable
fun NexaSplashScreen(
    onAnimationFinished: () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.90f) }

    LaunchedEffect(Unit) {
        // Smooth fade-in and subtle scale-in of the uploaded logo
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
        }

        // Display hold duration
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
        // Centered uploaded N logo
        Image(
            painter = painterResource(id = R.drawable.nexa_logo),
            contentDescription = "N Logo",
            modifier = Modifier
                .size(130.dp)
                .graphicsLayer {
                    alpha = alphaAnim.value
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                },
            contentScale = ContentScale.Fit
        )
    }
}
