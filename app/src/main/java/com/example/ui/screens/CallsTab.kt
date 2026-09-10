package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallDirection
import com.example.model.CallRecord
import com.example.model.User
import com.example.ui.components.AppFilterChip
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AppTheme
import com.example.ui.theme.OnlineGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallsTab(
    callHistory: List<CallRecord>,
    availableUsers: List<User> = emptyList(),
    onStartVoiceCall: (User) -> Unit,
    onStartVideoCall: (User) -> Unit,
    onOpenUserProfile: (User) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var showMissedOnly by remember { mutableStateOf(false) }
    var showNewCallDialog by remember { mutableStateOf(false) }

    val filteredCalls = remember(callHistory, showMissedOnly) {
        if (showMissedOnly) {
            callHistory.filter { it.isMissed() }
        } else {
            callHistory
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {
            // Header Action & Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFilterChip(
                        selected = !showMissedOnly,
                        onClick = { showMissedOnly = false },
                        label = "All",
                        badgeCount = callHistory.size,
                        modifier = Modifier.testTag("filter_all_calls")
                    )

                    val missedCount = callHistory.count { it.isMissed() }
                    AppFilterChip(
                        selected = showMissedOnly,
                        onClick = { showMissedOnly = true },
                        label = "Missed",
                        badgeCount = missedCount,
                        modifier = Modifier.testTag("filter_missed_calls")
                    )
                }

                // New Call Button
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (colors.isDark) Color(0xFF262626) else Color(0xFFEFEFEF),
                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF363636) else Color(0xFFDBDBDB)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showNewCallDialog = true }
                        .testTag("btn_new_call_header")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "New Call",
                            tint = if (colors.isDark) Color.White else Color(0xFF111111),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ New Call",
                            color = if (colors.isDark) Color.White else Color(0xFF111111),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (filteredCalls.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceVariant)
                                .border(1.dp, colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (showMissedOnly) Icons.Default.CallMissed else Icons.Default.PhoneCallback,
                                contentDescription = null,
                                tint = if (showMissedOnly) Color(0xFFEF4444) else colors.accentOrange,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (showMissedOnly) "No Missed Calls" else "No Recent Calls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (showMissedOnly) {
                                "You don't have any missed calls."
                            } else {
                                "Tap 'New Call' or select any contact to start 1-on-1 HD voice or video calling."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        if (!showMissedOnly && availableUsers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFF2563EB),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable { showNewCallDialog = true }
                                    .testTag("btn_empty_new_call")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Start a Call",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("calls_history_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCalls, key = { it.id.ifBlank { "${it.callId}_${it.timestamp}" } }) { callRecord ->
                        val otherUser = User(
                            id = callRecord.otherUserId,
                            displayName = callRecord.otherUserName.ifBlank { "User" },
                            username = callRecord.otherUserUsername,
                            photoUrl = callRecord.otherUserPhotoUrl,
                            avatarId = callRecord.otherUserAvatarId
                        )

                        CallHistoryItemCard(
                            record = callRecord,
                            onVoiceCall = { onStartVoiceCall(otherUser) },
                            onVideoCall = { onStartVideoCall(otherUser) },
                            onOpenProfile = { onOpenUserProfile(otherUser) }
                        )
                    }
                }
            }
        }
    }

    // New Call Contact Picker Modal
    if (showNewCallDialog) {
        var userSearchQuery by remember { mutableStateOf("") }
        val filteredUsers = remember(availableUsers, userSearchQuery) {
            if (userSearchQuery.isBlank()) {
                availableUsers
            } else {
                availableUsers.filter {
                    it.displayName.contains(userSearchQuery, ignoreCase = true) ||
                            it.username.contains(userSearchQuery, ignoreCase = true)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showNewCallDialog = false },
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Call",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = userSearchQuery,
                        onValueChange = { userSearchQuery = it },
                        placeholder = { Text("Search contact to call...", color = colors.textMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8E8E8E))
                        },
                        trailingIcon = {
                            if (userSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { userSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = colors.textMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedContainerColor = if (colors.isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5),
                            unfocusedContainerColor = if (colors.isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5),
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = if (colors.isDark) Color(0xFF363636) else Color(0xFFDBDBDB)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No contacts found",
                                color = colors.textMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredUsers, key = { it.id }) { user ->
                                Surface(
                                    color = colors.cardBackground,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, colors.borderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            UserAvatar(
                                                name = user.displayName.ifBlank { user.username },
                                                avatarId = user.avatarId,
                                                photoUrl = user.photoUrl,
                                                size = 38.dp,
                                                isOnline = user.isOnline
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = user.displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (user.username.isNotBlank()) {
                                                    Text(
                                                        text = "@${user.username}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colors.textMuted
                                                    )
                                                }
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    showNewCallDialog = false
                                                    onStartVoiceCall(user)
                                                },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(if (colors.isDark) Color(0xFF262626) else Color(0xFFDBEAFE), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = "Voice Call",
                                                    tint = Color(0xFF2563EB),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    showNewCallDialog = false
                                                    onStartVideoCall(user)
                                                },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(Color(0xFF38BDF8).copy(alpha = 0.15f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Videocam,
                                                    contentDescription = "Video Call",
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNewCallDialog = false }) {
                    Text("Close", color = colors.textSecondary)
                }
            },
            containerColor = colors.surfaceElevated
        )
    }
}

@Composable
fun CallHistoryItemCard(
    record: CallRecord,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val colors = AppTheme.colors
    val isMissed = record.isMissed()
    val isIncoming = record.direction == CallDirection.INCOMING.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("call_item_${record.callId}"),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // User Avatar & Call Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenProfile() }
            ) {
                UserAvatar(
                    name = record.otherUserName.ifBlank { "User" },
                    avatarId = record.otherUserAvatarId,
                    photoUrl = record.otherUserPhotoUrl,
                    size = 46.dp,
                    isOnline = false
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.otherUserName.ifBlank { "Unknown User" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isMissed) Color(0xFFEF4444) else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Direction Icon
                        val (directionIcon, directionTint) = when {
                            isMissed -> Icons.Default.CallMissed to Color(0xFFEF4444)
                            isIncoming -> Icons.Default.CallReceived to OnlineGreen
                            else -> Icons.Default.CallMade to colors.accentOrange
                        }
                        Icon(
                            imageVector = directionIcon,
                            contentDescription = null,
                            tint = directionTint,
                            modifier = Modifier.size(14.dp)
                        )

                        // Call Type Icon
                        Icon(
                            imageVector = if (record.isVideo()) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )

                        // Formatted Date / Time
                        Text(
                            text = formatCallTimestamp(record.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )

                        if (record.durationSeconds > 0) {
                            Text(
                                text = "• ${record.getFormattedDuration()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Quick Call Actions (Audio & Video)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onVoiceCall,
                    modifier = Modifier
                        .size(38.dp)
                        .background(if (colors.isDark) Color(0xFF262626) else Color(0xFFDBEAFE), CircleShape)
                        .testTag("call_item_voice_btn_${record.callId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Voice Call",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onVideoCall,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.14f), CircleShape)
                        .testTag("call_item_video_btn_${record.callId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatCallTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return "Recent"
    val diff = System.currentTimeMillis() - timestamp
    val oneDay = 24 * 60 * 60 * 1000L
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    return when {
        diff < 60 * 1000L -> "Just now"
        diff < oneDay -> "Today, ${timeFormat.format(Date(timestamp))}"
        diff < 2 * oneDay -> "Yesterday, ${timeFormat.format(Date(timestamp))}"
        else -> dateFormat.format(Date(timestamp))
    }
}
