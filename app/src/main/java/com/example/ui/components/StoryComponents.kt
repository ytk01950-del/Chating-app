package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Story
import com.example.model.User
import com.example.model.UserStoryGroup
import com.example.ui.theme.AppTheme

// Gradient for unread active stories (Instagram / Snapchat gradient style)
val StoryActiveGradient = Brush.sweepGradient(
    listOf(
        Color(0xFFFF8A00),
        Color(0xFFE52E71),
        Color(0xFF9B51E0),
        Color(0xFF00C6FF),
        Color(0xFFFF8A00)
    )
)

val StorySeenGradient = Brush.sweepGradient(
    listOf(
        Color(0xFF666666),
        Color(0xFF888888),
        Color(0xFF666666)
    )
)

@Composable
fun StoryAvatarRing(
    hasActiveStory: Boolean,
    hasUnseenStory: Boolean = true,
    size: Dp = 60.dp,
    ringWidth: Dp = 2.5.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!hasActiveStory) {
        Box(
            modifier = modifier
                .size(size)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    } else {
        val borderBrush = if (hasUnseenStory) StoryActiveGradient else StorySeenGradient
        Box(
            modifier = modifier
                .size(size)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .border(
                    width = ringWidth,
                    brush = borderBrush,
                    shape = CircleShape
                )
                .padding(ringWidth + 1.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * Modern Snapchat/Instagram-style top horizontal Story Bar
 */
@Composable
fun StoryTray(
    currentUser: User?,
    myStories: List<Story>,
    otherUsersStories: List<UserStoryGroup>,
    onOpenAddStory: () -> Unit,
    onViewUserStories: (User, List<Story>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Surface(
        color = colors.surface,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // "Your Story" slot
            item(key = "my_story_slot") {
                val hasMyStories = myStories.isNotEmpty()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clickable {
                            if (hasMyStories && currentUser != null) {
                                onViewUserStories(currentUser, myStories)
                            } else {
                                onOpenAddStory()
                            }
                        }
                        .testTag("my_story_tray_item")
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        StoryAvatarRing(
                            hasActiveStory = hasMyStories,
                            hasUnseenStory = false,
                            size = 58.dp
                        ) {
                            UserAvatar(
                                name = currentUser?.displayName ?: "Me",
                                avatarId = currentUser?.avatarId ?: 0,
                                photoUrl = currentUser?.photoUrl.orEmpty(),
                                size = if (hasMyStories) 50.dp else 56.dp
                            )
                        }

                        // Add (+) badge if no active story or as an affordance
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(colors.accentOrange)
                                .border(2.dp, colors.surface, CircleShape)
                                .clickable { onOpenAddStory() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Story",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (hasMyStories) "Your Story" else "Add Story",
                        color = if (colors.isDark) Color.White else colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (hasMyStories) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Other users' active stories
            items(
                items = otherUsersStories.filter { it.user.id != currentUser?.id && it.stories.isNotEmpty() },
                key = { it.user.id }
            ) { group ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clickable { onViewUserStories(group.user, group.stories) }
                        .testTag("user_story_${group.user.id}")
                ) {
                    StoryAvatarRing(
                        hasActiveStory = true,
                        hasUnseenStory = group.hasUnseenStories,
                        size = 58.dp
                    ) {
                        UserAvatar(
                            name = group.user.displayName,
                            avatarId = group.user.avatarId,
                            photoUrl = group.user.photoUrl,
                            size = 50.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = group.user.displayName.substringBefore(" "),
                        color = if (colors.isDark) Color.White else colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (group.hasUnseenStories) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
