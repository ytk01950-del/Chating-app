package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.model.CallDirection
import com.example.model.CallRecord
import com.example.model.CallStatus
import com.example.ui.screens.IncomingCallActivity
import com.example.util.WpChatNotificationHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

open class IncomingCallRingingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentCallId: String? = null
    private var currentCallerId: String? = null
    private var currentCallerName: String? = null
    private var currentCallType: String? = "AUDIO"
    private var callStatusListener: ValueEventListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private val databaseUrl = "https://chating-a9250-default-rtdb.firebaseio.com"

    private val timeoutRunnable = Runnable {
        Log.w(TAG, "Call ringing timed out (45s). Marking as MISSED.")
        handleCallTimeout()
    }

    companion object {
        private const val TAG = "IncomingCallService"
        const val ACTION_START_RINGING = "com.example.service.action.START_RINGING"
        const val ACTION_STOP_RINGING = "com.example.service.action.STOP_RINGING"
        const val ACTION_ACCEPT = "com.example.service.action.ACCEPT"
        const val ACTION_REJECT = "com.example.service.action.REJECT"
        const val ACTION_INCOMING_CALL_DISMISSED = "com.example.service.action.CALL_DISMISSED"

        const val NOTIFICATION_ID = 991102

        var isRinging: Boolean = false
            private set
        var activeRingingCallId: String? = null
            private set

        fun startRinging(
            context: Context,
            callId: String,
            callerId: String,
            callerName: String,
            callerUsername: String = "",
            callerPhotoUrl: String = "",
            callerAvatarId: Int = 0,
            callType: String = "AUDIO"
        ) {
            val intent = Intent(context, IncomingCallRingingService::class.java).apply {
                action = ACTION_START_RINGING
                putExtra(WpChatNotificationHelper.EXTRA_CALL_ID, callId)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_ID, callerId)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_NAME, callerName)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_USERNAME, callerUsername)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_PHOTO, callerPhotoUrl)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_AVATAR, callerAvatarId)
                putExtra(WpChatNotificationHelper.EXTRA_CALL_TYPE, callType)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start IncomingCallRingingService: ${e.message}", e)
            }
        }

        fun stopRinging(context: Context) {
            val intent = Intent(context, IncomingCallRingingService::class.java).apply {
                action = ACTION_STOP_RINGING
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send stop ringing intent: ${e.message}")
            }
        }

        fun acceptCall(
            context: Context,
            callId: String,
            callerId: String = "",
            callerName: String = "",
            callType: String = "AUDIO"
        ) {
            val intent = Intent(context, IncomingCallRingingService::class.java).apply {
                action = ACTION_ACCEPT
                putExtra(WpChatNotificationHelper.EXTRA_CALL_ID, callId)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_ID, callerId)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_NAME, callerName)
                putExtra(WpChatNotificationHelper.EXTRA_CALL_TYPE, callType)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send accept call intent: ${e.message}")
            }
        }

        fun rejectCall(
            context: Context,
            callId: String,
            callerId: String = "",
            callerName: String = "",
            callType: String = "AUDIO"
        ) {
            val intent = Intent(context, IncomingCallRingingService::class.java).apply {
                action = ACTION_REJECT
                putExtra(WpChatNotificationHelper.EXTRA_CALL_ID, callId)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_ID, callerId)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_NAME, callerName)
                putExtra(WpChatNotificationHelper.EXTRA_CALL_TYPE, callType)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send reject call intent: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "IncomingCallRingingService onCreate")
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "onStartCommand action: $action")

        when (action) {
            ACTION_START_RINGING -> {
                val callId = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALL_ID).orEmpty()
                val callerId = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_ID).orEmpty()
                val callerName = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_NAME).orEmpty()
                val callerUsername = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_USERNAME).orEmpty()
                val callerPhotoUrl = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_PHOTO).orEmpty()
                val callerAvatarId = intent.getIntExtra(WpChatNotificationHelper.EXTRA_CALLER_AVATAR, 0)
                val callType = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALL_TYPE) ?: "AUDIO"

                if (callId.isBlank()) {
                    Log.w(TAG, "Cannot start ringing with empty callId. Stopping service.")
                    stopSelf()
                    return START_NOT_STICKY
                }

                // Prevent duplicate ringing for same call
                if (isRinging && activeRingingCallId == callId) {
                    Log.d(TAG, "Already ringing for callId: $callId, ignoring duplicate start command.")
                    return START_STICKY
                }

                startForegroundRinging(
                    callId = callId,
                    callerId = callerId,
                    callerName = callerName,
                    callerUsername = callerUsername,
                    callerPhotoUrl = callerPhotoUrl,
                    callerAvatarId = callerAvatarId,
                    callType = callType
                )
            }

            ACTION_ACCEPT -> {
                val callId = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALL_ID).orEmpty()
                val callerId = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_ID).orEmpty()
                val callerName = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_NAME).orEmpty()
                val callType = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALL_TYPE) ?: "AUDIO"

                Log.i(TAG, "ACTION_ACCEPT received for callId: $callId")
                cleanupRinging()

                // Mark accepted in Firebase
                if (callId.isNotBlank()) {
                    try {
                        val db = FirebaseDatabase.getInstance(databaseUrl)
                        val updates = mapOf<String, Any>(
                            "status" to CallStatus.ACCEPTED.name,
                            "startedAt" to System.currentTimeMillis()
                        )
                        db.getReference("calls").child(callId).updateChildren(updates)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update call status to ACCEPTED: ${e.message}")
                    }
                }

                // Broadcast dismissal to IncomingCallActivity
                broadcastCallDismissed(callId)

                // Launch MainActivity into call
                val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    putExtra(WpChatNotificationHelper.EXTRA_CALL_ID, callId)
                    putExtra(WpChatNotificationHelper.EXTRA_CALLER_ID, callerId)
                    putExtra(WpChatNotificationHelper.EXTRA_CALLER_NAME, callerName)
                    putExtra(WpChatNotificationHelper.EXTRA_CALL_TYPE, callType)
                    putExtra(WpChatNotificationHelper.EXTRA_ACTION_CALL, "accept")
                }
                applicationContext.startActivity(mainIntent)

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_REJECT -> {
                val callId = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALL_ID).orEmpty()
                val callerId = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_ID).orEmpty()
                val callerName = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_NAME).orEmpty()
                val callType = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALL_TYPE) ?: "AUDIO"

                Log.i(TAG, "ACTION_REJECT received for callId: $callId")
                cleanupRinging()

                // Mark rejected in Firebase and log call record
                if (callId.isNotBlank()) {
                    try {
                        val db = FirebaseDatabase.getInstance(databaseUrl)
                        val now = System.currentTimeMillis()
                        db.getReference("calls").child(callId).updateChildren(
                            mapOf<String, Any>(
                                "status" to CallStatus.REJECTED.name,
                                "endedAt" to now
                            )
                        )
                        // Clear active calls
                        if (callerId.isNotBlank()) {
                            db.getReference("user_active_calls").child(callerId).removeValue()
                        }
                        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        if (!myUid.isNullOrBlank()) {
                            db.getReference("user_active_calls").child(myUid).removeValue()
                            // Log incoming rejected record for receiver
                            val record = CallRecord(
                                id = "${callId}_receiver",
                                callId = callId,
                                otherUserId = callerId,
                                otherUserName = callerName,
                                callType = callType,
                                direction = CallDirection.INCOMING.name,
                                status = CallStatus.REJECTED.name,
                                timestamp = now,
                                durationSeconds = 0L
                            )
                            db.getReference("call_history").child(myUid).child(callId).setValue(record)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update call rejection: ${e.message}")
                    }
                }

                // Broadcast dismissal to IncomingCallActivity
                broadcastCallDismissed(callId)

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_STOP_RINGING -> {
                Log.i(TAG, "ACTION_STOP_RINGING received.")
                cleanupRinging()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            else -> {
                cleanupRinging()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundRinging(
        callId: String,
        callerId: String,
        callerName: String,
        callerUsername: String,
        callerPhotoUrl: String,
        callerAvatarId: Int,
        callType: String
    ) {
        currentCallId = callId
        currentCallerId = callerId
        currentCallerName = callerName
        currentCallType = callType
        isRinging = true
        activeRingingCallId = callId

        // 1. Acquire WakeLock (45s max)
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Nexa:IncomingCallCpuWakeLock"
            )
            wakeLock?.acquire(45_000L)
            Log.d(TAG, "CPU WakeLock acquired for incoming call")

            // Wake up and turn screen on for incoming call
            try {
                @Suppress("DEPRECATION")
                val screenWakeLock = powerManager?.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "Nexa:IncomingCallScreenWakeLock"
                )
                screenWakeLock?.acquire(10_000L)
            } catch (e: Exception) {
                Log.w(TAG, "Screen wake lock note: ${e.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire WakeLock: ${e.message}")
        }

        // 2. Build and Start Foreground Notification
        val notification = WpChatNotificationHelper.buildIncomingCallNotification(
            context = applicationContext,
            callId = callId,
            callerId = callerId,
            callerName = callerName,
            callerUsername = callerUsername,
            callerPhotoUrl = callerPhotoUrl,
            callerAvatarId = callerAvatarId,
            callType = callType
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Launch IncomingCallActivity directly as foreground phoneCall service
        try {
            val activityIntent = Intent(applicationContext, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(WpChatNotificationHelper.EXTRA_CALL_ID, callId)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_ID, callerId)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_NAME, callerName)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_USERNAME, callerUsername)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_PHOTO, callerPhotoUrl)
                putExtra(WpChatNotificationHelper.EXTRA_CALLER_AVATAR, callerAvatarId)
                putExtra(WpChatNotificationHelper.EXTRA_CALL_TYPE, callType)
            }
            applicationContext.startActivity(activityIntent)
            Log.i(TAG, "IncomingCallActivity launched directly from phoneCall foreground service")
        } catch (e: Exception) {
            Log.w(TAG, "Direct launch of IncomingCallActivity note: ${e.message}")
        }

        // 3. Start Ringtone Audio Loop
        startRingtonePlayback()

        // 4. Start Vibration Loop
        startVibrationLoop()

        // 5. Attach Realtime Database Status Listener (cancel if remote hangs up)
        attachCallSessionListener(callId)

        // 6. Schedule 45s Timeout for Missed Call
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, 45_000L)

        Log.i(TAG, "Incoming call ringing started successfully for call: $callId from $callerName")
    }

    private fun startRingtonePlayback() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null

            val ringtoneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setLegacyStreamType(AudioManager.STREAM_RING)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "Ringtone MediaPlayer playback started")
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer ringtone playback failed: ${e.message}. Falling back to default.")
        }
    }

    private fun startVibrationLoop() {
        try {
            val pattern = WpChatNotificationHelper.CALL_VIBRATION_PATTERN
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0) // 0 means repeat at index 0
                val attributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
                vibrator?.vibrate(effect, attributes)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
            Log.d(TAG, "Vibration loop started")
        } catch (e: Exception) {
            Log.w(TAG, "Vibrator start failed: ${e.message}")
        }
    }

    private fun attachCallSessionListener(callId: String) {
        try {
            val db = FirebaseDatabase.getInstance(databaseUrl)
            val sessionRef = db.getReference("calls").child(callId)

            callStatusListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.i(TAG, "Call session $callId was removed remotely. Dismissing ringing.")
                        handleRemoteDismissal(callId)
                        return
                    }

                    val status = snapshot.child("status").getValue(String::class.java)
                    Log.d(TAG, "Remote call status update: $status")

                    if (status != null && status != CallStatus.RINGING.name && status != CallStatus.OUTGOING.name) {
                        Log.i(TAG, "Call status transitioned to $status. Dismissing ringing.")
                        handleRemoteDismissal(callId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Call session listener cancelled: ${error.message}")
                }
            }
            sessionRef.addValueEventListener(callStatusListener!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach call session listener: ${e.message}")
        }
    }

    private fun handleRemoteDismissal(callId: String) {
        cleanupRinging()
        broadcastCallDismissed(callId)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleCallTimeout() {
        val callId = currentCallId
        val callerId = currentCallerId.orEmpty()
        val callerName = currentCallerName.orEmpty()
        val callType = currentCallType ?: "AUDIO"

        cleanupRinging()

        if (!callId.isNullOrBlank()) {
            try {
                val db = FirebaseDatabase.getInstance(databaseUrl)
                val now = System.currentTimeMillis()
                db.getReference("calls").child(callId).updateChildren(
                    mapOf<String, Any>(
                        "status" to CallStatus.MISSED.name,
                        "endedAt" to now
                    )
                )

                // Clear user active calls
                if (callerId.isNotBlank()) {
                    db.getReference("user_active_calls").child(callerId).removeValue()
                }
                val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (!myUid.isNullOrBlank()) {
                    db.getReference("user_active_calls").child(myUid).removeValue()

                    // Log missed record for receiver
                    val record = CallRecord(
                        id = "${callId}_receiver",
                        callId = callId,
                        otherUserId = callerId,
                        otherUserName = callerName,
                        callType = callType,
                        direction = CallDirection.MISSED.name,
                        status = CallStatus.MISSED.name,
                        timestamp = now,
                        durationSeconds = 0L
                    )
                    db.getReference("call_history").child(myUid).child(callId).setValue(record)
                }

                // Show Missed Call Notification
                WpChatNotificationHelper.showMissedCallNotification(
                    context = applicationContext,
                    callId = callId,
                    callerId = callerId,
                    callerName = callerName,
                    callType = callType,
                    timestamp = now
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle call timeout: ${e.message}")
            }
            broadcastCallDismissed(callId)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun broadcastCallDismissed(callId: String) {
        val broadcastIntent = Intent(ACTION_INCOMING_CALL_DISMISSED).apply {
            putExtra(WpChatNotificationHelper.EXTRA_CALL_ID, callId)
            setPackage(packageName)
        }
        sendBroadcast(broadcastIntent)
    }

    private fun cleanupRinging() {
        handler.removeCallbacks(timeoutRunnable)

        // 1. Remove Firebase DB listener
        if (!currentCallId.isNullOrBlank() && callStatusListener != null) {
            try {
                val db = FirebaseDatabase.getInstance(databaseUrl)
                db.getReference("calls").child(currentCallId!!).removeEventListener(callStatusListener!!)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove status listener: ${e.message}")
            }
            callStatusListener = null
        }

        // 2. Stop and release MediaPlayer
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer release note: ${e.message}")
        }
        mediaPlayer = null

        // 3. Stop Vibrator
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Vibrator cancel note: ${e.message}")
        }

        // 4. Release WakeLock
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock release note: ${e.message}")
        }
        wakeLock = null

        isRinging = false
        activeRingingCallId = null
        Log.d(TAG, "Ringing resources cleaned up successfully")
    }

    override fun onDestroy() {
        Log.d(TAG, "IncomingCallRingingService onDestroy")
        cleanupRinging()
        super.onDestroy()
    }
}

/**
 * Foreground Service for call signaling and background delivery.
 * Registered in AndroidManifest with foregroundServiceType="phoneCall".
 */
class CallSignalingForegroundService : IncomingCallRingingService()

