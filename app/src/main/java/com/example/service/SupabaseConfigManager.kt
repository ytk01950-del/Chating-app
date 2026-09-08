package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Centralized Supabase configuration and runtime secret management module.
 *
 * Single Source of Truth for:
 * - Supabase Project URL
 * - Supabase Public Anon / Publishable API Key
 *
 * Hierarchy of Secret Resolution:
 * 1. SharedPreferences (User in-app override, validated for non-empty/non-placeholder)
 * 2. BuildConfig.ENV_SUPABASE_URL & BuildConfig.ENV_SUPABASE_KEY / PUBLISHABLE / ANON
 * 3. BuildConfig.SUPABASE_URL & BuildConfig.SUPABASE_PUBLISHABLE_KEY / SUPABASE_ANON_KEY
 */
object SupabaseConfigManager {

    private const val TAG = "SupabaseConfigManager"
    private const val PREFS_NAME = "supabase_storage_prefs"
    private const val KEY_PROJECT_URL = "supabase_project_url"
    private const val KEY_ANON_KEY = "supabase_anon_key"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class ConfigStatus(
        val isConfigured: Boolean,
        val hasUrl: Boolean,
        val hasKey: Boolean,
        val urlDisplay: String,
        val keyTypeDisplay: String,
        val keyMasked: String,
        val statusMessage: String
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if a given URL is a placeholder or invalid.
     */
    fun isPlaceholderUrl(url: String): Boolean {
        val trimmed = url.trim().lowercase()
        if (trimmed.isBlank() || trimmed == "null" || trimmed == "none") return true
        if (trimmed.contains("your-project") ||
            trimmed.contains("chating-free-storage") ||
            trimmed.contains("example.com") ||
            trimmed.contains("dummy") ||
            trimmed.contains("placeholder") ||
            trimmed.contains("pasand") ||
            trimmed.contains("flex69") ||
            trimmed.contains("sample")
        ) {
            return true
        }
        return false
    }

    /**
     * Checks if a given Anon / Publishable Key is a placeholder.
     */
    fun isPlaceholderKey(key: String): Boolean {
        val trimmed = key.trim().lowercase()
        if (trimmed.isBlank() || trimmed == "null" || trimmed == "none") return true
        if (trimmed.contains("your-supabase") ||
            trimmed.contains("sample_public") ||
            trimmed.contains("dummy") ||
            trimmed.contains("placeholder") ||
            trimmed.contains("pasand") ||
            trimmed.contains("flex69") ||
            trimmed.contains("your-key")
        ) {
            return true
        }
        // Valid Supabase anon JWT or publishable keys are at least 20 chars long
        return trimmed.length < 15
    }

    /**
     * Formats and validates the URL to ensure it has https:// scheme.
     */
    fun formatUrl(raw: String): String {
        var clean = raw.trim().removeSuffix("/")
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        return clean
    }

    /**
     * Safely masks the Supabase key for diagnostic display without leaking secret bytes.
     */
    fun maskKey(key: String): String {
        val trimmed = key.trim()
        if (trimmed.isBlank() || isPlaceholderKey(trimmed)) return "[Not Configured]"
        return if (trimmed.length > 10) {
            val start = trimmed.take(6)
            val end = trimmed.takeLast(4)
            "$start...$end (${trimmed.length} chars)"
        } else {
            "*** (${trimmed.length} chars)"
        }
    }

    /**
     * Retrieves the active Supabase Project URL.
     */
    fun getProjectUrl(context: Context? = null): String {
        // 1. Check SharedPreferences (user in-app override)
        if (context != null) {
            try {
                val saved = getPrefs(context).getString(KEY_PROJECT_URL, null)?.trim()?.removeSuffix("/")
                if (!saved.isNullOrBlank() && !isPlaceholderUrl(saved)) {
                    return formatUrl(saved)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error reading saved Supabase URL: ${e.message}")
            }
        }

        // 2. Check direct environment injection (BuildConfig.ENV_SUPABASE_URL)
        try {
            val envUrl = BuildConfig.ENV_SUPABASE_URL.trim().removeSuffix("/")
            if (envUrl.isNotBlank() && !isPlaceholderUrl(envUrl)) {
                return formatUrl(envUrl)
            }
        } catch (ignored: Throwable) {
        }

        // 3. Check Secrets Gradle Plugin (BuildConfig.SUPABASE_URL)
        try {
            val buildConfigUrl = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
            if (buildConfigUrl.isNotBlank() && !isPlaceholderUrl(buildConfigUrl)) {
                return formatUrl(buildConfigUrl)
            }
        } catch (ignored: Throwable) {
        }

        return ""
    }

    /**
     * Retrieves the active Supabase Anon / Publishable Public Key.
     * Supports both SUPABASE_PUBLISHABLE_KEY and SUPABASE_ANON_KEY naming conventions.
     */
    fun getAnonKey(context: Context? = null): String {
        // 1. Check SharedPreferences (user in-app override)
        if (context != null) {
            try {
                val saved = getPrefs(context).getString(KEY_ANON_KEY, null)?.trim()
                if (!saved.isNullOrBlank() && !isPlaceholderKey(saved)) {
                    return saved
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error reading saved Supabase key: ${e.message}")
            }
        }

        // 2. Check direct environment injection (BuildConfig.ENV_SUPABASE_KEY / PUBLISHABLE / ANON)
        val candidates = listOf(
            try { BuildConfig.ENV_SUPABASE_KEY } catch (e: Throwable) { "" },
            try { BuildConfig.ENV_SUPABASE_PUBLISHABLE_KEY } catch (e: Throwable) { "" },
            try { BuildConfig.ENV_SUPABASE_ANON_KEY } catch (e: Throwable) { "" },
            try { BuildConfig.SUPABASE_PUBLISHABLE_KEY } catch (e: Throwable) { "" },
            try { BuildConfig.SUPABASE_ANON_KEY } catch (e: Throwable) { "" }
        )

        for (candidate in candidates) {
            val trimmed = candidate.trim()
            if (trimmed.isNotBlank() && !isPlaceholderKey(trimmed)) {
                return trimmed
            }
        }

        return ""
    }

    /**
     * Checks if Supabase Storage has a valid, non-placeholder configuration.
     */
    fun isConfigured(context: Context? = null): Boolean {
        val url = getProjectUrl(context)
        val key = getAnonKey(context)
        return url.isNotBlank() && !isPlaceholderUrl(url) && key.isNotBlank() && !isPlaceholderKey(key)
    }

    /**
     * Returns full diagnostic status for development/UI info without exposing secret values.
     */
    fun getConfigStatus(context: Context? = null): ConfigStatus {
        val url = getProjectUrl(context)
        val key = getAnonKey(context)
        val hasUrl = url.isNotBlank() && !isPlaceholderUrl(url)
        val hasKey = key.isNotBlank() && !isPlaceholderKey(key)
        val configured = hasUrl && hasKey

        val keyType = when {
            !hasKey -> "Missing"
            key.startsWith("sb_publishable_") -> "Publishable Key"
            key.startsWith("ey") -> "JWT Anon Key"
            else -> "Public Client Key"
        }

        val msg = if (configured) {
            "Supabase Storage is ready (URL: $url, Key: $keyType)"
        } else {
            buildString {
                append("Supabase Storage is not ready: ")
                if (!hasUrl) append("[URL is missing] ")
                if (!hasKey) append("[Public key is missing] ")
            }
        }

        return ConfigStatus(
            isConfigured = configured,
            hasUrl = hasUrl,
            hasKey = hasKey,
            urlDisplay = if (hasUrl) url else "Not detected",
            keyTypeDisplay = keyType,
            keyMasked = maskKey(key),
            statusMessage = msg
        )
    }

    /**
     * Saves user-entered configuration to SharedPreferences.
     */
    fun saveConfig(context: Context, url: String, anonKey: String) {
        val cleanUrl = url.trim()
        val cleanKey = anonKey.trim()

        if (cleanUrl.isBlank() && cleanKey.isBlank()) {
            clearConfig(context)
            return
        }

        val formattedUrl = if (cleanUrl.isNotBlank()) formatUrl(cleanUrl) else ""
        getPrefs(context).edit()
            .putString(KEY_PROJECT_URL, formattedUrl)
            .putString(KEY_ANON_KEY, cleanKey)
            .apply()

        Log.d(TAG, "Saved Supabase configuration: URL=$formattedUrl, Key=${maskKey(cleanKey)}")
    }

    /**
     * Clears saved custom configuration from SharedPreferences so it reverts to environment secrets.
     */
    fun clearConfig(context: Context) {
        getPrefs(context).edit().clear().apply()
        Log.d(TAG, "Cleared custom Supabase storage preferences; reverted to environment secrets")
    }

    /**
     * Tests connection to Supabase Storage endpoint.
     */
    suspend fun testConnection(url: String, anonKey: String): Result<String> = withContext(Dispatchers.IO) {
        val formatted = formatUrl(url)
        if (isPlaceholderUrl(formatted)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid Supabase Project URL (e.g. https://xyz.supabase.co)"))
        }
        if (isPlaceholderKey(anonKey)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid Supabase Anon or Publishable Key"))
        }

        val testEndpoint = "$formatted/storage/v1/bucket"
        val request = Request.Builder()
            .url(testEndpoint)
            .addHeader("apikey", anonKey.trim())
            .addHeader("Authorization", "Bearer ${anonKey.trim()}")
            .get()
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                Result.success("Connected to Supabase Storage successfully!")
            } else if (response.code == 401 || response.code == 403) {
                Result.failure(IllegalStateException("Authentication failed (${response.code}). Please verify your anon/publishable key."))
            } else {
                Result.failure(IllegalStateException("Supabase returned status ${response.code}: $responseBody"))
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(IllegalStateException("Unable to resolve hostname for '$formatted'. Please double-check your Project URL."))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Connection failed: ${e.localizedMessage ?: e.message}"))
        }
    }
}
