package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.util.Log
import com.example.model.CallDirection
import com.example.model.CallRecord
import com.example.model.CallSession
import com.example.model.CallStatus
import com.example.model.CallType
import com.example.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Service managing WebRTC / 1-on-1 Audio & Video Call signaling, audio routing,
 * and call history logging via Firebase Realtime Database.
 */
class CallSignalingService(
    private val database: FirebaseDatabase
) {
    private val tag = "CallSignalingService"
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
        } catch (e: Exception) {
            Log.w(tag, "ToneGenerator init: ${e.message}")
        }
    }

    /**
     * Listens for any active incoming call for the specified current user.
     */
    fun observeIncomingCall(currentUserId: String): Flow<CallSession?> = callbackFlow {
        if (currentUserId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val activeCallRef = database.getReference("user_active_calls").child(currentUserId)
        var callListener: ValueEventListener? = null
        var sessionRef: com.google.firebase.database.DatabaseReference? = null

        val activeCallListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val callId = snapshot.getValue(String::class.java)
                if (callId.isNullOrBlank()) {
                    sessionRef?.removeEventListener(callListener ?: return)
                    trySend(null)
                    return
                }

                // Listen to the session itself
                sessionRef?.removeEventListener(callListener ?: return)
                sessionRef = database.getReference("calls").child(callId)
                callListener = object : ValueEventListener {
                    override fun onDataChange(callSnap: DataSnapshot) {
                        val session = callSnap.getValue(CallSession::class.java)
                        if (session != null && session.receiverId == currentUserId &&
                            (session.status == CallStatus.RINGING.name || session.status == CallStatus.OUTGOING.name)
                        ) {
                            if (session.status == CallStatus.OUTGOING.name) {
                                sessionRef?.child("status")?.setValue(CallStatus.RINGING.name)
                            }
                            trySend(session.copy(status = CallStatus.RINGING.name))
                        } else {
                            trySend(null)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        trySend(null)
                    }
                }
                sessionRef?.addValueEventListener(callListener!!)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        activeCallRef.addValueEventListener(activeCallListener)

        awaitClose {
            activeCallRef.removeEventListener(activeCallListener)
            sessionRef?.let { ref ->
                callListener?.let { l -> ref.removeEventListener(l) }
            }
        }
    }

    /**
     * Listens to a specific call session's status changes (e.g. accepted, rejected, ended).
     */
    fun observeCallSession(callId: String): Flow<CallSession?> = callbackFlow {
        if (callId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val sessionRef = database.getReference("calls").child(callId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val session = snapshot.getValue(CallSession::class.java)
                trySend(session)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        sessionRef.addValueEventListener(listener)
        awaitClose { sessionRef.removeEventListener(listener) }
    }

    /**
     * Initiates a new 1-on-1 Audio or Video Call.
     */
    suspend fun startCall(
        caller: User,
        receiver: User,
        callType: CallType
    ): Result<CallSession> {
        return try {
            val callId = "call_${System.currentTimeMillis()}_${(1000..9999).random()}"
            val channelName = "wpchat_${caller.id}_${receiver.id}_${System.currentTimeMillis()}"
            val now = System.currentTimeMillis()

            val session = CallSession(
                callId = callId,
                callerId = caller.id,
                callerName = caller.displayName.ifBlank { caller.username },
                callerUsername = caller.username,
                callerPhotoUrl = caller.photoUrl,
                callerAvatarId = caller.avatarId,
                receiverId = receiver.id,
                receiverName = receiver.displayName.ifBlank { receiver.username },
                receiverUsername = receiver.username,
                receiverPhotoUrl = receiver.photoUrl,
                receiverAvatarId = receiver.avatarId,
                callType = callType.name,
                status = CallStatus.OUTGOING.name,
                channelName = channelName,
                timestamp = now,
                startedAt = 0L,
                endedAt = 0L,
                durationSeconds = 0L
            )

            // 1. Write call session
            database.getReference("calls").child(callId).setValue(session).await()

            // 2. Set active call pointers for both users
            database.getReference("user_active_calls").child(caller.id).setValue(callId).await()
            database.getReference("user_active_calls").child(receiver.id).setValue(callId).await()

            // 3. Post rich high-priority call notification payload for receiver
            val notifPayload = mapOf(
                "type" to "call",
                "incoming_call" to "true",
                "callId" to callId,
                "callerId" to caller.id,
                "callerName" to caller.displayName.ifBlank { caller.username },
                "callerUsername" to caller.username,
                "callerPhotoUrl" to (caller.photoUrl ?: ""),
                "callerAvatarId" to caller.avatarId.toString(),
                "callType" to callType.name,
                "priority" to "high",
                "timestamp" to now
            )
            database.getReference("notifications").child(receiver.id).child(callId).setValue(notifPayload)

            Log.d(tag, "Call started: $callId (${callType.name}) to ${receiver.displayName}")
            Result.success(session)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start call: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Accepts an incoming call.
     */
    suspend fun acceptCall(callId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val updates = mapOf<String, Any>(
                "status" to CallStatus.ACCEPTED.name,
                "startedAt" to now
            )
            database.getReference("calls").child(callId).updateChildren(updates).await()
            Log.d(tag, "Call accepted: $callId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to accept call $callId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Rejects an incoming call.
     */
    suspend fun rejectCall(call: CallSession): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            database.getReference("calls").child(call.callId).updateChildren(
                mapOf<String, Any>(
                    "status" to CallStatus.REJECTED.name,
                    "endedAt" to now
                )
            ).await()

            // Remove active pointers
            clearActiveCalls(call.callerId, call.receiverId)

            // Log call history for caller (rejected/missed) & receiver (incoming rejected)
            logCallHistory(
                call = call.copy(status = CallStatus.REJECTED.name, endedAt = now, durationSeconds = 0L)
            )

            // Post rejected notification to dismiss incoming call on receiver
            val rejectPayload = mapOf(
                "type" to "call_rejected",
                "callId" to call.callId,
                "callerId" to call.callerId,
                "callerName" to call.callerName,
                "timestamp" to now
            )
            database.getReference("notifications").child(call.receiverId).child("rejected_${call.callId}").setValue(rejectPayload)

            Log.d(tag, "Call rejected: ${call.callId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to reject call ${call.callId}: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Ends an ongoing or outgoing call.
     */
    suspend fun endCall(call: CallSession): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val startedAt = if (call.startedAt > 0) call.startedAt else now
            val durationSec = if (call.status == CallStatus.ACCEPTED.name && call.startedAt > 0) {
                ((now - call.startedAt) / 1000).coerceAtLeast(0L)
            } else {
                0L
            }

            val finalStatus = if (call.status == CallStatus.ACCEPTED.name) {
                CallStatus.ENDED.name
            } else if (call.status == CallStatus.RINGING.name || call.status == CallStatus.OUTGOING.name) {
                CallStatus.MISSED.name
            } else {
                call.status
            }

            database.getReference("calls").child(call.callId).updateChildren(
                mapOf<String, Any>(
                    "status" to finalStatus,
                    "endedAt" to now,
                    "durationSeconds" to durationSec
                )
            ).await()

            // Remove active pointers
            clearActiveCalls(call.callerId, call.receiverId)

            // Log call history
            logCallHistory(
                call = call.copy(
                    status = finalStatus,
                    endedAt = now,
                    durationSeconds = durationSec
                )
            )

            // Post missed or ended notification to receiver so background state updates
            val notifType = if (finalStatus == CallStatus.MISSED.name) "call_missed" else "call_ended"
            val endPayload = mapOf(
                "type" to notifType,
                "callId" to call.callId,
                "callerId" to call.callerId,
                "callerName" to call.callerName,
                "callType" to call.callType,
                "timestamp" to now
            )
            database.getReference("notifications").child(call.receiverId).child("ended_${call.callId}").setValue(endPayload)

            Log.d(tag, "Call ended: ${call.callId}, duration: ${durationSec}s, status: $finalStatus")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to end call ${call.callId}: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun clearActiveCalls(callerId: String, receiverId: String) {
        try {
            if (callerId.isNotBlank()) {
                database.getReference("user_active_calls").child(callerId).removeValue().await()
            }
            if (receiverId.isNotBlank()) {
                database.getReference("user_active_calls").child(receiverId).removeValue().await()
            }
        } catch (e: Exception) {
            Log.w(tag, "clearActiveCalls note: ${e.message}")
        }
    }

    /**
     * Logs call history for both the caller and the receiver.
     */
    private suspend fun logCallHistory(call: CallSession) {
        try {
            val now = if (call.timestamp > 0) call.timestamp else System.currentTimeMillis()
            val isMissed = call.status == CallStatus.MISSED.name || call.status == CallStatus.REJECTED.name

            // 1. Caller log (Outgoing)
            val callerRecord = CallRecord(
                id = "${call.callId}_caller",
                callId = call.callId,
                otherUserId = call.receiverId,
                otherUserName = call.receiverName,
                otherUserUsername = call.receiverUsername,
                otherUserPhotoUrl = call.receiverPhotoUrl,
                otherUserAvatarId = call.receiverAvatarId,
                callType = call.callType,
                direction = CallDirection.OUTGOING.name,
                status = call.status,
                timestamp = now,
                durationSeconds = call.durationSeconds
            )
            database.getReference("call_history")
                .child(call.callerId)
                .child(call.callId)
                .setValue(callerRecord)
                .await()

            // 2. Receiver log (Incoming or Missed)
            val receiverDirection = if (isMissed) CallDirection.MISSED.name else CallDirection.INCOMING.name
            val receiverRecord = CallRecord(
                id = "${call.callId}_receiver",
                callId = call.callId,
                otherUserId = call.callerId,
                otherUserName = call.callerName,
                otherUserUsername = call.callerUsername,
                otherUserPhotoUrl = call.callerPhotoUrl,
                otherUserAvatarId = call.callerAvatarId,
                callType = call.callType,
                direction = receiverDirection,
                status = call.status,
                timestamp = now,
                durationSeconds = call.durationSeconds
            )
            database.getReference("call_history")
                .child(call.receiverId)
                .child(call.callId)
                .setValue(receiverRecord)
                .await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to log call history: ${e.message}", e)
        }
    }

    /**
     * Observes call history for a specific user.
     */
    fun observeCallHistory(userId: String): Flow<List<CallRecord>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = database.getReference("call_history").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CallRecord>()
                for (child in snapshot.children) {
                    val record = child.getValue(CallRecord::class.java)
                    if (record != null) {
                        list.add(record)
                    }
                }
                list.sortByDescending { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Audio routing helper (Speakerphone, Mic Mute).
     */
    fun setSpeakerphone(context: Context, enabled: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = enabled
            Log.d(tag, "Speakerphone set to $enabled")
        } catch (e: Exception) {
            Log.w(tag, "setSpeakerphone error: ${e.message}")
        }
    }

    fun setMicrophoneMute(context: Context, muted: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            audioManager.isMicrophoneMute = muted
            Log.d(tag, "Microphone mute set to $muted")
        } catch (e: Exception) {
            Log.w(tag, "setMicrophoneMute error: ${e.message}")
        }
    }
}
