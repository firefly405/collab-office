package com.collaboffice.core.network.api

import com.collaboffice.core.model.ApiResponse
import retrofit2.http.*

interface SearchApi {
    @GET("search")
    suspend fun search(
        @Query("keyword") keyword: String,
        @Query("type") type: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<Map<String, Any>>
}
