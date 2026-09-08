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
        Log.d(tag, "From: ${remoteMessage.from}")

        // Read payload from data or notification
        val data = remoteMessage.data
        val senderId = data["senderId"] ?: data["fromUserId"] ?: ""
        val senderName = data["senderName"] ?: remoteMessage.notification?.title ?: "WP CHAT"
        val chatId = data["chatId"] ?: ""
        val messageId = data["messageId"] ?: ""
        val text = data["text"] ?: data["message"] ?: remoteMessage.notification?.body ?: "New message received"

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

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
