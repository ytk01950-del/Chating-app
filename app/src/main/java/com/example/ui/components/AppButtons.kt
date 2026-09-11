package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.OnlineGreen

val AppButtonShape = RoundedCornerShape(14.dp)
val AppButtonSmallShape = RoundedCornerShape(10.dp)
val AppPillShape = RoundedCornerShape(24.dp)

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    shape: Shape = AppButtonShape,
    containerColor: Color = AppTheme.colors.accentOrange,
    contentColor: Color = Color.White,
    fontSize: TextUnit = 15.sp,
    testTag: String? = null
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "btnScale"
    )

    Button(
        onClick = { if (!isLoading && enabled) onClick() },
        enabled = enabled && !isLoading,
        modifier = modifier
            .scale(animatedScale)
            .height(height)
            .defaultMinSize(minHeight = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = colors.surfaceVariant,
            disabledContentColor = colors.textDisabled
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = contentColor,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                ),
                color = if (enabled) contentColor else colors.textDisabled,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 46.dp,
    shape: Shape = AppButtonShape,
    containerColor: Color = if (AppTheme.colors.isDark) Color(0xFF262626) else Color(0xFFEFEFEF),
    borderColor: Color = if (AppTheme.colors.isDark) Color(0xFF262626) else Color(0xFFDBDBDB),
    contentColor: Color = if (AppTheme.colors.isDark) Color(0xFFFFFFFF) else Color(0xFF111111),
    fontSize: TextUnit = 14.sp,
    testTag: String? = null
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "secBtnScale"
    )

    OutlinedButton(
        onClick = { if (!isLoading && enabled) onClick() },
        enabled = enabled && !isLoading,
        modifier = modifier
            .scale(animatedScale)
            .height(height)
            .defaultMinSize(minHeight = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = shape,
        interactionSource = interactionSource,
        border = BorderStroke(1.dp, if (enabled) borderColor else colors.border.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = colors.surface.copy(alpha = 0.5f),
            disabledContentColor = colors.textDisabled
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (enabled) contentColor else colors.textDisabled
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (enabled) contentColor else colors.textDisabled,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AppDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isFilled: Boolean = false,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    shape: Shape = AppButtonShape,
    testTag: String? = null
) {
    val errorRed = Color(0xFFEF4444)
    val errorRedContainer = Color(0xFF3F1618)
    val errorRedBorder = Color(0xFF8C1D24)
    val errorText = Color(0xFFFFB4AB)

    if (isFilled) {
        Button(
            onClick = { if (!isLoading && enabled) onClick() },
            enabled = enabled && !isLoading,
            modifier = modifier
                .height(height)
                .defaultMinSize(minHeight = 48.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = errorRed,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    } else {
        OutlinedButton(
            onClick = { if (!isLoading && enabled) onClick() },
            enabled = enabled && !isLoading,
            modifier = modifier
                .height(height)
                .defaultMinSize(minHeight = 48.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            shape = shape,
            border = BorderStroke(1.dp, errorRedBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = errorRedContainer.copy(alpha = 0.6f),
                contentColor = errorText
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = errorText,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = errorText)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = errorText)
        }
    }
}

@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.textPrimary,
    containerColor: Color = AppTheme.colors.surfaceVariant.copy(alpha = 0.8f),
    backgroundColor: Color = containerColor,
    borderColor: Color = AppTheme.colors.borderSubtle,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    shape: Shape = CircleShape,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val colors = AppTheme.colors
    val effectiveBg = if (backgroundColor != containerColor) backgroundColor else containerColor
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "iconBtnScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(animatedScale)
            .clip(shape)
            .background(if (enabled) effectiveBg else effectiveBg.copy(alpha = 0.4f))
            .border(
                1.dp,
                if (effectiveBg == Color.Transparent) Color.Transparent else if (enabled) borderColor else colors.border.copy(alpha = 0.4f),
                shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = tint),
                role = Role.Button,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else colors.textDisabled,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun AppSendButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    canSend: Boolean = true,
    enabled: Boolean = canSend,
    size: Dp = 42.dp,
    testTag: String = "send_message_button"
) {
    val colors = AppTheme.colors
    val isSendEnabled = canSend && enabled
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isSendEnabled) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "sendScale"
    )

    val bgColor = if (isSendEnabled) colors.accentOrange else colors.surfaceVariant
    val iconTint = if (isSendEnabled) Color.White else colors.textMuted

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, if (isSendEnabled) colors.accentOrange else colors.borderSubtle, CircleShape)
            .clickable(
                enabled = isSendEnabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                role = Role.Button,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send message",
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AppAttachmentGridItem(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "attachScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f))
                .border(1.dp, accentColor.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AppFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int? = null,
    showOnlineDot: Boolean = false
) {
    val colors = AppTheme.colors
    val containerColor = if (selected) {
        if (colors.isDark) Color.White else Color(0xFF111111)
    } else {
        if (colors.isDark) Color(0xFF1E1E1E) else Color(0xFFEFEFEF)
    }
    val borderColor = if (selected) {
        if (colors.isDark) Color.White else Color(0xFF111111)
    } else {
        if (colors.isDark) Color(0xFF333333) else Color(0xFFDBDBDB)
    }
    val contentColor = if (selected) {
        if (colors.isDark) Color.Black else Color.White
    } else {
        if (colors.isDark) Color.White else Color(0xFF111111)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showOnlineDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = contentColor
            )
            if (badgeCount != null && badgeCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($badgeCount)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        if (colors.isDark) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
                    } else if (colors.isDark) Color(0xFFE0E0E0) else colors.textMuted
                )
            }
        }
    }
}

@Composable
fun AppGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = AppTheme.colors.accentOrange,
    fontSize: TextUnit = 14.sp
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 44.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
        shape = AppButtonSmallShape
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = text, fontSize = fontSize, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val colors = AppTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = alpha))
    )
}

@Composable
fun AppChatListSkeleton(
    count: Int = 5,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppSkeletonBox(modifier = Modifier.size(52.dp), shape = CircleShape)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    AppSkeletonBox(modifier = Modifier.width(130.dp).height(16.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    AppSkeletonBox(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                AppSkeletonBox(modifier = Modifier.width(40.dp).height(10.dp))
            }
        }
    }
}

@Composable
fun instagramSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color(0xFF121212),
    checkedTrackColor = Color(0xFFEFEFEF),
    uncheckedThumbColor = Color(0xFF8E8E8E),
    uncheckedTrackColor = Color(0xFF363636),
    checkedBorderColor = Color.Transparent,
    uncheckedBorderColor = Color.Transparent
)

@Composable
fun InstagramSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val trackColor = if (checked) Color(0xFFEFEFEF) else Color(0xFF363636)
    val thumbColor = if (checked) Color(0xFF121212) else Color(0xFF8E8E8E)
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "instagram_thumb_offset"
    )

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) { onCheckedChange(!checked) }
            .padding(vertical = 3.dp, horizontal = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}
