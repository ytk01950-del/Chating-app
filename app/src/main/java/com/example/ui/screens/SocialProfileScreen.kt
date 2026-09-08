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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Post
import com.example.model.User
import com.example.ui.components.AvatarColorPairs
import com.example.ui.components.UserAvatar
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.AppSecondaryButton
import com.example.ui.components.AppIconButton
import com.example.ui.components.AppDestructiveButton
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialProfileScreen(
    currentUser: User,
    profileUser: User,
    posts: List<Post>,
    isUploadingPhoto: Boolean,
    isCreatingPost: Boolean,
    postUploadProgress: Float,
    isDeletingPost: Boolean,
    onBack: () -> Unit,
    onOpenChat: (User) -> Unit,
    onUploadProfilePhoto: (Uri) -> Unit,
    onCreatePost: (Uri, String) -> Unit,
    onDeletePost: (Post) -> Unit,
    onUpdateProfile: (String, String, String, Int) -> Unit,
    onClaimUsername: (String, (Boolean) -> Unit) -> Unit,
    onCheckUsernameAvailable: suspend (String) -> Boolean
) {
    val context = LocalContext.current
    val isOwnProfile = currentUser.id == profileUser.id

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showStorageConfigDialog by remember { mutableStateOf(false) }
    var selectedPostForView by remember { mutableStateOf<Post?>(null) }
    var pendingPostImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for profile photo upload
    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadProfilePhoto(uri)
        }
    }

    // Launcher for creating a new post
    val createPostPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingPostImageUri = uri
            showCreatePostDialog = true
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
                            text = if (isOwnProfile) "Your Social Profile" else "User Profile",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    if (isOwnProfile) {
                        AppIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Create Post",
                            onClick = {
                                createPostPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            tint = AccentBlue,
                            size = 38.dp,
                            iconSize = 22.dp,
                            testTag = "header_create_post_button"
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
                        // Profile Avatar with edit badge
                        Box(contentAlignment = Alignment.Center) {
                            UserAvatar(
                                name = profileUser.displayName,
                                avatarId = profileUser.avatarId,
                                photoUrl = profileUser.photoUrl,
                                size = 80.dp,
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

                            if (isOwnProfile) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(AccentBlue)
                                        .border(2.dp, DarkSurface, CircleShape)
                                        .clickable {
                                            profilePhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                        .testTag("change_photo_badge"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Stats columns
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatItem(
                                count = posts.size.toString(),
                                label = "Posts"
                            )
                            ProfileStatItem(
                                count = if (profileUser.isOnline) "Online" else "Away",
                                label = "Status",
                                isHighlight = profileUser.isOnline
                            )
                            val joinYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(profileUser.createdAt))
                            ProfileStatItem(
                                count = joinYear,
                                label = "Joined"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Display Name & Chat ID Badge
                    Text(
                        text = profileUser.displayName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    if (profileUser.username.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AlternateEmail,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = profileUser.username,
                                color = AccentBlue,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Bio / About section
                    if (profileUser.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = profileUser.bio,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }

                    // Status Message / About Quote
                    if (profileUser.statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💬",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = profileUser.statusMessage,
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isOwnProfile) {
                            AppSecondaryButton(
                                text = "Edit Profile",
                                onClick = { showEditProfileDialog = true },
                                icon = Icons.Default.Edit,
                                modifier = Modifier.weight(1f),
                                height = 44.dp,
                                testTag = "edit_profile_button"
                            )

                            AppPrimaryButton(
                                text = "Share Photo",
                                onClick = {
                                    createPostPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                icon = Icons.Default.AddAPhoto,
                                modifier = Modifier.weight(1f),
                                height = 44.dp,
                                testTag = "create_post_button"
                            )
                        } else {
                            AppPrimaryButton(
                                text = "Send Message",
                                onClick = { onOpenChat(profileUser) },
                                icon = Icons.AutoMirrored.Filled.Chat,
                                modifier = Modifier.fillMaxWidth(),
                                height = 44.dp,
                                testTag = "message_user_button"
                            )
                        }
                    }
                }
            }

            // Posts Grid Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "POSTS (${posts.size})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(DarkBorderSubtle))

            // Posts Grid / Empty State
            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
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
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isOwnProfile) "No Posts Yet" else "No photos shared yet",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isOwnProfile)
                                "Share photos and moments on your profile for other users to see."
                            else
                                "@${profileUser.username} hasn't published any photos yet.",
                            color = TextMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        if (isOwnProfile) {
                            Spacer(modifier = Modifier.height(18.dp))
                            AppPrimaryButton(
                                text = "Publish First Photo",
                                onClick = {
                                    createPostPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                icon = Icons.Default.Add,
                                height = 44.dp
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("profile_posts_grid")
                ) {
                    items(posts, key = { it.postId }) { post ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(DarkSurfaceVariant)
                                .clickable { selectedPostForView = post }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(post.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = post.caption.ifBlank { "Post photo" },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // Full Screen / Post Detail Dialog Viewer
    selectedPostForView?.let { post ->
        PostDetailViewerDialog(
            post = post,
            isOwner = currentUser.id == post.userId,
            isDeleting = isDeletingPost,
            onDismiss = { selectedPostForView = null },
            onDelete = {
                onDeletePost(post)
                selectedPostForView = null
            }
        )
    }

    // Create Post Dialog
    if (showCreatePostDialog && pendingPostImageUri != null) {
        CreatePostDialog(
            imageUri = pendingPostImageUri!!,
            isUploading = isCreatingPost,
            uploadProgress = postUploadProgress,
            onDismiss = {
                if (!isCreatingPost) {
                    showCreatePostDialog = false
                    pendingPostImageUri = null
                }
            },
            onPublish = { caption ->
                onCreatePost(pendingPostImageUri!!, caption)
                showCreatePostDialog = false
                pendingPostImageUri = null
            }
        )
    }

    // Supabase Storage Configuration Dialog
    if (showStorageConfigDialog) {
        com.example.ui.components.SupabaseStorageConfigDialog(
            onDismiss = { showStorageConfigDialog = false }
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog && isOwnProfile) {
        EditSocialProfileDialog(
            user = currentUser,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, bio, status, avatarId ->
                onUpdateProfile(name, bio, status, avatarId)
                showEditProfileDialog = false
            },
            onChangePhoto = {
                profilePhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onClaimUsername = onClaimUsername,
            onCheckUsernameAvailable = onCheckUsernameAvailable
        )
    }
}

@Composable
private fun ProfileStatItem(
    count: String,
    label: String,
    isHighlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            color = if (isHighlight) OnlineGreen else TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
fun PostDetailViewerDialog(
    post: Post,
    isOwner: Boolean,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBg.copy(alpha = 0.96f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dialog Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        tint = TextPrimary,
                        size = 38.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    UserAvatar(
                        name = post.userDisplayName,
                        avatarId = 0,
                        photoUrl = post.userPhotoUrl,
                        size = 36.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.userDisplayName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        val formattedDate = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                            .format(Date(post.timestamp))
                        Text(
                            text = formattedDate,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    if (isOwner) {
                        AppIconButton(
                            icon = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Post",
                            onClick = { showConfirmDelete = true },
                            tint = Color(0xFFFF6B6B),
                            size = 38.dp,
                            testTag = "delete_post_button"
                        )
                    }
                }

                // Main Image
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(post.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = post.caption,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Caption Footer
                if (post.caption.isNotBlank()) {
                    Surface(
                        color = DarkSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@${post.userUsername.ifBlank { post.userDisplayName }}",
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = post.caption,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Delete Post?", color = TextPrimary) },
            text = { Text("Are you sure you want to permanently delete this photo post?", color = TextSecondary) },
            containerColor = DarkSurface,
            confirmButton = {
                AppDestructiveButton(
                    text = "Delete",
                    onClick = {
                        showConfirmDelete = false
                        onDelete()
                    },
                    height = 40.dp
                )
            },
            dismissButton = {
                AppSecondaryButton(
                    text = "Cancel",
                    onClick = { showConfirmDelete = false },
                    height = 40.dp
                )
            }
        )
    }
}

@Composable
fun CreatePostDialog(
    imageUri: Uri,
    isUploading: Boolean,
    uploadProgress: Float,
    onDismiss: () -> Unit,
    onPublish: (String) -> Unit
) {
    var caption by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            color = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Photo Post",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    AppIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Cancel",
                        onClick = onDismiss,
                        tint = TextMuted,
                        enabled = !isUploading,
                        size = 32.dp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Image Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .build(),
                        contentDescription = "Post preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = AccentBlue,
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Publishing... ${(uploadProgress * 100).toInt()}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                if (isUploading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentBlue,
                        trackColor = DarkSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Caption Input
                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 300) caption = it },
                    label = { Text("Write a caption...") },
                    placeholder = { Text("What's on your mind?", color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_caption_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    ),
                    maxLines = 3,
                    enabled = !isUploading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppSecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                        enabled = !isUploading
                    )

                    AppPrimaryButton(
                        text = "Publish",
                        onClick = { onPublish(caption) },
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                        isLoading = isUploading,
                        enabled = !isUploading,
                        testTag = "publish_post_button"
                    )
                }
            }
        }
    }
}

@Composable
fun EditSocialProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (displayName: String, bio: String, statusMessage: String, avatarId: Int) -> Unit,
    onChangePhoto: () -> Unit,
    onClaimUsername: (String, (Boolean) -> Unit) -> Unit,
    onCheckUsernameAvailable: suspend (String) -> Boolean
) {
    var displayName by remember { mutableStateOf(user.displayName) }
    var bio by remember { mutableStateOf(user.bio) }
    var statusMessage by remember { mutableStateOf(user.statusMessage) }
    var selectedAvatarId by remember { mutableIntStateOf(user.avatarId) }

    var usernameInput by remember { mutableStateOf(user.username) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var usernameValidationError by remember { mutableStateOf<String?>(null) }

    val hasExistingUsername = user.username.isNotBlank()

    if (!hasExistingUsername) {
        val normalized = usernameInput.trim().lowercase().removePrefix("@")
        LaunchedEffect(normalized) {
            if (normalized.isBlank()) {
                usernameAvailable = null
                usernameValidationError = null
                return@LaunchedEffect
            }
            if (normalized.length < 4 || normalized.length > 20) {
                usernameValidationError = "Must be 4–20 characters"
                usernameAvailable = null
                return@LaunchedEffect
            }
            if (!"^[a-zA-Z0-9_.]{4,20}$".toRegex().matches(normalized)) {
                usernameValidationError = "Only letters, numbers, dot, or underscore"
                usernameAvailable = null
                return@LaunchedEffect
            }
            usernameValidationError = null
            isCheckingUsername = true
            delay(500)
            usernameAvailable = onCheckUsernameAvailable(normalized)
            isCheckingUsername = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Edit Profile",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar and Change Photo action
                Box(contentAlignment = Alignment.Center) {
                    UserAvatar(
                        name = displayName.ifBlank { user.displayName },
                        avatarId = selectedAvatarId,
                        photoUrl = user.photoUrl,
                        size = 72.dp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onChangePhoto,
                    modifier = Modifier.testTag("change_profile_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (user.photoUrl.isNotBlank()) "Change Photo" else "Upload Profile Photo",
                        color = AccentBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Display Name Input
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = AccentBlue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_display_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bio Input
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 150) bio = it },
                    label = { Text("Bio / About") },
                    placeholder = { Text("Tell other users about yourself...", color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_bio_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Status Message Input
                OutlinedTextField(
                    value = statusMessage,
                    onValueChange = { statusMessage = it },
                    label = { Text("Status Message") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = null, tint = AccentBlue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_status_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Avatar Fallback Theme Colors
                Text(
                    text = "Avatar Color Theme",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(AvatarColorPairs) { index, pair ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(pair.first)
                                .border(
                                    width = if (selectedAvatarId == index) 2.dp else 1.dp,
                                    color = if (selectedAvatarId == index) AccentBlue else DarkBorder,
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatarId = index }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppSecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 44.dp
                    )

                    AppPrimaryButton(
                        text = "Save Changes",
                        onClick = {
                            if (!hasExistingUsername && usernameInput.isNotBlank() && usernameAvailable == true) {
                                onClaimUsername(usernameInput) { _ -> }
                            }
                            onSave(displayName, bio, statusMessage, selectedAvatarId)
                        },
                        modifier = Modifier.weight(1f),
                        height = 44.dp,
                        testTag = "save_profile_button"
                    )
                }
            }
        }
    }
}
