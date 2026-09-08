package com.example.model

data class Post(
    val postId: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val userUsername: String = "",
    val userPhotoUrl: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
