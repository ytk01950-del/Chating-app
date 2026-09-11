package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.util.WpChatNotificationHelper

class CallActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallActionReceiver"
        const val ACTION_ACCEPT_CALL = "com.example.service.action.ACCEPT_CALL"
        const val ACTION_DECLINE_CALL = "com.example.service.action.DECLINE_CALL"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        val callId = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALL_ID).orEmpty()
        val callerId = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_ID).orEmpty()
        val callerName = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALLER_NAME).orEmpty()
        val callType = intent.getStringExtra(WpChatNotificationHelper.EXTRA_CALL_TYPE) ?: "AUDIO"

        Log.i(TAG, "onReceive action=$action for callId=$callId, caller=$callerName")

        when (action) {
            ACTION_ACCEPT_CALL -> {
                IncomingCallRingingService.acceptCall(
                    context = context,
                    callId = callId,
                    callerId = callerId,
                    callerName = callerName,
                    callType = callType
                )
            }
            ACTION_DECLINE_CALL -> {
                IncomingCallRingingService.rejectCall(
                    context = context,
                    callId = callId,
                    callerId = callerId,
                    callerName = callerName,
                    callType = callType
                )
                WpChatNotificationHelper.cancelCallNotification(context, callId)
            }
        }
    }
}
