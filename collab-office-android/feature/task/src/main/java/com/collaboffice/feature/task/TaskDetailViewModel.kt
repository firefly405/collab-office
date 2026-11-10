package com.collaboffice.feature.task

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.model.file.FileItem
import com.collaboffice.core.model.task.TaskComment
import com.collaboffice.core.model.task.TaskDetail
import com.collaboffice.core.model.user.UserInfo
import com.collaboffice.core.network.api.FileApi
import com.collaboffice.core.network.api.SpaceApi
import com.collaboffice.core.network.api.TaskApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class TaskDetailUiState(
    val isLoading: Boolean = false,
    val taskDetail: TaskDetail? = null,
    val members: List<UserInfo> = emptyList(),
    val isUploading: Boolean = false,
    val uploadProgress: Float? = null,
    val error: String? = null
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskApi: TaskApi,
    private val spaceApi: SpaceApi,
    private val fileApi: FileApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val taskId: String = savedStateHandle.get<String>("taskId") ?: ""

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        loadTaskDetail()
    }

    fun loadTaskDetail() {
        if (taskId.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = taskApi.getTaskDetail(taskId)
                if (response.isSuccess && response.data != null) {
                    val detail = response.data!!
                    _uiState.update { it.copy(isLoading = false, taskDetail = detail, error = null) }
                    // Load space members for assignment
                    loadMembersForProject(detail.projectId)
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadMembersForProject(projectId: String?) {
        viewModelScope.launch {
            try {
                val spacesResponse = spaceApi.getMySpaces()
                if (spacesResponse.isSuccess && spacesResponse.data != null) {
                    val spaces = spacesResponse.data!!
                    val activeSpaceId = spaces.firstOrNull()?.id ?: 1L
                    val membersResponse = spaceApi.getSpaceMembers(activeSpaceId)
                    if (membersResponse.isSuccess) {
                        _uiState.update { it.copy(members = membersResponse.data ?: emptyList()) }
                    }
                }
            } catch (e: Exception) {
                // Non-critical error, skip member loading
            }
        }
    }

    fun addComment(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                val response = taskApi.addComment(taskId, mapOf("content" to content))
                if (response.isSuccess) {
                    loadTaskDetail()
                } else {
                    _uiState.update { it.copy(error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateTask(updates: Map<String, Any?>) {
        viewModelScope.launch {
            try {
                val response = taskApi.updateTask(taskId, updates)
                if (response.isSuccess) {
                    loadTaskDetail()
                } else {
                    _uiState.update { it.copy(error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun uploadAttachment(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadProgress = 0f) }
            try {
                val contentResolver = context.contentResolver
                var fileName = "attachment"
                var fileSize = 0L

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }

                val tempFile = File(context.cacheDir, fileName)
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val requestFile = tempFile.asRequestBody(contentResolver.getType(uri)?.toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)

                val response = fileApi.uploadFile(body)
                if (response.isSuccess && response.data != null) {
                    val fileUrl = response.data!!["fileUrl"]?.toString() ?: ""
                    if (fileUrl.isNotEmpty()) {
                        addComment("[附件: $fileName]($fileUrl)")
                    }
                    tempFile.delete()
                } else {
                    _uiState.update { it.copy(error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isUploading = false, uploadProgress = null) }
            }
        }
    }
}
