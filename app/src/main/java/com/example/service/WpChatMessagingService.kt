package com.example.service

import android.content.Context
import android.content.SharedPreferences
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

        fun getCachedToken(context: Context): String? {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_CACHED_TOKEN, null)
        }

        fun saveCachedToken(context: Context, token: String) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CACHED_TOKEN, token).apply()
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
        Log.i(tag, "FCM Message Received from: ${remoteMessage.from}, priority: ${remoteMessage.priority}, data: ${remoteMessage.data}")

        val data = remoteMessage.data
        val notifType = (data["type"] ?: data["notificationType"] ?: "").lowercase()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        // 1. Handle Call Cancelled / Ended / Rejected
        if (notifType == "call_ended" || notifType == "call_canceled" || notifType == "call_rejected") {
            val callId = data["callId"] ?: data["call_id"] ?: ""
            Log.i(tag, "FCM Remote call ended/cancelled: $callId")
            IncomingCallRingingService.stopRinging(applicationContext)
            WpChatNotificationHelper.cancelCallNotification(applicationContext, callId)
            return
        }

        // 2. Handle Missed Call notification
        if (notifType == "call_missed" || notifType == "missed_call") {
            val callId = data["callId"] ?: data["call_id"] ?: ""
            val callerId = data["callerId"] ?: data["caller_id"] ?: data["senderId"] ?: ""
            val callerName = data["callerName"] ?: data["caller_name"] ?: data["senderName"] ?: "Nexa User"
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

        // 3. Handle Incoming Audio / Video Call
        val isIncomingCall = data["incoming_call"]?.toBoolean() == true ||
                data["incoming_call"] == "true" ||
                notifType == "call" ||
                notifType == "incoming_call" ||
                notifType == "audio_call" ||
                notifType == "video_call" ||
                (data.containsKey("callId") && !data.containsKey("messageId"))

        if (isIncomingCall) {
            val callId = data["callId"] ?: data["call_id"] ?: ""
            val callerId = data["callerId"] ?: data["caller_id"] ?: data["senderId"] ?: ""
            val callerName = data["callerName"] ?: data["caller_name"] ?: data["senderName"] ?: "Nexa User"
            val callerUsername = data["callerUsername"] ?: data["caller_username"] ?: ""
            val callerPhotoUrl = data["callerPhotoUrl"] ?: data["caller_photo_url"] ?: data["photoUrl"] ?: ""
            val callerAvatarId = data["callerAvatarId"]?.toIntOrNull() ?: data["caller_avatar_id"]?.toIntOrNull() ?: data["avatarId"]?.toIntOrNull() ?: 0
            val callType = data["callType"] ?: data["call_type"] ?: if (notifType == "video_call") "VIDEO" else "AUDIO"

            if (callerId.isNotBlank() && callerId == currentUserId) {
                Log.d(tag, "Ignoring incoming call from self ($callerId)")
                return
            }

            if (callId.isBlank()) {
                Log.w(tag, "Incoming call payload missing callId, cannot proceed.")
                return
            }

            Log.i(tag, "Processing incoming call: $callId from $callerName ($callType)")

            // Update call status to RINGING in Firebase RTDB so caller sees 'Ringing...' immediately
            try {
                val db = FirebaseDatabase.getInstance(databaseUrl)
                db.getReference("calls").child(callId).child("status").setValue("RINGING")
            } catch (e: Exception) {
                Log.w(tag, "Failed to update call status to RINGING: ${e.message}")
            }

            // Start Foreground Ringing Service with full-screen intent & ringtone playback
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

        // 4. Handle Chat Message Notification
        val senderId = data["senderId"] ?: data["fromUserId"] ?: data["sender_id"] ?: ""
        val senderName = data["senderName"] ?: data["sender_name"] ?: remoteMessage.notification?.title ?: "Nexa Messenger"
        val chatId = data["chatId"] ?: data["chat_id"] ?: ""
        val messageId = data["messageId"] ?: data["message_id"] ?: ""
        val text = data["text"] ?: data["message"] ?: data["body"] ?: remoteMessage.notification?.body ?: "New message received"

        // Do not notify sender about their own message
        if (senderId.isNotBlank() && senderId == currentUserId) {
            Log.d(tag, "Ignoring message notification for self ($senderId)")
            return
        }

        Log.i(tag, "Displaying chat notification for sender: $senderName, messageId: $messageId")
        WpChatNotificationHelper.showMessageNotification(
            context = applicationContext,
            senderId = senderId,
            senderName = senderName,
            chatId = chatId,
            messageId = messageId,
            messageText = text
        )
    }

    private fun sendRegistrationToServer(token: String) {
        try {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            if (!currentUid.isNullOrBlank()) {
                val db = FirebaseDatabase.getInstance(databaseUrl)
                db.getReference("fcm_tokens").child(currentUid).setValue(token)
                db.getReference("users").child(currentUid).child("fcmToken").setValue(token)
                Log.i(tag, "FCM token successfully synchronized with database for user: $currentUid")
            } else {
                Log.d(tag, "FCM token saved locally; will sync when user logs in.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to synchronize FCM token with database: ${e.message}", e)
        }
    }
}
