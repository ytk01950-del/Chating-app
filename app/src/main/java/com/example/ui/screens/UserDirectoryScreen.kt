package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallRecord
import com.example.model.Story
import com.example.model.User
import com.example.model.UserStoryGroup
import com.example.ui.components.AppFilterChip
import com.example.ui.components.AppIconButton
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.CustomNexaLogo
import com.example.ui.components.FloatingBottomNavBar
import com.example.ui.components.NavigationTab
import com.example.ui.components.NexaGeometricLogo
import com.example.ui.components.StoryAvatarRing
import com.example.ui.components.StoryTray
import com.example.ui.components.UserAvatar
import com.example.ui.components.clickableWithPress
import com.example.ui.components.pressScale
import com.example.ui.theme.AppTheme
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalThemeUpdater
import com.example.ui.theme.OnlineGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDirectoryScreen(
    currentUser: User,
    users: List<User>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchUserResult: User?,
    searchResults: List<User> = emptyList(),
    isSearchingUser: Boolean,
    searchUserNotFound: Boolean,
    myStories: List<Story> = emptyList(),
    groupedStories: List<UserStoryGroup> = emptyList(),
    callHistory: List<CallRecord> = emptyList(),
    onStartVoiceCall: (User) -> Unit = {},
    onStartVideoCall: (User) -> Unit = {},
    onOpenOtherUserProfile: (User) -> Unit = {},
    onOpenAddStory: () -> Unit = {},
    onViewUserStories: (User, List<Story>) -> Unit = { _, _ -> },
    onSearchByChatId: (String) -> Unit = {},
    onSearchDirectory: (String) -> Unit = {},
    onClearChatIdSearch: () -> Unit = {},
    onSelectUser: (User) -> Unit,
    onOpenProfile: () -> Unit = {},
    onUploadProfilePhoto: (Uri) -> Unit = {},
    onUpdateProfile: (String, String, String, Int, String) -> Unit = { _, _, _, _, _ -> },
    onUpdateProfileExtended: (String, String, String, Int, String, String, String) -> Unit = { _, _, _, _, _, _, _ -> },
    leaderboardUsers: List<User> = emptyList(),
    isLeaderboardLoading: Boolean = false,
    onRefreshLeaderboard: () -> Unit = {},
    onClaimUsername: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onCheckUsernameAvailable: suspend (String) -> Boolean = { true },
    onOpenSettings: () -> Unit = {},
    onOpenFollowers: (User) -> Unit = {},
    onOpenFollowing: (User) -> Unit = {},
    onFollowClick: (User) -> Unit = {},
    onUnfollowClick: (User) -> Unit = {},
    onUpdatePrivacy: (String, Boolean, Boolean, Boolean, String) -> Unit = { _, _, _, _, _ -> },
    isUploadingProfilePhoto: Boolean = false,
    currentThemeMode: AppThemeMode = AppTheme.mode,
    onThemeModeChange: (AppThemeMode) -> Unit = LocalThemeUpdater.current,
    onSignOut: () -> Unit
) {
    val colors = AppTheme.colors
    var selectedTab by remember { mutableStateOf(NavigationTab.CHATS) }
    var filterOnlineOnly by remember { mutableStateOf(false) }
    var isSearchUsersMode by remember { mutableStateOf(false) }
    var searchUserQuery by remember { mutableStateOf("") }

    val displayedUsers = remember(users, filterOnlineOnly) {
        users.filter { user ->
            if (filterOnlineOnly) user.isOnline else true
        }
    }

    val onlineCount = remember(users) { users.count { it.isOnline } }
    val missedCallsCount = remember(callHistory) { callHistory.count { it.isMissed() } }
    val hasNewStories = remember(groupedStories) { groupedStories.any { it.hasUnseenStories } }
    val needsUsername = remember(currentUser.username) { currentUser.username.isBlank() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = Color.Black,
        bottomBar = {
            FloatingBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                unreadChatsCount = 0,
                missedCallsCount = missedCallsCount,
                hasNewStories = hasNewStories
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(120)) togetherWith fadeOut(animationSpec = tween(120))
                },
                label = "tab_content_transition"
            ) { targetTab ->
                when (targetTab) {
                    NavigationTab.CHATS -> {
                        ChatsTabView(
                            currentUser = currentUser,
                            users = users,
                            displayedUsers = displayedUsers,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            filterOnlineOnly = filterOnlineOnly,
                            onToggleOnlineFilter = { filterOnlineOnly = !filterOnlineOnly },
                            onlineCount = onlineCount,
                            isSearchUsersMode = isSearchUsersMode,
                            onToggleSearchUsersMode = { isSearchUsersMode = it },
                            searchUserQuery = searchUserQuery,
                            onSearchUserQueryChange = { searchUserQuery = it },
                            searchUserResult = searchUserResult,
                            searchResults = searchResults,
                            isSearchingUser = isSearchingUser,
                            searchUserNotFound = searchUserNotFound,
                            onSearchByChatId = onSearchByChatId,
                            onSearchDirectory = onSearchDirectory,
                            onClearChatIdSearch = onClearChatIdSearch,
                            myStories = myStories,
                            groupedStories = groupedStories,
                            onOpenAddStory = onOpenAddStory,
                            onViewUserStories = onViewUserStories,
                            onSelectUser = onSelectUser,
                            onOpenProfile = { selectedTab = NavigationTab.PROFILE },
                            needsUsername = needsUsername
                        )
                    }

                    NavigationTab.CALLS -> {
                        CallsTab(
                            callHistory = callHistory,
                            availableUsers = users,
                            onStartVoiceCall = onStartVoiceCall,
                            onStartVideoCall = onStartVideoCall,
                            onOpenUserProfile = onOpenOtherUserProfile
                        )
                    }

                    NavigationTab.LEADERBOARD -> {
                        LeaderboardScreen(
                            currentUser = currentUser,
                            leaderboardUsers = leaderboardUsers,
                            isLoading = isLeaderboardLoading,
                            onRefresh = onRefreshLeaderboard,
                            onSelectUser = onSelectUser,
                            onOpenUserProfile = onOpenOtherUserProfile
                        )
                    }

                    NavigationTab.STORIES -> {
                        StoriesTab(
                            currentUser = currentUser,
                            myStories = myStories,
                            groupedStories = groupedStories,
                            onOpenAddStory = onOpenAddStory,
                            onViewUserStories = onViewUserStories
                        )
                    }

                    NavigationTab.PROFILE -> {
                        ProfileDetailsScreen(
                            currentUser = currentUser,
                            profileUser = currentUser,
                            isFollowing = false,
                            onBack = null,
                            onOpenChat = {},
                            onFollowClick = { targetUser -> onFollowClick(targetUser) },
                            onUnfollowClick = { targetUser -> onUnfollowClick(targetUser) },
                            onOpenFollowers = { targetUser -> onOpenFollowers(targetUser) },
                            onOpenFollowing = { targetUser -> onOpenFollowing(targetUser) },
                            onUpdatePrivacy = onUpdatePrivacy,
                            onUpdateProfile = onUpdateProfileExtended,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.padding(bottom = 76.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatsTabView(
    currentUser: User,
    users: List<User>,
    displayedUsers: List<User>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterOnlineOnly: Boolean,
    onToggleOnlineFilter: () -> Unit,
    onlineCount: Int,
    isSearchUsersMode: Boolean,
    onToggleSearchUsersMode: (Boolean) -> Unit,
    searchUserQuery: String,
    onSearchUserQueryChange: (String) -> Unit,
    searchUserResult: User?,
    searchResults: List<User>,
    isSearchingUser: Boolean,
    searchUserNotFound: Boolean,
    onSearchByChatId: (String) -> Unit,
    onSearchDirectory: (String) -> Unit,
    onClearChatIdSearch: () -> Unit,
    myStories: List<Story>,
    groupedStories: List<UserStoryGroup>,
    onOpenAddStory: () -> Unit,
    onViewUserStories: (User, List<Story>) -> Unit,
    onSelectUser: (User) -> Unit,
    onOpenProfile: () -> Unit,
    needsUsername: Boolean
) {
    val colors = AppTheme.colors
    val storiesMap = remember(groupedStories) {
        groupedStories.associateBy { it.user.id }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { }
            .background(Color.Black)
            .testTag("chats_directory_list"),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Natural Collapsing Screen Header (Title + Camera + Search)
        item(key = "chats_screen_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CustomNexaLogo(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        hasBorder = false,
                        testTag = "chats_tab_nexa_logo"
                    )
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = colors.textPrimary,
                        modifier = Modifier.testTag("dynamic_tab_title")
                    )
                }

                IconButton(
                    onClick = onOpenAddStory,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("top_bar_camera_action")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 2. Claim username banner
        if (needsUsername) {
            item(key = "claim_username_banner") {
                Surface(
                    color = if (colors.isDark) Color(0xFF262626) else Color(0xFFF0F7FF),
                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF363636) else Color(0xFFBFDBFE)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickableWithPress { onOpenProfile() }
                        .testTag("claim_username_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Choose Your Unique Chat ID",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Tap here to claim your @ChatID in Settings so others can find you.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Active Story Tray (Quick 24-Hour Stories Access)
        item(key = "story_tray_section") {
            StoryTray(
                currentUser = currentUser,
                myStories = myStories,
                otherUsersStories = groupedStories,
                onOpenAddStory = onOpenAddStory,
                onViewUserStories = onViewUserStories
            )
        }

        // 4. Search Bar
        item(key = "search_and_filters_section") {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                // Chats Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Filter chats by name or @chat_id...",
                            color = Color(0xFF9E9E9E),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF9E9E9E)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = if (colors.isDark) Color(0xFF262626) else Color(0xFFEFEFEF),
                        unfocusedContainerColor = if (colors.isDark) Color(0xFF262626) else Color(0xFFEFEFEF),
                        focusedBorderColor = colors.textPrimary,
                        unfocusedBorderColor = if (colors.isDark) Color(0xFF363636) else Color(0xFFDBDBDB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_user_input")
                )
            }
        }

        // 5. Main Conversation List
        if (displayedUsers.isEmpty()) {
            item(key = "empty_conversations") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(colors.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching conversations found" else "No active conversations yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Make sure the name or Chat ID is spelled correctly." else "Your direct messages will appear here. Find someone by their @ChatID or name to begin chatting!",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = displayedUsers,
                key = { it.id },
                contentType = { "user_chat_item" }
            ) { user ->
                val userStoryGroup = storiesMap[user.id]
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                    UserItemCard(
                        user = user,
                        userStories = userStoryGroup?.stories ?: emptyList(),
                        hasUnseenStories = userStoryGroup?.hasUnseenStories ?: false,
                        onStoryClick = {
                            if (userStoryGroup != null && userStoryGroup.stories.isNotEmpty()) {
                                onViewUserStories(user, userStoryGroup.stories)
                            } else {
                                onSelectUser(user)
                            }
                        },
                        onClick = { onSelectUser(user) }
                    )
                }
            }
        }
    }
}

@Composable
fun UserItemCard(
    user: User,
    userStories: List<Story> = emptyList(),
    hasUnseenStories: Boolean = false,
    onStoryClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val hasStories = userStories.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { }
            .clickableWithPress { onClick() }
            .testTag("user_item_${user.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoryAvatarRing(
                hasActiveStory = hasStories,
                hasUnseenStory = hasUnseenStories,
                size = 52.dp,
                onClick = onStoryClick
            ) {
                UserAvatar(
                    name = user.displayName.ifBlank { user.username },
                    avatarId = user.avatarId,
                    photoUrl = user.photoUrl,
                    size = if (hasStories) 44.dp else 48.dp,
                    isOnline = null
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (user.username.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val statusText = if (user.isOnline) {
                        "Online"
                    } else {
                        com.example.util.DateTimeUtils.formatLastSeen(user.lastSeen)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (user.isOnline) OnlineGreen else colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = user.statusMessage.ifBlank { "Hey there! I am using Nexachat." },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            AppIconButton(
                icon = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "Start Chat",
                onClick = onClick,
                tint = colors.accentOrange,
                size = 38.dp,
                iconSize = 18.dp
            )
        }
    }
}
