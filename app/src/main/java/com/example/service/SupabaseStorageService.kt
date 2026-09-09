package com.example.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Centralized Service for uploading and managing media files in Supabase Storage.
 *
 * Bucket Layout:
 * - Chat media:     bucket "chat-media",     path "{chatId}/{messageId}/{fileName}"
 * - Profile photos: bucket "profile-photos", path "{userId}/avatar.jpg"
 * - Stories media:  bucket "stories" (or "chat-media"), path "stories/{userId}/{storyId}/story.{ext}"
 *
 * Free Tier limits: max 50 MB per file.
 */
object SupabaseStorageService {

    private const val TAG = "SupabaseStorageService"
    const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024L // 50 MB max limit

    // Standard bucket names
    const val BUCKET_CHAT_MEDIA = "chat-media"
    const val BUCKET_PROFILE_PHOTOS = "profile-photos"
    const val BUCKET_STORIES = "stories"
    const val BUCKET_POSTS = "posts"

    data class StoryMediaUploadResult(
        val publicUrl: String,
        val storagePath: String,
        val bucket: String
    )

    data class PostMediaUploadResult(
        val publicUrl: String,
        val storagePath: String,
        val bucket: String
    )

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves the configured Supabase Project URL.
     */
    fun getSupabaseUrl(context: Context? = null): String {
        return SupabaseConfigManager.getProjectUrl(context)
    }

    /**
     * Resolves the configured Supabase Public / Anon / Publishable API Key.
     */
    fun getSupabaseAnonKey(context: Context? = null): String {
        return SupabaseConfigManager.getAnonKey(context)
    }

    /**
     * Checks if Supabase Storage is configured.
     */
    fun isConfigured(context: Context? = null): Boolean {
        return SupabaseConfigManager.isConfigured(context)
    }

    /**
     * Constructs the public download URL for an object in a Supabase public bucket.
     */
    fun getPublicUrl(bucket: String, path: String, context: Context? = null): String {
        val baseUrl = getSupabaseUrl(context).removeSuffix("/")
        val cleanPath = path.trimStart('/')
        return "$baseUrl/storage/v1/object/public/$bucket/$cleanPath"
    }

