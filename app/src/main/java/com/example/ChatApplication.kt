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

            // Create Android notification channel for chat messages
            com.example.util.WpChatNotificationHelper.createNotificationChannel(this)

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




