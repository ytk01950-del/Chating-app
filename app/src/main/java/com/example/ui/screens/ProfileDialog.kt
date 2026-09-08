package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.ui.components.AvatarColorPairs
import com.example.ui.components.UserAvatar
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.AppSecondaryButton
import com.example.ui.components.AppDestructiveButton
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueDark
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

val StatusPresets = listOf(
    "Available",
    "Reviewing docs 💻",
    "In a meeting 📅",
    "Coding in Kotlin ⚡",
    "Coffee break ☕",
    "Away from keyboard"
)

@Composable
fun ProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSaveProfile: (String, String, Int) -> Unit,
    onClaimUsername: (String, (Boolean) -> Unit) -> Unit = { _, cb -> cb(true) },
    onCheckUsernameAvailable: suspend (String) -> Boolean = { true },
    onSignOut: () -> Unit
) {
    var displayName by remember { mutableStateOf(user.displayName) }
    var statusMessage by remember { mutableStateOf(user.statusMessage) }
    var selectedAvatarId by remember { mutableIntStateOf(user.avatarId) }

    var usernameInput by remember { mutableStateOf(user.username) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var usernameValidationError by remember { mutableStateOf<String?>(null) }
    var showStorageConfigDialog by remember { mutableStateOf(false) }

    val hasExistingUsername = user.username.isNotBlank()

    if (!hasExistingUsername) {
        val normalized = usernameInput.trim().lowercase().removePrefix("@")
        LaunchedEffect(normalized) {
            if (normalized.isBlank()) {
                usernameAvailable = null
                usernameValidationError = null
                isCheckingUsername = false
                return@LaunchedEffect
            }
            val regex = "^[a-zA-Z0-9_.]{4,20}$".toRegex()
            if (!regex.matches(normalized)) {
                if (normalized.length < 4) {
                    usernameValidationError = "Must be 4–20 characters"
                } else if (normalized.length > 20) {
                    usernameValidationError = "Maximum 20 characters"
                } else {
                    usernameValidationError = "Only letters, numbers, _, and . allowed"
                }
                usernameAvailable = null
                isCheckingUsername = false
                return@LaunchedEffect
            }
            usernameValidationError = null
            isCheckingUsername = true
            delay(400)
            val available = onCheckUsernameAvailable(normalized)
            usernameAvailable = available
            isCheckingUsername = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Preview
                UserAvatar(
                    name = displayName.ifBlank { user.displayName },
                    avatarId = selectedAvatarId,
                    size = 64.dp,
                    isOnline = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Avatar Color Style",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(AvatarColorPairs) { idx, _ ->
                        val isSelected = selectedAvatarId == idx
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { selectedAvatarId = idx }
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) AccentBlue else Color.Transparent,
                                    shape = CircleShape
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            UserAvatar(
                                name = displayName.ifBlank { "U" },
                                avatarId = idx,
                                size = 30.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chat ID / Username Field
                if (hasExistingUsername) {
                    OutlinedTextField(
                        value = "@${user.username}",
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Chat ID (Permanent)") },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = AccentBlue) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledContainerColor = DarkBg,
                            disabledBorderColor = DarkBorderSubtle,
                            disabledLabelColor = TextSecondary,
                            disabledLeadingIconColor = AccentBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { input ->
                            val clean = input.filter { it.isLetterOrDigit() || it == '_' || it == '.' || it == '@' }
                            usernameInput = clean
                        },
                        label = { Text("Choose Chat ID (Unique)") },
                        placeholder = { Text("e.g. alex_99", color = TextSecondary) },
                        prefix = { Text("@", color = AccentBlue, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = AccentBlue) },
                        trailingIcon = {
                            when {
                                isCheckingUsername -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentBlue)
                                usernameAvailable == true -> Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = OnlineGreen)
                                usernameAvailable == false || usernameValidationError != null -> Icon(Icons.Default.ErrorOutline, contentDescription = "Unavailable", tint = Color(0xFFFFB4AB))
                            }
                        },
                        supportingText = {
                            when {
                                isCheckingUsername -> Text("Checking...", color = TextSecondary, fontSize = 11.sp)
                                usernameValidationError != null -> Text(usernameValidationError.orEmpty(), color = Color(0xFFFFB4AB), fontSize = 11.sp)
                                usernameAvailable == true -> Text("✓ Chat ID is available!", color = OnlineGreen, fontSize = 11.sp)
                                usernameAvailable == false -> Text("✗ Already taken", color = Color(0xFFFFB4AB), fontSize = 11.sp)
                                else -> Text("4–20 characters", color = TextMuted, fontSize = 11.sp)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg,
                            focusedBorderColor = if (usernameAvailable == true) OnlineGreen else AccentBlue,
                            unfocusedBorderColor = DarkBorderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_username_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AccentBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
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
                        .testTag("profile_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = statusMessage,
                    onValueChange = { statusMessage = it },
                    label = { Text("Status Message") },
                    leadingIcon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = AccentBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
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
                        .testTag("profile_status_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick status presets
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(StatusPresets) { _, preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { statusMessage = preset }
                                .border(
                                    width = 1.dp,
                                    color = DarkBorderSubtle,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Storage settings shortcut
                AppSecondaryButton(
                    text = "Media Storage Settings (Free Tier)",
                    onClick = { showStorageConfigDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    height = 42.dp,
                    testTag = "profile_storage_settings_button"
                )
            }
        },
        confirmButton = {
            AppPrimaryButton(
                text = "Save Changes",
                onClick = {
                    if (!hasExistingUsername && usernameInput.isNotBlank() && usernameAvailable == true) {
                        onClaimUsername(usernameInput) { success ->
                            if (success) {
                                onSaveProfile(displayName, statusMessage, selectedAvatarId)
                                onDismiss()
                            }
                        }
                    } else {
                        onSaveProfile(displayName, statusMessage, selectedAvatarId)
                        onDismiss()
                    }
                },
                height = 42.dp,
                testTag = "save_profile_button"
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppDestructiveButton(
                    text = "Sign Out",
                    onClick = {
                        onDismiss()
                        onSignOut()
                    },
                    height = 42.dp,
                    testTag = "profile_sign_out_button"
                )
                AppSecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    height = 42.dp,
                    testTag = "profile_cancel_button"
                )
            }
        }
    )

    if (showStorageConfigDialog) {
        com.example.ui.components.SupabaseStorageConfigDialog(
            onDismiss = { showStorageConfigDialog = false }
        )
    }
}
