package com.collaboffice.feature.file

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.model.file.FileItem
import com.collaboffice.core.network.api.FileApi
import com.collaboffice.core.network.api.SpaceApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class FileListUiState(
    val isLoading: Boolean = false,
    val files: List<FileItem> = emptyList(),
    val currentFolderId: String? = null,
    val isUploading: Boolean = false,
    val uploadProgress: Float? = null,
    val error: String? = null
)

@HiltViewModel
class FileListViewModel @Inject constructor(
    private val fileApi: FileApi,
    private val spaceApi: SpaceApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileListUiState())
    val uiState: StateFlow<FileListUiState> = _uiState.asStateFlow()

    fun loadFiles(folderId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentFolderId = folderId) }
            try {
                // Fetch default spaceId
                val spacesResponse = spaceApi.getMySpaces()
                val spaceId = spacesResponse.data?.firstOrNull()?.id ?: 1L
                val response = fileApi.getFiles(spaceId = spaceId, folderId = folderId)
                if (response.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, files = response.data ?: emptyList(), error = null) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun openFile(file: FileItem) {
        if (file.isFolder) {
            loadFiles(file.fileId)
        }
    }

    fun uploadFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadProgress = 0f) }
            try {
                val contentResolver = context.contentResolver
                var fileName = "file"
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

                val contentType = contentResolver.getType(uri) ?: "application/octet-stream"
                val progressRequestBody = ProgressRequestBody(tempFile, contentType) { progress ->
                    _uiState.update { it.copy(uploadProgress = progress) }
                }

                val body = MultipartBody.Part.createFormData("file", tempFile.name, progressRequestBody)
                val response = fileApi.uploadFile(body)
                if (response.isSuccess) {
                    loadFiles(_uiState.value.currentFolderId)
                } else {
                    _uiState.update { it.copy(error = response.message) }
                }
                tempFile.delete()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isUploading = false, uploadProgress = null) }
            }
        }
    }

    fun downloadFile(file: FileItem, context: Context) {
        viewModelScope.launch {
            try {
                val response = fileApi.getDownloadUrl(file.fileId)
                if (response.isSuccess && response.data != null) {
                    val downloadUrl = response.data!!["downloadUrl"] ?: ""
                    if (downloadUrl.isNotEmpty()) {
                        val request = DownloadManager.Request(Uri.parse(downloadUrl))
                            .setTitle(file.fileName)
                            .setDescription("正在下载文件...")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.fileName)
                        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        manager.enqueue(request)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}

class ProgressRequestBody(
    private val file: File,
    private val contentType: String,
    private val onProgress: (progress: Float) -> Unit
) : RequestBody() {
    override fun contentType() = contentType.toMediaTypeOrNull()
    override fun contentLength() = file.length()
    override fun writeTo(sink: BufferedSink) {
        val fileLength = file.length()
        val buffer = ByteArray(2048)
        var uploaded = 0L
        file.inputStream().use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                onProgress(uploaded.toFloat() / fileLength)
            }
        }
    }
}
