package com.collaboffice.core.model.file

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FileItem(
    val fileId: String = "",
    val fileName: String = "",
    val fileType: String? = null,
    val fileSize: Long = 0,
    val isFolder: Boolean = false,
    val parentId: String? = null,
    val uploaderId: Long = 0,
    val uploaderName: String? = null,
    val downloadUrl: String? = null,
    val createTime: String? = null
)
