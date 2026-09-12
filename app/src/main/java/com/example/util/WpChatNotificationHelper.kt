package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.service.CallActionReceiver
import com.example.ui.screens.IncomingCallActivity

object WpChatNotificationHelper {

    private const val TAG = "WpChatNotification"

    // Version 4 Notification Channels (High Priority & Heads-Up Alerting)
    const val CHANNEL_ID_MESSAGES = "nexa_messages_v4"
    const val CHANNEL_ID_AUDIO_CALLS = "nexa_incoming_audio_calls_v4"
    const val CHANNEL_ID_VIDEO_CALLS = "nexa_incoming_video_calls_v4"
    const val CHANNEL_ID_MISSED_CALLS = "nexa_missed_calls_v4"

    // Legacy Channels for cleanup
    private val LEGACY_CHANNELS = listOf(
        "wpchat_messages_channel",
        "wpchat_calls_channel",
        "nexa_messages_channel_v2",
        "nexa_calls_channel_v2",
        "nexa_messages_v3",
        "nexa_incoming_audio_calls_v3",
        "nexa_incoming_video_calls_v3",
        "nexa_missed_calls_v3"
    )

    const val EXTRA_USER_ID = "extra_chat_user_id"
    const val EXTRA_CHAT_ID = "extra_chat_id"
    const val EXTRA_USER_NAME = "extra_chat_user_name"
    const val EXTRA_CALL_ID = "extra_call_id"
    const val EXTRA_CALLER_ID = "extra_caller_id"
    const val EXTRA_CALLER_NAME = "extra_caller_name"
    const val EXTRA_CALLER_USERNAME = "extra_caller_username"
    const val EXTRA_CALLER_PHOTO = "extra_caller_photo"
    const val EXTRA_CALLER_AVATAR = "extra_caller_avatar"
    const val EXTRA_CALL_TYPE = "extra_call_type"
    const val EXTRA_ACTION_CALL = "extra_action_call"

    private const val PREFS_NAME = "nexa_notifications_prefs"
    private const val KEY_SEEN_MSG_PREFIX = "seen_msg_"

    val CALL_VIBRATION_PATTERN = longArrayOf(0, 1000, 1000, 1000, 1000, 1000)
    val MESSAGE_VIBRATION_PATTERN = longArrayOf(0, 250, 250, 250)

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Clean up legacy channels to purge silent/misconfigured settings
            for (legacyId in LEGACY_CHANNELS) {
                try {
                    notificationManager.deleteNotificationChannel(legacyId)
                } catch (e: Exception) {
                    Log.w(TAG, "Legacy channel cleanup note ($legacyId): ${e.message}")
                }
            }

