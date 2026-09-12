package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.os.PowerManager
import android.util.Log
import com.example.util.WpChatNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class WpChatMessagingService : FirebaseMessagingService() {

    private val tag = "NexaMessagingService"
    private val databaseUrl = "https://chating-a9250-default-rtdb.firebaseio.com"

    companion object {
        private const val PREFS_NAME = "nexa_fcm_prefs"
        private const val KEY_CACHED_TOKEN = "cached_fcm_token"
        private const val KEY_CACHED_USER_ID = "cached_user_id"

        fun getCachedToken(context: Context): String? {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_CACHED_TOKEN, null)
        }

        fun saveCachedToken(context: Context, token: String) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CACHED_TOKEN, token).apply()
        }

        fun saveCurrentUserId(context: Context, userId: String) {
            if (userId.isBlank()) return
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CACHED_USER_ID, userId).apply()
        }

        fun getCurrentUserId(context: Context): String {
            val authUid = try { FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }
            if (!authUid.isNullOrBlank()) return authUid
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_CACHED_USER_ID, "").orEmpty()
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(tag, "New FCM Registration Token generated: $token")
        saveCachedToken(applicationContext, token)
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Acquire short partial wake lock (5s) to guarantee background processing when device sleeps / app is killed
        var wakeLock: PowerManager.WakeLock? = null
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Nexa:FcmOnMessageReceived")
            wakeLock?.acquire(5_000L)
        } catch (e: Exception) {
            Log.w(tag, "FCM wake lock note: ${e.message}")
        }

        try {
            val data = remoteMessage.data
            val isHighPriority = remoteMessage.priority == RemoteMessage.PRIORITY_HIGH ||
                    remoteMessage.originalPriority == RemoteMessage.PRIORITY_HIGH ||
                    data["priority"]?.equals("high", ignoreCase = true) == true

            Log.i(tag, "FCM Message Received [HighPriority=$isHighPriority, Priority=${remoteMessage.priority}, from=${remoteMessage.from}]: data=$data")

            val notifType = (data["type"] ?: data["notificationType"] ?: data["messageType"] ?: data["action"] ?: "").lowercase()
            val currentUserId = getCurrentUserId(applicationContext)

            // 2. Handle Call Cancelled / Ended / Rejected
            if (notifType == "call_ended" || notifType == "call_canceled" || notifType == "call_cancelled" || notifType == "call_rejected") {
                val callId = data["callId"] ?: data["call_id"].orEmpty()
                Log.i(tag, "FCM Remote call ended/cancelled: $callId")
                IncomingCallRingingService.stopRinging(applicationContext)
                WpChatNotificationHelper.cancelCallNotification(applicationContext, callId)
                return
            }

            // 3. Handle Missed Call notification
            if (notifType == "call_missed" || notifType == "missed_call") {
                val callId = data["callId"] ?: data["call_id"].orEmpty()
                val callerId = data["callerId"] ?: data["caller_id"] ?: data["senderId"].orEmpty()
                val callerName = (data["callerName"] ?: data["caller_name"] ?: data["senderName"]).orEmpty().ifBlank { "Nexa User" }
                val callType = data["callType"] ?: data["call_type"] ?: "AUDIO"
                val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()

                Log.i(tag, "FCM Missed call received: $callId from $callerName")
                IncomingCallRingingService.stopRinging(applicationContext)
                WpChatNotificationHelper.cancelCallNotification(applicationContext, callId)
                WpChatNotificationHelper.showMissedCallNotification(
                    context = applicationContext,
                    callId = callId,
                    callerId = callerId,
                    callerName = callerName,
                    callType = callType,
                    timestamp = timestamp
                )
                return
            }

            // 4. Handle Incoming Audio / Video Call (type == "call")
            val isIncomingCall = notifType == "call" ||
                    notifType == "incoming_call" ||
                    notifType == "audio_call" ||
                    notifType == "video_call" ||
                    data["incoming_call"]?.toBoolean() == true ||
                    data["incoming_call"] == "true" ||
                    (data.containsKey("callId") && !data.containsKey("messageId") && !notifType.startsWith("call_"))

            if (isIncomingCall) {
                val callId = data["callId"] ?: data["call_id"].orEmpty()
                val callerId = data["callerId"] ?: data["caller_id"] ?: data["senderId"].orEmpty()
                val callerName = (data["callerName"] ?: data["caller_name"] ?: data["senderName"]).orEmpty().ifBlank { "Incoming Caller" }
                val callerUsername = data["callerUsername"] ?: data["caller_username"].orEmpty()
                val callerPhotoUrl = data["callerPhotoUrl"] ?: data["caller_photo_url"] ?: data["photoUrl"].orEmpty()
                val callerAvatarId = data["callerAvatarId"]?.toIntOrNull()
                    ?: data["caller_avatar_id"]?.toIntOrNull()
                    ?: data["avatarId"]?.toIntOrNull()
                    ?: 0
                val callType = data["callType"] ?: data["call_type"]
                    ?: if (notifType == "video_call" || notifType.contains("video")) "VIDEO" else "AUDIO"

                if (callerId.isNotBlank() && currentUserId.isNotBlank() && callerId == currentUserId) {
                    Log.d(tag, "Ignoring incoming call from self ($callerId)")
                    return
                }

                if (callId.isBlank()) {
                    Log.w(tag, "Incoming call payload missing callId, cannot proceed.")
                    return
                }

                Log.i(tag, "Launching high-priority Foreground Service for incoming call: $callId from $callerName ($callType)")

                // Update call status to RINGING in Firebase RTDB so caller sees 'Ringing...' immediately
                try {
                    val db = FirebaseDatabase.getInstance(databaseUrl)
                    db.getReference("calls").child(callId).child("status").setValue("RINGING")
                } catch (e: Exception) {
                    Log.w(tag, "Failed to update call status to RINGING: ${e.message}")
                }

                // Launch high-priority Notification with FullScreenIntent immediately using Foreground Service
                IncomingCallRingingService.startRinging(
                    context = applicationContext,
                    callId = callId,
                    callerId = callerId,
                    callerName = callerName,
                    callerUsername = callerUsername,
                    callerPhotoUrl = callerPhotoUrl,
                    callerAvatarId = callerAvatarId,
                    callType = callType
                )
                return
            }

            // 5. Handle Chat Message Notification (type == "message")
            val isChatMessage = notifType == "message" ||
                    notifType == "chat" ||
                    notifType == "text" ||
                    notifType == "new_message" ||
                    data.containsKey("messageId") ||
                    data.containsKey("chatId") ||
                    remoteMessage.notification != null

            if (isChatMessage) {
                val senderId = data["senderId"] ?: data["fromUserId"] ?: data["sender_id"] ?: data["userId"].orEmpty()
                val notifTitle = remoteMessage.notification?.title.orEmpty()
                val notifBody = remoteMessage.notification?.body.orEmpty()
                val senderName = (data["senderName"] ?: data["sender_name"] ?: data["fromUserName"] ?: notifTitle).ifBlank { "Nexa User" }
                val chatId = data["chatId"] ?: data["chat_id"].orEmpty()
                val messageId = data["messageId"] ?: data["message_id"] ?: remoteMessage.messageId ?: "msg_${System.currentTimeMillis()}"
                val text = (data["text"] ?: data["message"] ?: data["body"] ?: notifBody).ifBlank { "New message received" }

                // Do not notify sender about their own message
                if (senderId.isNotBlank() && currentUserId.isNotBlank() && senderId == currentUserId) {
                    Log.d(tag, "Ignoring message notification for self ($senderId)")
                    return
                }

                Log.i(tag, "Displaying heads-up chat notification for sender: $senderName, messageId: $messageId")
                WpChatNotificationHelper.showMessageNotification(
                    context = applicationContext,
                    senderId = senderId,
                    senderName = senderName,
                    chatId = chatId,
                    messageId = messageId,
                    messageText = text
                )
            }
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            } catch (e: Exception) {}
        }
    }

    private fun sendRegistrationToServer(token: String) {
        try {
            val currentUid = getCurrentUserId(applicationContext)
            if (currentUid.isNotBlank()) {
                val db = FirebaseDatabase.getInstance(databaseUrl)
                db.getReference("fcm_tokens").child(currentUid).setValue(token)
                db.getReference("users").child(currentUid).child("fcmToken").setValue(token)
                Log.i(tag, "FCM token synchronized with database for user: $currentUid")
            } else {
                Log.d(tag, "FCM token saved locally; will sync when user logs in.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to synchronize FCM token with database: ${e.message}", e)
        }
    }
}

