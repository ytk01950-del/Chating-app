package com.example.model

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarId: Int = 0,
    val photoUrl: String = "",
    val bio: String = "",
    val statusMessage: String = "Hey there! I am using WP CHAT.",
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val postsCount: Int = 0
) {
    fun getInitials(): String {
        val names = displayName.trim().split(" ").filter { it.isNotBlank() }
        return when {
            names.size >= 2 -> "${names[0].take(1)}${names[1].take(1)}".uppercase()
            names.isNotEmpty() -> names[0].take(2).uppercase()
            username.isNotEmpty() -> username.take(2).uppercase()
            email.isNotEmpty() -> email.take(2).uppercase()
            else -> "??"
        }
    }
}
