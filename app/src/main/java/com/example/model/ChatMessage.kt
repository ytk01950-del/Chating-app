package com.example.model

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    FILE,
    DOCUMENT,
    ZIP,
    AUDIO
}

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val status: String = "sent", // "sent", "delivered", "seen"
    val deliveredAt: Long = 0L,
    val seenAt: Long = 0L,
    val reaction: String = "",
    val messageType: String = "TEXT",
    val fileName: String = "",
    val fileUrl: String = "",
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val thumbnailUrl: String = "",
    val durationSeconds: Int = 0,
    val isTemporary: Boolean = false,
    val viewedAt: Long = 0L,
    val expiresAt: Long = 0L,
    val isExpired: Boolean = false
) {
    fun isMediaExpired(): Boolean {
        if (isExpired) return true
        if (isTemporary && expiresAt > 0L && System.currentTimeMillis() >= expiresAt) return true
        return false
    }

    fun getRemainingExpirySeconds(): Int {
        if (!isTemporary || expiresAt <= 0L) return 0
        val remaining = expiresAt - System.currentTimeMillis()
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }
    fun getResolvedType(): MessageType {
        return try {
            MessageType.valueOf(messageType.uppercase())
        } catch (e: Exception) {
            MessageType.TEXT
        }
    }

    fun isMediaMessage(): Boolean {
        return getResolvedType() != MessageType.TEXT
    }

    fun getNotificationSummary(): String {
        return when (getResolvedType()) {
            MessageType.IMAGE -> if (text.isNotBlank()) "📷 $text" else "📷 Photo"
            MessageType.VIDEO -> if (text.isNotBlank()) "🎥 $text" else "🎥 Video"
            MessageType.DOCUMENT -> "📄 ${fileName.ifBlank { "Document" }}"
            MessageType.ZIP -> "📦 ${fileName.ifBlank { "ZIP archive" }}"
            MessageType.AUDIO -> "🎵 ${fileName.ifBlank { "Audio" }}"
            MessageType.FILE -> "📎 ${fileName.ifBlank { "File" }}"
            MessageType.TEXT -> text.ifBlank { "New message" }
        }
    }
}