    /**
     * Uploads bytes to a Supabase storage bucket at the given path.
     * Uses POST with x-upsert: true to allow overwrite/updates.
     */
    suspend fun uploadFile(
        bucket: String,
        path: String,
        bytes: ByteArray,
        mimeType: String,
        context: Context? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfigManager.isConfigured(context)) {
            val status = SupabaseConfigManager.getConfigStatus(context)
            val errorMsg = buildString {
                append("Supabase Storage is not configured. ")
                if (!status.hasUrl) append("Supabase Project URL is missing. ")
                if (!status.hasKey) append("Supabase Public/Anon key is missing. ")
                append("Please configure SUPABASE_URL and SUPABASE_ANON_KEY (or SUPABASE_PUBLISHABLE_KEY) in AI Studio Secrets or via Storage Settings.")
            }
            Log.e(TAG, errorMsg)
            return@withContext Result.failure(IllegalStateException(errorMsg))
        }

        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            val sizeMb = String.format(java.util.Locale.US, "%.1f", bytes.size.toDouble() / (1024 * 1024))
            return@withContext Result.failure(
                IllegalArgumentException("File size ($sizeMb MB) exceeds the 50 MB upload limit.")
            )
        }

        if (bytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Cannot upload empty file."))
        }

        val baseUrl = getSupabaseUrl(context).removeSuffix("/")
        val anonKey = getSupabaseAnonKey(context)
        val cleanPath = path.trimStart('/')

        // Endpoint: POST /storage/v1/object/{bucket}/{path}
        val uploadUrl = "$baseUrl/storage/v1/object/$bucket/$cleanPath"
        val publicUrl = getPublicUrl(bucket, cleanPath, context)

        val mediaType = (if (mimeType.isNotBlank()) mimeType else "application/octet-stream").toMediaTypeOrNull()

        val progressBody = ProgressRequestBody(bytes, mediaType) { progress ->
            onProgress(progress)
        }

        val request = Request.Builder()
            .url(uploadUrl)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("x-upsert", "true")
            .addHeader("Content-Type", mimeType.ifBlank { "application/octet-stream" })
            .post(progressBody)
            .build()

        Log.d(TAG, "Uploading ${bytes.size} bytes to Supabase Storage: $uploadUrl (Content-Type: $mimeType, Key: ${SupabaseConfigManager.maskKey(anonKey)})")

        try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                Log.d(TAG, "Supabase upload succeeded for $bucket/$cleanPath. Public URL: $publicUrl")
                onProgress(1f)
                Result.success(publicUrl)
            } else {
                val errorMsg = "Supabase Storage upload failed (${response.code}): $responseBody"
                Log.e(TAG, errorMsg)

                if (response.code == 404 && responseBody.contains("Bucket not found", ignoreCase = true)) {
                    Result.failure(
                        IllegalStateException("Supabase bucket '$bucket' was not found. Please create public bucket '$bucket' in your Supabase dashboard.")
                    )
                } else if (response.code == 403 || response.code == 401) {
                    Result.failure(
                        IllegalStateException("Supabase Storage permission denied (${response.code}). Please ensure bucket '$bucket' is Public and has INSERT/UPDATE policies enabled.")
                    )
                } else {
                    Result.failure(IllegalStateException(errorMsg))
                }
            }
        } catch (e: UnknownHostException) {
            val error = "Unable to resolve Supabase hostname for '$baseUrl'. Please check your Supabase Project URL."
            Log.e(TAG, error, e)
            Result.failure(IllegalStateException(error))
        } catch (e: Exception) {
            Log.e(TAG, "Network exception during Supabase upload to $uploadUrl: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads Chat Media (Image, Video, Audio, Document, Zip):
     * Bucket: "chat-media"
     * Path: chat-media/{chatId}/{messageId}/{fileName}
     */
    suspend fun uploadChatMedia(
        chatId: String,
        messageId: String,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String,
        context: Context? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        val sanitized = FileUtils.sanitizeFileName(fileName)
        val path = "$chatId/$messageId/$sanitized"
        return uploadFile(
            bucket = BUCKET_CHAT_MEDIA,
            path = path,
            bytes = fileBytes,
            mimeType = mimeType,
            context = context,
            onProgress = onProgress
        )
    }

    /**
     * Uploads a Profile Photo:
     * Bucket: "profile-photos"
     * Path: profile-photos/{userId}/avatar.jpg
     */
    suspend fun uploadProfilePhoto(
        userId: String,
        imageUri: Uri,
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        val imageBytes = FileUtils.compressImageForUpload(context, imageUri, maxDimension = 1080, quality = 85)
            ?: return Result.failure(IllegalStateException("Unable to read selected photo"))

        val path = "$userId/avatar.jpg"
        return uploadFile(
            bucket = BUCKET_PROFILE_PHOTOS,
            path = path,
            bytes = imageBytes,
            mimeType = "image/jpeg",
            context = context,
            onProgress = onProgress
        )
    }

    /**
     * Uploads Story Media (Photo or Video) for the 24-Hour Story system:
     * Attempts bucket "stories" first; gracefully falls back to "chat-media" if "stories" bucket doesn't exist.
     * Path: stories/{userId}/{storyId}/story.{ext}
     */
    suspend fun uploadStoryMedia(
        userId: String,
        storyId: String,
        uri: Uri,
        isVideo: Boolean,
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): Result<StoryMediaUploadResult> {
        val extension = if (isVideo) "mp4" else "jpg"
        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
        val path = "stories/$userId/$storyId/story.$extension"

        val bytes = if (isVideo) {
            FileUtils.readBytesFromUri(context, uri)
                ?: return Result.failure(IllegalStateException("Unable to read selected video file"))
        } else {
            FileUtils.compressImageForUpload(context, uri, maxDimension = 1920, quality = 88)
                ?: return Result.failure(IllegalStateException("Unable to process selected photo"))
        }

        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            return Result.failure(IllegalStateException("Story media exceeds 50MB limit (${FileUtils.formatFileSize(bytes.size.toLong())})"))
        }

        // Try primary bucket BUCKET_STORIES
        val primaryResult = uploadFile(
            bucket = BUCKET_STORIES,
            path = path,
            bytes = bytes,
            mimeType = mimeType,
            context = context,
            onProgress = onProgress
        )

        if (primaryResult.isSuccess) {
            val url = primaryResult.getOrThrow()
            return Result.success(StoryMediaUploadResult(url, path, BUCKET_STORIES))
        }

        // If bucket not found or forbidden, fallback to existing BUCKET_CHAT_MEDIA
        Log.w(TAG, "Stories bucket upload fallback to chat-media: ${primaryResult.exceptionOrNull()?.message}")
        val fallbackResult = uploadFile(
            bucket = BUCKET_CHAT_MEDIA,
            path = path,
            bytes = bytes,
            mimeType = mimeType,
            context = context,
            onProgress = onProgress
        )

        return fallbackResult.map { url ->
            StoryMediaUploadResult(url, path, BUCKET_CHAT_MEDIA)
        }
    }

    /**
     * Uploads Post Media (Photo or Video) for the Feed Post system:
     * Attempts bucket "posts" first; gracefully falls back to "chat-media" if "posts" bucket doesn't exist.
     * Path: posts/{userId}/{postId}/post.{ext}
     */
    suspend fun uploadPostMedia(
        userId: String,
        postId: String,
        uri: Uri,
        isVideo: Boolean,
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): Result<PostMediaUploadResult> {
        val extension = if (isVideo) "mp4" else "jpg"
        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
        val path = "posts/$userId/$postId/post.$extension"

        val bytes = if (isVideo) {
            FileUtils.readBytesFromUri(context, uri)
                ?: return Result.failure(IllegalStateException("Unable to read selected video file"))
        } else {
            FileUtils.compressImageForUpload(context, uri, maxDimension = 1920, quality = 88)
                ?: return Result.failure(IllegalStateException("Unable to process selected photo"))
        }

        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            return Result.failure(IllegalStateException("Post media exceeds 50MB limit (${FileUtils.formatFileSize(bytes.size.toLong())})"))
        }

        // Try primary bucket BUCKET_POSTS
        val primaryResult = uploadFile(
            bucket = BUCKET_POSTS,
            path = path,
            bytes = bytes,
            mimeType = mimeType,
            context = context,
            onProgress = onProgress
        )

        if (primaryResult.isSuccess) {
            val url = primaryResult.getOrThrow()
            return Result.success(PostMediaUploadResult(url, path, BUCKET_POSTS))
        }

        // If bucket not found or forbidden, fallback to existing BUCKET_CHAT_MEDIA
        Log.w(TAG, "Posts bucket upload fallback to chat-media: ${primaryResult.exceptionOrNull()?.message}")
        val fallbackResult = uploadFile(
            bucket = BUCKET_CHAT_MEDIA,
            path = path,
            bytes = bytes,
            mimeType = mimeType,
            context = context,
            onProgress = onProgress
        )

        return fallbackResult.map { url ->
            PostMediaUploadResult(url, path, BUCKET_CHAT_MEDIA)
        }
    }

    /**
     * Deletes a file from Supabase Storage by its public URL.
     */
    suspend fun deleteFileByUrl(publicUrl: String, context: Context? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!publicUrl.contains("/storage/v1/object/public/")) {
                return@withContext Result.success(Unit) // Not a Supabase URL, ignore
            }

            val parts = publicUrl.substringAfter("/storage/v1/object/public/").split("/", limit = 2)
            if (parts.size < 2) return@withContext Result.success(Unit)

            val bucket = parts[0]
            val path = parts[1]

            val baseUrl = getSupabaseUrl(context).removeSuffix("/")
            val anonKey = getSupabaseAnonKey(context)
            if (baseUrl.isBlank() || anonKey.isBlank()) return@withContext Result.success(Unit)

            val deleteUrl = "$baseUrl/storage/v1/object/$bucket/$path"

            val request = Request.Builder()
                .url(deleteUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .delete()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "Successfully deleted file from Supabase: $bucket/$path")
            } else {
                Log.w(TAG, "Supabase file delete notice (${response.code}): ${response.body?.string()}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Supabase delete file error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Custom RequestBody for reporting upload progress.
     */
    private class ProgressRequestBody(
        private val data: ByteArray,
        private val mediaType: okhttp3.MediaType?,
        private val onProgress: (Float) -> Unit
    ) : RequestBody() {

        override fun contentType(): okhttp3.MediaType? = mediaType

        override fun contentLength(): Long = data.size.toLong()

        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink) {
            val totalBytes = data.size.toLong()
            if (totalBytes <= 0) {
                onProgress(1f)
                return
            }

            val bufferSize = 8192
            var bytesWritten = 0L

            var offset = 0
            while (offset < data.size) {
                val chunkSize = Math.min(bufferSize, data.size - offset)
                sink.write(data, offset, chunkSize)
                sink.flush()
                bytesWritten += chunkSize
                offset += chunkSize
                val progress = (bytesWritten.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                onProgress(progress)
            }
        }
    }
}
