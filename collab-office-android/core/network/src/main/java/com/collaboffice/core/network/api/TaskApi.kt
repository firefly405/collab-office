package com.collaboffice.core.network.api

import com.collaboffice.core.model.ApiResponse
import com.collaboffice.core.model.task.*
import retrofit2.http.*

interface TaskApi {
    @GET("task/list")
    suspend fun getTasks(
        @Query("projectId") projectId: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<List<Task>>

    @POST("task/create")
    suspend fun createTask(
        @Query("title") title: String,
        @Query("projectId") projectId: String? = null
    ): ApiResponse<String>

    @PUT("task/{taskId}/status")
    suspend fun updateStatus(
        @Path("taskId") taskId: String,
        @Query("status") status: Int
    ): ApiResponse<Unit>

    @GET("task/projects")
    suspend fun getProjects(@Query("spaceId") spaceId: Long? = null): ApiResponse<List<TaskProject>>

    @GET("task/kanban")
    suspend fun getKanban(
        @Query("projectId") projectId: String? = null
    ): ApiResponse<Map<String, List<Task>>>

    @GET("task/{taskId}")
    suspend fun getTaskDetail(
        @Path("taskId") taskId: String
    ): ApiResponse<TaskDetail>

    @PUT("task/{taskId}")
    suspend fun updateTask(
        @Path("taskId") taskId: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): ApiResponse<Unit>

    @POST("task/{taskId}/comment")
    suspend fun addComment(
        @Path("taskId") taskId: String,
        @Body body: Map<String, String>
    ): ApiResponse<Unit>
}
