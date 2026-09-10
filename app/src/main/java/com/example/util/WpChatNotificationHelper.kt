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

    const val CHANNEL_ID_CALLS = "wpchat_calls_channel"
    const val CHANNEL_NAME_CALLS = "WP CHAT Incoming Calls"
    const val CHANNEL_DESC_CALLS = "High-priority notifications and alerts for incoming audio and video calls"

    const val EXTRA_USER_ID = "extra_chat_user_id"
    const val EXTRA_CHAT_ID = "extra_chat_id"
    const val EXTRA_USER_NAME = "extra_chat_user_name"
    const val EXTRA_CALL_ID = "extra_call_id"
    const val EXTRA_ACTION_CALL = "extra_action_call"

    private const val PREFS_NAME = "wpchat_notifications_prefs"
    private const val KEY_SEEN_MSG_PREFIX = "seen_msg_"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            // Message Channel
            val msgChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                CHANNEL_NAME_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_MESSAGES
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(msgChannel)

            // Incoming Call Channel (High Priority with Ringtone)
            val callSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val callChannel = NotificationChannel(
                CHANNEL_ID_CALLS,
                CHANNEL_NAME_CALLS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_CALLS
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                setSound(callSoundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(callChannel)

            Log.d(TAG, "Notification channels initialized: $CHANNEL_ID_MESSAGES & $CHANNEL_ID_CALLS")
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

    fun showIncomingCallNotification(
        context: Context,
        callId: String,
        callerId: String,
        callerName: String,
        callType: String
    ) {
        createNotificationChannel(context)

        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_USER_ID, callerId)
            putExtra(EXTRA_USER_NAME, callerName)
            putExtra(EXTRA_ACTION_CALL, "incoming")
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val isVideo = callType.equals("VIDEO", ignoreCase = true)
        val title = "Incoming ${if (isVideo) "Video" else "Audio"} Call"
        val body = "$callerName is calling you..."

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_CALLS)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(callSoundUri)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(callId.hashCode(), notificationBuilder.build())
        Log.d(TAG, "Incoming call notification posted for callId: $callId, caller: $callerName")
    }

    fun cancelCallNotification(context: Context, callId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(callId.hashCode())
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
