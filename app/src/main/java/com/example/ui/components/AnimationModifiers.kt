package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role

/**
 * Premium micro-interaction animation spec for responsive tap scaling.
 * Damped spring prevents excessive bounciness while giving an immediate, natural tactile response.
 */
val PremiumPressSpringSpec = spring<Float>(
    dampingRatio = 0.85f,
    stiffness = 650f
)

val SmoothEnterTweenSpec = tween<Float>(
    durationMillis = 220,
    easing = FastOutSlowInEasing
)

/**
 * Smoothly scales an element slightly down to [scaleDown] (default 0.97f) when pressed,
 * and smoothly returns to 1.0f on release.
 */
fun Modifier.pressScale(
    scaleDown: Float = 0.97f,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = PremiumPressSpringSpec,
        label = "globalPressScale"
    )

    this.scale(animatedScale)
}

/**
 * Clickable modifier combining the standard click/ripple behavior with a smooth,
 * natural press scale micro-interaction.
 */
fun Modifier.clickableWithPress(
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    rippleColor: Color = Color.Unspecified,
    boundedRipple: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = PremiumPressSpringSpec,
        label = "clickableWithPressScale"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = boundedRipple, color = rippleColor),
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
}
