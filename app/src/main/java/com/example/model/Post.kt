package com.example.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val userUsername: String = "",
    val userPhotoUrl: String = "",
    val mediaUrl: String = "",
    val storagePath: String = "",
    val mediaType: String = "image", // "image" or "video"
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val likes: Map<String, Boolean> = emptyMap() // userId -> true
) {
    fun isLikedBy(userId: String): Boolean {
        return likes[userId] == true
    }

    fun getTimeAgoFormatted(): String {
        val diff = System.currentTimeMillis() - createdAt
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 7 * 86400_000 -> "${diff / 86400_000}d ago"
            else -> "Recently"
        }
    }
}
