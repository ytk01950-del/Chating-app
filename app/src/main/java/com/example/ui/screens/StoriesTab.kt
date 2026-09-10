package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Story
import com.example.model.User
import com.example.model.UserStoryGroup
import com.example.ui.components.AppIconButton
import com.example.ui.components.StoryAvatarRing
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StoriesTab(
    currentUser: User,
    myStories: List<Story>,
    groupedStories: List<UserStoryGroup>,
    onOpenAddStory: () -> Unit,
    onViewUserStories: (User, List<Story>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val hasMyStories = myStories.isNotEmpty()
    val otherStoriesWithActive = groupedStories.filter { it.stories.isNotEmpty() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("stories_tab_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // My Status Section
            item {
                Text(
                    text = "My Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            if (hasMyStories) {
                                onViewUserStories(currentUser, myStories)
                            } else {
                                onOpenAddStory()
                            }
                        }
                        .testTag("my_status_card"),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar with Active Story Ring or Add Icon
                        Box(contentAlignment = Alignment.Center) {
                            StoryAvatarRing(
                                hasActiveStory = hasMyStories,
                                hasUnseenStory = true,
                                size = 60.dp,
                                onClick = {
                                    if (hasMyStories) {
                                        onViewUserStories(currentUser, myStories)
                                    } else {
                                        onOpenAddStory()
                                    }
                                }
                            ) {
                                UserAvatar(
                                    name = currentUser.displayName.ifBlank { currentUser.username },
                                    avatarId = currentUser.avatarId,
                                    photoUrl = currentUser.photoUrl,
                                    size = if (hasMyStories) 48.dp else 52.dp,
                                    isOnline = true
                                )
                            }

                            if (!hasMyStories) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(colors.accentOrange)
                                        .border(2.dp, colors.surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Story",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "My Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            if (hasMyStories) {
                                val latest = myStories.maxByOrNull { it.createdAt }
                                val hoursLeft = latest?.let {
                                    val elapsed = System.currentTimeMillis() - it.createdAt
                                    val left = 24 - (elapsed / 3600000L).toInt()
                                    if (left > 0) "$left hours left" else "Expiring soon"
                                } ?: "24 hours active"

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${myStories.size} ${if (myStories.size == 1) "update" else "updates"} • $hoursLeft",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.accentOrange,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = "Tap to add a 24-hour photo or video story",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                        }

                        AppIconButton(
                            icon = if (hasMyStories) Icons.Default.Add else Icons.Default.PhotoCamera,
                            contentDescription = "Add Story",
                            onClick = onOpenAddStory,
                            tint = colors.accentOrange,
                            backgroundColor = colors.accentOrangePill,
                            size = 40.dp,
                            iconSize = 20.dp,
                            testTag = "add_story_btn_status"
                        )
                    }
                }
            }

            // Info Privacy Banner
            item {
                Surface(
                    color = if (colors.isDark) Color(0xFF262626) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF363636) else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = colors.accentOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Status updates disappear automatically after 24 hours.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Recent Updates Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Updates (${otherStoriesWithActive.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }

            if (otherStoriesWithActive.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "No Recent Status Updates",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "When your contacts post 24-hour stories, their status rings and updates will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(otherStoriesWithActive, key = { it.user.id }) { group ->
                    val latestStory = group.stories.maxByOrNull { it.createdAt }
                    val formattedTime = latestStory?.let { formatStoryTimestamp(it.createdAt) } ?: "Recent"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onViewUserStories(group.user, group.stories) }
                            .testTag("story_user_item_${group.user.id}"),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status update ring around avatar
                            StoryAvatarRing(
                                hasActiveStory = true,
                                hasUnseenStory = group.hasUnseenStories,
                                size = 56.dp,
                                onClick = { onViewUserStories(group.user, group.stories) }
                            ) {
                                UserAvatar(
                                    name = group.user.displayName.ifBlank { group.user.username },
                                    avatarId = group.user.avatarId,
                                    photoUrl = group.user.photoUrl,
                                    size = 46.dp,
                                    isOnline = group.user.isOnline
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.user.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = formattedTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (group.hasUnseenStories) colors.accentOrange else colors.textMuted,
                                        fontWeight = if (group.hasUnseenStories) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                val captionText = latestStory?.caption?.ifBlank {
                                    if (latestStory.mediaType == "video") "Shared a video story" else "Shared a photo story"
                                } ?: "Status update"

                                Text(
                                    text = captionText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (group.hasUnseenStories) colors.textSecondary else colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${group.stories.size} ${if (group.stories.size == 1) "story" else "stories"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accentOrange.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatStoryTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return "Just now"
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}
