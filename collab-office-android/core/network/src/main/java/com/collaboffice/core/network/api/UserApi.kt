package com.collaboffice.core.network.api

import com.collaboffice.core.model.ApiResponse
import com.collaboffice.core.model.user.UserInfo
import retrofit2.http.*

interface UserApi {
    @GET("user/info")
    suspend fun getUserInfo(): ApiResponse<UserInfo>

    @PUT("user/update")
    suspend fun updateProfile(@Body body: Map<String, Any>): ApiResponse<UserInfo>

    @POST("user/avatar")
    suspend fun updateAvatar(@Body body: Map<String, String>): ApiResponse<String>

    @PUT("user/password")
    suspend fun changePassword(@Body body: Map<String, String>): ApiResponse<Unit>
}
