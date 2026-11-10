package com.collaboffice.core.model.ws

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebSocketMessage(
    val type: String,
    val roomId: String? = null,
    val userId: String? = null,
    val clientTimestamp: Long? = null,
    val serverTimestamp: Long? = null,
    val seq: Long? = null,
    val data: Map<String, Any?>? = null
)
