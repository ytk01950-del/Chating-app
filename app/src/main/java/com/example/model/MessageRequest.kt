package com.example.model

data class MessageRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromUser: User = User(),
    val initialMessage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "pending" // "pending", "accepted", "declined", "blocked"
)

data class UserReport(
    val reportId: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reportedUserId: String = "",
    val reportedUserName: String = "",
    val reason: String = "",
    val details: String = "",
    val contentSnippet: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
