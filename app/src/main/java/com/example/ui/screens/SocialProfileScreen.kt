package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Story
import com.example.model.User
import com.example.ui.components.AppDestructiveButton
import com.example.ui.components.AppIconButton
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.AppSecondaryButton
import com.example.ui.components.AvatarColorPairs
import com.example.ui.components.StoryAvatarRing
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkBg
import androidx.compose.foundation.lazy.items
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
fun SocialProfileScreen(
    currentUser: User,
    profileUser: User,
    stories: List<Story>,
    isUploadingPhoto: Boolean,
    isCreatingStory: Boolean,
    storyUploadProgress: Float,
    isDeletingStory: Boolean,
    onBack: () -> Unit,
    onOpenChat: (User) -> Unit,
    onUploadProfilePhoto: (Uri) -> Unit,
    onCreateStory: (Uri, Boolean, String) -> Unit,
    onDeleteStory: (Story) -> Unit,
    onUpdateProfile: (String, String, String, Int, String) -> Unit,
    onClaimUsername: (String, (Boolean) -> Unit) -> Unit,
    onCheckUsernameAvailable: suspend (String) -> Boolean,
    onStoryViewed: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isOwnProfile = currentUser.id == profileUser.id

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    var showStorageConfigDialog by remember { mutableStateOf(false) }
    var showStoryViewer by remember { mutableStateOf(false) }
    var initialStoryIndex by remember { mutableIntStateOf(0) }

    var pendingStoryMediaUri by remember { mutableStateOf<Uri?>(null) }
    var pendingStoryIsVideo by remember { mutableStateOf(false) }

    // Launcher for profile photo upload
    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadProfilePhoto(uri)
        }
    }

    // Launcher for photo story
    val photoStoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingStoryMediaUri = uri
            pendingStoryIsVideo = false
            showCreateStoryDialog = true
        }
    }

    // Launcher for video story
    val videoStoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingStoryMediaUri = uri
            pendingStoryIsVideo = true
            showCreateStoryDialog = true
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        size = 38.dp,
                        iconSize = 22.dp,
                        testTag = "profile_back_button"
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (profileUser.username.isNotBlank()) "@${profileUser.username}" else profileUser.displayName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (profileUser.isOnline) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(OnlineGreen)
                                )
                            }
                        }
                        Text(
                            text = if (isOwnProfile) "Your Profile & Stories" else "User Profile",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    if (isOwnProfile) {
                        AppIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Add Story",
                            onClick = {
                                photoStoryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            tint = AccentBlue,
                            size = 38.dp,
                            iconSize = 22.dp,
                            testTag = "header_add_story_button"
                        )
                        AppIconButton(
                            icon = Icons.Default.Cloud,
                            contentDescription = "Storage Settings",
                            onClick = { showStorageConfigDialog = true },
                            tint = AccentBlue,
                            size = 38.dp,
                            iconSize = 20.dp,
                            testTag = "header_storage_config_button"
                        )
                        AppIconButton(
                            icon = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            onClick = { showEditProfileDialog = true },
                            tint = TextSecondary,
                            size = 38.dp,
                            iconSize = 20.dp,
                            testTag = "header_edit_profile_button"
                        )
                    }
                }
            }
        },
        containerColor = DarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Profile Info Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, DarkBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar with Story Ring if active stories exist
                        val hasActiveStories = stories.isNotEmpty()
                        StoryAvatarRing(
                            hasActiveStory = hasActiveStories,
                            hasUnseenStory = true,
                            size = 86.dp,
                            onClick = {
                                if (hasActiveStories) {
                                    initialStoryIndex = 0
                                    showStoryViewer = true
                                } else if (isOwnProfile) {
                                    profilePhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                UserAvatar(
                                    name = profileUser.displayName,
                                    avatarId = profileUser.avatarId,
                                    photoUrl = profileUser.photoUrl,
                                    size = if (hasActiveStories) 74.dp else 80.dp,
                                    isOnline = profileUser.isOnline,
                                    modifier = Modifier.testTag("social_profile_avatar")
                                )

                                if (isUploadingPhoto) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = AccentBlue,
                                            modifier = Modifier.size(32.dp),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }

                                if (isOwnProfile && !isUploadingPhoto && !hasActiveStories) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(AccentBlue)
                                            .border(2.dp, DarkSurface, CircleShape)
                                            .clickable {
                                                profilePhotoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Change Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        // Profile Stats
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${stories.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = if (stories.isNotEmpty()) AccentBlue else TextPrimary
                                )
                                Text(
                                    text = "24h Stories",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = profileUser.gender.ifBlank { "Male" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Gender",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (profileUser.isOnline) "Active" else "Offline",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (profileUser.isOnline) OnlineGreen else TextMuted
                                )
                                Text(
                                    text = "Status",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Name and Username
                    Text(
                        text = profileUser.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )

                    if (profileUser.username.isNotBlank()) {
                        Text(
                            text = "@${profileUser.username}",
                            fontSize = 13.sp,
                            color = AccentBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (profileUser.statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = profileUser.statusMessage,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    if (profileUser.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = profileUser.bio,
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons
                    if (isOwnProfile) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppPrimaryButton(
                                text = "+ Photo Story",
                                onClick = {
                                    photoStoryPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                height = 40.dp,
                                testTag = "add_photo_story_button"
                            )
                            AppSecondaryButton(
                                text = "+ Video Story",
                                onClick = {
                                    videoStoryPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                height = 40.dp,
                                testTag = "add_video_story_button"
                            )
                            AppSecondaryButton(
                                text = "Edit",
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.width(72.dp),
                                height = 40.dp,
                                testTag = "profile_edit_action_button"
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppPrimaryButton(
                                text = "Send Message",
                                onClick = { onOpenChat(profileUser) },
                                modifier = Modifier.weight(1f),
                                height = 40.dp,
                                icon = Icons.AutoMirrored.Filled.Chat,
                                testTag = "profile_send_message_button"
                            )
                            if (stories.isNotEmpty()) {
                                AppSecondaryButton(
                                    text = "Watch Story (${stories.size})",
                                    onClick = {
                                        initialStoryIndex = 0
                                        showStoryViewer = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    height = 40.dp,
                                    testTag = "watch_user_story_button"
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 24-Hour Active Stories Section
            Surface(
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, DarkBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = OnlineGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "24-Hour Active Stories",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    if (stories.isNotEmpty()) {
                        Surface(
                            color = OnlineGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${stories.size} Active",
                                color = OnlineGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Stories Grid or Empty State
            if (stories.isEmpty()) {
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
                        Surface(
                            shape = CircleShape,
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkBorderSubtle),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isOwnProfile) "No Active Stories" else "${profileUser.displayName} has no active stories",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isOwnProfile)
                                "Share photos and videos that automatically disappear after exactly 24 hours."
                            else
                                "24-hour stories posted by this user will show up here.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        if (isOwnProfile) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AppPrimaryButton(
                                    text = "+ Photo Story",
                                    onClick = {
                                        photoStoryPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    testTag = "empty_add_photo_story_button"
                                )
                                AppSecondaryButton(
                                    text = "+ Video Story",
                                    onClick = {
                                        videoStoryPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    testTag = "empty_add_video_story_button"
                                )
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = stories, key = { it.storyId }) { story ->
                        val storyIdx = stories.indexOf(story)
                        Box(
                            modifier = Modifier
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceVariant)
                                .clickable {
                                    initialStoryIndex = storyIdx
                                    showStoryViewer = true
                                }
                                .testTag("story_grid_item_${story.storyId}")
                        ) {
                            if (story.mediaType == "video") {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = "Video Story",
                                        tint = AccentBlue,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(story.mediaUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Story Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Dark overlay gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.4f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.75f)
                                            )
                                        )
                                    )
                            )

                            // Media Type tag (top left)
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (story.mediaType == "video") Icons.Default.Movie else Icons.Default.Photo,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }

                            // Remaining countdown badge (bottom)
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = story.getRemainingTimeFormatted(),
                                    color = OnlineGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isOwnProfile && story.viewers.isNotEmpty()) {
                                    Text(
                                        text = "👁️ ${story.viewers.size}",
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Story Viewer Dialog
    if (showStoryViewer && stories.isNotEmpty()) {
        StoryViewerDialog(
            user = profileUser,
            stories = stories,
            currentUserId = currentUser.id,
            initialIndex = initialStoryIndex,
            onDismiss = { showStoryViewer = false },
            onDeleteStory = { story ->
                onDeleteStory(story)
            },
            onStoryViewed = onStoryViewed
        )
    }

    // Create Story Dialog
    if (showCreateStoryDialog && pendingStoryMediaUri != null) {
        CreateStoryDialog(
            mediaUri = pendingStoryMediaUri!!,
            isVideo = pendingStoryIsVideo,
            isUploading = isCreatingStory,
            uploadProgress = storyUploadProgress,
            onDismiss = {
                showCreateStoryDialog = false
                pendingStoryMediaUri = null
            },
            onShareStory = { uri, isVideo, caption ->
                onCreateStory(uri, isVideo, caption)
                showCreateStoryDialog = false
                pendingStoryMediaUri = null
            }
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        ProfileEditFullDialog(
            user = currentUser,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, bio, status, avatarId, gender ->
                onUpdateProfile(name, bio, status, avatarId, gender)
                showEditProfileDialog = false
            },
            onClaimUsername = onClaimUsername,
            onCheckUsernameAvailable = onCheckUsernameAvailable
        )
    }

    // Storage Config Dialog
    if (showStorageConfigDialog) {
        com.example.ui.components.SupabaseStorageConfigDialog(
            onDismiss = { showStorageConfigDialog = false }
        )
    }
}

@Composable
fun ProfileEditFullDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, String) -> Unit,
    onClaimUsername: (String, (Boolean) -> Unit) -> Unit,
    onCheckUsernameAvailable: suspend (String) -> Boolean
) {
    var displayName by remember { mutableStateOf(user.displayName) }
    var bio by remember { mutableStateOf(user.bio) }
    var statusMessage by remember { mutableStateOf(user.statusMessage) }
    var selectedAvatarId by remember { mutableIntStateOf(user.avatarId) }
    var selectedGender by remember { mutableStateOf(user.gender.ifBlank { "Male" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Edit Profile & Details", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar preview
                UserAvatar(
                    name = displayName.ifBlank { user.displayName },
                    avatarId = selectedAvatarId,
                    photoUrl = user.photoUrl,
                    size = 64.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Avatar color palette
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(AvatarColorPairs) { idx, _ ->
                        val isSelected = selectedAvatarId == idx
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { selectedAvatarId = idx }
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) AccentBlue else Color.Transparent,
                                    shape = CircleShape
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            UserAvatar(
                                name = displayName.ifBlank { "U" },
                                avatarId = idx,
                                size = 28.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gender selector
                Text(
                    text = "Gender Selection",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Male", "Female").forEach { genderOption ->
                        val isSelected = selectedGender.equals(genderOption, ignoreCase = true)
                        Surface(
                            color = if (isSelected) AccentBlue.copy(alpha = 0.2f) else DarkBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) AccentBlue else DarkBorderSubtle
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedGender = genderOption }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (genderOption == "Male") "👨 Male" else "👩 Female",
                                    color = if (isSelected) AccentBlue else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
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
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_display_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = statusMessage,
                    onValueChange = { statusMessage = it },
                    label = { Text("Status Message") },
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
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_status_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_bio_input")
                )
            }
        },
        confirmButton = {
            AppPrimaryButton(
                text = "Save",
                onClick = {
                    onSave(displayName, bio, statusMessage, selectedAvatarId, selectedGender)
                },
                testTag = "edit_profile_save_button"
            )
        },
        dismissButton = {
            AppSecondaryButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}
