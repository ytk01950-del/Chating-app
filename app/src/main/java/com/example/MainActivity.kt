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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.SocialProfileScreen
import com.example.ui.screens.UserDirectoryScreen
import com.example.ui.theme.MyApplicationTheme
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

        setContent {
            MyApplicationTheme {
                WpChatApp(viewModel = chatViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val targetUserId = intent?.getStringExtra(WpChatNotificationHelper.EXTRA_USER_ID)
        if (!targetUserId.isNullOrBlank()) {
            chatViewModel.openChatByUserId(targetUserId)
        }
    }
}

@Composable
fun WpChatApp(
    viewModel: ChatViewModel = viewModel()
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

    // Social profile & Posts state
    val selectedProfileUser by viewModel.selectedProfileUser.collectAsStateWithLifecycle()
    val userPosts by viewModel.userPosts.collectAsStateWithLifecycle()
    val isUploadingProfilePhoto by viewModel.isUploadingProfilePhoto.collectAsStateWithLifecycle()
    val isCreatingPost by viewModel.isCreatingPost.collectAsStateWithLifecycle()
    val postUploadProgress by viewModel.postUploadProgress.collectAsStateWithLifecycle()
    val isDeletingPost by viewModel.isDeletingPost.collectAsStateWithLifecycle()

    // Chat Media uploads state
    val isUploadingMedia by viewModel.isUploadingMedia.collectAsStateWithLifecycle()
    val mediaUploadProgress by viewModel.mediaUploadProgress.collectAsStateWithLifecycle()
    val uploadingFileName by viewModel.uploadingFileName.collectAsStateWithLifecycle()

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            AnimatedContent(
                targetState = when {
                    currentUser == null -> ScreenState.Auth
                    selectedProfileUser != null -> ScreenState.SocialProfile
                    activeContact != null -> ScreenState.ChatDetail
                    else -> ScreenState.Directory
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
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
                                onSearchByChatId = { viewModel.searchUserByChatId(it) },
                                onSearchDirectory = { viewModel.searchDirectory(it) },
                                onClearChatIdSearch = { viewModel.clearSearchUserResult() },
                                onSelectUser = { viewModel.openChatWith(it) },
                                onOpenProfile = { viewModel.openCurrentProfile() },
                                onSignOut = { viewModel.signOut() }
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
                                posts = userPosts,
                                isUploadingPhoto = isUploadingProfilePhoto,
                                isCreatingPost = isCreatingPost,
                                postUploadProgress = postUploadProgress,
                                isDeletingPost = isDeletingPost,
                                onBack = { viewModel.closeUserProfile() },
                                onOpenChat = { targetUser ->
                                    viewModel.closeUserProfile()
                                    viewModel.openChatWith(targetUser)
                                },
                                onUploadProfilePhoto = { photoUri ->
                                    viewModel.uploadProfilePhoto(photoUri, context)
                                },
                                onCreatePost = { imageUri, caption ->
                                    viewModel.createPost(imageUri, caption, context)
                                },
                                onDeletePost = { post ->
                                    viewModel.deletePost(post)
                                },
                                onUpdateProfile = { name, bio, status, avatarId ->
                                    viewModel.updateProfileDetails(name, bio, status, avatarId)
                                },
                                onClaimUsername = { username, callback ->
                                    viewModel.claimUsernameForCurrentUser(username, callback)
                                },
                                onCheckUsernameAvailable = { username ->
                                    viewModel.checkUsernameAvailability(username)
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
                                onSendMediaMessage = { fileUri, forcedType, caption ->
                                    viewModel.sendMediaMessage(fileUri, forcedType, caption, context)
                                },
                                onInputChange = { viewModel.onMessageInputChanged(it) },
                                onAddReaction = { msgId, reaction ->
                                    viewModel.addReaction(msgId, reaction)
                                },
                                onOpenUserProfile = { targetUser ->
                                    viewModel.openUserProfile(targetUser)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class ScreenState {
    Auth,
    Directory,
    SocialProfile,
    ChatDetail
}
