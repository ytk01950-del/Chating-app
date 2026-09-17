package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AppTheme
import com.example.util.DateTimeUtils

private val GoldColor = Color(0xFFFFD700)
private val SilverColor = Color(0xFFC0C0C0)
private val BronzeColor = Color(0xFFCD7F32)

@Composable
fun LeaderboardScreen(
    currentUser: User?,
    leaderboardUsers: List<User>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSelectUser: (User) -> Unit,
    onOpenUserProfile: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    // Calculate current user's rank
    val currentUserRankInfo by remember(leaderboardUsers, currentUser) {
        derivedStateOf {
            if (currentUser == null) return@derivedStateOf null
            val index = leaderboardUsers.indexOfFirst { it.id == currentUser.id }
            if (index >= 0) {
                Pair(index + 1, true) // rank number, is in top 100
            } else {
                Pair(101, false) // outside top 100
            }
        }
    }

    val top3Users = remember(leaderboardUsers) {
        leaderboardUsers.take(3)
    }

    val remainingUsers = remember(leaderboardUsers) {
        if (leaderboardUsers.size > 3) leaderboardUsers.drop(3) else emptyList()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("leaderboard_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { }
                .testTag("leaderboard_list"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 160.dp // Padding for floating nav bar and sticky user rank card
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Header Section
            item(key = "leaderboard_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1E1E1E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Leaderboard Trophy",
                                    tint = GoldColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Leaderboard",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 26.sp,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "Top 100 Most Active Members",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A1A1A))
                                .testTag("leaderboard_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Leaderboard",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Info banner explaining active time
                    Surface(
                        color = Color(0xFF141414),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF262626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rankings update in real-time based on active time spent using Nexachat in foreground.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // 2. Loading Indicator
            if (isLoading && leaderboardUsers.isEmpty()) {
                item(key = "leaderboard_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = GoldColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // 3. Top 3 Podium Section
            if (top3Users.isNotEmpty()) {
                item(key = "leaderboard_podium") {
                    LeaderboardPodium(
                        topUsers = top3Users,
                        currentUserId = currentUser?.id.orEmpty(),
                        onUserClick = onOpenUserProfile
                    )
                }
            }

            // 4. Section divider / subtitle for Rank 4 to 100
            if (remainingUsers.isNotEmpty()) {
                item(key = "leaderboard_section_title") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp, start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TOP 100 RANKINGS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${leaderboardUsers.size} Ranked",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 5. Remaining Ranks List (Rank 4 to 100)
            itemsIndexed(
                items = remainingUsers,
                key = { _, user -> user.id.ifBlank { "user_${user.username}" } },
                contentType = { _, _ -> "leaderboard_row" }
            ) { index, user ->
                val rank = index + 4
                val isCurrentUser = user.id == currentUser?.id

                LeaderboardRowItem(
                    rank = rank,
                    user = user,
                    isCurrentUser = isCurrentUser,
                    onClick = { onOpenUserProfile(user) }
                )
            }

            // 6. Empty State if no users found
            if (!isLoading && leaderboardUsers.isEmpty()) {
                item(key = "leaderboard_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Leaderboard is currently updating",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Active usage times are being tallied. Keep chatting!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }

        // 7. Sticky Floating "Your Ranking" Card at bottom (above bottom nav bar)
        if (currentUser != null) {
            val (rankNumber, isInTop100) = currentUserRankInfo ?: Pair(0, false)

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 86.dp)
                    .fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF161616),
                    border = BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color.Black)
                        .testTag("user_ranking_card")
                        .clickable { onOpenUserProfile(currentUser) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Rank Badge
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (rankNumber) {
                                            1 -> GoldColor.copy(alpha = 0.2f)
                                            2 -> SilverColor.copy(alpha = 0.2f)
                                            3 -> BronzeColor.copy(alpha = 0.2f)
                                            else -> Color(0xFF2A2A2A)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isInTop100) "#$rankNumber" else "100+",
                                    color = when (rankNumber) {
                                        1 -> GoldColor
                                        2 -> SilverColor
                                        3 -> BronzeColor
                                        else -> Color.White
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isInTop100 && rankNumber < 100) 12.sp else 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            UserAvatar(
                                name = currentUser.displayName.ifBlank { currentUser.username },
                                avatarId = currentUser.avatarId,
                                photoUrl = currentUser.photoUrl,
                                size = 40.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentUser.displayName.ifBlank { currentUser.username }.ifBlank { "You" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(You)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = if (isInTop100) "Rank #$rankNumber of Top 100" else "Outside Top 100 • Keep using to climb",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isInTop100) GoldColor.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Usage Time Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF252525),
                            border = BorderStroke(1.dp, Color(0xFF383838))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = GoldColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = DateTimeUtils.formatActiveUsageTime(currentUser.effectiveActiveTimeMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top 3 Podium component highlighting #1 (Center, Gold), #2 (Left, Silver), #3 (Right, Bronze)
 */
@Composable
private fun LeaderboardPodium(
    topUsers: List<User>,
    currentUserId: String,
    onUserClick: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val first = topUsers.getOrNull(0)
    val second = topUsers.getOrNull(1)
    val third = topUsers.getOrNull(2)

    Surface(
        color = Color(0xFF101010),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF222222)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Rank #2 (Silver) - Left
                if (second != null) {
                    PodiumColumn(
                        user = second,
                        rank = 2,
                        badgeColor = SilverColor,
                        badgeText = "2",
                        isCurrentUser = second.id == currentUserId,
                        avatarSize = 56.dp,
                        podiumHeight = 85.dp,
                        onClick = { onUserClick(second) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Rank #1 (Gold) - Center (Elevated)
                if (first != null) {
                    PodiumColumn(
                        user = first,
                        rank = 1,
                        badgeColor = GoldColor,
                        badgeText = "1",
                        isCurrentUser = first.id == currentUserId,
                        avatarSize = 68.dp,
                        podiumHeight = 110.dp,
                        isFirstPlace = true,
                        onClick = { onUserClick(first) },
                        modifier = Modifier.weight(1.15f)
                    )
                }

                // Rank #3 (Bronze) - Right
                if (third != null) {
                    PodiumColumn(
                        user = third,
                        rank = 3,
                        badgeColor = BronzeColor,
                        badgeText = "3",
                        isCurrentUser = third.id == currentUserId,
                        avatarSize = 54.dp,
                        podiumHeight = 70.dp,
                        onClick = { onUserClick(third) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    user: User,
    rank: Int,
    badgeColor: Color,
    badgeText: String,
    isCurrentUser: Boolean,
    avatarSize: androidx.compose.ui.unit.Dp,
    podiumHeight: androidx.compose.ui.unit.Dp,
    isFirstPlace: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Crown / Trophy icon for #1
        if (isFirstPlace) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Champion Crown",
                tint = GoldColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Avatar with colored rank border
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize + 4.dp)
                    .clip(CircleShape)
                    .border(2.dp, badgeColor, CircleShape)
                    .padding(2.dp)
            ) {
                UserAvatar(
                    name = user.displayName.ifBlank { user.username },
                    avatarId = user.avatarId,
                    photoUrl = user.photoUrl,
                    size = avatarSize
                )
            }

            // Rank Badge Tag
            Box(
                modifier = Modifier
                    .offset(y = 6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // User Display Name
        Text(
            text = user.displayName.ifBlank { user.username }.ifBlank { "User" },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isFirstPlace) FontWeight.Bold else FontWeight.Medium,
            color = if (isCurrentUser) GoldColor else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = if (isFirstPlace) 13.sp else 11.5.sp
        )

        // Chat ID
        if (user.username.isNotBlank()) {
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Podium Block
        Surface(
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = if (isFirstPlace) Color(0xFF1E1E1E) else Color(0xFF161616),
            border = BorderStroke(1.dp, if (isFirstPlace) GoldColor.copy(alpha = 0.4f) else Color(0xFF2A2A2A)),
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when (rank) {
                        1 -> "1st"
                        2 -> "2nd"
                        else -> "3rd"
                    },
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    fontSize = if (isFirstPlace) 15.sp else 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Time spent
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = DateTimeUtils.formatActiveUsageTime(user.effectiveActiveTimeMs),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Standard Leaderboard Row item for Ranks 4 through 100
 */
@Composable
private fun LeaderboardRowItem(
    rank: Int,
    user: User,
    isCurrentUser: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrentUser) Color(0xFF1A1A1A) else Color(0xFF0F0F0F),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCurrentUser) GoldColor.copy(alpha = 0.5f) else Color(0xFF1F1F1F)
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { }
            .clickable(onClick = onClick)
            .testTag("leaderboard_user_row_$rank")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank Number
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser) GoldColor else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(36.dp),
                    fontSize = 13.sp
                )

                // Avatar
                UserAvatar(
                    name = user.displayName.ifBlank { user.username },
                    avatarId = user.avatarId,
                    photoUrl = user.photoUrl,
                    size = 42.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Name and Chat ID
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName.ifBlank { user.username }.ifBlank { "User" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(You)",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (user.username.isNotBlank()) {
                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Usage Time Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E1E1E),
                border = BorderStroke(1.dp, Color(0xFF2D2D2D))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (isCurrentUser) GoldColor else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = DateTimeUtils.formatActiveUsageTime(user.effectiveActiveTimeMs),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}
