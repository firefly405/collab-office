package com.collaboffice.core.network.api

import com.collaboffice.core.model.ApiResponse
import com.collaboffice.core.model.user.SpaceInfo
import com.collaboffice.core.model.user.UserInfo
import retrofit2.http.*

interface SpaceApi {
    @GET("space/my")
    suspend fun getMySpaces(): ApiResponse<List<SpaceInfo>>

    @POST("space/create")
    suspend fun createSpace(@Body body: Map<String, String>): ApiResponse<SpaceInfo>

    @GET("space/{spaceId}")
    suspend fun getSpaceDetail(@Path("spaceId") spaceId: Long): ApiResponse<SpaceInfo>

    @GET("space/{spaceId}/members")
    suspend fun getSpaceMembers(@Path("spaceId") spaceId: Long): ApiResponse<List<UserInfo>>
}
