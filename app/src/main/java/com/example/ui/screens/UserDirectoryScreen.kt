package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.User
import com.example.ui.components.UserAvatar
import com.example.ui.components.AppIconButton
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.AppFilterChip
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueDark
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
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
    onSearchByChatId: (String) -> Unit,
    onSearchDirectory: (String) -> Unit = {},
    onClearChatIdSearch: () -> Unit,
    onSelectUser: (User) -> Unit,
    onOpenProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    var activeSearchTab by remember { mutableIntStateOf(0) } // 0 = Chats, 1 = Search Directory
    var chatIdQuery by remember { mutableStateOf("") }
    var filterOnlineOnly by remember { mutableStateOf(false) }

    val displayedUsers = users.filter { user ->
        if (filterOnlineOnly) user.isOnline else true
    }

    val onlineCount = users.count { it.isOnline }
    val needsUsername = currentUser.username.isBlank()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = DarkBg,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenProfile() }
                    ) {
                        UserAvatar(
                            name = currentUser.displayName.ifBlank { currentUser.username },
                            avatarId = currentUser.avatarId,
                            photoUrl = currentUser.photoUrl,
                            size = 40.dp,
                            isOnline = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentUser.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (currentUser.username.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "@${currentUser.username}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentBlue,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(OnlineGreen)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Online • Real-time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppIconButton(
                            icon = Icons.Default.Person,
                            contentDescription = "Profile",
                            onClick = onOpenProfile,
                            tint = AccentBlue,
                            size = 40.dp,
                            iconSize = 20.dp,
                            testTag = "profile_button"
                        )

                        AppIconButton(
                            icon = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            onClick = onSignOut,
                            tint = TextSecondary,
                            size = 40.dp,
                            iconSize = 20.dp,
                            testTag = "signout_button"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBg)
        ) {
            // Prompt banner for existing users who haven't claimed a Chat ID yet
            if (needsUsername) {
                Surface(
                    color = Color(0xFF2A1C0A),
                    border = BorderStroke(1.dp, Color(0xFF8A5812)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Choose Your Unique Chat ID",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFE0B2)
                            )
                            Text(
                                text = "Tap here to claim your @ChatID so other users can search and message you.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFCC80),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Mode Tabs: "All Contacts" vs "Search by Chat ID"
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column {
                    TabRow(
                        selectedTabIndex = activeSearchTab,
                        containerColor = DarkSurface,
                        contentColor = TextPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeSearchTab]),
                                color = AccentBlue,
                                height = 3.dp
                            )
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = activeSearchTab == 0,
                            onClick = { activeSearchTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = if (activeSearchTab == 0) AccentBlue else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Chats (${users.size})",
                                        color = if (activeSearchTab == 0) AccentBlue else TextSecondary,
                                        fontWeight = if (activeSearchTab == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_contacts")
                        )
                        Tab(
                            selected = activeSearchTab == 1,
                            onClick = {
                                if (needsUsername) {
                                    onOpenProfile()
                                } else {
                                    activeSearchTab = 1
                                }
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = if (activeSearchTab == 1) AccentBlue else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Search Users",
                                        color = if (activeSearchTab == 1) AccentBlue else TextSecondary,
                                        fontWeight = if (activeSearchTab == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_search_chat_id")
                        )
                    }

                    // Content for Tab 0: Regular Filter on existing conversations
                    if (activeSearchTab == 0) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = {
                                    Text(
                                        "Filter chats by name or @chat_id...",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = AccentBlue
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = TextSecondary
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkBg,
                                    unfocusedContainerColor = DarkBg,
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = DarkBorderSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_user_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Filter Chips Row
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    AppFilterChip(
                                        selected = !filterOnlineOnly,
                                        onClick = { filterOnlineOnly = false },
                                        label = "All",
                                        badgeCount = users.size
                                    )
                                }
                                item {
                                    AppFilterChip(
                                        selected = filterOnlineOnly,
                                        onClick = { filterOnlineOnly = true },
                                        label = "Online",
                                        badgeCount = onlineCount,
                                        showOnlineDot = true
                                    )
                                }
                            }
                        }
                    } else {
                        // Content for Tab 1: Search Users by Chat ID or Name
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = chatIdQuery,
                                    onValueChange = { input ->
                                        chatIdQuery = input.filter { it.isLetterOrDigit() || it == '_' || it == '.' || it == '@' || it == ' ' }
                                        onClearChatIdSearch()
                                    },
                                    placeholder = {
                                        Text("Search @chat_id or display name...", color = TextSecondary, fontSize = 14.sp)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.PersonSearch,
                                            contentDescription = "Search Users",
                                            tint = AccentBlue
                                        )
                                    },
                                    trailingIcon = {
                                        if (chatIdQuery.isNotEmpty()) {
                                            IconButton(onClick = {
                                                chatIdQuery = ""
                                                onClearChatIdSearch()
                                            }) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "Clear",
                                                    tint = TextSecondary
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = DarkBg,
                                        unfocusedContainerColor = DarkBg,
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = DarkBorderSubtle
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_id_search_input")
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                AppPrimaryButton(
                                    text = "Search",
                                    onClick = {
                                        onSearchByChatId(chatIdQuery)
                                        onSearchDirectory(chatIdQuery)
                                    },
                                    isLoading = isSearchingUser,
                                    enabled = chatIdQuery.trim().length >= 2 && !isSearchingUser,
                                    height = 50.dp,
                                    testTag = "chat_id_search_button"
                                )
                            }
                        }
                    }
                }
            }

            // View rendering based on active tab
            if (activeSearchTab == 1) {
                // Search users results view
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when {
                        isSearchingUser -> {
                            Column(
                                modifier = Modifier.padding(top = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = AccentBlue)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Searching user directory...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        searchUserResult != null -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("matched_user_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(1.dp, DarkBorder)
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
                                        color = TextPrimary
                                    )

                                    if (searchUserResult.username.isNotBlank()) {
                                        Text(
                                            text = "@${searchUserResult.username}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentBlue
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = searchUserResult.statusMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
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
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text(
                                        text = "Matching Users (${searchResults.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AccentBlue,
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
                                        .background(DarkSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AlternateEmail,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No user found matching '$chatIdQuery'",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Make sure the Chat ID or name is spelled correctly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
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
                                    tint = AccentBlue,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Find Anyone on WP CHAT",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Enter another user's unique @ChatID or display name above to find them and start a private conversation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Chats conversation list
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
                                    .background(DarkSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching conversations found" else "No active conversations yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Try searching by their exact Chat ID or name in Search tab." else "Your direct messages will appear here. Find someone by their @ChatID or name to begin chatting!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            AppPrimaryButton(
                                text = "Find Users to Chat",
                                onClick = { activeSearchTab = 1 },
                                icon = Icons.Default.Search,
                                height = 44.dp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayedUsers, key = { it.id }) { user ->
                            UserItemCard(
                                user = user,
                                onClick = { onSelectUser(user) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserItemCard(
    user: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("user_item_${user.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = user.displayName.ifBlank { user.username },
                avatarId = user.avatarId,
                photoUrl = user.photoUrl,
                size = 48.dp,
                isOnline = user.isOnline
            )

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
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (user.username.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentBlue,
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
                        color = if (user.isOnline) OnlineGreen else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = user.statusMessage.ifBlank { "Hey there! I am using WP CHAT." },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            AppIconButton(
                icon = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "Start Chat",
                onClick = onClick,
                tint = AccentBlue,
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
