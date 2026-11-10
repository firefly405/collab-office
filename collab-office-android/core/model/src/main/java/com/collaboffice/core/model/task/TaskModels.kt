package com.collaboffice.core.model.task

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Task(
    val taskId: String = "",
    val title: String = "",
    val description: String? = null,
    val status: Int = 0,  // 0=todo, 1=in_progress, 2=done
    val priority: Int = 2,  // 1=紧急 2=高 3=中 4=低
    val progress: Int = 0,
    val assigneeId: Long? = null,
    val assigneeName: String? = null,
    val creatorId: Long = 0,
    val creatorName: String? = null,
    val projectId: String? = null,
    val columnId: String? = null,
    val dueDate: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null
)

@JsonClass(generateAdapter = true)
data class TaskProject(
    val projectId: String = "",
    val name: String = "",
    val description: String? = null,
    val spaceId: Long = 0,
    val memberCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class KanbanColumn(
    val columnId: String = "",
    val name: String = "",
    val projectId: String = "",
    val sortOrder: Int = 0,
    val wipLimit: Int? = null,
    val tasks: List<Task> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val projectId: String,
    val columnId: String? = null,
    val assigneeId: Long? = null,
    val priority: Int = 3,
    val dueDate: String? = null
)

@JsonClass(generateAdapter = true)
data class TaskComment(
    val id: Long? = null,
    val userId: Long = 0,
    val content: String = "",
    val createTime: String? = null,
    val userName: String? = null
)

@JsonClass(generateAdapter = true)
data class TaskDetail(
    val taskId: String = "",
    val title: String = "",
    val description: String? = null,
    val projectId: String? = null,
    val status: Int = 0,
    val priority: Int = 2,
    val progress: Int = 0,
    val assigneeId: Long? = null,
    val dueDate: String? = null,
    val createTime: String? = null,
    val comments: List<TaskComment> = emptyList()
)
