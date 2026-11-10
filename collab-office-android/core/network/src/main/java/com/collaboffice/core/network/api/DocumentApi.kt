package com.collaboffice.core.network.api

import com.collaboffice.core.model.ApiResponse
import com.collaboffice.core.model.doc.*
import retrofit2.http.*

interface DocumentApi {
    @GET("document/list")
    suspend fun getDocuments(
        @Query("spaceId") spaceId: Long? = null,
        @Query("folderId") folderId: String? = null,
        @Query("type") type: Int? = null
    ): ApiResponse<List<Document>>

    @POST("document/create")
    suspend fun createDocument(@Body body: Map<String, Any>): ApiResponse<Document>

    @GET("document/{docId}/content")
    suspend fun getContent(@Path("docId") docId: String): ApiResponse<String>

    @GET("document/{docId}")
    suspend fun getDocumentDetail(@Path("docId") docId: String): ApiResponse<Document>

    @PUT("document/{docId}/content")
    suspend fun saveContent(@Path("docId") docId: String, @Body body: Map<String, String>): ApiResponse<Unit>

    @GET("document/{docId}/versions")
    suspend fun getVersions(@Path("docId") docId: String): ApiResponse<List<DocVersion>>
}
