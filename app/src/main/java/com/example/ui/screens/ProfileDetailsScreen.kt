package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.ui.components.AppIconButton
import com.example.ui.components.InstagramSwitch
import com.example.ui.components.UserAvatar
import com.example.ui.components.clickableWithPress
import com.example.ui.components.pressScale
import com.example.ui.theme.AppTheme
import com.example.ui.theme.OnlineGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsScreen(
    currentUser: User,
    profileUser: User,
    isFollowing: Boolean,
    onBack: (() -> Unit)? = null,
    onOpenChat: (User) -> Unit = {},
    onFollowClick: (User) -> Unit = {},
    onUnfollowClick: (User) -> Unit = {},
    onOpenFollowers: (User) -> Unit = {},
    onOpenFollowing: (User) -> Unit = {},
    onUpdatePrivacy: (String, Boolean, Boolean, Boolean, String) -> Unit = { _, _, _, _, _ -> },
    onUpdateProfile: (String, String, String, Int, String, String, String) -> Unit = { _, _, _, _, _, _, _ -> },
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isSelf = currentUser.id == profileUser.id

    var showUnfollowDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Privacy settings local states (editable if isSelf)
    var whoCanFollow by remember(profileUser.whoCanFollow) { mutableStateOf(profileUser.whoCanFollow) }
    var showFollowersCount by remember(profileUser.showFollowersCount) { mutableStateOf(profileUser.showFollowersCount) }
    var showFollowingCount by remember(profileUser.showFollowingCount) { mutableStateOf(profileUser.showFollowingCount) }
    var showFollowingList by remember(profileUser.showFollowingList) { mutableStateOf(profileUser.showFollowingList) }
    var showDateOfBirth by remember(profileUser.showDateOfBirth) { mutableStateOf(profileUser.showDateOfBirth) }

    fun syncPrivacy(
        newWhoCanFollow: String = whoCanFollow,
        newShowFollowers: Boolean = showFollowersCount,
        newShowFollowing: Boolean = showFollowingCount,
        newShowList: Boolean = showFollowingList,
        newShowDob: String = showDateOfBirth
    ) {
        whoCanFollow = newWhoCanFollow
        showFollowersCount = newShowFollowers
        showFollowingCount = newShowFollowing
        showFollowingList = newShowList
        showDateOfBirth = newShowDob
        if (isSelf) {
            onUpdatePrivacy(newWhoCanFollow, newShowFollowers, newShowFollowing, newShowList, newShowDob)
        }
    }

    // Format join date e.g. "September 2026"
    val joinDateFormatted = remember(profileUser.createdAt) {
        val date = if (profileUser.createdAt > 0) Date(profileUser.createdAt) else Date()
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
    }

    // Privacy-aware display values
    val canViewFollowersCount = isSelf || profileUser.showFollowersCount
    val canViewFollowingCount = isSelf || profileUser.showFollowingCount
    val canViewDob = when {
        isSelf -> true
        profileUser.showDateOfBirth == "Everyone" -> true
        profileUser.showDateOfBirth == "Followers" && isFollowing -> true
        else -> false
    }

    val followersDisplay = if (canViewFollowersCount) "${profileUser.followersCount}" else "—"
    val followingDisplay = if (canViewFollowingCount) "${profileUser.followingCount}" else "—"
    val dobDisplay = if (canViewDob) {
        profileUser.dateOfBirth.ifBlank { "Not specified" }
    } else {
        "Private"
    }

    Scaffold(
        topBar = {
            // ==================================================
            // TOP HEADER
            // ==================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Back arrow inside a rounded square button (if back action available)
                if (onBack != null) {
                    Surface(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .pressScale()
                            .testTag("profile_back_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.cardBackground,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(42.dp))
                }

                Text(
                    text = if (profileUser.username.isNotBlank()) "@${profileUser.username}" else "Profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Right side: Three-dot menu inside a rounded square button
                Box {
                    Surface(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(42.dp)
                            .pressScale()
                            .testTag("profile_more_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.cardBackground,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    ) {
                        if (isSelf) {
                            DropdownMenuItem(
                                text = { Text("Edit Profile", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = colors.textPrimary) },
                                onClick = {
                                    showMenu = false
                                    showEditProfileDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = colors.textPrimary) },
                                onClick = {
                                    showMenu = false
                                    onOpenSettings?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Profile", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = colors.textPrimary) },
                                onClick = {
                                    showMenu = false
                                    Toast.makeText(context, "Sharing @${profileUser.username}'s profile", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy Profile Link", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = colors.textPrimary) },
                                onClick = {
                                    showMenu = false
                                    clipboardManager.setText(AnnotatedString("https://wpchat.app/@${profileUser.username}"))
                                    Toast.makeText(context, "Profile link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Share Profile", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = colors.textPrimary) },
                                onClick = {
                                    showMenu = false
                                    Toast.makeText(context, "Sharing @${profileUser.username}'s profile", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy Profile Link", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = colors.textPrimary) },
                                onClick = {
                                    showMenu = false
                                    clipboardManager.setText(AnnotatedString("https://wpchat.app/@${profileUser.username}"))
                                    Toast.makeText(context, "Profile link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report User", color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    showMenu = false
                                    Toast.makeText(context, "Report submitted for @${profileUser.username}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = colors.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("profile_details_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================================================
            // MAIN PROFILE CARD
            // ==================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_profile_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // PROFILE AVATAR
                        Box(contentAlignment = Alignment.Center) {
                            UserAvatar(
                                name = profileUser.displayName.ifBlank { profileUser.username },
                                avatarId = profileUser.avatarId,
                                photoUrl = profileUser.photoUrl,
                                size = 84.dp,
                                isOnline = profileUser.isOnline
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // USER INFORMATION
                        Text(
                            text = profileUser.displayName.ifBlank { profileUser.username.ifBlank { "User" } },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )

                        if (profileUser.username.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "@${profileUser.username}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (profileUser.bio.isNotBlank()) {
                            Text(
                                text = profileUser.bio,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        } else if (isSelf) {
                            Text(
                                text = "Tap Edit Profile to add a bio",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // FOLLOWERS AND FOLLOWING STATS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Stat: Followers
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickableWithPress {
                                        onOpenFollowers(profileUser)
                                    }
                                    .padding(vertical = 6.dp)
                                    .testTag("followers_stat_button"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = followersDisplay,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Followers",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp
                                    ),
                                    color = colors.textSecondary
                                )
                            }

                            // Subtle vertical divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(colors.borderSubtle)
                            )

                            // Right Stat: Following
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickableWithPress {
                                        onOpenFollowing(profileUser)
                                    }
                                    .padding(vertical = 6.dp)
                                    .testTag("following_stat_button"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = followingDisplay,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Following",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp
                                    ),
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // FOLLOW AND MESSAGE BUTTONS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelf) {
                                // For Self Profile: Edit Profile Button
                                Button(
                                    onClick = { showEditProfileDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .pressScale()
                                        .testTag("edit_profile_self_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (colors.isDark) Color.White else Color(0xFF111111),
                                        contentColor = if (colors.isDark) Color.Black else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.wrapContentWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Edit Profile",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                // Left Button: Follow / Following Toggle
                                if (isFollowing) {
                                    // State: Following -> Click opens Unfollow Confirmation Dialog
                                    OutlinedButton(
                                        onClick = { showUnfollowDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .pressScale()
                                            .testTag("profile_following_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF3E3E3E) else Color(0xFFCCCCCC)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (colors.isDark) Color(0xFF1E1E1E) else Color(0xFFEFEFEF),
                                            contentColor = colors.textPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Following",
                                                modifier = Modifier.size(20.dp),
                                                tint = colors.textPrimary
                                            )
                                        }
                                    }
                                } else {
                                    // State: Follow -> Black filled button
                                    Button(
                                        onClick = { onFollowClick(profileUser) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .pressScale()
                                            .testTag("profile_follow_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (colors.isDark) Color.White else Color(0xFF111111),
                                            contentColor = if (colors.isDark) Color.Black else Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PersonAdd,
                                                contentDescription = "Follow",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Right Button: Message
                            OutlinedButton(
                                onClick = { onOpenChat(profileUser) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .pressScale()
                                    .testTag("profile_message_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (colors.isDark) Color.White else Color(0xFF111111)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = colors.cardBackground,
                                    contentColor = colors.textPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Message",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================================================
            // PROFILE DETAILS SECTION
            // ==================================================
            item {
                Text(
                    text = "Profile Details",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                )
            }

            // CARD 1: About
            item {
                ProfileDetailCard(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = profileUser.bio.ifBlank { "No bio added" },
                    onClick = {
                        if (isSelf) showEditProfileDialog = true
                    }
                )
            }

            // CARD 2: Website
            item {
                ProfileDetailCard(
                    icon = Icons.Default.Language,
                    title = "Website",
                    subtitle = profileUser.website.ifBlank { "Not added" },
                    onClick = {
                        if (isSelf) showEditProfileDialog = true
                    }
                )
            }

            // CARD 3: Joined
            item {
                ProfileDetailCard(
                    icon = Icons.Default.CalendarToday,
                    title = "Joined",
                    subtitle = joinDateFormatted,
                    onClick = {}
                )
            }

            // CARD 4: Date of Birth
            item {
                ProfileDetailCard(
                    icon = Icons.Default.Cake,
                    title = "Date of Birth",
                    subtitle = dobDisplay,
                    onClick = {
                        if (isSelf) showEditProfileDialog = true
                    }
                )
            }

            // ==================================================
            // FOLLOW PRIVACY SECTION
            // ==================================================
            item {
                Text(
                    text = "Follow Privacy",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("follow_privacy_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // SETTING 1: Who can follow me
                        Column {
                            Text(
                                text = "Who can follow me",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val followOptions = listOf("Everyone", "Followers Only", "Private")
                                followOptions.forEach { opt ->
                                    val isSelected = whoCanFollow == opt
                                    val animatedBg by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            if (colors.isDark) Color.White else Color(0xFF111111)
                                        } else if (colors.isDark) Color(0xFF1E1E1E) else Color(0xFFEFEFEF),
                                        label = "who_can_follow_bg"
                                    )
                                    val animatedBorder by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            if (colors.isDark) Color.White else Color(0xFF111111)
                                        } else if (colors.isDark) Color(0xFF333333) else Color(0xFFDBDBDB),
                                        label = "who_can_follow_border"
                                    )
                                    val animatedContentColor by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            if (colors.isDark) Color.Black else Color.White
                                        } else if (colors.isDark) Color.White else Color(0xFF111111),
                                        label = "who_can_follow_text"
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickableWithPress(enabled = isSelf) {
                                                syncPrivacy(newWhoCanFollow = opt)
                                            }
                                            .testTag("privacy_follow_option_$opt"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = animatedBg,
                                        border = BorderStroke(1.dp, animatedBorder)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = opt,
                                                color = animatedContentColor,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)

                        // SETTING 2: Show Followers Count
                        PrivacyToggleRow(
                            title = "Show Followers Count",
                            subtitle = "Allow others to see your followers count",
                            checked = showFollowersCount,
                            enabled = isSelf,
                            onCheckedChange = { checked ->
                                syncPrivacy(newShowFollowers = checked)
                            }
                        )

                        HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)

                        // SETTING 3: Show Following Count
                        PrivacyToggleRow(
                            title = "Show Following Count",
                            subtitle = "Allow others to see how many people you follow",
                            checked = showFollowingCount,
                            enabled = isSelf,
                            onCheckedChange = { checked ->
                                syncPrivacy(newShowFollowing = checked)
                            }
                        )

                        HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)

                        // SETTING 4: Show Following List
                        PrivacyToggleRow(
                            title = "Show Following List",
                            subtitle = "Allow others to view the accounts you follow",
                            checked = showFollowingList,
                            enabled = isSelf,
                            onCheckedChange = { checked ->
                                syncPrivacy(newShowList = checked)
                            }
                        )

                        HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)

                        // SETTING 5: Show Date of Birth
                        Column {
                            Text(
                                text = "Show Date of Birth",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choose who can see your birth date",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val dobOptions = listOf("Everyone", "Followers", "Only Me")
                                dobOptions.forEach { opt ->
                                    val isSelected = showDateOfBirth == opt
                                    val animatedBg by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            if (colors.isDark) Color.White else Color(0xFF111111)
                                        } else if (colors.isDark) Color(0xFF1E1E1E) else Color(0xFFEFEFEF),
                                        label = "dob_bg"
                                    )
                                    val animatedBorder by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            if (colors.isDark) Color.White else Color(0xFF111111)
                                        } else if (colors.isDark) Color(0xFF333333) else Color(0xFFDBDBDB),
                                        label = "dob_border"
                                    )
                                    val animatedContentColor by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            if (colors.isDark) Color.Black else Color.White
                                        } else if (colors.isDark) Color.White else Color(0xFF111111),
                                        label = "dob_text"
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickableWithPress(enabled = isSelf) {
                                                syncPrivacy(newShowDob = opt)
                                            }
                                            .testTag("privacy_dob_option_$opt"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = animatedBg,
                                        border = BorderStroke(1.dp, animatedBorder)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = opt,
                                                color = animatedContentColor,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ==================================================
    // UNFOLLOW CONFIRMATION DIALOG
    // ==================================================
    if (showUnfollowDialog) {
        AlertDialog(
            onDismissRequest = { showUnfollowDialog = false },
            title = {
                Text(
                    text = "Unfollow @${profileUser.username.ifBlank { profileUser.displayName }}?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Their updates will no longer appear in your feed. You can follow them back anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUnfollowClick(profileUser)
                        showUnfollowDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.isDark) Color.White else Color(0xFF111111),
                        contentColor = if (colors.isDark) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Unfollow", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnfollowDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surfaceElevated,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // ==================================================
    // EDIT PROFILE DIALOG (FOR SELF)
    // ==================================================
    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(currentUser.displayName) }
        var editBio by remember { mutableStateOf(currentUser.bio) }
        var editWebsite by remember { mutableStateOf(currentUser.website) }
        var editDob by remember { mutableStateOf(currentUser.dateOfBirth.ifBlank { "12 January 2000" }) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "Edit Profile Details",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.textPrimary,
                            unfocusedBorderColor = colors.border
                        )
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio / About") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.textPrimary,
                            unfocusedBorderColor = colors.border
                        )
                    )
                    OutlinedTextField(
                        value = editWebsite,
                        onValueChange = { editWebsite = it },
                        label = { Text("Website") },
                        singleLine = true,
                        placeholder = { Text("https://example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.textPrimary,
                            unfocusedBorderColor = colors.border
                        )
                    )
                    OutlinedTextField(
                        value = editDob,
                        onValueChange = { editDob = it },
                        label = { Text("Date of Birth") },
                        singleLine = true,
                        placeholder = { Text("12 January 2000") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.textPrimary,
                            unfocusedBorderColor = colors.border
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(
                            editName.trim(),
                            editBio.trim(),
                            currentUser.statusMessage,
                            currentUser.avatarId,
                            currentUser.gender,
                            editWebsite.trim(),
                            editDob.trim()
                        )
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.isDark) Color.White else Color(0xFF111111),
                        contentColor = if (colors.isDark) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surfaceElevated,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun ProfileDetailCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickableWithPress(onClick = onClick)
            .testTag("profile_detail_card_$title"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rounded icon container on the left
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right arrow icon
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PrivacyToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        InstagramSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
