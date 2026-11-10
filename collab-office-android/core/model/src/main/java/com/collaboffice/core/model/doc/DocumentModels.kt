package com.collaboffice.core.model.doc

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Document(
    val docId: String = "",
    val title: String = "",
    val type: Int = 1,  // 1=文档 2=表格 3=思维导图 4=白板
    val creatorId: Long = 0,
    val creatorName: String? = null,
    val spaceId: Long? = null,
    val folderId: String? = null,
    val version: Int = 1,
    val wordCount: Int = 0,
    val createTime: String? = null,
    val updateTime: String? = null
)

@JsonClass(generateAdapter = true)
data class DocVersion(
    val versionId: Long = 0,
    val docId: String = "",
    val version: Int = 0,
    val snapshot: String? = null,
    val editorId: Long = 0,
    val editorName: String? = null,
    val createTime: String? = null
)
