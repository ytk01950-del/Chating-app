package com.example.model

data class ChatSummary(
    val chatId: String,
    val otherUser: User,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0,
    val isOtherUserTyping: Boolean = false
)
