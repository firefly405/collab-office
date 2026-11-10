package com.collaboffice.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val sessionId: String,
    val name: String = "",
    val type: Int = 1,
    val avatar: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val unreadCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    indices = [Index("sessionId", "seq")]
)
data class ChatMessageEntity(
    @PrimaryKey val msgId: String,
    val sessionId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String? = null,
    val content: String = "",
    val contentType: Int = 1,
    val seq: Long = 0,
    val sendTime: Long = 0,
    val status: Int = 0,  // 0=发送中 1=已发送 2=已读 3=已撤回
    val replyToId: String? = null,
    val replyToName: String? = null,
    val replyToContent: String? = null,
    val extra: String? = null
)
