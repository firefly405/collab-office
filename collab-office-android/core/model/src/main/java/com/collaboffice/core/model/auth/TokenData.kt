package com.collaboffice.core.model.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenData(
    val token: String,
    @Json(name = "refreshToken") val refreshToken: String? = null,
    @Json(name = "accessToken") val accessToken: String? = null
) {
    fun getEffectiveToken(): String = accessToken ?: token
}

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val account: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val username: String,
    val phone: String,
    val password: String,
    val smsCode: String
)

@JsonClass(generateAdapter = true)
data class SmsCodeRequest(val phone: String)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(val refreshToken: String)
