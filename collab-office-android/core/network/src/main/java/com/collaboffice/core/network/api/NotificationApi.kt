package com.collaboffice.core.network.api

import com.collaboffice.core.model.ApiResponse
import retrofit2.http.*

interface NotificationApi {
    @GET("notification/list")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<List<Map<String, Any>>>

    @PUT("notification/{id}/read")
    suspend fun markAsRead(@Path("id") id: Long): ApiResponse<Unit>

    @PUT("notification/read-all")
    suspend fun markAllAsRead(): ApiResponse<Unit>
}
