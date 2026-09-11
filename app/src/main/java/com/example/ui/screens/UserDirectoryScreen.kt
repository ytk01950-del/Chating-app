package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.example.ui.components.FloatingBottomNavBar
import com.example.ui.components.NavigationTab
import com.example.ui.components.StoryAvatarRing
import com.example.ui.components.StoryTray
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AppTheme
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalThemeUpdater
import com.example.ui.theme.OnlineGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onClaimUsername: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onCheckUsernameAvailable: suspend (String) -> Boolean = { true },
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

    val displayedUsers = users.filter { user ->
        if (filterOnlineOnly) user.isOnline else true
    }

    val onlineCount = users.count { it.isOnline }
    val missedCallsCount = callHistory.count { it.isMissed() }
    val hasNewStories = groupedStories.any { it.hasUnseenStories }
    val needsUsername = currentUser.username.isBlank()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = colors.background,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (colors.isDark) Color(0xFF000000) else colors.surface,
                border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF262626) else colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedTab) {
                            NavigationTab.CHATS -> "Chats"
                            NavigationTab.CALLS -> "Calls"
                            NavigationTab.STORIES -> "Stories"
                            NavigationTab.SETTINGS -> "Settings"
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = colors.textPrimary,
                        modifier = Modifier.testTag("dynamic_tab_title")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        IconButton(
                            onClick = {
                                if (selectedTab != NavigationTab.CHATS) {
                                    selectedTab = NavigationTab.CHATS
                                }
                                isSearchUsersMode = !isSearchUsersMode
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("top_bar_search_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchUsersMode) colors.accentOrange else colors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
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
                .padding(top = paddingValues.calculateTopPadding())
                .background(colors.background)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
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
                            onOpenProfile = { selectedTab = NavigationTab.SETTINGS },
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

                    NavigationTab.STORIES -> {
                        StoriesTab(
                            currentUser = currentUser,
                            myStories = myStories,
                            groupedStories = groupedStories,
                            onOpenAddStory = onOpenAddStory,
                            onViewUserStories = onViewUserStories
                        )
                    }

                    NavigationTab.SETTINGS -> {
                        SettingsTab(
                            currentUser = currentUser,
                            onUploadProfilePhoto = onUploadProfilePhoto,
                            onUpdateProfile = onUpdateProfile,
                            onClaimUsername = onClaimUsername,
                            onCheckUsernameAvailable = onCheckUsernameAvailable,
                            isUploadingPhoto = isUploadingProfilePhoto,
                            currentThemeMode = currentThemeMode,
                            onThemeModeChange = onThemeModeChange,
                            onSignOut = onSignOut
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Claim username banner
        if (needsUsername) {
            Surface(
                color = if (colors.isDark) Color(0xFF262626) else Color(0xFFF0F7FF),
                border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF363636) else Color(0xFFBFDBFE)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onOpenProfile() }
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

        // Active Story Tray (Quick 24-Hour Stories Access)
        StoryTray(
            currentUser = currentUser,
            myStories = myStories,
            otherUsersStories = groupedStories,
            onOpenAddStory = onOpenAddStory,
            onViewUserStories = onViewUserStories
        )

        // Search and Filter Bar
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            if (!isSearchUsersMode) {
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
            } else {
                // Global User Directory Search Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchUserQuery,
                        onValueChange = { input ->
                            val clean = input.filter { it.isLetterOrDigit() || it == '_' || it == '.' || it == '@' || it == ' ' }
                            onSearchUserQueryChange(clean)
                            onClearChatIdSearch()
                        },
                        placeholder = {
                            Text("Search @chat_id or display name...", color = Color(0xFF9E9E9E), fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PersonSearch,
                                contentDescription = "Search Users",
                                tint = Color(0xFF9E9E9E)
                            )
                        },
                        trailingIcon = {
                            if (searchUserQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    onSearchUserQueryChange("")
                                    onClearChatIdSearch()
                                }) {
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
                            .weight(1f)
                            .testTag("chat_id_search_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    AppPrimaryButton(
                        text = "Find",
                        onClick = {
                            onSearchByChatId(searchUserQuery)
                            onSearchDirectory(searchUserQuery)
                        },
                        isLoading = isSearchingUser,
                        enabled = searchUserQuery.trim().length >= 2 && !isSearchingUser,
                        height = 48.dp,
                        testTag = "chat_id_search_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    AppFilterChip(
                        selected = !isSearchUsersMode && !filterOnlineOnly,
                        onClick = {
                            onToggleSearchUsersMode(false)
                            if (filterOnlineOnly) onToggleOnlineFilter()
                        },
                        label = "All Chats",
                        badgeCount = users.size
                    )
                }
                item {
                    AppFilterChip(
                        selected = !isSearchUsersMode && filterOnlineOnly,
                        onClick = {
                            onToggleSearchUsersMode(false)
                            if (!filterOnlineOnly) onToggleOnlineFilter()
                        },
                        label = "Online",
                        badgeCount = onlineCount,
                        showOnlineDot = true
                    )
                }
                item {
                    AppFilterChip(
                        selected = isSearchUsersMode,
                        onClick = {
                            onToggleSearchUsersMode(!isSearchUsersMode)
                        },
                        label = "Find @ChatID",
                        badgeCount = if (searchResults.isNotEmpty()) searchResults.size else 0
                    )
                }
            }
        }

        // Body Content
        if (isSearchUsersMode) {
            // Search Users Mode Body
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                when {
                    isSearchingUser -> {
                        Column(
                            modifier = Modifier.padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = colors.accentOrange)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Searching user directory...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                    searchUserResult != null -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("matched_user_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                UserAvatar(
                                    name = searchUserResult.displayName.ifBlank { searchUserResult.username },
                                    avatarId = searchUserResult.avatarId,
                                    photoUrl = searchUserResult.photoUrl,
                                    size = 72.dp,
                                    isOnline = searchUserResult.isOnline
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = searchUserResult.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )

                                if (searchUserResult.username.isNotBlank()) {
                                    Text(
                                        text = "@${searchUserResult.username}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.accentOrange
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = searchUserResult.statusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                AppPrimaryButton(
                                    text = "Message @${searchUserResult.username.ifBlank { searchUserResult.displayName }}",
                                    onClick = { onSelectUser(searchUserResult) },
                                    icon = Icons.AutoMirrored.Filled.Chat,
                                    height = 48.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "message_matched_user_button"
                                )
                            }
                        }
                    }
                    searchResults.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "Matching Users (${searchResults.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.accentOrange,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            items(searchResults, key = { it.id }) { matchedUser ->
                                UserItemCard(
                                    user = matchedUser,
                                    onClick = { onSelectUser(matchedUser) }
                                )
                            }
                        }
                    }
                    searchUserNotFound -> {
                        Column(
                            modifier = Modifier.padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AlternateEmail,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No user found matching '$searchUserQuery'",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Make sure the Chat ID or name is spelled correctly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier.padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = colors.accentOrange,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Find Anyone on WP CHAT",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enter another user's unique @ChatID or display name above to find them and start a conversation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Main Conversation List
            if (displayedUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                            text = if (searchQuery.isNotEmpty()) "Try searching by their exact Chat ID or name in Search tab." else "Your direct messages will appear here. Find someone by their @ChatID or name to begin chatting!",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AppPrimaryButton(
                            text = "Find Users to Chat",
                            onClick = { onToggleSearchUsersMode(true) },
                            icon = Icons.Default.Search,
                            height = 44.dp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedUsers, key = { it.id }) { user ->
                        val userStoryGroup = groupedStories.firstOrNull { it.user.id == user.id }
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
            .clickable { onClick() }
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
                    isOnline = user.isOnline
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
                        formatLastSeen(user.lastSeen)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (user.isOnline) OnlineGreen else colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = user.statusMessage.ifBlank { "Hey there! I am using WP CHAT." },
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

fun formatLastSeen(timestamp: Long): String {
    if (timestamp <= 0) return "Offline"
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
