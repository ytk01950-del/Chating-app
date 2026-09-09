package com.example.service

import android.content.Context
import android.util.Log

/**
 * Built-in Supabase Configuration & Initialization.
 *
 * Hardcodes the default Supabase Project URL and Public Anon Key so that
 * storage, avatars, stories, feed posts, and media messages work automatically
 * out-of-the-box without requiring manual setup or user input dialogs.
 */
object SupabaseConfig {

    private const val TAG = "SupabaseConfig"

    // Default Supabase Project URL (Public REST & Storage endpoint)
    const val DEFAULT_SUPABASE_URL = "https://bibcwocmuplcjjizlmha.supabase.co"

    // Default Supabase Public Anon / Publishable Key (Client-safe public key)
    const val DEFAULT_SUPABASE_ANON_KEY = "sb_publishable_YvXo53axWYs5prN-nNRztg_RVozqYf_"

    /**
     * Initializes the built-in Supabase client configuration on app launch.
     */
    fun initialize(context: Context) {
        val url = getProjectUrl(context)
        val key = getAnonKey(context)
        Log.i(TAG, "Supabase client auto-initialized successfully on app launch: URL=$url")
    }

    /**
     * Returns the active Supabase Project URL.
     * Uses environment/BuildConfig values if present and valid; otherwise returns the built-in default.
     */
    fun getProjectUrl(context: Context? = null): String {
        val resolved = SupabaseConfigManager.getProjectUrl(context)
        return if (resolved.isNotBlank() && !SupabaseConfigManager.isPlaceholderUrl(resolved)) {
            resolved
        } else {
            DEFAULT_SUPABASE_URL
        }
    }

    /**
     * Returns the active Supabase Public Anon Key.
     * Uses environment/BuildConfig values if present and valid; otherwise returns the built-in default.
     */
    fun getAnonKey(context: Context? = null): String {
        val resolved = SupabaseConfigManager.getAnonKey(context)
        return if (resolved.isNotBlank() && !SupabaseConfigManager.isPlaceholderKey(resolved)) {
            resolved
        } else {
            DEFAULT_SUPABASE_ANON_KEY
        }
    }
}
