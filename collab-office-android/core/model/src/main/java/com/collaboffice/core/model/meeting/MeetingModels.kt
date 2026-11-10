package com.collaboffice.core.model.meeting

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MeetingRoom(
    val roomId: String = "",
    val title: String = "",
    val status: Int = 1,
    val creatorId: Long = 0,
    val creatorName: String? = null,
    val maxParticipants: Int = 50,
    val participantCount: Int = 0,
    val startTime: String? = null
)
