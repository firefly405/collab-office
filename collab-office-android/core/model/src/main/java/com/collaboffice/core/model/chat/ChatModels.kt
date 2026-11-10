package com.collaboffice.core.model.chat

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatSession(
    val sessionId: String = "",
    val name: String = "",
    val type: Int = 1,   // 1=单聊 2=群聊
    val avatar: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val unreadCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val msgId: String = "",
    val sessionId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String? = null,
    val content: String = "",
    val contentType: Int = 1,  // 1=文本 2=图片 3=文件 4=语音 5=位置
    val seq: Long = 0,
    val sendTime: Long = 0,
    val isRead: Boolean = false,
    val isRecalled: Boolean = false,
    val replyToId: String? = null,
    val replyToName: String? = null,
    val replyToContent: String? = null,
    val extra: String? = null
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    val sessionId: String,
    val content: String,
    val contentType: Int = 1
)
