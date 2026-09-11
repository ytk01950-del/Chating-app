package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase

class ChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:830188890451:android:113be44fdb9960737fd9a6")
                    .setApiKey("AIzaSyBk9kSPwBv62-v9vTpGLw18Rsm1FE42NTU")
                    .setDatabaseUrl("https://chating-a9250-default-rtdb.firebaseio.com")
                    .setProjectId("chating-a9250")
                    .setGcmSenderId("830188890451")
                    .setStorageBucket("chating-a9250.firebasestorage.app")
                    .build()
                val app = FirebaseApp.initializeApp(this, options)
                Log.d("ChatApplication", "Firebase successfully initialized: ${app.name}")
            } else {
                Log.d("ChatApplication", "Firebase already initialized: ${FirebaseApp.getInstance().name}")
            }

            // Enable offline disk persistence for Realtime Database
            try {
                FirebaseDatabase.getInstance("https://chating-a9250-default-rtdb.firebaseio.com").setPersistenceEnabled(true)
            } catch (e: Exception) {
                // Persistence already configured or active
            }

            // Create Android v3 notification channels for messages and calls
            com.example.util.WpChatNotificationHelper.createNotificationChannels(this)

            // Ensure Firebase Cloud Messaging auto-initialization
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = true
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.i("ChatApplication", "FCM token retrieved at app startup: $token")
                        if (!token.isNullOrBlank()) {
                            com.example.service.WpChatMessagingService.saveCachedToken(this, token)
                            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                            if (!uid.isNullOrBlank()) {
                                val db = FirebaseDatabase.getInstance("https://chating-a9250-default-rtdb.firebaseio.com")
                                db.getReference("fcm_tokens").child(uid).setValue(token)
                                db.getReference("users").child(uid).child("fcmToken").setValue(token)
                            }
                        }
                    } else {
                        Log.w("ChatApplication", "FCM token retrieval failed: ${task.exception?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w("ChatApplication", "FCM initialization note: ${e.message}")
            }

            // Supabase client auto-initialization on app launch with built-in credentials
            com.example.service.SupabaseConfig.initialize(this)
            val supabaseStatus = com.example.service.SupabaseConfigManager.getConfigStatus(this)
            Log.i("ChatApplication", "=== Supabase Storage Runtime Diagnostics ===")
            Log.i("ChatApplication", "SUPABASE_URL detected = ${supabaseStatus.urlDetected} (${supabaseStatus.urlDisplay})")
            Log.i("ChatApplication", "SUPABASE_PUBLIC_KEY detected = ${supabaseStatus.publicKeyDetected}")
            Log.i("ChatApplication", "=============================================")
        } catch (e: Exception) {
            Log.e("ChatApplication", "FATAL: Application onCreate initialization failed: ${e.message}", e)
        }
    }
}




