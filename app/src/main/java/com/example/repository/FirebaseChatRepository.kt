package com.example.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.model.ChatMessage
import com.example.model.MessageType
import com.example.model.Post
import com.example.model.User
import com.example.service.SupabaseStorageService
import com.example.util.FileUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseChatRepository {

    private val tag = "FirebaseChatRepo"
    private val databaseUrl = "https://chating-a9250-default-rtdb.firebaseio.com"

    private val app: FirebaseApp
        get() = FirebaseApp.getInstance()

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val database: FirebaseDatabase
        get() = try {
            FirebaseDatabase.getInstance(databaseUrl)
        } catch (e: Exception) {
            try {
                FirebaseDatabase.getInstance(app, databaseUrl)
            } catch (ex: Exception) {
                Log.w(tag, "Falling back to default database instance: ${ex.message}")
                FirebaseDatabase.getInstance()
            }
        }

    val currentFirebaseUser: FirebaseUser?
        get() = try {
            auth.currentUser
        } catch (e: Exception) {
            Log.e(tag, "Error accessing current user: ${e.message}")
            null
        }

    fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    // -------------------------------------------------------------
    // USERNAME & CHAT ID MANAGEMENT
    // -------------------------------------------------------------

    fun normalizeUsername(raw: String): String {
        return raw.trim().lowercase().removePrefix("@")
    }

    fun isValidUsernameFormat(username: String): Boolean {
        val clean = normalizeUsername(username)
        val regex = "^[a-zA-Z0-9_.]{4,20}$".toRegex()
        return regex.matches(clean)
    }

    suspend fun isUsernameAvailable(rawUsername: String, currentUserId: String = ""): Boolean {
        val clean = normalizeUsername(rawUsername)
        if (!isValidUsernameFormat(clean)) return false
        return try {
            val key = clean.replace(".", "_")
            val snapshot = database.getReference("usernames").child(key).get().await()
            val ownerUid = snapshot.getValue(String::class.java)
            ownerUid == null || (currentUserId.isNotBlank() && ownerUid == currentUserId)
        } catch (e: Exception) {
            Log.w(tag, "Error checking username availability (assuming available): ${e.message}")
            true
        }
    }

    suspend fun claimUsername(rawUsername: String, userId: String): Result<String> {
        val clean = normalizeUsername(rawUsername)
        if (!isValidUsernameFormat(clean)) {
            return Result.failure(IllegalArgumentException("Username must be 4-20 characters (letters, numbers, dot, underscore)"))
        }
        return try {
            val key = clean.replace(".", "_")
            val usernameRef = database.getReference("usernames").child(key)
            val currentOwner = usernameRef.get().await().getValue(String::class.java)
            if (currentOwner != null && currentOwner != userId) {
                return Result.failure(IllegalStateException("Username '@$clean' is already taken"))
            }

            // Register in usernames index
            usernameRef.setValue(userId).await()

            // Update user profile in /users/{uid}/username
            database.getReference("users").child(userId).child("username").setValue(clean).await()
            Result.success(clean)
        } catch (e: Exception) {
            Log.e(tag, "Error claiming username: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun sanitizePublicUser(user: User): User {
        return user.copy(email = "")
    }

    suspend fun searchUserByUsername(rawUsername: String): User? {
        val clean = normalizeUsername(rawUsername)
        if (clean.length < 3) return null
        return try {
            val key = clean.replace(".", "_")
            // 1. Fast direct lookup via usernames index
            val directUid = database.getReference("usernames").child(key).get().await().getValue(String::class.java)
            if (directUid != null) {
                val user = fetchUserProfile(directUid)
                if (user != null) return sanitizePublicUser(user)
            }

            // 2. Query index on /users by username
            val snapshot = database.getReference("users")
                .orderByChild("username")
                .equalTo(clean)
                .get()
                .await()

            for (child in snapshot.children) {
                val user = child.getValue(User::class.java)
                if (user != null) return sanitizePublicUser(user)
            }
            null
        } catch (e: Exception) {
            Log.e(tag, "Error searching user by username: ${e.message}", e)
            null
        }
    }

    suspend fun searchUsers(query: String, currentUserId: String): List<User> {
        val clean = query.trim().lowercase().removePrefix("@")
        if (clean.length < 2) return emptyList()

        val results = mutableListOf<User>()
        val seenIds = mutableSetOf<String>()

        try {
            // 1. Check exact Chat ID
            val key = clean.replace(".", "_")
            val directUid = database.getReference("usernames").child(key).get().await().getValue(String::class.java)
            if (directUid != null && directUid != currentUserId) {
                val user = fetchUserProfile(directUid)
                if (user != null && seenIds.add(user.id)) {
                    results.add(sanitizePublicUser(user))
                }
            }

            // 2. Query users matching username
            val usernameSnap = database.getReference("users")
                .orderByChild("username")
                .startAt(clean)
                .endAt(clean + "\uf8ff")
                .limitToFirst(15)
                .get()
                .await()

            for (child in usernameSnap.children) {
                val user = child.getValue(User::class.java)
                if (user != null && user.id != currentUserId && user.id.isNotBlank() && seenIds.add(user.id)) {
                    results.add(sanitizePublicUser(user))
                }
            }

            // 3. Query users matching displayName
            val nameSnap = database.getReference("users")
                .orderByChild("displayName")
                .startAt(query.trim())
                .endAt(query.trim() + "\uf8ff")
                .limitToFirst(15)
                .get()
                .await()

            for (child in nameSnap.children) {
                val user = child.getValue(User::class.java)
                if (user != null && user.id != currentUserId && user.id.isNotBlank() && seenIds.add(user.id)) {
                    results.add(sanitizePublicUser(user))
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Search users query error: ${e.message}")
        }
        return results
    }

    suspend fun syncFcmToken(userId: String) {
        if (userId.isBlank()) return
        try {
            val messaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
            val token = messaging.token.await()
            if (!token.isNullOrBlank()) {
                database.getReference("fcm_tokens").child(userId).setValue(token).await()
                database.getReference("users").child(userId).child("fcmToken").setValue(token).await()
                Log.d(tag, "FCM token synced successfully for user: $userId")
            }
        } catch (t: Throwable) {
            Log.d(tag, "FCM registration skipped or not supported on this device/emulator: ${t.message}")
        }
    }

    // -------------------------------------------------------------
    // AUTHENTICATION
    // -------------------------------------------------------------

    suspend fun signUp(
        email: String,
        pass: String,
        displayName: String,
        username: String,
        avatarId: Int
    ): Result<User> {
        val cleanEmail = email.trim().lowercase()
        val cleanUsername = normalizeUsername(username)

        if (!isValidUsernameFormat(cleanUsername)) {
            return Result.failure(IllegalArgumentException("Username must be 4-20 characters and use only letters, numbers, dot, or underscore."))
        }

        return try {
            // Authenticate user first so subsequent database operations have valid authenticated credentials
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
            val firebaseUser = authResult.user ?: auth.currentUser 
                ?: throw IllegalStateException("Firebase user creation failed: No authenticated user returned.")

            val uid = firebaseUser.uid
            val usernameKey = cleanUsername.replace(".", "_")
            val usernameRef = database.getReference("usernames").child(usernameKey)

            // Check if username is already taken by a different user
            val currentOwner = try {
                usernameRef.get().await().getValue(String::class.java)
            } catch (e: Exception) {
                Log.w(tag, "Username check note: ${e.message}")
                null
            }

            if (currentOwner != null && currentOwner != uid) {
                try {
                    firebaseUser.delete().await()
                } catch (delEx: Exception) {
                    Log.w(tag, "Could not delete user after duplicate username: ${delEx.message}")
                }
                return Result.failure(IllegalStateException("Chat ID '@$cleanUsername' is already taken. Please choose another."))
            }

            val resolvedName = displayName.trim().ifBlank {
                cleanUsername.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString()
                }
            }

            try {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(resolvedName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            } catch (e: Exception) {
                Log.w(tag, "Optional profile update in Firebase Auth notice: ${e.message}")
            }

            val newUser = User(
                id = uid,
                username = cleanUsername,
                email = cleanEmail,
                displayName = resolvedName,
                avatarId = avatarId,
                statusMessage = "Hey there! I am using WP CHAT.",
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )

            // Save user profile and claim username index
            saveUserProfile(newUser)
            try {
                usernameRef.setValue(uid).await()
            } catch (e: Exception) {
                Log.w(tag, "Could not write username index: ${e.message}")
            }
            setupPresence(uid)
            Result.success(newUser)
        } catch (e: Exception) {
            Log.e(tag, "SignUp failed: ${e.message}", e)
            val friendlyError = when (e) {
                is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                    Exception("An account already exists with this email. Please switch to Log In tab.", e)
                is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                    Exception("Password is too weak. Please use a stronger password with at least 6 characters.", e)
                is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                    Exception("The email address is invalid. Please enter a valid email.", e)
                is com.google.firebase.FirebaseNetworkException ->
                    Exception("Network connection error. Please check your internet connection.", e)
                else -> e
            }
            Result.failure(friendlyError)
        }
    }

    suspend fun logIn(email: String, pass: String): Result<User> {
        val cleanEmail = email.trim().lowercase()
        return try {
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, pass).await()
            val firebaseUser = authResult.user ?: auth.currentUser
                ?: throw IllegalStateException("Firebase login failed: No authenticated user returned.")

            val uid = firebaseUser.uid
            var user = fetchUserProfile(uid)
            if (user == null) {
                val fallbackName = firebaseUser.displayName ?: cleanEmail.substringBefore("@").replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() 
                }
                user = User(
                    id = uid,
                    email = cleanEmail,
                    displayName = fallbackName,
                    avatarId = 0,
                    statusMessage = "Hey there! I am using WP CHAT.",
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                saveUserProfile(user)
            } else {
                updateUserOnlineStatus(uid, true)
            }
            setupPresence(uid)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "Login failed in Firebase: ${e.message}", e)
            val friendlyError = when (e) {
                is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException,
                is com.google.firebase.auth.FirebaseAuthInvalidUserException -> 
                    Exception("Incorrect email or password.", e)
                is com.google.firebase.FirebaseNetworkException -> 
                    Exception("Network connection error. Please check your internet connection.", e)
                is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException -> 
                    Exception("Please log in again to continue.", e)
                else -> {
                    val msg = e.message.orEmpty()
                    if (msg.contains("invalid credential", ignoreCase = true) ||
                        msg.contains("malformed or has expired", ignoreCase = true) ||
                        msg.contains("user-not-found", ignoreCase = true) ||
                        msg.contains("wrong-password", ignoreCase = true)) {
                        Exception("Incorrect email or password.", e)
                    } else {
                        e
                    }
                }
            }
            Result.failure(friendlyError)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: auth.currentUser
                ?: throw IllegalStateException("Google sign-in failed: No authenticated user returned.")

            val uid = firebaseUser.uid
            val email = firebaseUser.email.orEmpty().trim().lowercase()
            var user = fetchUserProfile(uid)
            if (user == null) {
                val fallbackName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@").replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString()
                    }.ifBlank { "User" }
                user = User(
                    id = uid,
                    email = email,
                    displayName = fallbackName,
                    avatarId = 0,
                    statusMessage = "Hey there! I am using WP CHAT.",
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                saveUserProfile(user)
            } else {
                updateUserOnlineStatus(uid, true)
            }
            setupPresence(uid)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "Google sign-in failed in Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Password reset failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(currentUserId: String) {
        try {
            if (currentUserId.isNotBlank()) {
                updateUserOnlineStatus(currentUserId, false)
            }
            auth.signOut()
        } catch (e: Exception) {
            Log.e(tag, "Sign out error: ${e.message}", e)
        }
    }

    // -------------------------------------------------------------
    // USER PROFILES & PRESENCE
    // -------------------------------------------------------------

    suspend fun saveUserProfile(user: User) {
        try {
            database.getReference("users").child(user.id).setValue(user).await()
        } catch (e: Exception) {
            Log.e(tag, "Error saving user profile: ${e.message}", e)
            throw e
        }
    }

    suspend fun fetchUserProfile(userId: String): User? {
        return try {
            val snapshot = database.getReference("users").child(userId).get().await()
            snapshot.getValue(User::class.java)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching user $userId: ${e.message}", e)
            null
        }
    }

    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean) {
        try {
            val updates = mapOf<String, Any>(
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
            database.getReference("users").child(userId).updateChildren(updates).await()
        } catch (e: Exception) {
            Log.e(tag, "Error updating status for $userId: ${e.message}", e)
        }
    }

    fun setupPresence(userId: String) {
        try {
            val connectedRef = database.getReference(".info/connected")
            val userStatusRef = database.getReference("users").child(userId)

            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        userStatusRef.child("isOnline").setValue(true)
                        userStatusRef.child("lastSeen").setValue(System.currentTimeMillis())
                        userStatusRef.child("isOnline").onDisconnect().setValue(false)
                        userStatusRef.child("lastSeen").onDisconnect().setValue(System.currentTimeMillis())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(tag, "Presence listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(tag, "Presence setup error: ${e.message}", e)
        }
    }

    // -------------------------------------------------------------
    // USER CONVERSATIONS (Only users who have active/existing chats)
    // -------------------------------------------------------------

    fun observeRecentConversations(currentUserId: String): Flow<List<User>> = callbackFlow {
        if (currentUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val userChatsRef = database.getReference("user_chats").child(currentUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val otherUserIds = mutableListOf<String>()
                val chatTimestamps = mutableMapOf<String, Long>()

                for (child in snapshot.children) {
                    val otherUid = child.child("otherUserId").getValue(String::class.java)
                    val lastTimestamp = child.child("lastTimestamp").getValue(Long::class.java) ?: 0L
                    if (!otherUid.isNullOrBlank() && otherUid != currentUserId) {
                        otherUserIds.add(otherUid)
                        chatTimestamps[otherUid] = lastTimestamp
                    }
                }

                if (otherUserIds.isEmpty()) {
                    trySend(emptyList())
                    return
                }

                val distinctIds = otherUserIds.distinct()
                val usersList = mutableListOf<User>()
                var remaining = distinctIds.size

                for (otherUid in distinctIds) {
                    database.getReference("users").child(otherUid).get().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = task.result?.getValue(User::class.java)
                            if (user != null && user.id.isNotBlank()) {
                                synchronized(usersList) {
                                    usersList.add(sanitizePublicUser(user))
                                }
                            }
                        }
                        remaining--
                        if (remaining <= 0) {
                            val sorted = usersList.sortedByDescending { chatTimestamps[it.id] ?: it.lastSeen }
                            trySend(sorted)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Conversations observer cancelled: ${error.message}")
                trySend(emptyList())
            }
        }
        userChatsRef.addValueEventListener(listener)
        awaitClose { userChatsRef.removeEventListener(listener) }
    }

    fun observeAllUsers(currentUserId: String): Flow<List<User>> {
        return observeRecentConversations(currentUserId)
    }

    // -------------------------------------------------------------
    // REAL-TIME INCOMING NOTIFICATIONS OBSERVER
    // -------------------------------------------------------------

    fun observeIncomingNotifications(
        currentUserId: String,
        onNotification: (senderId: String, senderName: String, chatId: String, messageId: String, text: String) -> Unit
    ) {
        if (currentUserId.isBlank()) return
        try {
            val notifRef = database.getReference("notifications").child(currentUserId)
            notifRef.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val msgId = snapshot.child("messageId").getValue(String::class.java) ?: snapshot.key.orEmpty()
                    val senderId = snapshot.child("senderId").getValue(String::class.java).orEmpty()
                    val senderName = snapshot.child("senderName").getValue(String::class.java).orEmpty()
                    val chatId = snapshot.child("chatId").getValue(String::class.java).orEmpty()
                    val text = snapshot.child("text").getValue(String::class.java).orEmpty()

                    if (senderId.isNotBlank() && senderId != currentUserId) {
                        onNotification(senderId, senderName, chatId, msgId, text)
                    }
                    snapshot.ref.removeValue()
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            Log.w(tag, "Incoming notifications listener note: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // REAL-TIME ONE-TO-ONE MESSAGING
    // -------------------------------------------------------------

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val messagesRef = database.getReference("chats").child(chatId).child("messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val msg = child.getValue(ChatMessage::class.java)
                    if (msg != null) {
                        messageList.add(msg)
                    }
                }
                messageList.sortBy { it.timestamp }
                trySend(messageList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Messages observer error: ${error.message}")
                trySend(emptyList())
            }
        }
        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    suspend fun sendMessage(
        chatId: String,
        sender: User,
        receiverId: String,
        text: String
    ): Result<ChatMessage> {
        return try {
            val messagesRef = database.getReference("chats").child(chatId).child("messages")
            val msgId = messagesRef.push().key ?: "msg_${System.currentTimeMillis()}"

            val message = ChatMessage(
                id = msgId,
                senderId = sender.id,
                senderName = sender.displayName.ifBlank { sender.username },
                receiverId = receiverId,
                text = text.trim(),
                timestamp = System.currentTimeMillis(),
                isRead = false
            )

            // 1. Write the message into Realtime Database
            messagesRef.child(msgId).setValue(message).await()

            // 2. Write chat index snippet for sender and receiver
            val senderSnippet = mapOf(
                "otherUserId" to receiverId,
                "lastMessage" to text.trim(),
                "lastTimestamp" to message.timestamp
            )
            database.getReference("user_chats").child(sender.id).child(chatId).setValue(senderSnippet)

            val receiverSnippet = mapOf(
                "otherUserId" to sender.id,
                "lastMessage" to text.trim(),
                "lastTimestamp" to message.timestamp
            )
            database.getReference("user_chats").child(receiverId).child(chatId).setValue(receiverSnippet)

            // 3. Post notification payload for recipient
            val notifPayload = mapOf(
                "messageId" to msgId,
                "chatId" to chatId,
                "senderId" to sender.id,
                "senderName" to sender.displayName.ifBlank { sender.username },
                "receiverId" to receiverId,
                "text" to text.trim(),
                "timestamp" to message.timestamp
            )
            database.getReference("notifications").child(receiverId).child(msgId).setValue(notifPayload)

            // Reset typing status
            setTypingStatus(chatId, sender.id, false)

            Result.success(message)
        } catch (e: Exception) {
            Log.e(tag, "Firebase send message failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun setMessageReaction(chatId: String, messageId: String, reaction: String) {
        try {
            database.getReference("chats")
                .child(chatId)
                .child("messages")
                .child(messageId)
                .child("reaction")
                .setValue(reaction)
                .await()
        } catch (e: Exception) {
            Log.e(tag, "Error setting reaction: ${e.message}", e)
        }
    }

    // -------------------------------------------------------------
    // REAL-TIME TYPING INDICATOR
    // -------------------------------------------------------------

    fun setTypingStatus(chatId: String, userId: String, isTyping: Boolean) {
        try {
            val ref = database.getReference("chats").child(chatId).child("typing").child(userId)
            ref.setValue(isTyping)
            if (isTyping) {
                ref.onDisconnect().setValue(false)
            }
        } catch (e: Exception) {
            Log.e(tag, "Typing indicator error: ${e.message}", e)
        }
    }

    fun observeTypingStatus(chatId: String, otherUserId: String): Flow<Boolean> = callbackFlow {
        val ref = database.getReference("chats").child(chatId).child("typing").child(otherUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isTyping = snapshot.getValue(Boolean::class.java) ?: false
                trySend(isTyping)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(false)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // -------------------------------------------------------------
    // SOCIAL PROFILE & PHOTO UPLOADS (SUPABASE STORAGE $0 FREE TIER)
    // -------------------------------------------------------------

    suspend fun uploadProfilePhoto(
        userId: String,
        imageUri: Uri,
        context: Context,
        oldPhotoUrl: String = ""
    ): Result<String> {
        return try {
            Log.d(tag, "Starting Supabase profile photo upload for userId: $userId, uri: $imageUri")
            val uploadResult = SupabaseStorageService.uploadProfilePhoto(
                userId = userId,
                imageUri = imageUri,
                context = context
            )

            uploadResult.fold(
                onSuccess = { downloadUrl ->
                    Log.d(tag, "Supabase profile photo uploaded successfully. Download URL: $downloadUrl")
                    // Update user profile in Firebase database only after successful storage upload and URL retrieval
                    database.getReference("users").child(userId).child("photoUrl").setValue(downloadUrl).await()
                    Log.d(tag, "Successfully updated database photoUrl for user $userId")

                    // Clean up old profile photo in background if one existed
                    if (oldPhotoUrl.isNotBlank() && oldPhotoUrl != downloadUrl) {
                        try {
                            SupabaseStorageService.deleteFileByUrl(oldPhotoUrl, context)
                        } catch (delErr: Exception) {
                            Log.w(tag, "Old photo cleanup notice (non-fatal): ${delErr.message}")
                        }
                    }

                    Result.success(downloadUrl)
                },
                onFailure = { err ->
                    Log.e(tag, "Failed to upload profile photo to Supabase: ${err.message}", err)
                    Result.failure(err)
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Exception during uploadProfilePhoto: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createPost(
        userId: String,
        user: User,
        imageUri: Uri,
        caption: String,
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): Result<Post> {
        return try {
            Log.d(tag, "Starting createPost for user: $userId, uri: $imageUri")
            val postId = "post_${System.currentTimeMillis()}_${(1000..9999).random()}"

            val uploadResult = SupabaseStorageService.uploadPostPhoto(
                userId = userId,
                postId = postId,
                imageUri = imageUri,
                context = context,
                onProgress = onProgress
            )

            uploadResult.fold(
                onSuccess = { downloadUrl ->
                    Log.d(tag, "Supabase post image uploaded successfully. Download URL: $downloadUrl")

                    val post = Post(
                        postId = postId,
                        userId = userId,
                        userDisplayName = user.displayName.ifBlank { user.username },
                        userUsername = user.username,
                        userPhotoUrl = user.photoUrl,
                        imageUrl = downloadUrl,
                        caption = caption.trim(),
                        timestamp = System.currentTimeMillis()
                    )

                    // Save post in user's posts in Firebase database
                    database.getReference("posts").child(userId).child(postId).setValue(post).await()

                    // Update user posts count
                    try {
                        val postsSnap = database.getReference("posts").child(userId).get().await()
                        val count = postsSnap.childrenCount.toInt()
                        database.getReference("users").child(userId).child("postsCount").setValue(count).await()
                    } catch (ex: Exception) {
                        Log.w(tag, "Posts count update note: ${ex.message}")
                    }

                    onProgress(1f)
                    Result.success(post)
                },
                onFailure = { err ->
                    Log.e(tag, "Failed to upload post image to Supabase: ${err.message}", err)
                    Result.failure(err)
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Exception during createPost: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun observeUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val postsRef = database.getReference("posts").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val postList = mutableListOf<Post>()
                for (child in snapshot.children) {
                    val post = child.getValue(Post::class.java)
                    if (post != null) {
                        postList.add(post)
                    }
                }
                postList.sortByDescending { it.timestamp }
                trySend(postList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "User posts observer error: ${error.message}")
                trySend(emptyList())
            }
        }
        postsRef.addValueEventListener(listener)
        awaitClose { postsRef.removeEventListener(listener) }
    }

    suspend fun deletePost(userId: String, postId: String, imageUrl: String): Result<Unit> {
        return try {
            val currentAuthUid = currentFirebaseUser?.uid.orEmpty()
            if (currentAuthUid != userId) {
                return Result.failure(IllegalAccessException("You can only delete your own posts"))
            }

            // Remove from Firebase database
            database.getReference("posts").child(userId).child(postId).removeValue().await()

            // Update user posts count
            try {
                val postsSnap = database.getReference("posts").child(userId).get().await()
                val count = postsSnap.childrenCount.toInt()
                database.getReference("users").child(userId).child("postsCount").setValue(count).await()
            } catch (ex: Exception) {
                Log.w(tag, "Posts count update note: ${ex.message}")
            }

            // Remove file from Supabase Storage
            if (imageUrl.isNotBlank()) {
                try {
                    SupabaseStorageService.deleteFileByUrl(imageUrl)
                } catch (delErr: Exception) {
                    Log.w(tag, "Storage post file deletion note: ${delErr.message}")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete post: ${e.message}", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // MEDIA & FILE SHARING IN CHAT (SUPABASE STORAGE $0 FREE TIER)
    // -------------------------------------------------------------

    suspend fun uploadAndSendMediaMessage(
        chatId: String,
        sender: User,
        receiverId: String,
        fileUri: Uri,
        forcedType: MessageType? = null,
        caption: String = "",
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): Result<ChatMessage> {
        return try {
            val meta = FileUtils.queryFileMeta(context, fileUri, forcedType)

            // Validate 50 MB Free tier upload limit before loading or uploading
            if (meta.size > SupabaseStorageService.MAX_FILE_SIZE_BYTES) {
                val sizeMb = String.format(java.util.Locale.US, "%.1f", meta.size.toDouble() / (1024 * 1024))
                return Result.failure(
                    IllegalArgumentException("File size ($sizeMb MB) exceeds the 50 MB Free Tier limit.")
                )
            }

            val fileBytes = if (meta.messageType == MessageType.IMAGE) {
                FileUtils.compressImageForUpload(context, fileUri, maxDimension = 1920, quality = 85)
                    ?: FileUtils.readBytesFromUri(context, fileUri)
            } else {
                FileUtils.readBytesFromUri(context, fileUri)
            } ?: return Result.failure(IllegalStateException("Could not read attachment file"))

            if (fileBytes.isEmpty()) {
                return Result.failure(IllegalStateException("Attachment file is empty"))
            }

            if (fileBytes.size > SupabaseStorageService.MAX_FILE_SIZE_BYTES) {
                val sizeMb = String.format(java.util.Locale.US, "%.1f", fileBytes.size.toDouble() / (1024 * 1024))
                return Result.failure(
                    IllegalArgumentException("File size ($sizeMb MB) exceeds the 50 MB Free Tier limit.")
                )
            }

            val msgId = "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
            val mime = meta.mimeType.ifBlank { "application/octet-stream" }

            val uploadResult = SupabaseStorageService.uploadChatMedia(
                chatId = chatId,
                messageId = msgId,
                fileName = meta.name,
                fileBytes = fileBytes,
                mimeType = mime,
                context = context,
                onProgress = onProgress
            )

            uploadResult.fold(
                onSuccess = { downloadUrl ->
                    Log.d(tag, "Chat media uploaded to Supabase. Download URL: $downloadUrl")

                    val message = ChatMessage(
                        id = msgId,
                        senderId = sender.id,
                        senderName = sender.displayName.ifBlank { sender.username },
                        receiverId = receiverId,
                        text = caption.trim(),
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        messageType = meta.messageType.name,
                        fileName = meta.name,
                        fileUrl = downloadUrl,
                        mimeType = meta.mimeType,
                        fileSize = fileBytes.size.toLong()
                    )

                    // 1. Write message to Firebase database
                    database.getReference("chats").child(chatId).child("messages").child(msgId).setValue(message).await()

                    // 2. Write chat index snippet
                    val summaryText = message.getNotificationSummary()
                    val senderSnippet = mapOf(
                        "otherUserId" to receiverId,
                        "lastMessage" to summaryText,
                        "lastTimestamp" to message.timestamp
                    )
                    database.getReference("user_chats").child(sender.id).child(chatId).setValue(senderSnippet)

                    val receiverSnippet = mapOf(
                        "otherUserId" to sender.id,
                        "lastMessage" to summaryText,
                        "lastTimestamp" to message.timestamp
                    )
                    database.getReference("user_chats").child(receiverId).child(chatId).setValue(receiverSnippet)

                    // 3. Post notification payload
                    val notifPayload = mapOf(
                        "messageId" to msgId,
                        "chatId" to chatId,
                        "senderId" to sender.id,
                        "senderName" to sender.displayName.ifBlank { sender.username },
                        "receiverId" to receiverId,
                        "text" to summaryText,
                        "timestamp" to message.timestamp
                    )
                    database.getReference("notifications").child(receiverId).child(msgId).setValue(notifPayload)

                    // Reset typing
                    setTypingStatus(chatId, sender.id, false)
                    onProgress(1f)

                    Result.success(message)
                },
                onFailure = { err ->
                    Log.e(tag, "Failed to upload chat media to Supabase: ${err.message}", err)
                    Result.failure(err)
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Exception in uploadAndSendMediaMessage: ${e.message}", e)
            Result.failure(e)
        }
    }
}

