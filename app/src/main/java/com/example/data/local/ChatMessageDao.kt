package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE (chatId = :chatId AND chatId != '') OR (senderId = :userId1 AND receiverId = :userId2) OR (senderId = :userId2 AND receiverId = :userId1) ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String, userId1: String, userId2: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE (chatId = :chatId AND chatId != '') OR (senderId = :userId1 AND receiverId = :userId2) OR (senderId = :userId2 AND receiverId = :userId1) ORDER BY timestamp ASC")
    suspend fun getMessagesForChatDirect(chatId: String, userId1: String, userId2: String): List<ChatMessage>

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesByChatIdDirect(chatId: String): List<ChatMessage>

    @Query("SELECT * FROM chat_messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): ChatMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>)

    @Update
    suspend fun updateMessage(message: ChatMessage)

    @Query("UPDATE chat_messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE chat_messages SET isRead = 1, status = 'seen', seenAt = :seenAt WHERE ((chatId = :chatId AND chatId != '') OR (receiverId = :currentUserId AND senderId = :otherUserId)) AND receiverId = :currentUserId")
    suspend fun markMessagesRead(chatId: String, currentUserId: String, otherUserId: String, seenAt: Long = System.currentTimeMillis())

    @Query("UPDATE chat_messages SET status = 'delivered', deliveredAt = :deliveredAt WHERE ((chatId = :chatId AND chatId != '') OR (receiverId = :currentUserId AND senderId = :otherUserId)) AND receiverId = :currentUserId AND status = 'sent'")
    suspend fun markMessagesDelivered(chatId: String, currentUserId: String, otherUserId: String, deliveredAt: Long = System.currentTimeMillis())

    @Query("UPDATE chat_messages SET reaction = :reaction WHERE id = :messageId")
    suspend fun updateMessageReaction(messageId: String, reaction: String)

    @Query("UPDATE chat_messages SET isExpired = 1, fileUrl = '', thumbnailUrl = '' WHERE id = :messageId")
    suspend fun markMediaExpired(messageId: String)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM chat_messages WHERE chatId = :chatId")
    suspend fun clearMessagesForChat(chatId: String)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>
}