            val defaultNotifSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val defaultCallRingtoneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val callAudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val messageAudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // 2. Messages Channel (NotificationManager.IMPORTANCE_HIGH)
            val msgChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                context.getString(R.string.notification_channel_messages),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_messages_desc)
                enableLights(true)
                enableVibration(true)
                vibrationPattern = MESSAGE_VIBRATION_PATTERN
                setSound(defaultNotifSoundUri, messageAudioAttributes)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(msgChannel)

            // 3. Incoming Audio Calls Channel (IMPORTANCE_HIGH with sound, vibration, and VISIBILITY_PUBLIC)
            val audioCallChannel = NotificationChannel(
                CHANNEL_ID_AUDIO_CALLS,
                context.getString(R.string.notification_channel_audio_calls),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_audio_calls_desc)
                enableLights(true)
                enableVibration(true)
                vibrationPattern = CALL_VIBRATION_PATTERN
                setSound(defaultCallRingtoneUri, callAudioAttributes)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
            }
            notificationManager.createNotificationChannel(audioCallChannel)

            // 4. Incoming Video Calls Channel (IMPORTANCE_HIGH with sound, vibration, and VISIBILITY_PUBLIC)
            val videoCallChannel = NotificationChannel(
                CHANNEL_ID_VIDEO_CALLS,
                context.getString(R.string.notification_channel_video_calls),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_video_calls_desc)
                enableLights(true)
                enableVibration(true)
                vibrationPattern = CALL_VIBRATION_PATTERN
                setSound(defaultCallRingtoneUri, callAudioAttributes)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
            }
            notificationManager.createNotificationChannel(videoCallChannel)

            // 5. Missed Calls Channel (IMPORTANCE_HIGH)
            val missedCallChannel = NotificationChannel(
                CHANNEL_ID_MISSED_CALLS,
                context.getString(R.string.notification_channel_missed_calls),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_missed_calls_desc)
                enableLights(true)
                enableVibration(true)
                vibrationPattern = MESSAGE_VIBRATION_PATTERN
                setSound(defaultNotifSoundUri, messageAudioAttributes)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(missedCallChannel)

            Log.i(TAG, "All v4 high-priority notification channels initialized successfully.")
        }
    }

    fun isMessageAlreadyNotified(context: Context, messageId: String): Boolean {
        if (messageId.isBlank()) return false
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong(KEY_SEEN_MSG_PREFIX + messageId, 0L)
        return timestamp > 0 && (System.currentTimeMillis() - timestamp < 4_000L)
    }

    fun markMessageAsNotified(context: Context, messageId: String) {
        if (messageId.isBlank()) return
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_SEEN_MSG_PREFIX + messageId, System.currentTimeMillis()).apply()
    }

    /**
     * Builds a full-screen, high-priority incoming call notification for foreground service or notification manager.
     */
    fun buildIncomingCallNotification(
        context: Context,
        callId: String,
        callerId: String,
        callerName: String,
        callerUsername: String = "",
        callerPhotoUrl: String = "",
        callerAvatarId: Int = 0,
        callType: String = "AUDIO"
    ): Notification {
        createNotificationChannels(context)

        val isVideo = callType.equals("VIDEO", ignoreCase = true)
        val channelId = if (isVideo) CHANNEL_ID_VIDEO_CALLS else CHANNEL_ID_AUDIO_CALLS

        // Full Screen Intent pointing to IncomingCallActivity
        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_CALLER_ID, callerId)
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALLER_USERNAME, callerUsername)
            putExtra(EXTRA_CALLER_PHOTO, callerPhotoUrl)
            putExtra(EXTRA_CALLER_AVATAR, callerAvatarId)
            putExtra(EXTRA_CALL_TYPE, if (isVideo) "VIDEO" else "AUDIO")
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            ("fs_" + callId).hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Decline Call
        val declineIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_DECLINE_CALL
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_CALLER_ID, callerId)
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALL_TYPE, callType)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            ("dec_" + callId).hashCode(),
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Accept Call
        val acceptIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_ACCEPT_CALL
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_CALLER_ID, callerId)
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALLER_USERNAME, callerUsername)
            putExtra(EXTRA_CALLER_PHOTO, callerPhotoUrl)
            putExtra(EXTRA_CALLER_AVATAR, callerAvatarId)
            putExtra(EXTRA_CALL_TYPE, callType)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            ("acc_" + callId).hashCode(),
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val callTypeTitle = if (isVideo) {
            context.getString(R.string.incoming_video_call)
        } else {
            context.getString(R.string.incoming_audio_call)
        }
        val contentText = if (callerUsername.isNotBlank()) {
            "$callerName (@$callerUsername) is calling..."
        } else {
            "$callerName is calling..."
        }

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (isVideo) android.R.drawable.stat_sys_phone_call else android.R.drawable.stat_sys_phone_call)
            .setContentTitle(callTypeTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(ringtoneUri)
            .setVibrate(CALL_VIBRATION_PATTERN)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.call_action_decline),
                declinePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_call,
                context.getString(R.string.call_action_accept),
                acceptPendingIntent
            )
            .build()
    }

    /**
     * Posts an incoming call notification directly via NotificationManager.
     */
    fun showIncomingCallNotification(
        context: Context,
        callId: String,
        callerId: String,
        callerName: String,
        callerUsername: String = "",
        callerPhotoUrl: String = "",
        callerAvatarId: Int = 0,
        callType: String = "AUDIO"
    ) {
        val notification = buildIncomingCallNotification(
            context = context,
            callId = callId,
            callerId = callerId,
            callerName = callerName,
            callerUsername = callerUsername,
            callerPhotoUrl = callerPhotoUrl,
            callerAvatarId = callerAvatarId,
            callType = callType
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(callId.hashCode(), notification)
        Log.i(TAG, "Incoming call notification posted for callId: $callId, caller: $callerName ($callType)")
    }

    /**
     * Cancels an incoming call notification.
     */
    fun cancelCallNotification(context: Context, callId: String) {
        if (callId.isBlank()) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(callId.hashCode())
        Log.d(TAG, "Call notification cancelled for callId: $callId")
    }

    /**
     * Posts a missed call notification.
     */
    fun showMissedCallNotification(
        context: Context,
        callId: String,
        callerId: String,
        callerName: String,
        callType: String = "AUDIO",
        timestamp: Long = System.currentTimeMillis()
    ) {
        createNotificationChannels(context)

        val isVideo = callType.equals("VIDEO", ignoreCase = true)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_USER_ID, callerId)
            putExtra(EXTRA_USER_NAME, callerName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            ("missed_" + callId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isVideo) {
            context.getString(R.string.missed_video_call)
        } else {
            context.getString(R.string.missed_audio_call)
        }
        val body = "$callerName called you"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MISSED_CALLS)
            .setSmallIcon(android.R.drawable.stat_notify_missed_call)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setWhen(timestamp)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(("missed_" + callId).hashCode(), notification)
        Log.i(TAG, "Missed call notification posted for caller: $callerName, callId: $callId")
    }

    /**
     * Posts a chat message notification with deduplication.
     */
    fun showMessageNotification(
        context: Context,
        senderId: String,
        senderName: String,
        chatId: String,
        messageId: String,
        messageText: String
    ) {
        createNotificationChannels(context)

        // Deduplication
        if (messageId.isNotBlank() && isMessageAlreadyNotified(context, messageId)) {
            Log.d(TAG, "Message notification already displayed for messageId: $messageId, skipping.")
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
            ("msg_" + senderId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = senderName.ifBlank { "New Message" }
        val body = messageText.ifBlank { "Sent you a message" }

        val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(defaultSoundUri)
            .setVibrate(MESSAGE_VIBRATION_PATTERN)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notifId = if (messageId.isNotBlank()) messageId.hashCode() else senderId.hashCode()
        notificationManager?.notify(notifId, notification)

        if (messageId.isNotBlank()) {
            markMessageAsNotified(context, messageId)
        }
        Log.i(TAG, "Chat message notification posted for sender: $senderName ($messageId)")
    }
}
