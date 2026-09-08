package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.ChatMessage
import com.example.model.MessageType
import com.example.model.User
import com.example.ui.components.UserAvatar
import com.example.ui.components.AppIconButton
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.AppSecondaryButton
import com.example.ui.components.AppSendButton
import com.example.ui.components.AppAttachmentGridItem
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueDark
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MessageBubbleMe
import com.example.ui.theme.MessageBubbleMeText
import com.example.ui.theme.MessageBubbleOther
import com.example.ui.theme.MessageBubbleOtherText
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FileUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val QuickEmojis = listOf("👍", "❤️", "😂", "🔥", "🎉", "👏", "🙏", "😮", "🚀", "✨")
private val ReactionEmojis = listOf("❤️", "👍", "🔥", "😂", "😮", "😢")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    currentUser: User,
    otherUser: User,
    messages: List<ChatMessage>,
    isOtherUserTyping: Boolean,
    isUploadingMedia: Boolean = false,
    mediaUploadProgress: Float = 0f,
    uploadingFileName: String = "",
    onSendMessage: (String) -> Unit,
    onSendMediaMessage: (fileUri: Uri, forcedType: MessageType?, caption: String) -> Unit = { _, _, _ -> },
    onInputChange: (String) -> Unit,
    onAddReaction: (messageId: String, reaction: String) -> Unit,
    onOpenUserProfile: (User) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var showEmojiBar by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var selectedMessageForReaction by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedImageForPreview by remember { mutableStateOf<String?>(null) }

    // Pending attachment to confirm with caption
    var pendingAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var pendingForcedType by remember { mutableStateOf<MessageType?>(null) }
    var pendingAttachmentMeta by remember { mutableStateOf<FileUtils.FileMeta?>(null) }

    // Activity Result Launchers
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val meta = FileUtils.queryFileMeta(context, uri, MessageType.IMAGE)
            pendingAttachmentUri = uri
            pendingForcedType = MessageType.IMAGE
            pendingAttachmentMeta = meta
        }
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val meta = FileUtils.queryFileMeta(context, uri, MessageType.VIDEO)
            pendingAttachmentUri = uri
            pendingForcedType = MessageType.VIDEO
            pendingAttachmentMeta = meta
        }
    }

    val pickDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val meta = FileUtils.queryFileMeta(context, uri, null)
            pendingAttachmentUri = uri
            pendingForcedType = meta.messageType
            pendingAttachmentMeta = meta
        }
    }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val meta = FileUtils.queryFileMeta(context, uri, MessageType.AUDIO)
            pendingAttachmentUri = uri
            pendingForcedType = MessageType.AUDIO
            pendingAttachmentMeta = meta
        }
    }

    val pickZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val meta = FileUtils.queryFileMeta(context, uri, MessageType.ZIP)
            pendingAttachmentUri = uri
            pendingForcedType = MessageType.ZIP
            pendingAttachmentMeta = meta
        }
    }

    val pickAnyFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val meta = FileUtils.queryFileMeta(context, uri, null)
            pendingAttachmentUri = uri
            pendingForcedType = null
            pendingAttachmentMeta = meta
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

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
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                            size = 38.dp,
                            iconSize = 22.dp,
                            testTag = "chat_back_button"
                        )

                        // User Avatar & Info (Clickable to view Profile)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenUserProfile(otherUser) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("chat_header_profile_target")
                        ) {
                            UserAvatar(
                                name = otherUser.displayName,
                                avatarId = otherUser.avatarId,
                                photoUrl = otherUser.photoUrl,
                                size = 42.dp,
                                isOnline = otherUser.isOnline
                            )

                            Column {
                                Text(
                                    text = otherUser.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitle = when {
                                    isOtherUserTyping -> "Typing..."
                                    otherUser.isOnline -> "Online"
                                    otherUser.username.isNotBlank() -> "@${otherUser.username}"
                                    else -> formatChatLastSeen(otherUser.lastSeen)
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOtherUserTyping || otherUser.isOnline) OnlineGreen else TextMuted
                                )
                            }
                        }
                    }

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppIconButton(
                            icon = Icons.Default.Person,
                            contentDescription = "View Profile",
                            onClick = { onOpenUserProfile(otherUser) },
                            tint = AccentBlue,
                            size = 38.dp,
                            iconSize = 20.dp,
                            testTag = "chat_header_view_profile"
                        )

                        AppIconButton(
                            icon = Icons.Default.Mood,
                            contentDescription = "Emojis",
                            onClick = { showEmojiBar = !showEmojiBar },
                            tint = if (showEmojiBar) AccentBlue else TextSecondary,
                            size = 38.dp,
                            iconSize = 20.dp
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
            // Reaction Bar Overlay
            AnimatedVisibility(
                visible = selectedMessageForReaction != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurfaceVariant,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add reaction:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReactionEmojis.forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 22.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            selectedMessageForReaction?.let { msg ->
                                                onAddReaction(msg.id, emoji)
                                            }
                                            selectedMessageForReaction = null
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { selectedMessageForReaction = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // In-Progress Upload Banner
            if (isUploadingMedia) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                CircularProgressIndicator(
                                    color = AccentBlue,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Uploading ${uploadingFileName.ifBlank { "attachment" }}...",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${(mediaUploadProgress * 100).toInt()}%",
                                color = AccentBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { mediaUploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = AccentBlue,
                            trackColor = DarkSurfaceVariant
                        )
                    }
                }
            }

            // Message Stream Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            UserAvatar(
                                name = otherUser.displayName,
                                avatarId = otherUser.avatarId,
                                photoUrl = otherUser.photoUrl,
                                size = 64.dp
                            )
                            Text(
                                text = "Start a conversation with ${otherUser.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            // Quick Icebreaker Chips
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                listOf("👋 Say Hello", "📷 Share a Photo", "📄 Send File").forEach { chipText ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = DarkSurface,
                                        border = BorderStroke(1.dp, DarkBorder),
                                        modifier = Modifier.clickable {
                                            if (chipText.contains("Photo")) {
                                                pickPhotoLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            } else if (chipText.contains("File")) {
                                                pickDocumentLauncher.launch(arrayOf("*/*"))
                                            } else {
                                                onSendMessage("Hello! 👋")
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = chipText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AccentBlue,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            val isMe = message.senderId == currentUser.id
                            SophisticatedMessageBubble(
                                message = message,
                                isMe = isMe,
                                onSelectForReaction = {
                                    selectedMessageForReaction = if (selectedMessageForReaction == message) null else message
                                },
                                onImageClick = { url ->
                                    selectedImageForPreview = url
                                },
                                onFileClick = {
                                    FileUtils.openUrlInExternalViewer(
                                        context = context,
                                        url = message.fileUrl,
                                        mimeType = message.mimeType,
                                        fileName = message.fileName
                                    )
                                }
                            )
                        }

                        if (isOtherUserTyping) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    TypingIndicator()
                                }
                            }
                        }
                    }
                }
            }

            // Quick Emojis Drawer
            AnimatedVisibility(visible = showEmojiBar) {
                Surface(
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(QuickEmojis) { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        inputText += emoji
                                        onInputChange(inputText)
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            // Composer Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime),
                color = DarkBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkBorderSubtle, RoundedCornerShape(26.dp))
                            .padding(start = 4.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment Plus Button
                        AppIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Add attachment",
                            onClick = { showAttachmentSheet = true },
                            tint = AccentBlue,
                            size = 38.dp,
                            iconSize = 22.dp,
                            backgroundColor = Color.Transparent,
                            testTag = "chat_attachment_button"
                        )

                        // Text Field
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Message...",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = inputText,
                                onValueChange = {
                                    inputText = it
                                    onInputChange(it)
                                },
                                textStyle = TextStyle(
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(AccentBlue),
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (inputText.isNotBlank()) {
                                        onSendMessage(inputText)
                                        inputText = ""
                                        onInputChange("")
                                    }
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_input_field")
                            )
                        }

                        // Quick Camera / Gallery Action Shortcut
                        AppIconButton(
                            icon = Icons.Default.Image,
                            contentDescription = "Photos",
                            onClick = {
                                pickPhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            tint = TextSecondary,
                            size = 36.dp,
                            iconSize = 18.dp,
                            backgroundColor = Color.Transparent
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Send Button
                        val canSend = inputText.isNotBlank()
                        AppSendButton(
                            enabled = canSend,
                            onClick = {
                                if (canSend) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                    onInputChange("")
                                }
                            },
                            size = 38.dp,
                            testTag = "send_message_button"
                        )
                    }
                }
            }
        }
    }

    // Attachment Options Bottom Sheet
    if (showAttachmentSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Share with ${otherUser.displayName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppAttachmentGridItem(
                        icon = Icons.Default.Image,
                        label = "Photos",
                        accentColor = Color(0xFF64B5F6),
                        onClick = {
                            showAttachmentSheet = false
                            pickPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    AppAttachmentGridItem(
                        icon = Icons.Default.Videocam,
                        label = "Videos",
                        accentColor = Color(0xFFFF8A65),
                        onClick = {
                            showAttachmentSheet = false
                            pickVideoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    )
                    AppAttachmentGridItem(
                        icon = Icons.Default.Description,
                        label = "Document",
                        accentColor = Color(0xFF81C784),
                        onClick = {
                            showAttachmentSheet = false
                            pickDocumentLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "text/plain"
                                )
                            )
                        }
                    )
                    AppAttachmentGridItem(
                        icon = Icons.Default.Audiotrack,
                        label = "Audio",
                        accentColor = Color(0xFFBA68C8),
                        onClick = {
                            showAttachmentSheet = false
                            pickAudioLauncher.launch(arrayOf("audio/*"))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    AppAttachmentGridItem(
                        icon = Icons.Default.FolderZip,
                        label = "ZIP Archive",
                        accentColor = Color(0xFFFFD54F),
                        onClick = {
                            showAttachmentSheet = false
                            pickZipLauncher.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/x-zip-compressed",
                                    "application/octet-stream"
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.width(28.dp))
                    AppAttachmentGridItem(
                        icon = Icons.Default.AttachFile,
                        label = "Any File",
                        accentColor = Color(0xFF4DB6AC),
                        onClick = {
                            showAttachmentSheet = false
                            pickAnyFileLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }
            }
        }
    }

    // Attachment Confirmation & Caption Dialog
    if (pendingAttachmentUri != null && pendingAttachmentMeta != null) {
        val meta = pendingAttachmentMeta!!
        AttachmentConfirmDialog(
            uri = pendingAttachmentUri!!,
            meta = meta,
            onDismiss = {
                pendingAttachmentUri = null
                pendingAttachmentMeta = null
                pendingForcedType = null
            },
            onSend = { caption ->
                onSendMediaMessage(pendingAttachmentUri!!, pendingForcedType, caption)
                pendingAttachmentUri = null
                pendingAttachmentMeta = null
                pendingForcedType = null
            }
        )
    }

    // Full Screen Image Viewer Dialog
    selectedImageForPreview?.let { imageUrl ->
        Dialog(
            onDismissRequest = { selectedImageForPreview = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Image preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    AppIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = { selectedImageForPreview = null },
                        tint = Color.White,
                        backgroundColor = Color.Black.copy(alpha = 0.5f),
                        size = 40.dp,
                        iconSize = 22.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AttachmentConfirmDialog(
    uri: Uri,
    meta: FileUtils.FileMeta,
    onDismiss: () -> Unit,
    onSend: (caption: String) -> Unit
) {
    var caption by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            color = DarkSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Send ${meta.messageType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    AppIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        tint = TextMuted,
                        size = 28.dp,
                        iconSize = 18.dp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Preview Card
                if (meta.messageType == MessageType.IMAGE) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(uri)
                                .build(),
                            contentDescription = "Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkBg),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (icon, tint) = when (meta.messageType) {
                                MessageType.VIDEO -> Pair(Icons.Default.Videocam, Color(0xFFFF8A65))
                                MessageType.AUDIO -> Pair(Icons.Default.Audiotrack, Color(0xFFBA68C8))
                                MessageType.DOCUMENT -> Pair(Icons.Default.Description, Color(0xFF81C784))
                                MessageType.ZIP -> Pair(Icons.Default.FolderZip, Color(0xFFFFD54F))
                                else -> Pair(Icons.AutoMirrored.Filled.InsertDriveFile, AccentBlue)
                            }

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(tint.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meta.name,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${FileUtils.formatFileSize(meta.size)} • ${meta.mimeType}",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Caption Input
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Add a caption...", color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attachment_caption_input"),
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
                        height = 44.dp
                    )

                    AppPrimaryButton(
                        text = "Send",
                        onClick = { onSend(caption) },
                        icon = Icons.AutoMirrored.Filled.Send,
                        modifier = Modifier.weight(1f),
                        height = 44.dp,
                        testTag = "confirm_send_attachment_button"
                    )
                }
            }
        }
    }
}

@Composable
fun SophisticatedMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    onSelectForReaction: () -> Unit,
    onImageClick: (String) -> Unit = {},
    onFileClick: () -> Unit = {}
) {
    val bubbleShape = if (isMe) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 4.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    }

    val resolvedType = message.getResolvedType()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(if (isMe) MessageBubbleMe else MessageBubbleOther)
                    .border(
                        width = 1.dp,
                        color = if (isMe) AccentBlue.copy(alpha = 0.25f) else DarkBorderSubtle,
                        shape = bubbleShape
                    )
                    .clickable { onSelectForReaction() }
            ) {
                Column {
                    when (resolvedType) {
                        MessageType.IMAGE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clickable { onImageClick(message.fileUrl) }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(message.fileUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = message.text.ifBlank { "Shared photo" },
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                )
                            }
                            if (message.text.isNotBlank()) {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMe) MessageBubbleMeText else MessageBubbleOtherText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        MessageType.VIDEO -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color.Black)
                                    .clickable { onFileClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = "Play Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(54.dp)
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = FileUtils.formatFileSize(message.fileSize),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (message.text.isNotBlank()) {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMe) MessageBubbleMeText else MessageBubbleOtherText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        MessageType.AUDIO -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFileClick() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isMe) AccentBlueDark else AccentBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Audio",
                                        tint = if (isMe) AccentBlue else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.fileName.ifBlank { "Audio recording" },
                                        color = if (isMe) MessageBubbleMeText else MessageBubbleOtherText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = FileUtils.formatFileSize(message.fileSize),
                                        color = if (isMe) MessageBubbleMeText.copy(alpha = 0.7f) else TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (message.text.isNotBlank()) {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMe) MessageBubbleMeText else MessageBubbleOtherText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        MessageType.DOCUMENT, MessageType.ZIP, MessageType.FILE -> {
                            val (badgeIcon, badgeColor) = when (resolvedType) {
                                MessageType.DOCUMENT -> Pair(Icons.Default.Description, Color(0xFF81C784))
                                MessageType.ZIP -> Pair(Icons.Default.FolderZip, Color(0xFFFFD54F))
                                else -> Pair(Icons.AutoMirrored.Filled.InsertDriveFile, Color(0xFF64B5F6))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFileClick() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(badgeColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = badgeIcon,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.fileName.ifBlank { "Attachment" },
                                        color = if (isMe) MessageBubbleMeText else MessageBubbleOtherText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${FileUtils.formatFileSize(message.fileSize)} • Tap to open",
                                        color = if (isMe) MessageBubbleMeText.copy(alpha = 0.7f) else TextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open file",
                                    tint = if (isMe) MessageBubbleMeText.copy(alpha = 0.7f) else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (message.text.isNotBlank()) {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMe) MessageBubbleMeText else MessageBubbleOtherText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        MessageType.TEXT -> {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMe) MessageBubbleMeText else MessageBubbleOtherText,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Timestamp + Done All receipt
            Row(
                modifier = Modifier.padding(
                    top = 4.dp,
                    start = if (isMe) 0.dp else 4.dp,
                    end = if (isMe) 4.dp else 0.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatMessageTimestamp(message.timestamp).uppercase(),
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )

                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Check,
                        contentDescription = if (message.isRead) "Read" else "Sent",
                        tint = if (message.isRead) AccentBlue else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Reaction Badge
            if (message.reaction.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                ) {
                    Text(
                        text = message.reaction,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, 0), RepeatMode.Reverse),
        label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, 200), RepeatMode.Reverse),
        label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, 400), RepeatMode.Reverse),
        label = "dot3"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = dot1))
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = dot2))
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = dot3))
            )
        }
    }
}

private fun formatChatLastSeen(lastSeen: Long): String {
    if (lastSeen <= 0) return "Offline"
    val diff = System.currentTimeMillis() - lastSeen
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Last seen just now"
        minutes < 60 -> "Last seen $minutes m ago"
        hours < 24 -> "Last seen $hours h ago"
        days < 7 -> "Last seen $days d ago"
        else -> "Offline"
    }
}

private fun formatMessageTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
