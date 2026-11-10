package com.collaboffice.core.network.api

import com.collaboffice.core.model.ApiResponse
import com.collaboffice.core.model.chat.*
import retrofit2.http.*

interface ChatApi {
    @GET("chat/conversations")
    suspend fun getSessions(): ApiResponse<List<ChatSession>>

    @GET("chat/messages/{sessionId}")
    suspend fun getHistory(
        @Path("sessionId") sessionId: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 50
    ): ApiResponse<List<ChatMessage>>

    @POST("chat/send")
    suspend fun sendMessage(
        @Query("sessionId") sessionId: String,
        @Query("content") content: String,
        @Query("msgType") msgType: Int = 1
    ): ApiResponse<Void>

    @POST("chat/recall/{msgId}")
    suspend fun recallMessage(@Path("msgId") msgId: String): ApiResponse<Void>

    @POST("chat/read/{sessionId}")
    suspend fun markRead(@Path("sessionId") sessionId: String): ApiResponse<Void>
}

