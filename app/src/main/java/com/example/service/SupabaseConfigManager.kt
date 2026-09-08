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
 * Manages Supabase Project URL and Anon Public Key configuration.
 *
 * Precedence:
 * 1. User-configured values saved in SharedPreferences (via in-app dialog)
 * 2. BuildConfig / .env injected values (SUPABASE_URL, SUPABASE_ANON_KEY)
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

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if a given URL is a placeholder or invalid.
     */
    fun isPlaceholderUrl(url: String): Boolean {
        val trimmed = url.trim().lowercase()
        return trimmed.isBlank() ||
                trimmed.contains("your-project-ref") ||
                trimmed.contains("chating-free-storage") ||
                trimmed.contains("sample") ||
                trimmed.contains("example.com") ||
                trimmed.contains("pasand") ||
                trimmed.contains("flex69") ||
                trimmed.contains("placeholder") ||
                trimmed.contains("dummy") ||
                trimmed == "none"
    }

    /**
     * Checks if a given Anon Key is a placeholder.
     */
    fun isPlaceholderKey(key: String): Boolean {
        val trimmed = key.trim().lowercase()
        return trimmed.isBlank() ||
                trimmed.contains("your-supabase-anon-key") ||
                trimmed.contains("sample_public_anon_key") ||
                trimmed.contains("placeholder") ||
                trimmed.contains("pasand") ||
                trimmed.contains("flex69") ||
                trimmed.contains("dummy") ||
                trimmed.contains("your-key") ||
                trimmed == "none"
    }

    /**
     * Retrieves the active Supabase Project URL.
     */
    fun getProjectUrl(context: Context?): String {
        // 1. Check SharedPreferences if context is available (user in-app override)
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
        return try {
            val buildConfigUrl = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
            if (buildConfigUrl.isNotBlank() && !isPlaceholderUrl(buildConfigUrl)) {
                formatUrl(buildConfigUrl)
            } else {
                ""
            }
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * Retrieves the active Supabase Anon Public Key.
     */
    fun getAnonKey(context: Context?): String {
        // 1. Check SharedPreferences (user in-app override)
        if (context != null) {
            try {
                val saved = getPrefs(context).getString(KEY_ANON_KEY, null)?.trim()
                if (!saved.isNullOrBlank() && !isPlaceholderKey(saved)) {
                    return saved
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error reading saved Supabase anon key: ${e.message}")
            }
        }

        // 2. Check direct environment injection (BuildConfig.ENV_SUPABASE_ANON_KEY)
        try {
            val envKey = BuildConfig.ENV_SUPABASE_ANON_KEY.trim()
            if (envKey.isNotBlank() && !isPlaceholderKey(envKey)) {
                return envKey
            }
        } catch (ignored: Throwable) {
        }

        // 3. Check Secrets Gradle Plugin (BuildConfig.SUPABASE_ANON_KEY)
        return try {
            val buildConfigKey = BuildConfig.SUPABASE_ANON_KEY.trim()
            if (buildConfigKey.isNotBlank() && !isPlaceholderKey(buildConfigKey)) {
                buildConfigKey
            } else {
                ""
            }
        } catch (e: Throwable) {
            ""
        }
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
     * Checks if Supabase Storage has a valid, non-placeholder configuration.
     */
    fun isConfigured(context: Context?): Boolean {
        val url = getProjectUrl(context)
        val key = getAnonKey(context)
        return url.isNotBlank() && !isPlaceholderUrl(url) && key.isNotBlank() && !isPlaceholderKey(key)
    }

    /**
     * Saves user-entered configuration to SharedPreferences.
     */
    fun saveConfig(context: Context, url: String, anonKey: String) {
        val formattedUrl = formatUrl(url)
        getPrefs(context).edit()
            .putString(KEY_PROJECT_URL, formattedUrl)
            .putString(KEY_ANON_KEY, anonKey.trim())
            .apply()
        Log.d(TAG, "Saved Supabase configuration: URL=$formattedUrl")
    }

    /**
     * Clears saved custom configuration.
     */
    fun clearConfig(context: Context) {
        getPrefs(context).edit().clear().apply()
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
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid Supabase anon/public key"))
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
                Result.failure(IllegalStateException("Authentication failed (${response.code}). Please verify your anon/public key."))
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
