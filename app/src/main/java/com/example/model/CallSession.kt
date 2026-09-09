package com.example.model

/**
 * Represents a 1-on-1 Audio or Video Call Session between two users.
 * Supports real-time WebRTC / Agora signaling, ICE negotiation, and state tracking.
 */
enum class CallType {
    AUDIO,
    VIDEO
}

enum class CallStatus {
    IDLE,
    OUTGOING,      // Caller initiated, waiting for response / ringing
    RINGING,       // Receiver device is ringing
    ACCEPTED,      // In progress / connected
    REJECTED,      // Receiver rejected
    MISSED,        // Caller timed out or receiver didn't answer
    BUSY,          // Receiver is currently on another call
    ENDED          // Call concluded normally
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

data class CallSession(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerUsername: String = "",
    val callerPhotoUrl: String = "",
    val callerAvatarId: Int = 1,
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverUsername: String = "",
    val receiverPhotoUrl: String = "",
    val receiverAvatarId: Int = 1,
    val callType: String = CallType.AUDIO.name,
    val status: String = CallStatus.RINGING.name,
    val channelName: String = "",
    val timestamp: Long = 0L,
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val durationSeconds: Long = 0L,
    val sdpOffer: String = "",
    val sdpAnswer: String = "",
    val candidate: String = ""
) {
    val isVideo: Boolean get() = callType.equals(CallType.VIDEO.name, ignoreCase = true)

    fun isVideoCall(): Boolean = isVideo
    
    fun isActive(): Boolean = status == CallStatus.ACCEPTED.name || status == CallStatus.RINGING.name || status == CallStatus.OUTGOING.name
}
