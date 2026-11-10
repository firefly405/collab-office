package com.collaboffice.core.model.user

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserInfo(
    val id: Long = 0,
    val username: String = "",
    val nickname: String? = null,
    val avatar: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val gender: Int = 0,
    val status: Int = 1,
    val onlineStatus: Int = 0,
    val roleId: Long? = null,
    val roleName: String? = null
)

@JsonClass(generateAdapter = true)
data class SpaceInfo(
    val id: Long = 0,
    val spaceName: String = "",
    val description: String? = null,
    val ownerId: Long = 0,
    val ownerName: String? = null,
    val storageUsed: Long = 0,
    val storageQuota: Long = 0,
    val memberCount: Int = 0
)
