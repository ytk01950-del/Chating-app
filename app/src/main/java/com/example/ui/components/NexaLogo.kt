package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * NexaChat Logo Component using the exact uploaded asset 205564.png.
 */
@Composable
fun NexaGeometricLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    shape: Shape = RoundedCornerShape(20.dp),
    hasBorder: Boolean = false,
    borderColor: Color = Color(0xFF262626),
    testTag: String = "nexa_geometric_logo"
) {
    val context = LocalContext.current
    val assetBitmap = remember(context) {
        try {
            context.assets.open("205564.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            try {
                context.assets.open("logo.png").use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                }
            } catch (e2: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF000000))
            .then(if (hasBorder) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (assetBitmap != null) {
            Image(
                bitmap = assetBitmap,
                contentDescription = "NexaChat Logo (205564.png)",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.logo_205564),
                contentDescription = "NexaChat Logo (205564.png)",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * Animated Splash Screen:
 * - Pure Black background (#000000)
 * - Exact uploaded logo image 205564.png centered with 'NexaChat' text below it
 * - Smooth 1.5-second Scale & Fade-in animation on launch, followed by auto-redirect
 */
@Composable
fun NexaSplashScreen(
    onAnimationFinished: () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.80f) }

    LaunchedEffect(Unit) {
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = FastOutSlowInEasing
                )
            )
        }
        // 1.5-second animation completion
        delay(1500L)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .testTag("nexa_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .graphicsLayer {
                    alpha = alphaAnim.value
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                }
        ) {
            NexaGeometricLogo(
                size = 130.dp,
                shape = RoundedCornerShape(26.dp),
                hasBorder = false,
                testTag = "splash_nexa_logo"
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "NexaChat",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Next-Generation Real-Time Messaging",
                fontSize = 12.sp,
                color = Color(0xFF8E8E8E),
                letterSpacing = 0.3.sp
            )
        }
    }
}
