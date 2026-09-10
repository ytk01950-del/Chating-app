package com.example.service

import android.util.Log
import com.example.util.WpChatNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class WpChatMessagingService : FirebaseMessagingService() {

    private val tag = "WpChatMessagingService"
    private val databaseUrl = "https://chating-a9250-default-rtdb.firebaseio.com"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(tag, "Refreshed FCM token received")
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(tag, "From: ${remoteMessage.from}, data: ${remoteMessage.data}")

        val data = remoteMessage.data
        val notifType = data["type"] ?: ""

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        if (notifType == "call" || data.containsKey("callId")) {
            val callId = data["callId"] ?: ""
            val callerId = data["callerId"] ?: data["senderId"] ?: ""
            val callerName = data["callerName"] ?: data["senderName"] ?: "WP CHAT User"
            val callType = data["callType"] ?: "AUDIO"

            if (callerId.isNotBlank() && callerId == currentUserId) {
                return
            }

            // Update call status to RINGING in Firebase RTDB so caller sees 'Ringing...'
            if (callId.isNotBlank()) {
                try {
                    val db = FirebaseDatabase.getInstance(databaseUrl)
                    db.getReference("calls").child(callId).child("status").setValue("RINGING")
                    Log.d(tag, "Call $callId status updated to RINGING on receiver FCM receipt")
                } catch (e: Exception) {
                    Log.w(tag, "Failed to update call status to RINGING: ${e.message}")
                }
            }

            // Show incoming call notification banner with system ringtone
            WpChatNotificationHelper.showIncomingCallNotification(
                context = applicationContext,
                callId = callId,
                callerId = callerId,
                callerName = callerName,
                callType = callType
            )
            return
        }

        // Read payload for chat message
        val senderId = data["senderId"] ?: data["fromUserId"] ?: ""
        val senderName = data["senderName"] ?: remoteMessage.notification?.title ?: "WP CHAT"
        val chatId = data["chatId"] ?: ""
        val messageId = data["messageId"] ?: ""
        val text = data["text"] ?: data["message"] ?: remoteMessage.notification?.body ?: "New message received"

        // Do not notify sender about their own message
        if (senderId.isNotBlank() && senderId == currentUserId) {
            Log.d(tag, "Ignoring message notification for self: $senderId")
            return
        }

        // Display notification
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
                Log.d(tag, "FCM token updated in Firebase for user: $currentUid")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to update FCM token in database: ${e.message}", e)
        }
    }
}
