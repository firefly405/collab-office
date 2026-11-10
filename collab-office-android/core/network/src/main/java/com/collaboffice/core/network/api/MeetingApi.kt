package com.collaboffice.core.network.api

import com.collaboffice.core.model.ApiResponse
import com.collaboffice.core.model.meeting.MeetingRoom
import retrofit2.http.*

interface MeetingApi {
    @POST("meeting/create")
    suspend fun createMeeting(@Body body: Map<String, Any>): ApiResponse<MeetingRoom>

    @GET("meeting/list")
    suspend fun getMeetings(): ApiResponse<List<MeetingRoom>>

    @POST("meeting/{roomId}/join")
    suspend fun joinMeeting(@Path("roomId") roomId: String): ApiResponse<Unit>

    @POST("meeting/{roomId}/leave")
    suspend fun leaveMeeting(@Path("roomId") roomId: String): ApiResponse<Unit>

    @POST("meeting/{roomId}/raise-hand")
    suspend fun raiseHand(
        @Path("roomId") roomId: String,
        @Query("isRaised") isRaised: Boolean
    ): ApiResponse<Unit>
}
