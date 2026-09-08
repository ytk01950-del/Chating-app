package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ChatMessage
import com.example.model.MessageType
import com.example.model.Story
import com.example.model.User
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
    // CHAT MEDIA UPLOADS STATE
    // -------------------------------------------------------------
    private val _isUploadingMedia = MutableStateFlow(false)
    val isUploadingMedia: StateFlow<Boolean> = _isUploadingMedia.asStateFlow()

    private val _mediaUploadProgress = MutableStateFlow(0f)
    val mediaUploadProgress: StateFlow<Float> = _mediaUploadProgress.asStateFlow()

    private val _uploadingFileName = MutableStateFlow("")
    val uploadingFileName: StateFlow<String> = _uploadingFileName.asStateFlow()

    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    private var usersJob: Job? = null
    private var activeStoriesJob: Job? = null
    private var profileStoriesJob: Job? = null
    private var typingDebounceJob: Job? = null

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
            _activeStoryViewer.value = null
            _authUiState.value = AuthUiState.Idle
            usersJob?.cancel()
            messagesJob?.cancel()
            typingJob?.cancel()
            activeStoriesJob?.cancel()
            profileStoriesJob?.cancel()
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

        // Real-time active 24-hour stories observer
        activeStoriesJob?.cancel()
        activeStoriesJob = viewModelScope.launch {
            repository.observeActiveStories().collect { stories ->
                _activeStories.value = stories
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

        // Observe messages
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.observeMessages(chatId).collect { msgList ->
                _activeMessages.value = msgList
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

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearInfo() {
        _infoMessage.value = null
    }
}
