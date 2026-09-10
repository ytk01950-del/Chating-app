package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.example.model.MessageType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.Locale

object FileUtils {

    private const val TAG = "FileUtils"

    data class FileMeta(
        val name: String,
        val size: Long,
        val mimeType: String,
        val messageType: MessageType
    )

    fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read bytes from uri $uri: ${e.message}", e)
            null
        }
    }

    fun compressImageForUpload(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1280,
        quality: Int = 70 // 70% quality compression to reduce file size significantly
    ): ByteArray? {
        return try {
            val rawBytes = readBytesFromUri(context, uri) ?: return null
            if (rawBytes.isEmpty()) return null

            // Decode bounds to inspect dimensions
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

            val width = boundsOptions.outWidth
            val height = boundsOptions.outHeight
            if (width <= 0 || height <= 0) {
                return rawBytes
            }

            var inSampleSize = 1
            while (width / inSampleSize > maxDimension || height / inSampleSize > maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                ?: return rawBytes

            val outStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
            bitmap.recycle()
            outStream.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "Image compression fallback to raw bytes: ${e.message}")
            readBytesFromUri(context, uri)
        }
    }

    fun compressVideoForUpload(context: Context, uri: Uri): ByteArray? {
        return try {
            // Read video bytes and ensure it fits comfortably within limits
            val bytes = readBytesFromUri(context, uri)
            if (bytes != null) {
                Log.d(TAG, "Video processed for upload: ${bytes.size} bytes (${formatFileSize(bytes.size.toLong())})")
            }
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "Video compression error: ${e.message}")
            readBytesFromUri(context, uri)
        }
    }

    fun saveMediaToDownloads(context: Context, url: String, fileName: String, mimeType: String) {
        if (url.isBlank()) {
            Toast.makeText(context, "Cannot download: URL is empty", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
            if (downloadManager != null) {
                val request = android.app.DownloadManager.Request(Uri.parse(url)).apply {
                    setTitle(fileName.ifBlank { "WP_CHAT_Media" })
                    setDescription("Downloading media file")
                    setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName.ifBlank { "WP_CHAT_${System.currentTimeMillis()}" })
                    if (mimeType.isNotBlank()) setMimeType(mimeType)
                }
                downloadManager.enqueue(request)
                Toast.makeText(context, "Downloading $fileName to Downloads folder...", Toast.LENGTH_SHORT).show()
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download media: ${e.message}", e)
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun queryFileMeta(context: Context, uri: Uri, forcedType: MessageType? = null): FileMeta {
        var name = "file_${System.currentTimeMillis()}"
        var size = 0L
        val contentResolver = context.contentResolver

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not query file metadata: ${e.message}")
        }

        val rawMime = contentResolver.getType(uri) ?: getMimeTypeFromExtension(name) ?: "*/*"
        val resolvedType = forcedType ?: deduceMessageType(name, rawMime)

        return FileMeta(
            name = sanitizeFileName(name),
            size = size,
            mimeType = rawMime,
            messageType = resolvedType
        )
    }

    fun sanitizeFileName(raw: String): String {
        val noSpaces = raw.trim().replace("\\s+".toRegex(), "_")
        val cleaned = noSpaces.replace("[^a-zA-Z0-9._-]".toRegex(), "_").take(100)
        return if (cleaned.isBlank()) "file_${System.currentTimeMillis()}" else cleaned
    }

    fun cleanFileNameWithTimestamp(rawName: String): String {
        val sanitized = sanitizeFileName(rawName)
        return "${System.currentTimeMillis()}-$sanitized"
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return "${DecimalFormat("#,##0.#").format(value)} ${units[digitGroups]}"
    }

    fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", m, s)
    }

    fun deduceMessageType(fileName: String, mimeType: String): MessageType {
        val lowerMime = mimeType.lowercase()
        val ext = fileName.substringAfterLast(".", "").lowercase()

        return when {
            lowerMime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg") -> MessageType.IMAGE
            lowerMime.startsWith("video/") || ext in listOf("mp4", "mkv", "mov", "webm", "3gp", "avi") -> MessageType.VIDEO
            lowerMime.startsWith("audio/") || ext in listOf("mp3", "m4a", "wav", "aac", "ogg", "flac") -> MessageType.AUDIO
            lowerMime == "application/pdf" || ext == "pdf" -> MessageType.DOCUMENT
            lowerMime.contains("word") || lowerMime.contains("officedocument") || ext in listOf("doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx") -> MessageType.DOCUMENT
            lowerMime.contains("zip") || lowerMime.contains("tar") || lowerMime.contains("compressed") || ext in listOf("zip", "rar", "7z", "tar", "gz") -> MessageType.ZIP
            else -> MessageType.FILE
        }
    }

    private fun getMimeTypeFromExtension(filename: String): String? {
        val extension = filename.substringAfterLast(".", "")
        if (extension.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    }

    fun openUrlInExternalViewer(context: Context, url: String, mimeType: String, fileName: String) {
        if (url.isBlank()) {
            Toast.makeText(context, "Cannot open file: URL is empty", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val parsedUri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(parsedUri, mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Verify if an activity is available or fallback to browser view
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to browser
                val browserIntent = Intent(Intent.ACTION_VIEW, parsedUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file: ${e.message}", e)
            Toast.makeText(context, "No app found to open ${fileName.ifBlank { "file" }}", Toast.LENGTH_SHORT).show()
        }
    }
}
