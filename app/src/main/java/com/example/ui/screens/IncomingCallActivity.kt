package com.example.ui.screens

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.CallStatus
import com.example.service.IncomingCallRingingService
import com.example.ui.components.UserAvatar
import com.example.ui.theme.DarkBg
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.util.WpChatNotificationHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class IncomingCallActivity : ComponentActivity() {

    private var callId: String = ""
    private var callerId: String = ""
    private var callerName: String = ""
    private var callerUsername: String = ""
    private var callerPhotoUrl: String = ""
    private var callerAvatarId: Int = 0
    private var callType: String = "AUDIO"

    private var sessionListener: ValueEventListener? = null
    private var dismissalReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure window for lock screen & waking up display
        setupLockScreenFlags()

        parseIntentExtras(intent)

        // Register dismissal receiver
        dismissalReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val dismissedId = intent?.getStringExtra(WpChatNotificationHelper.EXTRA_CALL_ID)
                if (dismissedId.isNullOrBlank() || dismissedId == callId) {
                    finishAndRemoveTask()
                }
            }
        }
        val filter = IntentFilter(IncomingCallRingingService.ACTION_INCOMING_CALL_DISMISSED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissalReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dismissalReceiver, filter)
        }

        // Attach remote database listener
        attachDatabaseListener()

        setContent {
            IncomingCallScreenContent(
                callerName = callerName,
                callerUsername = callerUsername,
                callerPhotoUrl = callerPhotoUrl,
                callerAvatarId = callerAvatarId,
                callType = callType,
                onAccept = {
                    IncomingCallRingingService.acceptCall(
                        context = this@IncomingCallActivity,
                        callId = callId,
                        callerId = callerId,
                        callerName = callerName,
                        callType = callType
                    )
                    finishAndRemoveTask()
                },
                onReject = {
                    IncomingCallRingingService.rejectCall(
                        context = this@IncomingCallActivity,
                        callId = callId,
                        callerId = callerId,
                        callerName = callerName,
                        callType = callType
                    )
                    finishAndRemoveTask()
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntentExtras(intent)
    }

    private fun parseIntentExtras(intent: Intent?) {
        intent?.let {
            callId = it.getStringExtra(WpChatNotificationHelper.EXTRA_CALL_ID).orEmpty()
            callerId = it.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_ID).orEmpty()
            callerName = it.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_NAME).orEmpty().ifBlank { "Incoming Caller" }
            callerUsername = it.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_USERNAME).orEmpty()
            callerPhotoUrl = it.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_PHOTO).orEmpty()
            callerAvatarId = it.getIntExtra(WpChatNotificationHelper.EXTRA_CALLER_AVATAR, 0)
            callType = it.getStringExtra(WpChatNotificationHelper.EXTRA_CALL_TYPE) ?: "AUDIO"
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager?.requestDismissKeyguard(this, null)
        }
    }

    private fun attachDatabaseListener() {
        if (callId.isBlank()) return
        try {
            val db = FirebaseDatabase.getInstance("https://chating-a9250-default-rtdb.firebaseio.com")
            val ref = db.getReference("calls").child(callId)
            sessionListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        finishAndRemoveTask()
                        return
                    }
                    val status = snapshot.child("status").getValue(String::class.java)
                    if (status != null && status != CallStatus.RINGING.name && status != CallStatus.OUTGOING.name) {
                        finishAndRemoveTask()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(sessionListener!!)
        } catch (e: Exception) {
            // Ignore listener setup failure
        }
    }

    override fun onDestroy() {
        dismissalReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {}
            dismissalReceiver = null
        }
        if (callId.isNotBlank() && sessionListener != null) {
            try {
                val db = FirebaseDatabase.getInstance("https://chating-a9250-default-rtdb.firebaseio.com")
                db.getReference("calls").child(callId).removeEventListener(sessionListener!!)
            } catch (e: Exception) {}
            sessionListener = null
        }
        super.onDestroy()
    }
}

@Composable
fun IncomingCallScreenContent(
    callerName: String,
    callerUsername: String,
    callerPhotoUrl: String,
    callerAvatarId: Int,
    callType: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val isVideo = callType.equals("VIDEO", ignoreCase = true)

    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF000000)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF111111),
                            Color(0xFF070707),
                            Color(0xFF000000)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Header / Call Type Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E1E))
                            .border(1.dp, Color(0xFF333333), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isVideo) "Incoming Video Call" else "Incoming Voice Call",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Center Caller Details with Pulsing Ring
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(220.dp)
                    ) {
                        // Pulsing Outer Rings
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = pulseAlpha * 0.4f))
                        )
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E1E))
                                .border(2.dp, Color(0xFF444444), CircleShape)
                        )

                        // Caller Avatar
                        UserAvatar(
                            name = callerName,
                            avatarId = callerAvatarId,
                            photoUrl = callerPhotoUrl,
                            size = 130.dp,
                            isOnline = true
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = callerName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    if (callerUsername.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "@$callerUsername",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFB0B0B0),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Ringing...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF888888)
                    )
                }

                // Bottom Action Controls (Accept & Reject)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reject Call Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = onReject,
                            shape = CircleShape,
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(8.dp),
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("incoming_call_decline_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline Call",
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Text(
                            text = "Decline",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Accept Call Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = onAccept,
                            shape = CircleShape,
                            containerColor = Color(0xFF22C55E),
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(8.dp),
                            modifier = Modifier
                                .size(72.dp)
                                .scale(pulseScale.coerceIn(1f, 1.15f))
                                .testTag("incoming_call_accept_button")
                        ) {
                            Icon(
                                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = "Accept Call",
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Text(
                            text = "Accept",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF22C55E),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
