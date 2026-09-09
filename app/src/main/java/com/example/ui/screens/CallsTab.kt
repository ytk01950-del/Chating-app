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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallDirection
import com.example.model.CallRecord
import com.example.model.User
import com.example.ui.components.AppIconButton
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AccentBlue
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

@Composable
fun CallsTab(
    callHistory: List<CallRecord>,
    onStartVoiceCall: (User) -> Unit,
    onStartVideoCall: (User) -> Unit,
    onOpenUserProfile: (User) -> Unit = {}
) {
    var showMissedOnly by remember { mutableStateOf(false) }

    val filteredCalls = remember(callHistory, showMissedOnly) {
        if (showMissedOnly) {
            callHistory.filter { it.isMissed() }
        } else {
            callHistory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !showMissedOnly,
                onClick = { showMissedOnly = false },
                label = { Text("All (${callHistory.size})", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                    selectedLabelColor = AccentBlue,
                    containerColor = DarkSurface,
                    labelColor = TextSecondary
                ),
                border = BorderStroke(
                    1.dp,
                    if (!showMissedOnly) AccentBlue else DarkBorder
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("filter_all_calls")
            )

            val missedCount = callHistory.count { it.isMissed() }
            FilterChip(
                selected = showMissedOnly,
                onClick = { showMissedOnly = true },
                label = { Text("Missed ($missedCount)", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                    selectedLabelColor = Color(0xFFEF4444),
                    containerColor = DarkSurface,
                    labelColor = TextSecondary
                ),
                border = BorderStroke(
                    1.dp,
                    if (showMissedOnly) Color(0xFFEF4444) else DarkBorder
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("filter_missed_calls")
            )
        }

        if (filteredCalls.isEmpty()) {
            // Empty State
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
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (showMissedOnly) Icons.Default.CallMissed else Icons.Default.PhoneCallback,
                            contentDescription = null,
                            tint = if (showMissedOnly) Color(0xFFEF4444) else AccentBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (showMissedOnly) "No Missed Calls" else "No Recent Calls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (showMissedOnly) {
                            "You don't have any missed calls."
                        } else {
                            "Start 1-on-1 HD voice or video calling with your contacts using the call buttons in any chat."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("calls_history_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

@Composable
fun CallHistoryItemCard(
    record: CallRecord,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val isMissed = record.isMissed()
    val isIncoming = record.direction == CallDirection.INCOMING.name
    val isOutgoing = record.direction == CallDirection.OUTGOING.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("call_item_${record.callId}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DarkBorderSubtle)
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
                        color = if (isMissed) Color(0xFFF87171) else TextPrimary,
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
                            else -> Icons.Default.CallMade to AccentBlue
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
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )

                        // Formatted Date / Time
                        Text(
                            text = formatCallTimestamp(record.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )

                        if (record.durationSeconds > 0) {
                            Text(
                                text = "• ${record.getFormattedDuration()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Quick Call Actions (Audio & Video)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onVoiceCall,
                    modifier = Modifier
                        .size(38.dp)
                        .background(AccentBlue.copy(alpha = 0.12f), CircleShape)
                        .testTag("call_item_voice_btn_${record.callId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Voice Call",
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onVideoCall,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.12f), CircleShape)
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
