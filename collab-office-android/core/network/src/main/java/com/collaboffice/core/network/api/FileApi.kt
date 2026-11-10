package com.collaboffice.core.network.api

import com.collaboffice.core.model.ApiResponse
import com.collaboffice.core.model.file.FileItem
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface FileApi {
    @GET("file/list")
    suspend fun getFiles(
        @Query("spaceId") spaceId: Long,
        @Query("folderId") folderId: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<List<FileItem>>
    @Multipart
    @POST("file/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("folderId") folderId: RequestBody? = null
    ): ApiResponse<Map<String, @JvmSuppressWildcards Any>>
    @GET("file/download/{fileId}")
    suspend fun getDownloadUrl(@Path("fileId") fileId: String): ApiResponse<Map<String, String>>

    @DELETE("file/{fileId}")
    suspend fun deleteFile(@Path("fileId") fileId: String): ApiResponse<Unit>
}

