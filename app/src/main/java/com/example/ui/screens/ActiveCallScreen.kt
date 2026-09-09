package com.example.ui.screens

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.CallSession
import com.example.model.CallStatus
import com.example.model.User
import com.example.ui.components.UserAvatar
import com.example.ui.theme.DarkBg
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ActiveCallScreen(
    currentUser: User,
    callSession: CallSession,
    durationSeconds: Long,
    isSpeakerOn: Boolean,
    isMicMuted: Boolean,
    isVideoCameraOff: Boolean,
    isFrontCamera: Boolean,
    onToggleSpeaker: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleVideoCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    val isCaller = callSession.callerId == currentUser.id
    val isVideo = callSession.isVideoCall()
    val isConnected = callSession.status == CallStatus.ACCEPTED.name

    val otherUserName = if (isCaller) callSession.receiverName else callSession.callerName
    val otherUserUsername = if (isCaller) callSession.receiverUsername else callSession.callerUsername
    val otherUserPhotoUrl = if (isCaller) callSession.receiverPhotoUrl else callSession.callerPhotoUrl
    val otherUserAvatarId = if (isCaller) callSession.receiverAvatarId else callSession.callerAvatarId

    val statusText = when {
        isConnected -> {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
        isCaller -> "Calling..."
        else -> "Connecting..."
    }

    // Sound wave pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale"
    )

    Dialog(
        onDismissRequest = { /* Must use end call button */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF090D16),
                                Color(0xFF020617)
                            )
                        )
                    )
            ) {
                // If Video Call and Camera is on, display camera surface
                if (isVideo) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Remote User Video Canvas / Placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0A0F1D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isConnected) {
                                        Box(
                                            modifier = Modifier
                                                .size(160.dp)
                                                .scale(waveScale)
                                                .clip(CircleShape)
                                                .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                                        )
                                    }
                                    UserAvatar(
                                        name = otherUserName,
                                        avatarId = otherUserAvatarId,
                                        photoUrl = otherUserPhotoUrl,
                                        size = 110.dp,
                                        isOnline = true
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = otherUserName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isConnected) "Video Connected • HD" else "Connecting video feed...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isConnected) OnlineGreen else TextMuted
                                )
                            }
                        }

                        // Local Picture-in-Picture Camera Preview (Floating Window)
                        if (!isVideoCameraOff) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .statusBarsPadding()
                                    .padding(top = 16.dp, end = 16.dp)
                                    .size(width = 110.dp, height = 160.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                            ) {
                                CameraPreviewView(
                                    isFrontCamera = isFrontCamera,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Switch Camera Button Overlay
                                IconButton(
                                    onClick = onSwitchCamera,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .size(32.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .testTag("call_pip_switch_camera")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cameraswitch,
                                        contentDescription = "Switch Camera",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Voice Call Canvas
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(220.dp)
                        ) {
                            if (isConnected) {
                                Box(
                                    modifier = Modifier
                                        .size(200.dp)
                                        .scale(waveScale)
                                        .clip(CircleShape)
                                        .background(OnlineGreen.copy(alpha = 0.15f))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .border(
                                        2.dp,
                                        if (isConnected) OnlineGreen else Color(0xFF38BDF8),
                                        CircleShape
                                    )
                            )
                            UserAvatar(
                                name = otherUserName,
                                avatarId = otherUserAvatarId,
                                photoUrl = otherUserPhotoUrl,
                                size = 120.dp,
                                isOnline = true
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = otherUserName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        if (otherUserUsername.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "@$otherUserUsername",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            if (isConnected) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = OnlineGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isConnected) OnlineGreen else TextMuted
                            )
                        }
                    }
                }

                // Top Bar with Caller Details & Call Type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isVideo) "1-on-1 Video Call" else "1-on-1 Voice Call",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConnected) OnlineGreen else Color(0xFF38BDF8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Speakerphone Toggle Quick Badge
                    IconButton(
                        onClick = onToggleSpeaker,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (isSpeakerOn) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isSpeakerOn) Color(0xFF38BDF8) else Color(0xFF334155),
                                CircleShape
                            )
                            .testTag("call_speaker_toggle_header")
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Toggle Speaker",
                            tint = if (isSpeakerOn) Color(0xFF38BDF8) else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bottom In-Call Floating Controls Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        shape = RoundedCornerShape(28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mic Mute / Unmute Button
                            IconButton(
                                onClick = onToggleMute,
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        if (isMicMuted) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                        CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        if (isMicMuted) Color(0xFFEF4444) else Color(0xFF334155),
                                        CircleShape
                                    )
                                    .testTag("call_mute_button")
                            ) {
                                Icon(
                                    imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Toggle Microphone",
                                    tint = if (isMicMuted) Color(0xFFEF4444) else TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Speakerphone Toggle
                            IconButton(
                                onClick = onToggleSpeaker,
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        if (isSpeakerOn) Color(0xFF38BDF8).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                        CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        if (isSpeakerOn) Color(0xFF38BDF8) else Color(0xFF334155),
                                        CircleShape
                                    )
                                    .testTag("call_speaker_button")
                            ) {
                                Icon(
                                    imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                    contentDescription = "Toggle Speaker",
                                    tint = if (isSpeakerOn) Color(0xFF38BDF8) else TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // If Video Call: Camera Off / On Toggle
                            if (isVideo) {
                                IconButton(
                                    onClick = onToggleVideoCamera,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(
                                            if (isVideoCameraOff) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (isVideoCameraOff) Color(0xFFEF4444) else Color(0xFF334155),
                                            CircleShape
                                        )
                                        .testTag("call_camera_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isVideoCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                        contentDescription = "Toggle Video Camera",
                                        tint = if (isVideoCameraOff) Color(0xFFEF4444) else TextPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Flip Camera Button
                                IconButton(
                                    onClick = onSwitchCamera,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(Color(0xFF1E293B), CircleShape)
                                        .border(1.dp, Color(0xFF334155), CircleShape)
                                        .testTag("call_flip_camera_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cameraswitch,
                                        contentDescription = "Switch Camera Front/Back",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // End Call (Hang Up) Red Floating Button
                            FloatingActionButton(
                                onClick = onEndCall,
                                shape = CircleShape,
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("call_end_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "End Call",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Embedded CameraX local preview view for live video stream.
 */
@Composable
fun CameraPreviewView(
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (e: Exception) {
                    android.util.Log.w("CameraPreviewView", "Camera binding: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(context))
        },
        modifier = modifier
    )
}
