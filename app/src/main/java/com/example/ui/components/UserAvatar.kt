package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.OfflineGray
import com.example.ui.theme.OnlineGreen

val AvatarColorPairs = listOf(
    Pair(Color(0xFFA8C7FA), Color(0xFF062E6F)),
    Pair(Color(0xFF80CBC4), Color(0xFF003833)),
    Pair(Color(0xFFFFB4AB), Color(0xFF690005)),
    Pair(Color(0xFFD0BCFF), Color(0xFF381E72)),
    Pair(Color(0xFFC4EED0), Color(0xFF0D381E)),
    Pair(Color(0xFFFFDCC2), Color(0xFF552200)),
    Pair(Color(0xFFE2E2E6), Color(0xFF2E3036)),
    Pair(Color(0xFFA6EEFF), Color(0xFF00363D))
)

val AvatarGradients = AvatarColorPairs.map { listOf(it.first, it.first) }

@Composable
fun UserAvatar(
    name: String,
    avatarId: Int,
    photoUrl: String = "",
    size: Dp = 48.dp,
    isOnline: Boolean? = null,
    modifier: Modifier = Modifier
) {
    val colorIndex = (avatarId.coerceAtLeast(0)) % AvatarColorPairs.size
    val (bgColor, textColor) = AvatarColorPairs[colorIndex]
    val initials = if (name.isNotBlank()) {
        val parts = name.trim().split(" ")
        if (parts.size >= 2) "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        else name.take(2).uppercase()
    } else "??"

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Main Avatar Circle
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile photo of $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(DarkSurface)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp
                )
            }
        }

        // Online/Offline Indicator Badge
        if (isOnline != null) {
            val badgeSize = (size * 0.28f).coerceAtLeast(10.dp)
            val badgeOffset = (size * 0.04f)

            if (isOnline) {
                // Pulsing glow behind
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = -badgeOffset, y = -badgeOffset)
                        .size(badgeSize)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(OnlineGreen.copy(alpha = 0.35f))
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = -badgeOffset, y = -badgeOffset)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(if (isOnline) OnlineGreen else OfflineGray)
                    .border(2.dp, DarkSurface, CircleShape)
            )
        }
    }
}
