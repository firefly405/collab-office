package com.collaboffice.core.network.api

import com.collaboffice.core.model.ApiResponse
import com.collaboffice.core.model.auth.*
import retrofit2.http.*

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<TokenData>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<TokenData>

    @POST("auth/send-sms-code")
    suspend fun sendSmsCode(@Body body: SmsCodeRequest): ApiResponse<Unit>

    @POST("auth/refresh-token")
    suspend fun refreshToken(@Body body: RefreshTokenRequest): ApiResponse<TokenData>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Unit>
}
