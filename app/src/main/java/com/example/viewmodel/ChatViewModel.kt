package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CallRecord
import com.example.model.CallSession
import com.example.model.CallStatus
import com.example.model.CallType
import com.example.model.ChatMessage
import com.example.model.MessageRequest
import com.example.model.MessageType
import com.example.model.Post
import com.example.model.Story
import com.example.model.User
import com.example.model.UserReport
import com.example.model.UserStoryGroup
import com.example.repository.FirebaseChatRepository
import com.example.util.FileUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Authenticated(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class ChatViewModel(
    private val repository: FirebaseChatRepository = FirebaseChatRepository()
) : ViewModel() {

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeContact = MutableStateFlow<User?>(null)
    val activeContact: StateFlow<User?> = _activeContact.asStateFlow()

    private val _activeMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeMessages: StateFlow<List<ChatMessage>> = _activeMessages.asStateFlow()

    private val _isOtherUserTyping = MutableStateFlow(false)
    val isOtherUserTyping: StateFlow<Boolean> = _isOtherUserTyping.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    // -------------------------------------------------------------
    // SOCIAL PROFILE & 24-HOUR STORIES STATE
    // -------------------------------------------------------------
    private val _selectedProfileUser = MutableStateFlow<User?>(null)
    val selectedProfileUser: StateFlow<User?> = _selectedProfileUser.asStateFlow()

    private val _activeStories = MutableStateFlow<List<Story>>(emptyList())
    val activeStories: StateFlow<List<Story>> = _activeStories.asStateFlow()

    private val _userStories = MutableStateFlow<List<Story>>(emptyList())
    val userStories: StateFlow<List<Story>> = _userStories.asStateFlow()

    private val _isUploadingProfilePhoto = MutableStateFlow(false)
    val isUploadingProfilePhoto: StateFlow<Boolean> = _isUploadingProfilePhoto.asStateFlow()

    private val _isCreatingStory = MutableStateFlow(false)
    val isCreatingStory: StateFlow<Boolean> = _isCreatingStory.asStateFlow()

    private val _storyUploadProgress = MutableStateFlow(0f)
    val storyUploadProgress: StateFlow<Float> = _storyUploadProgress.asStateFlow()

    private val _isDeletingStory = MutableStateFlow(false)
    val isDeletingStory: StateFlow<Boolean> = _isDeletingStory.asStateFlow()

    private val _activeStoryViewer = MutableStateFlow<Pair<User, List<Story>>?>(null)
    val activeStoryViewer: StateFlow<Pair<User, List<Story>>?> = _activeStoryViewer.asStateFlow()

    // -------------------------------------------------------------
    // ONLINE USERS, FEED POSTS, REQUESTS & BLOCKS STATE
    // -------------------------------------------------------------
    private val _onlineUsers = MutableStateFlow<List<User>>(emptyList())
    val onlineUsers: StateFlow<List<User>> = _onlineUsers.asStateFlow()

    private val _feedPosts = MutableStateFlow<List<Post>>(emptyList())
    val feedPosts: StateFlow<List<Post>> = _feedPosts.asStateFlow()

    private val _messageRequests = MutableStateFlow<List<MessageRequest>>(emptyList())
    val messageRequests: StateFlow<List<MessageRequest>> = _messageRequests.asStateFlow()

    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    private val _isUploadingPost = MutableStateFlow(false)
    val isUploadingPost: StateFlow<Boolean> = _isUploadingPost.asStateFlow()

    private val _postUploadProgress = MutableStateFlow(0f)
    val postUploadProgress: StateFlow<Float> = _postUploadProgress.asStateFlow()

    // -------------------------------------------------------------
    // CHAT MEDIA UPLOADS STATE
    // -------------------------------------------------------------
    private val _isUploadingMedia = MutableStateFlow(false)
    val isUploadingMedia: StateFlow<Boolean> = _isUploadingMedia.asStateFlow()

    private val _mediaUploadProgress = MutableStateFlow(0f)
    val mediaUploadProgress: StateFlow<Float> = _mediaUploadProgress.asStateFlow()

    private val _uploadingFileName = MutableStateFlow("")
    val uploadingFileName: StateFlow<String> = _uploadingFileName.asStateFlow()

    // -------------------------------------------------------------
    // WEBRTC / 1-ON-1 AUDIO & VIDEO CALLING STATE
    // -------------------------------------------------------------
    private val _incomingCall = MutableStateFlow<CallSession?>(null)
    val incomingCall: StateFlow<CallSession?> = _incomingCall.asStateFlow()

    private val _activeCallSession = MutableStateFlow<CallSession?>(null)
    val activeCallSession: StateFlow<CallSession?> = _activeCallSession.asStateFlow()

    private val _callHistory = MutableStateFlow<List<CallRecord>>(emptyList())
    val callHistory: StateFlow<List<CallRecord>> = _callHistory.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isVideoCameraOff = MutableStateFlow(false)
    val isVideoCameraOff: StateFlow<Boolean> = _isVideoCameraOff.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0L)
    val callDurationSeconds: StateFlow<Long> = _callDurationSeconds.asStateFlow()

    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    private var usersJob: Job? = null
    private var onlineUsersJob: Job? = null
    private var activeStoriesJob: Job? = null
    private var feedPostsJob: Job? = null
    private var messageRequestsJob: Job? = null
    private var blockedUsersJob: Job? = null
    private var profileStoriesJob: Job? = null
    private var typingDebounceJob: Job? = null
    private var incomingCallJob: Job? = null
    private var activeCallJob: Job? = null
    private var callHistoryJob: Job? = null
    private var callTimerJob: Job? = null

    // Grouped active stories for the Instagram/Snapchat style story tray
    val groupedStories: StateFlow<List<UserStoryGroup>> = combine(_activeStories, _allUsers, _currentUser) { stories, users, current ->
        val userMap = (users + listOfNotNull(current)).associateBy { it.id }
        val storiesByUser = stories.groupBy { it.userId }
        storiesByUser.map { (userId, uStories) ->
            val u = userMap[userId] ?: User(
                id = userId,
                displayName = uStories.firstOrNull()?.userDisplayName.orEmpty().ifBlank { "User" },
                username = uStories.firstOrNull()?.userUsername.orEmpty(),
                photoUrl = uStories.firstOrNull()?.userPhotoUrl.orEmpty()
            )
            val hasUnseen = uStories.any { story ->
                current != null && !story.viewers.containsKey(current.id) && story.userId != current.id
            }
            UserStoryGroup(user = u, stories = uStories.sortedBy { it.createdAt }, hasUnseenStories = hasUnseen)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered users by search query
    val filteredUsers: StateFlow<List<User>> = combine(_allUsers, _searchQuery) { users, query ->
        if (query.isBlank()) {
            users
        } else {
            val cleanQuery = query.trim().lowercase().removePrefix("@")
            users.filter {
                it.displayName.contains(cleanQuery, ignoreCase = true) ||
                        it.username.contains(cleanQuery, ignoreCase = true) ||
                        it.statusMessage.contains(cleanQuery, ignoreCase = true) ||
                        it.bio.contains(cleanQuery, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat ID & User Search State
    private val _searchUserResult = MutableStateFlow<User?>(null)
    val searchUserResult: StateFlow<User?> = _searchUserResult.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isSearchingUser = MutableStateFlow(false)
    val isSearchingUser: StateFlow<Boolean> = _isSearchingUser.asStateFlow()

    private val _searchUserNotFound = MutableStateFlow(false)
    val searchUserNotFound: StateFlow<Boolean> = _searchUserNotFound.asStateFlow()

    init {
        checkCurrentAuth()
    }

    private fun checkCurrentAuth() {
        val fbUser = repository.currentFirebaseUser
        if (fbUser != null) {
            viewModelScope.launch {
                val profile = repository.fetchUserProfile(fbUser.uid) ?: User(
                    id = fbUser.uid,
                    email = fbUser.email ?: "",
                    displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "User",
                    isOnline = true
                )
                _currentUser.value = profile
                _authUiState.value = AuthUiState.Authenticated(profile)
                repository.updateUserOnlineStatus(profile.id, true)
                startObservingUsers(profile.id)
            }
        }
    }

    fun signUp(email: String, pass: String, displayName: String, username: String, avatarId: Int) {
        if (email.isBlank() || !email.contains("@")) {
            _errorMessage.value = "Please enter a valid email address"
            return
        }
        val cleanUsername = repository.normalizeUsername(username)
        if (!repository.isValidUsernameFormat(cleanUsername)) {
            _errorMessage.value = "Chat ID must be 4–20 characters (letters, numbers, dot, underscore)"
            return
        }
        if (pass.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            return
        }
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = repository.signUp(email.trim(), pass, displayName.trim(), cleanUsername, avatarId)
            result.onSuccess { user ->
                _currentUser.value = user
                _authUiState.value = AuthUiState.Authenticated(user)
                _infoMessage.value = "Welcome to WP CHAT, @${user.username}!"
                startObservingUsers(user.id)
            }.onFailure { err ->
                _authUiState.value = AuthUiState.Error(err.localizedMessage ?: "Sign up failed")
                _errorMessage.value = err.localizedMessage ?: "Sign up failed. Please try again."
            }
        }
    }

    suspend fun checkUsernameAvailability(rawUsername: String): Boolean {
        val currentUserId = _currentUser.value?.id.orEmpty()
        return repository.isUsernameAvailable(rawUsername, currentUserId)
    }

    fun claimUsernameForCurrentUser(rawUsername: String, onComplete: (Boolean) -> Unit) {
        val current = _currentUser.value ?: return
        val clean = repository.normalizeUsername(rawUsername)
        if (!repository.isValidUsernameFormat(clean)) {
            _errorMessage.value = "Chat ID must be 4–20 characters (letters, numbers, dot, underscore)"
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val result = repository.claimUsername(clean, current.id)
            result.onSuccess { assignedUsername ->
                val updated = current.copy(username = assignedUsername)
                _currentUser.value = updated
                _infoMessage.value = "Chat ID set to @$assignedUsername"
                onComplete(true)
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Could not claim Chat ID"
                onComplete(false)
            }
        }
    }

    fun searchUserByChatId(rawUsername: String) {
        val clean = repository.normalizeUsername(rawUsername)
        if (clean.isBlank()) {
            _searchUserResult.value = null
            _searchUserNotFound.value = false
            return
        }
        val current = _currentUser.value
        if (current != null && repository.normalizeUsername(current.username) == clean) {
            _errorMessage.value = "You entered your own Chat ID"
            _searchUserResult.value = null
            _searchUserNotFound.value = false
            return
        }

        viewModelScope.launch {
            _isSearchingUser.value = true
            _searchUserNotFound.value = false
            _searchUserResult.value = null

            val user = repository.searchUserByUsername(clean)
            _isSearchingUser.value = false
            if (user != null) {
                if (user.id == current?.id) {
                    _errorMessage.value = "You entered your own Chat ID"
                } else {
                    _searchUserResult.value = user
                }
            } else {
                _searchUserNotFound.value = true
            }
        }
    }

    fun searchDirectory(query: String) {
        val current = _currentUser.value
        val clean = repository.normalizeUsername(query)
        if (clean.length < 2) {
            _searchResults.value = emptyList()
            _searchUserNotFound.value = false
            return
        }

        viewModelScope.launch {
            _isSearchingUser.value = true
            _searchUserNotFound.value = false
            val results = repository.searchUsers(query, current?.id.orEmpty())
            _isSearchingUser.value = false
            _searchResults.value = results
            _searchUserNotFound.value = results.isEmpty()
        }
    }

    fun clearSearchUserResult() {
        _searchUserResult.value = null
        _searchResults.value = emptyList()
        _searchUserNotFound.value = false
        _isSearchingUser.value = false
    }

    fun logIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Please enter email and password"
            return
        }
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = repository.logIn(email.trim(), pass)
            result.onSuccess { user ->
                _currentUser.value = user
                _authUiState.value = AuthUiState.Authenticated(user)
                _infoMessage.value = "Welcome back, ${user.displayName}!"
                startObservingUsers(user.id)
            }.onFailure { err ->
                _authUiState.value = AuthUiState.Error(err.localizedMessage ?: "Login failed")
                _errorMessage.value = err.localizedMessage ?: "Invalid credentials. Please check and retry."
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _errorMessage.value = "Google sign-in credential was empty"
            return
        }
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = repository.signInWithGoogle(idToken)
            result.onSuccess { user ->
                _currentUser.value = user
                _authUiState.value = AuthUiState.Authenticated(user)
                _infoMessage.value = "Welcome, ${user.displayName}!"
                startObservingUsers(user.id)
            }.onFailure { err ->
                _authUiState.value = AuthUiState.Error(err.localizedMessage ?: "Google sign-in failed")
                _errorMessage.value = err.localizedMessage ?: "Google sign-in failed. Please try again."
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank() || !email.contains("@")) {
            _errorMessage.value = "Enter your email to receive password reset link"
            return
        }
        viewModelScope.launch {
            val result = repository.resetPassword(email.trim())
            result.onSuccess {
                _infoMessage.value = "Password reset email sent to $email"
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Could not send reset email"
            }
        }
    }

    fun signOut() {
        val user = _currentUser.value
        viewModelScope.launch {
            if (user != null) {
                repository.signOut(user.id)
            }
            _currentUser.value = null
            _activeContact.value = null
            _activeMessages.value = emptyList()
            _selectedProfileUser.value = null
            _userStories.value = emptyList()
            _activeStories.value = emptyList()
            _onlineUsers.value = emptyList()
            _feedPosts.value = emptyList()
            _messageRequests.value = emptyList()
            _blockedUserIds.value = emptySet()
            _activeStoryViewer.value = null
            _incomingCall.value = null
            _activeCallSession.value = null
            _callHistory.value = emptyList()
            _authUiState.value = AuthUiState.Idle
            usersJob?.cancel()
            onlineUsersJob?.cancel()
            messagesJob?.cancel()
            typingJob?.cancel()
            activeStoriesJob?.cancel()
            feedPostsJob?.cancel()
            messageRequestsJob?.cancel()
            blockedUsersJob?.cancel()
            profileStoriesJob?.cancel()
            incomingCallJob?.cancel()
            activeCallJob?.cancel()
            callHistoryJob?.cancel()
            callTimerJob?.cancel()
        }
    }

    private fun startObservingUsers(currentUserId: String) {
        usersJob?.cancel()
        usersJob = viewModelScope.launch {
            repository.syncFcmToken(currentUserId)
            repository.observeRecentConversations(currentUserId).collect { userList ->
                _allUsers.value = userList
            }
        }

        // Real-time online users observer
        onlineUsersJob?.cancel()
        onlineUsersJob = viewModelScope.launch {
            repository.observeOnlineUsers(currentUserId).collect { onlineList ->
                _onlineUsers.value = onlineList
            }
        }

        // Real-time feed posts observer
        feedPostsJob?.cancel()
        feedPostsJob = viewModelScope.launch {
            repository.observeFeedPosts().collect { posts ->
                _feedPosts.value = posts
            }
        }

        // Real-time message requests observer
        messageRequestsJob?.cancel()
        messageRequestsJob = viewModelScope.launch {
            repository.observeMessageRequests(currentUserId).collect { reqs ->
                _messageRequests.value = reqs
            }
        }

        // Real-time blocked users observer
        blockedUsersJob?.cancel()
        blockedUsersJob = viewModelScope.launch {
            repository.observeBlockedUsers(currentUserId).collect { blocked ->
                _blockedUserIds.value = blocked
            }
        }

        // Real-time active 24-hour stories observer
        activeStoriesJob?.cancel()
        activeStoriesJob = viewModelScope.launch {
            repository.observeActiveStories().collect { stories ->
                _activeStories.value = stories
            }
        }

        // Real-time incoming call observer
        incomingCallJob?.cancel()
        incomingCallJob = viewModelScope.launch {
            repository.observeIncomingCall(currentUserId).collect { session ->
                // Only show incoming call if we aren't already on an active call
                if (_activeCallSession.value == null) {
                    _incomingCall.value = session
                }
            }
        }

        // Real-time call history observer
        callHistoryJob?.cancel()
        callHistoryJob = viewModelScope.launch {
            repository.observeCallHistory(currentUserId).collect { history ->
                _callHistory.value = history
            }
        }

        // Setup real-time incoming notification dispatch
        repository.observeIncomingNotifications(currentUserId) { senderId, senderName, chatId, messageId, text ->
            if (_activeContact.value?.id != senderId) {
                val ctx = com.example.MainActivity.appContext
                if (ctx != null) {
                    com.example.util.WpChatNotificationHelper.showMessageNotification(
                        context = ctx,
                        senderId = senderId,
                        senderName = senderName,
                        chatId = chatId,
                        messageId = messageId,
                        messageText = text
                    )
                }
            }
        }
    }

    fun openChatByUserId(userId: String) {
        if (userId.isBlank()) return
        val current = _currentUser.value
        if (current == null || current.id == userId) return

        val existing = _allUsers.value.find { it.id == userId }
        if (existing != null) {
            openChatWith(existing)
            return
        }

        viewModelScope.launch {
            val user = repository.fetchUserProfile(userId)
            if (user != null) {
                val sanitized = repository.sanitizePublicUser(user)
                openChatWith(sanitized)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openChatWith(otherUser: User) {
        val user = _currentUser.value ?: return
        _activeContact.value = otherUser
        _selectedProfileUser.value = null
        val chatId = repository.getChatId(user.id, otherUser.id)

        // Mark incoming messages as read & delivered
        viewModelScope.launch {
            repository.markMessagesAsDelivered(chatId, user.id)
            repository.markMessagesAsRead(chatId, user.id)
        }

        // Observe messages
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.observeMessages(chatId).collect { msgList ->
                _activeMessages.value = msgList
                // Check if any incoming unread messages need reading
                if (msgList.any { it.receiverId == user.id && (!it.isRead || it.status != "seen") }) {
                    repository.markMessagesAsRead(chatId, user.id)
                }
            }
        }

        // Observe typing indicator of the other user
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            repository.observeTypingStatus(chatId, otherUser.id).collect { isTyping ->
                _isOtherUserTyping.value = isTyping
            }
        }
    }

    fun closeChat() {
        val user = _currentUser.value
        val contact = _activeContact.value
        if (user != null && contact != null) {
            val chatId = repository.getChatId(user.id, contact.id)
            repository.setTypingStatus(chatId, user.id, false)
        }
        messagesJob?.cancel()
        typingJob?.cancel()
        _activeContact.value = null
        _activeMessages.value = emptyList()
        _isOtherUserTyping.value = false
    }

    fun onMessageInputChanged(text: String) {
        val user = _currentUser.value ?: return
        val contact = _activeContact.value ?: return
        val chatId = repository.getChatId(user.id, contact.id)

        val isTyping = text.isNotBlank()
        repository.setTypingStatus(chatId, user.id, isTyping)

        typingDebounceJob?.cancel()
        if (isTyping) {
            typingDebounceJob = viewModelScope.launch {
                delay(2500)
                repository.setTypingStatus(chatId, user.id, false)
            }
        }
    }

    fun sendMessage(text: String) {
        val messageText = text.trim()
        if (messageText.isBlank()) return

        val user = _currentUser.value ?: return
        val contact = _activeContact.value ?: return
        val chatId = repository.getChatId(user.id, contact.id)

        viewModelScope.launch {
            val result = repository.sendMessage(chatId, user, contact.id, messageText)
            result.onFailure {
                _errorMessage.value = "Failed to send message: ${it.localizedMessage}"
            }
        }
    }

    fun sendMediaMessage(
        fileUri: Uri,
        forcedType: MessageType? = null,
        caption: String = "",
        viewLimit: Int = 0,
        allowDownload: Boolean = true,
        context: Context
    ) {
        val user = _currentUser.value ?: return
        val contact = _activeContact.value ?: return
        val chatId = repository.getChatId(user.id, contact.id)

        val meta = FileUtils.queryFileMeta(context, fileUri, forcedType)
        _uploadingFileName.value = meta.name
        _isUploadingMedia.value = true
        _mediaUploadProgress.value = 0f

        viewModelScope.launch {
            val result = repository.uploadAndSendMediaMessage(
                chatId = chatId,
                sender = user,
                receiverId = contact.id,
                fileUri = fileUri,
                forcedType = forcedType,
                caption = caption,
                viewLimit = viewLimit,
                allowDownload = allowDownload,
                context = context,
                onProgress = { progress ->
                    _mediaUploadProgress.value = progress
                }
            )

            _isUploadingMedia.value = false
            _mediaUploadProgress.value = 0f
            _uploadingFileName.value = ""

            result.onSuccess {
                _infoMessage.value = "${meta.messageType.name.lowercase().replaceFirstChar { it.uppercase() }} sent"
            }.onFailure { err ->
                _errorMessage.value = "Failed to send ${meta.name}: ${err.localizedMessage}"
            }
        }
    }

    fun addReaction(messageId: String, reaction: String) {
        val user = _currentUser.value ?: return
        val contact = _activeContact.value ?: return
        val chatId = repository.getChatId(user.id, contact.id)

        viewModelScope.launch {
            repository.setMessageReaction(chatId, messageId, reaction)
        }
    }

    // -------------------------------------------------------------
    // SOCIAL PROFILE & 24-HOUR STORY OPERATIONS
    // -------------------------------------------------------------

    fun openUserProfile(user: User) {
        _selectedProfileUser.value = user
        profileStoriesJob?.cancel()
        profileStoriesJob = viewModelScope.launch {
            repository.observeUserStories(user.id).collect { stories ->
                _userStories.value = stories
            }
        }
    }

    fun openCurrentProfile() {
        val current = _currentUser.value ?: return
        openUserProfile(current)
    }

    fun closeUserProfile() {
        profileStoriesJob?.cancel()
        _selectedProfileUser.value = null
        _userStories.value = emptyList()
    }

    fun openStoryViewer(user: User, stories: List<Story>) {
        if (stories.isNotEmpty()) {
            _activeStoryViewer.value = Pair(user, stories)
        }
    }

    fun closeStoryViewer() {
        _activeStoryViewer.value = null
    }

    fun markStoryViewed(storyId: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markStoryViewed(storyId, current.id)
        }
    }

    fun uploadProfilePhoto(uri: Uri, context: Context) {
        val current = _currentUser.value ?: return
        _isUploadingProfilePhoto.value = true

        viewModelScope.launch {
            val oldPhotoUrl = current.photoUrl
            val result = repository.uploadProfilePhoto(
                userId = current.id,
                imageUri = uri,
                context = context,
                oldPhotoUrl = oldPhotoUrl
            )
            _isUploadingProfilePhoto.value = false
            result.onSuccess { newPhotoUrl ->
                val updated = current.copy(photoUrl = newPhotoUrl)
                _currentUser.value = updated
                if (_selectedProfileUser.value?.id == current.id) {
                    _selectedProfileUser.value = updated
                }
                _infoMessage.value = "Profile photo updated!"
            }.onFailure { err ->
                _errorMessage.value = "Failed to update profile photo: ${err.localizedMessage ?: err.message ?: "Unknown error"}"
            }
        }
    }

    fun createStory(mediaUri: Uri, isVideo: Boolean, caption: String, context: Context, onComplete: () -> Unit = {}) {
        val current = _currentUser.value ?: return
        _isCreatingStory.value = true
        _storyUploadProgress.value = 0f

        viewModelScope.launch {
            val result = repository.createStory(
                userId = current.id,
                user = current,
                mediaUri = mediaUri,
                isVideo = isVideo,
                caption = caption,
                context = context,
                onProgress = { _storyUploadProgress.value = it }
            )

            _isCreatingStory.value = false
            _storyUploadProgress.value = 0f

            result.onSuccess {
                _infoMessage.value = "Story shared for 24 hours!"
                onComplete()
            }.onFailure { err ->
                _errorMessage.value = "Failed to share story: ${err.localizedMessage ?: "Upload error"}"
            }
        }
    }

    fun deleteStory(story: Story) {
        val current = _currentUser.value ?: return
        if (story.userId != current.id) {
            _errorMessage.value = "You can only delete your own stories"
            return
        }

        _isDeletingStory.value = true
        viewModelScope.launch {
            val result = repository.deleteStory(current.id, story.storyId, story.mediaUrl)
            _isDeletingStory.value = false
            result.onSuccess {
                _infoMessage.value = "Story deleted"
                // If viewer is open on this story, close or update viewer
                val viewerState = _activeStoryViewer.value
                if (viewerState != null) {
                    val remainingStories = viewerState.second.filter { it.storyId != story.storyId }
                    if (remainingStories.isEmpty()) {
                        _activeStoryViewer.value = null
                    } else {
                        _activeStoryViewer.value = Pair(viewerState.first, remainingStories)
                    }
                }
            }.onFailure { err ->
                _errorMessage.value = "Failed to delete story: ${err.localizedMessage ?: "Delete error"}"
            }
        }
    }

    fun updateProfile(displayName: String, statusMessage: String, avatarId: Int, gender: String = "Male") {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            displayName = displayName.trim().ifBlank { current.displayName },
            statusMessage = statusMessage.trim().ifBlank { current.statusMessage },
            avatarId = avatarId,
            gender = gender.ifBlank { current.gender }
        )
        _currentUser.value = updated
        if (_selectedProfileUser.value?.id == current.id) {
            _selectedProfileUser.value = updated
        }
        viewModelScope.launch {
            repository.saveUserProfile(updated)
            _infoMessage.value = "Profile updated successfully"
        }
    }

    fun updateProfileDetails(displayName: String, bio: String, statusMessage: String, avatarId: Int, gender: String = "Male") {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            displayName = displayName.trim().ifBlank { current.displayName },
            bio = bio.trim(),
            statusMessage = statusMessage.trim().ifBlank { current.statusMessage },
            avatarId = avatarId,
            gender = gender.ifBlank { current.gender }
        )
        _currentUser.value = updated
        if (_selectedProfileUser.value?.id == current.id) {
            _selectedProfileUser.value = updated
        }
        viewModelScope.launch {
            repository.saveUserProfile(updated)
            _infoMessage.value = "Profile updated successfully"
        }
    }

    // -------------------------------------------------------------
    // FEED POSTS OPERATIONS
    // -------------------------------------------------------------

    fun createPost(
        mediaUri: Uri,
        isVideo: Boolean,
        caption: String,
        context: Context,
        onComplete: () -> Unit = {}
    ) {
        val current = _currentUser.value ?: return
        _isUploadingPost.value = true
        _postUploadProgress.value = 0f

        viewModelScope.launch {
            val result = repository.createPost(
                userId = current.id,
                user = current,
                mediaUri = mediaUri,
                isVideo = isVideo,
                caption = caption,
                context = context,
                onProgress = { _postUploadProgress.value = it }
            )

            _isUploadingPost.value = false
            _postUploadProgress.value = 0f

            result.onSuccess {
                _infoMessage.value = "Post shared to feed!"
                onComplete()
            }.onFailure { err ->
                _errorMessage.value = "Failed to upload post: ${err.localizedMessage ?: "Upload error"}"
            }
        }
    }

    fun toggleLikePost(postId: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleLikePost(postId, current.id)
        }
    }

    fun deletePost(post: Post) {
        val current = _currentUser.value ?: return
        if (post.userId != current.id) {
            _errorMessage.value = "You can only delete your own posts"
            return
        }

        viewModelScope.launch {
            val result = repository.deletePost(current.id, post.id, post.mediaUrl)
            result.onSuccess {
                _infoMessage.value = "Post deleted"
            }.onFailure { err ->
                _errorMessage.value = "Failed to delete post: ${err.localizedMessage ?: "Delete error"}"
            }
        }
    }

    // -------------------------------------------------------------
    // MESSAGE REQUESTS OPERATIONS
    // -------------------------------------------------------------

    fun acceptMessageRequest(request: MessageRequest) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.acceptMessageRequest(current.id, request.fromUserId, request.requestId)
            _infoMessage.value = "Message request accepted from ${request.fromUser.displayName}"
        }
    }

    fun declineMessageRequest(request: MessageRequest) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.declineMessageRequest(current.id, request.requestId)
            _infoMessage.value = "Message request declined"
        }
    }

    // -------------------------------------------------------------
    // MODERATION & USER BLOCKING
    // -------------------------------------------------------------

    fun blockUser(targetUserId: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.blockUser(current.id, targetUserId)
            _infoMessage.value = "User blocked"
            if (_activeContact.value?.id == targetUserId) {
                closeChat()
            }
        }
    }

    fun unblockUser(targetUserId: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.unblockUser(current.id, targetUserId)
            _infoMessage.value = "User unblocked"
        }
    }

    fun submitReport(
        reportedUserId: String,
        reportedUserName: String,
        reason: String,
        details: String = "",
        contentSnippet: String = ""
    ) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            val report = UserReport(
                reportId = "report_${System.currentTimeMillis()}",
                reporterId = current.id,
                reporterName = current.displayName.ifBlank { current.username },
                reportedUserId = reportedUserId,
                reportedUserName = reportedUserName,
                reason = reason,
                details = details,
                contentSnippet = contentSnippet,
                timestamp = System.currentTimeMillis()
            )
            val result = repository.submitReport(report)
            result.onSuccess {
                _infoMessage.value = "Report submitted. Thank you for keeping our community safe."
            }.onFailure { err ->
                _errorMessage.value = "Could not submit report: ${err.localizedMessage}"
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit = {}) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.deleteAccount(current.id, current.username)
            _currentUser.value = null
            _activeContact.value = null
            _authUiState.value = AuthUiState.Idle
            result.onSuccess {
                _infoMessage.value = "Your account has been deleted."
                onSuccess()
            }.onFailure {
                _infoMessage.value = "Account signed out and deleted."
                onSuccess()
            }
        }
    }

    // -------------------------------------------------------------
    // TEMPORARY / DISAPPEARING MEDIA
    // -------------------------------------------------------------

    fun markTemporaryMediaViewed(messageId: String) {
        val user = _currentUser.value ?: return
        val contact = _activeContact.value ?: return
        val chatId = repository.getChatId(user.id, contact.id)
        viewModelScope.launch {
            val msg = _activeMessages.value.find { it.id == messageId }
            // SENDER previewing sent media must NOT mark it as viewed or expired
            if (msg != null && msg.senderId == user.id) {
                return@launch
            }
            repository.markTemporaryMediaViewed(chatId, messageId, user.id)
        }
    }

    fun markMediaExpired(messageId: String) {
        val user = _currentUser.value ?: return
        val contact = _activeContact.value ?: return
        val chatId = repository.getChatId(user.id, contact.id)
        viewModelScope.launch {
            val msg = _activeMessages.value.find { it.id == messageId }
            // SENDER previewing sent media must NOT mark it as expired
            if (msg != null && msg.senderId == user.id) {
                return@launch
            }
            repository.markMediaExpired(chatId, messageId, user.id)
        }
    }

    fun unsendMessage(messageId: String) {
        val user = _currentUser.value ?: return
        val contact = _activeContact.value ?: return
        val chatId = repository.getChatId(user.id, contact.id)
        viewModelScope.launch {
            val result = repository.unsendMessage(chatId, messageId, user.id)
            if (result.isSuccess) {
                _infoMessage.value = "Message unsent"
            } else {
                _errorMessage.value = "Failed to unsend message"
            }
        }
    }

    // -------------------------------------------------------------
    // WEBRTC / 1-ON-1 AUDIO & VIDEO CALLING ACTIONS
    // -------------------------------------------------------------

    fun startAudioCall(receiver: User, context: Context? = null) {
        initiateCall(receiver, CallType.AUDIO, context)
    }

    fun startVideoCall(receiver: User, context: Context? = null) {
        initiateCall(receiver, CallType.VIDEO, context)
    }

    private fun initiateCall(receiver: User, callType: CallType, context: Context? = null) {
        val current = _currentUser.value ?: return
        if (current.id == receiver.id) {
            _errorMessage.value = "Cannot call yourself"
            return
        }

        viewModelScope.launch {
            _isSpeakerOn.value = callType == CallType.VIDEO // default speaker on for video
            _isMicMuted.value = false
            _isVideoCameraOff.value = false
            _isFrontCamera.value = true
            _callDurationSeconds.value = 0L

            if (context != null) {
                repository.setSpeakerphone(context, _isSpeakerOn.value)
                repository.setMicrophoneMute(context, false)
            }

            val result = repository.startCall(current, receiver, callType)
            result.onSuccess { session ->
                _activeCallSession.value = session
                listenToActiveCallSession(session.callId)
            }.onFailure { err ->
                _errorMessage.value = "Failed to start call: ${err.localizedMessage}"
            }
        }
    }

    fun acceptIncomingCall(context: Context? = null) {
        val incoming = _incomingCall.value ?: return
        viewModelScope.launch {
            _incomingCall.value = null
            _isSpeakerOn.value = incoming.isVideoCall()
            _isMicMuted.value = false
            _isVideoCameraOff.value = false
            _isFrontCamera.value = true
            _callDurationSeconds.value = 0L

            if (context != null) {
                repository.setSpeakerphone(context, _isSpeakerOn.value)
                repository.setMicrophoneMute(context, false)
            }

            val result = repository.acceptCall(incoming.callId)
            result.onSuccess {
                val updatedSession = incoming.copy(
                    status = CallStatus.ACCEPTED.name,
                    startedAt = System.currentTimeMillis()
                )
                _activeCallSession.value = updatedSession
                listenToActiveCallSession(incoming.callId)
                startCallTimer()
            }.onFailure { err ->
                _errorMessage.value = "Failed to accept call: ${err.localizedMessage}"
            }
        }
    }

    fun rejectIncomingCall() {
        val incoming = _incomingCall.value ?: return
        viewModelScope.launch {
            _incomingCall.value = null
            repository.rejectCall(incoming)
        }
    }

    fun endActiveCall(context: Context? = null) {
        val active = _activeCallSession.value
        val incoming = _incomingCall.value

        viewModelScope.launch {
            callTimerJob?.cancel()
            activeCallJob?.cancel()

            if (active != null) {
                _activeCallSession.value = null
                repository.endCall(active)
            } else if (incoming != null) {
                _incomingCall.value = null
                repository.rejectCall(incoming)
            }

            if (context != null) {
                repository.setSpeakerphone(context, false)
                repository.setMicrophoneMute(context, false)
            }

            _callDurationSeconds.value = 0L
        }
    }

    private fun listenToActiveCallSession(callId: String) {
        activeCallJob?.cancel()
        activeCallJob = viewModelScope.launch {
            repository.observeCallSession(callId).collect { session ->
                if (session == null || session.status == CallStatus.ENDED.name ||
                    session.status == CallStatus.REJECTED.name || session.status == CallStatus.MISSED.name
                ) {
                    callTimerJob?.cancel()
                    _activeCallSession.value = null
                    _callDurationSeconds.value = 0L
                    activeCallJob?.cancel()
                } else {
                    _activeCallSession.value = session
                    if (session.status == CallStatus.ACCEPTED.name && callTimerJob == null) {
                        startCallTimer()
                    }
                }
            }
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            _callDurationSeconds.value = 0L
            while (true) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    fun toggleSpeaker(context: Context) {
        val next = !_isSpeakerOn.value
        _isSpeakerOn.value = next
        repository.setSpeakerphone(context, next)
    }

    fun toggleMute(context: Context) {
        val next = !_isMicMuted.value
        _isMicMuted.value = next
        repository.setMicrophoneMute(context, next)
    }

    fun toggleVideoCamera() {
        _isVideoCameraOff.value = !_isVideoCameraOff.value
    }

    fun switchCameraFacing() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun showInfo(message: String) {
        _infoMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearInfo() {
        _infoMessage.value = null
    }
}
