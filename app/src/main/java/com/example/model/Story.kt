package com.example.model

data class Story(
    val storyId: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val userUsername: String = "",
    val userPhotoUrl: String = "",
    val mediaUrl: String = "",
    val storagePath: String = "",
    val mediaType: String = "image", // "image" or "video"
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
    val viewers: Map<String, Long> = emptyMap(), // userId -> timestamp
    val status: String = "active" // "active" or "expired"
) {
    val isExpired: Boolean
        get() = status == "expired" || System.currentTimeMillis() >= expiresAt

    fun getRemainingTimeFormatted(): String {
        val remaining = expiresAt - System.currentTimeMillis()
        if (remaining <= 0) return "Expired"
        val hours = remaining / (1000 * 60 * 60)
        val minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60)
        return when {
            hours > 0 -> "${hours}h ${minutes}m left"
            minutes > 0 -> "${minutes}m left"
            else -> "<1m left"
        }
    }

    fun getTimeAgoFormatted(): String {
        val diff = System.currentTimeMillis() - createdAt
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> "Today"
        }
    }
}

data class UserStoryGroup(
    val user: User,
    val stories: List<Story> = emptyList(),
    val hasUnseenStories: Boolean = true
)
