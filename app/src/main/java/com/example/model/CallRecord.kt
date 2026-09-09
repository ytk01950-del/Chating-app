package com.example.model

/**
 * Represents an entry in the user's Call History (Missed, Incoming, Outgoing).
 */
data class CallRecord(
    val id: String = "",
    val callId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserUsername: String = "",
    val otherUserPhotoUrl: String = "",
    val otherUserAvatarId: Int = 1,
    val callType: String = CallType.AUDIO.name,       // "AUDIO" or "VIDEO"
    val direction: String = CallDirection.OUTGOING.name, // "INCOMING", "OUTGOING", "MISSED"
    val status: String = CallStatus.ENDED.name,
    val timestamp: Long = 0L,
    val durationSeconds: Long = 0L
) {
    fun isVideo(): Boolean = callType.equals(CallType.VIDEO.name, ignoreCase = true)
    
    fun isMissed(): Boolean = direction == CallDirection.MISSED.name || status == CallStatus.MISSED.name
    
    fun getFormattedDuration(): String {
        if (durationSeconds <= 0) return if (isMissed()) "Missed" else "Not answered"
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
    }
}
