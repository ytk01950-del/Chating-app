package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.User
import com.example.ui.components.NexaSplashScreen
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.CreateStoryDialog
import com.example.ui.screens.FollowersScreen
import com.example.ui.screens.FollowingScreen
import com.example.ui.screens.IncomingCallDialog
import com.example.ui.screens.ProfileDetailsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SocialProfileScreen
import com.example.ui.screens.StoryViewerDialog
import com.example.ui.screens.UserDirectoryScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemePreferences
import com.example.util.WpChatNotificationHelper
import com.example.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    companion object {
        var appContext: Context? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        enableEdgeToEdge()

        handleNotificationIntent(intent)

        val themePreferences = ThemePreferences(applicationContext)

        setContent {
            val currentThemeMode by themePreferences.themeMode.collectAsStateWithLifecycle()

            MyApplicationTheme(
                themeMode = currentThemeMode,
                onThemeModeChange = { newMode ->
                    themePreferences.setThemeMode(newMode)
                }
            ) {
                WpChatApp(
                    viewModel = chatViewModel,
                    currentThemeMode = currentThemeMode,
                    onThemeModeChange = { newMode ->
                        themePreferences.setThemeMode(newMode)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return

        val callId = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALL_ID)
        val callAction = intent.getStringExtra(WpChatNotificationHelper.EXTRA_ACTION_CALL)
        if (!callId.isNullOrBlank()) {
            chatViewModel.handleCallFromIntent(callId, callAction ?: "incoming", this)
            return
        }

        val targetUserId = intent.getStringExtra(WpChatNotificationHelper.EXTRA_USER_ID)
        if (!targetUserId.isNullOrBlank()) {
            chatViewModel.openChatByUserId(targetUserId)
        }
    }
}

@Composable
fun WpChatApp(
    viewModel: ChatViewModel = viewModel(),
    currentThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeModeChange: (AppThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val users by viewModel.filteredUsers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchUserResult by viewModel.searchUserResult.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearchingUser by viewModel.isSearchingUser.collectAsStateWithLifecycle()
    val searchUserNotFound by viewModel.searchUserNotFound.collectAsStateWithLifecycle()
    val activeContact by viewModel.activeContact.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val isOtherUserTyping by viewModel.isOtherUserTyping.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val infoMessage by viewModel.infoMessage.collectAsStateWithLifecycle()

    // Social profile & 24-Hour Stories state
    val selectedProfileUser by viewModel.selectedProfileUser.collectAsStateWithLifecycle()
    val userStories by viewModel.userStories.collectAsStateWithLifecycle()
    val activeStories by viewModel.activeStories.collectAsStateWithLifecycle()
    val groupedStories by viewModel.groupedStories.collectAsStateWithLifecycle()
    val isUploadingProfilePhoto by viewModel.isUploadingProfilePhoto.collectAsStateWithLifecycle()
    val isCreatingStory by viewModel.isCreatingStory.collectAsStateWithLifecycle()
    val storyUploadProgress by viewModel.storyUploadProgress.collectAsStateWithLifecycle()
    val isDeletingStory by viewModel.isDeletingStory.collectAsStateWithLifecycle()
    val activeStoryViewer by viewModel.activeStoryViewer.collectAsStateWithLifecycle()

    // Chat Media uploads state
    val isUploadingMedia by viewModel.isUploadingMedia.collectAsStateWithLifecycle()
    val mediaUploadProgress by viewModel.mediaUploadProgress.collectAsStateWithLifecycle()
    val uploadingFileName by viewModel.uploadingFileName.collectAsStateWithLifecycle()

    // 1-on-1 Audio and Video Calling State
    val incomingCall by viewModel.incomingCall.collectAsStateWithLifecycle()
    val activeCallSession by viewModel.activeCallSession.collectAsStateWithLifecycle()
    val callHistory by viewModel.callHistory.collectAsStateWithLifecycle()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
    val isMicMuted by viewModel.isMicMuted.collectAsStateWithLifecycle()
    val isVideoCameraOff by viewModel.isVideoCameraOff.collectAsStateWithLifecycle()
    val isFrontCamera by viewModel.isFrontCamera.collectAsStateWithLifecycle()
    val callDurationSeconds by viewModel.callDurationSeconds.collectAsStateWithLifecycle()

    // Followers, Following & Privacy State
    val followedUserIds by viewModel.followedUserIds.collectAsStateWithLifecycle()
    val viewingFollowersOfUser by viewModel.viewingFollowersOfUser.collectAsStateWithLifecycle()
    val viewingFollowingOfUser by viewModel.viewingFollowingOfUser.collectAsStateWithLifecycle()
    val followersList by viewModel.followersList.collectAsStateWithLifecycle()
    val followingList by viewModel.followingList.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()

    var pendingCallTarget by remember { mutableStateOf<User?>(null) }
    var pendingCallIsVideo by remember { mutableStateOf(false) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val target = pendingCallTarget
        val isVideo = pendingCallIsVideo
        pendingCallTarget = null

        if (target != null) {
            if (isVideo) {
                if (recordAudioGranted && cameraGranted) {
                    viewModel.startVideoCall(target, context)
                } else {
                    viewModel.showError("Microphone and Camera permissions are required for video calls")
                }
            } else {
                if (recordAudioGranted) {
                    viewModel.startAudioCall(target, context)
                } else {
                    viewModel.showError("Microphone permission is required for voice calls")
                }
            }
        }
    }

    val requestAndStartCall: (User, Boolean) -> Unit = { targetUser, isVideo ->
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        if (isVideo) {
            if (hasAudio && hasCamera) {
                viewModel.startVideoCall(targetUser, context)
            } else {
                pendingCallTarget = targetUser
                pendingCallIsVideo = true
                callPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
            }
        } else {
            if (hasAudio) {
                viewModel.startAudioCall(targetUser, context)
            } else {
                pendingCallTarget = targetUser
                pendingCallIsVideo = false
                callPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            }
        }
    }

    // Local dialog state for adding stories directly from directory tray
    var pendingTrayMediaUri by remember { androidx.compose.runtime.mutableStateOf<android.net.Uri?>(null) }
    var pendingTrayIsVideo by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showTrayCreateStoryDialog by remember { androidx.compose.runtime.mutableStateOf(false) }

    val trayStoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val isVid = context.contentResolver.getType(uri)?.startsWith("video/") == true
            pendingTrayMediaUri = uri
            pendingTrayIsVideo = isVid
            showTrayCreateStoryDialog = true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Request Notification permission on Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(currentUser) {
        if (currentUser != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(infoMessage) {
        infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfo()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    var isSplashVisible by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isSplashVisible) {
            NexaSplashScreen(
                onAnimationFinished = {
                    isSplashVisible = false
                }
            )
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
            AnimatedContent(
                targetState = when {
                    currentUser == null -> ScreenState.Auth
                    isSettingsOpen -> ScreenState.Settings
                    viewingFollowersOfUser != null -> ScreenState.Followers
                    viewingFollowingOfUser != null -> ScreenState.Following
                    selectedProfileUser != null -> ScreenState.ProfileDetails
                    activeContact != null -> ScreenState.ChatDetail
                    else -> ScreenState.Directory
                },
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + slideInHorizontally(
                        animationSpec = tween(220),
                        initialOffsetX = { fullWidth -> fullWidth / 8 }
                    )) togetherWith (fadeOut(animationSpec = tween(180)) + slideOutHorizontally(
                        animationSpec = tween(180),
                        targetOffsetX = { fullWidth -> -fullWidth / 8 }
                    ))
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                label = "ScreenTransition"
            ) { state ->
                when (state) {
                    ScreenState.Auth -> {
                        AuthScreen(
                            authUiState = authUiState,
                            errorMessage = errorMessage,
                            onSignUp = { email, pass, name, username, avatarId ->
                                viewModel.signUp(email, pass, name, username, avatarId)
                            },
                            onLogIn = { email, pass ->
                                viewModel.logIn(email, pass)
                            },
                            onGoogleSignIn = { idToken ->
                                viewModel.signInWithGoogle(idToken)
                            },
                            onResetPassword = { email ->
                                viewModel.resetPassword(email)
                            },
                            onClearError = {
                                viewModel.clearError()
                            },
                            onCheckUsernameAvailable = { username ->
                                viewModel.checkUsernameAvailability(username)
                            }
                        )
                    }

                    ScreenState.Directory -> {
                        val user = currentUser
                        if (user != null) {
                            UserDirectoryScreen(
                                currentUser = user,
                                users = users,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                searchUserResult = searchUserResult,
                                searchResults = searchResults,
                                isSearchingUser = isSearchingUser,
                                searchUserNotFound = searchUserNotFound,
                                myStories = activeStories.filter { it.userId == user.id },
                                groupedStories = groupedStories,
                                callHistory = callHistory,
                                onStartVoiceCall = { targetUser ->
                                    requestAndStartCall(targetUser, false)
                                },
                                onStartVideoCall = { targetUser ->
                                    requestAndStartCall(targetUser, true)
                                },
                                onOpenOtherUserProfile = { targetUser ->
                                    viewModel.openUserProfile(targetUser)
                                },
                                onOpenAddStory = {
                                    trayStoryPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                onViewUserStories = { storyUser, stories ->
                                    viewModel.openStoryViewer(storyUser, stories)
                                },
                                onSearchByChatId = { viewModel.searchUserByChatId(it) },
                                onSearchDirectory = { viewModel.searchDirectory(it) },
                                onClearChatIdSearch = { viewModel.clearSearchUserResult() },
                                onSelectUser = { viewModel.openChatWith(it) },
                                onOpenProfile = { viewModel.openCurrentProfile() },
                                onUploadProfilePhoto = { uri ->
                                    viewModel.uploadProfilePhoto(uri, context)
                                },
                                onUpdateProfile = { name, bio, status, avatarId, gender ->
                                    viewModel.updateProfileDetails(name, bio, status, avatarId, gender)
                                },
                                onUpdateProfileExtended = { name, bio, status, avatarId, gender, website, dob ->
                                    viewModel.updateProfileDetails(name, bio, status, avatarId, gender, website, dob)
                                },
                                onClaimUsername = { username, callback ->
                                    viewModel.claimUsernameForCurrentUser(username, callback)
                                },
                                onCheckUsernameAvailable = { username ->
                                    viewModel.checkUsernameAvailability(username)
                                },
                                onOpenSettings = { viewModel.openSettings() },
                                onOpenFollowers = { targetUser ->
                                    viewModel.openFollowersScreen(targetUser)
                                },
                                onOpenFollowing = { targetUser ->
                                    viewModel.openFollowingScreen(targetUser)
                                },
                                onFollowClick = { targetUser ->
                                    viewModel.followUser(targetUser)
                                },
                                onUnfollowClick = { targetUser ->
                                    viewModel.unfollowUser(targetUser)
                                },
                                onUpdatePrivacy = { whoCanFollow, showFollowers, showFollowing, showList, showDob ->
                                    viewModel.updatePrivacySettings(whoCanFollow, showFollowers, showFollowing, showList, showDob)
                                },
                                isUploadingProfilePhoto = isUploadingProfilePhoto,
                                currentThemeMode = currentThemeMode,
                                onThemeModeChange = onThemeModeChange,
                                onSignOut = { viewModel.signOut() }
                            )
                        }
                    }

                    ScreenState.Settings -> {
                        val user = currentUser
                        if (user != null) {
                            BackHandler {
                                viewModel.closeSettings()
                            }
                            SettingsScreen(
                                currentUser = user,
                                onBack = { viewModel.closeSettings() },
                                onUploadProfilePhoto = { uri ->
                                    viewModel.uploadProfilePhoto(uri, context)
                                },
                                onUpdateProfile = { name, bio, status, avatarId, gender ->
                                    viewModel.updateProfileDetails(name, bio, status, avatarId, gender)
                                },
                                onClaimUsername = { username, callback ->
                                    viewModel.claimUsernameForCurrentUser(username, callback)
                                },
                                onCheckUsernameAvailable = { username ->
                                    viewModel.checkUsernameAvailability(username)
                                },
                                isUploadingPhoto = isUploadingProfilePhoto,
                                currentThemeMode = currentThemeMode,
                                onThemeModeChange = onThemeModeChange,
                                onSignOut = {
                                    viewModel.closeSettings()
                                    viewModel.signOut()
                                }
                            )
                        }
                    }

                    ScreenState.ProfileDetails -> {
                        val user = currentUser
                        val profileUser = selectedProfileUser
                        if (user != null && profileUser != null) {
                            BackHandler {
                                viewModel.closeUserProfile()
                            }
                            ProfileDetailsScreen(
                                currentUser = user,
                                profileUser = profileUser,
                                isFollowing = followedUserIds.contains(profileUser.id),
                                onBack = { viewModel.closeUserProfile() },
                                onOpenChat = { targetUser ->
                                    viewModel.closeUserProfile()
                                    viewModel.openChatWith(targetUser)
                                },
                                onFollowClick = { targetUser ->
                                    viewModel.followUser(targetUser)
                                },
                                onUnfollowClick = { targetUser ->
                                    viewModel.unfollowUser(targetUser)
                                },
                                onOpenFollowers = { targetUser ->
                                    viewModel.openFollowersScreen(targetUser)
                                },
                                onOpenFollowing = { targetUser ->
                                    viewModel.openFollowingScreen(targetUser)
                                },
                                onUpdatePrivacy = { whoCanFollow, showFollowers, showFollowing, showList, showDob ->
                                    viewModel.updatePrivacySettings(whoCanFollow, showFollowers, showFollowing, showList, showDob)
                                },
                                onUpdateProfile = { name, bio, status, avatarId, gender, website, dob ->
                                    viewModel.updateProfileDetails(name, bio, status, avatarId, gender, website, dob)
                                },
                                onOpenSettings = {
                                    viewModel.openSettings()
                                }
                            )
                        }
                    }

                    ScreenState.Followers -> {
                        val user = currentUser
                        val profileUser = viewingFollowersOfUser
                        if (user != null && profileUser != null) {
                            BackHandler {
                                viewModel.closeFollowersScreen()
                            }
                            FollowersScreen(
                                currentUser = user,
                                profileUser = profileUser,
                                followersList = followersList,
                                followedUserIds = followedUserIds,
                                onBack = { viewModel.closeFollowersScreen() },
                                onUserClick = { clickedUser ->
                                    viewModel.closeFollowersScreen()
                                    viewModel.openUserProfile(clickedUser)
                                },
                                onFollowClick = { targetUser ->
                                    viewModel.followUser(targetUser)
                                },
                                onUnfollowClick = { targetUser ->
                                    viewModel.unfollowUser(targetUser)
                                }
                            )
                        }
                    }

                    ScreenState.Following -> {
                        val user = currentUser
                        val profileUser = viewingFollowingOfUser
                        if (user != null && profileUser != null) {
                            BackHandler {
                                viewModel.closeFollowingScreen()
                            }
                            FollowingScreen(
                                currentUser = user,
                                profileUser = profileUser,
                                followingList = followingList,
                                followedUserIds = followedUserIds,
                                onBack = { viewModel.closeFollowingScreen() },
                                onUserClick = { clickedUser ->
                                    viewModel.closeFollowingScreen()
                                    viewModel.openUserProfile(clickedUser)
                                },
                                onFollowClick = { targetUser ->
                                    viewModel.followUser(targetUser)
                                },
                                onUnfollowClick = { targetUser ->
                                    viewModel.unfollowUser(targetUser)
                                }
                            )
                        }
                    }

                    ScreenState.SocialProfile -> {
                        val user = currentUser
                        val profileUser = selectedProfileUser
                        if (user != null && profileUser != null) {
                            BackHandler {
                                viewModel.closeUserProfile()
                            }
                            SocialProfileScreen(
                                currentUser = user,
                                profileUser = profileUser,
                                stories = userStories,
                                isUploadingPhoto = isUploadingProfilePhoto,
                                isCreatingStory = isCreatingStory,
                                storyUploadProgress = storyUploadProgress,
                                isDeletingStory = isDeletingStory,
                                onBack = { viewModel.closeUserProfile() },
                                onOpenChat = { targetUser ->
                                    viewModel.closeUserProfile()
                                    viewModel.openChatWith(targetUser)
                                },
                                onUploadProfilePhoto = { photoUri ->
                                    viewModel.uploadProfilePhoto(photoUri, context)
                                },
                                onCreateStory = { mediaUri, isVideo, caption ->
                                    viewModel.createStory(mediaUri, isVideo, caption, context)
                                },
                                onDeleteStory = { story ->
                                    viewModel.deleteStory(story)
                                },
                                onUpdateProfile = { name, bio, status, avatarId, gender ->
                                    viewModel.updateProfileDetails(name, bio, status, avatarId, gender)
                                },
                                onClaimUsername = { username, callback ->
                                    viewModel.claimUsernameForCurrentUser(username, callback)
                                },
                                onCheckUsernameAvailable = { username ->
                                    viewModel.checkUsernameAvailability(username)
                                },
                                onStoryViewed = { storyId ->
                                    viewModel.markStoryViewed(storyId)
                                }
                            )
                        }
                    }

                    ScreenState.ChatDetail -> {
                        val user = currentUser
                        val contact = activeContact
                        if (user != null && contact != null) {
                            BackHandler {
                                viewModel.closeChat()
                            }
                            ChatDetailScreen(
                                currentUser = user,
                                otherUser = contact,
                                messages = activeMessages,
                                isOtherUserTyping = isOtherUserTyping,
                                isUploadingMedia = isUploadingMedia,
                                mediaUploadProgress = mediaUploadProgress,
                                uploadingFileName = uploadingFileName,
                                onBack = { viewModel.closeChat() },
                                onSendMessage = { viewModel.sendMessage(it) },
                                onSendMediaMessage = { fileUri, forcedType, caption, viewLimit, allowDownload ->
                                    viewModel.sendMediaMessage(
                                        fileUri = fileUri,
                                        forcedType = forcedType,
                                        caption = caption,
                                        viewLimit = viewLimit,
                                        allowDownload = allowDownload,
                                        context = context
                                    )
                                },
                                onMarkMediaViewed = { msgId ->
                                    viewModel.markTemporaryMediaViewed(msgId)
                                },
                                onMarkMediaExpired = { msgId ->
                                    viewModel.markMediaExpired(msgId)
                                },
                                onInputChange = { viewModel.onMessageInputChanged(it) },
                                onAddReaction = { msgId, reaction ->
                                    viewModel.addReaction(msgId, reaction)
                                },
                                onOpenUserProfile = { targetUser ->
                                    viewModel.openUserProfile(targetUser)
                                },
                                onStartVoiceCall = {
                                    requestAndStartCall(contact, false)
                                },
                                onStartVideoCall = {
                                    requestAndStartCall(contact, true)
                                }
                            )
                        }
                    }
                }
            }

            // Global Incoming Call Screen Overlay
            incomingCall?.let { session ->
                IncomingCallDialog(
                    callSession = session,
                    onAccept = {
                        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

                        if (session.isVideo && (!hasAudio || !hasCamera)) {
                            pendingCallTarget = null
                            callPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
                        } else if (!session.isVideo && !hasAudio) {
                            pendingCallTarget = null
                            callPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        } else {
                            viewModel.acceptIncomingCall(context)
                        }
                    },
                    onReject = {
                        viewModel.rejectIncomingCall(context)
                    }
                )
            }

            // Global Active Call Screen (Outgoing / Ringing / Connected)
            val currentSession = activeCallSession
            val currentLoggedInUser = currentUser
            if (currentSession != null && currentLoggedInUser != null) {
                ActiveCallScreen(
                    currentUser = currentLoggedInUser,
                    callSession = currentSession,
                    durationSeconds = callDurationSeconds,
                    isSpeakerOn = isSpeakerOn,
                    isMicMuted = isMicMuted,
                    isVideoCameraOff = isVideoCameraOff,
                    isFrontCamera = isFrontCamera,
                    onToggleSpeaker = { viewModel.toggleSpeaker(context) },
                    onToggleMute = { viewModel.toggleMute(context) },
                    onToggleVideoCamera = { viewModel.toggleVideoCamera() },
                    onSwitchCamera = { viewModel.switchCameraFacing() },
                    onEndCall = { viewModel.endActiveCall(context) }
                )
            }

            // Global Full-Screen 24-Hour Story Viewer
            activeStoryViewer?.let { viewerState ->
                val current = currentUser
                if (current != null) {
                    StoryViewerDialog(
                        user = viewerState.first,
                        stories = viewerState.second,
                        currentUserId = current.id,
                        initialIndex = 0,
                        onDismiss = { viewModel.closeStoryViewer() },
                        onDeleteStory = { story ->
                            viewModel.deleteStory(story)
                        },
                        onStoryViewed = { storyId ->
                            viewModel.markStoryViewed(storyId)
                        }
                    )
                }
            }

            // Global Create Story Dialog (e.g. from Directory Story Tray)
            if (showTrayCreateStoryDialog && pendingTrayMediaUri != null) {
                CreateStoryDialog(
                    mediaUri = pendingTrayMediaUri!!,
                    isVideo = pendingTrayIsVideo,
                    isUploading = isCreatingStory,
                    uploadProgress = storyUploadProgress,
                    onDismiss = {
                        showTrayCreateStoryDialog = false
                        pendingTrayMediaUri = null
                    },
                    onShareStory = { mediaUri, isVideo, caption ->
                        viewModel.createStory(mediaUri, isVideo, caption, context)
                        showTrayCreateStoryDialog = false
                        pendingTrayMediaUri = null
                    }
                )
            }
        }
        }
    }
}

enum class ScreenState {
    Auth,
    Directory,
    ProfileDetails,
    Followers,
    Following,
    SocialProfile,
    ChatDetail,
    Settings
}
