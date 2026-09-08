package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object WpChatNotificationHelper {

    private const val TAG = "WpChatNotification"
    const val CHANNEL_ID_MESSAGES = "wpchat_messages_channel"
    const val CHANNEL_NAME_MESSAGES = "WP CHAT Messages"
    const val CHANNEL_DESC_MESSAGES = "Notifications for incoming one-to-one chat messages"

    const val EXTRA_USER_ID = "extra_chat_user_id"
    const val EXTRA_CHAT_ID = "extra_chat_id"
    const val EXTRA_USER_NAME = "extra_chat_user_name"

    private const val PREFS_NAME = "wpchat_notifications_prefs"
    private const val KEY_SEEN_MSG_PREFIX = "seen_msg_"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                CHANNEL_NAME_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_MESSAGES
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID_MESSAGES")
        }
    }

    fun isMessageAlreadyNotified(context: Context, messageId: String): Boolean {
        if (messageId.isBlank()) return false
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_SEEN_MSG_PREFIX + messageId)
    }

    fun markMessageAsNotified(context: Context, messageId: String) {
        if (messageId.isBlank()) return
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_SEEN_MSG_PREFIX + messageId, System.currentTimeMillis()).apply()
    }

    fun showMessageNotification(
        context: Context,
        senderId: String,
        senderName: String,
        chatId: String,
        messageId: String,
        messageText: String
    ) {
        // Prevent duplicate notifications
        if (messageId.isNotBlank() && isMessageAlreadyNotified(context, messageId)) {
            Log.d(TAG, "Notification already shown for message: $messageId, skipping.")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_USER_ID, senderId)
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_USER_NAME, senderName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            senderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val title = if (senderName.isNotBlank()) senderName else "New Message"
        val body = if (messageText.isNotBlank()) messageText else "Sent you a message"

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = if (messageId.isNotBlank()) messageId.hashCode() else senderId.hashCode()
        notificationManager?.notify(notificationId, notificationBuilder.build())

        if (messageId.isNotBlank()) {
            markMessageAsNotified(context, messageId)
        }
        Log.d(TAG, "Notification posted for sender: $senderName, messageId: $messageId")
    }
}
