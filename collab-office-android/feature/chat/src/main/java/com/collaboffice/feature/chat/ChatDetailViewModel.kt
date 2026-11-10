package com.collaboffice.feature.chat

import android.content.Context
import android.net.Uri
import android.media.MediaRecorder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.database.dao.ChatMessageDao
import com.collaboffice.core.database.entity.ChatMessageEntity
import com.collaboffice.core.model.chat.ChatMessage
import com.collaboffice.core.model.ws.MessageType
import com.collaboffice.core.model.ws.WebSocketMessage
import com.collaboffice.core.network.api.ChatApi
import com.collaboffice.core.network.api.FileApi
import com.collaboffice.core.network.auth.TokenManager
import com.collaboffice.core.network.websocket.CollabWebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

data class ChatDetailUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val isRecording: Boolean = false,
    val recordingDuration: Int = 0,
    val isUploading: Boolean = false,
    val uploadingProgress: Float = 0f,
    val replyingTo: ChatMessage? = null,
    val error: String? = null
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatApi: ChatApi,
    private val fileApi: FileApi,
    private val webSocket: CollabWebSocket,
    private val tokenManager: TokenManager,
    private val chatMessageDao: ChatMessageDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    val currentUserId: String get() = tokenManager.userId.toString()

    private var mediaRecorder: MediaRecorder? = null
    private var recordFile: File? = null
    private var recordStartTime: Long = 0
    private var recordingTimerJob: Job? = null
    private var observeJob: Job? = null
    private var sessionId: String = ""

    init {
        // Listen for incoming WebSocket messages
        viewModelScope.launch {
            webSocket.messages.collect { msg ->
                if (msg.roomId == sessionId) {
                    when (msg.type) {
                        MessageType.CHAT_RECEIVE.code -> {
                            val data = msg.data ?: return@collect
                            val senderId = msg.userId ?: ""
                            val chatMsg = ChatMessageEntity(
                                msgId = data["msgId"]?.toString() ?: UUID.randomUUID().toString(),
                                sessionId = msg.roomId ?: "",
                                senderId = senderId,
                                senderName = data["senderName"]?.toString() ?: "",
                                content = data["content"]?.toString() ?: "",
                                contentType = (data["contentType"] as? Number)?.toInt() ?: 1,
                                seq = msg.seq ?: 0,
                                sendTime = msg.serverTimestamp ?: System.currentTimeMillis(),
                                status = if (senderId == currentUserId) 1 else 2,
                                replyToId = data["replyToId"]?.toString(),
                                replyToName = data["replyToName"]?.toString(),
                                replyToContent = data["replyToContent"]?.toString(),
                                extra = data["extra"]?.toString()
                            )
                            chatMessageDao.insert(chatMsg)
                            if (senderId != currentUserId) {
                                markSessionAsRead(sessionId)
                            }
                        }
                        MessageType.CHAT_READ.code -> {
                            chatMessageDao.markAllAsRead(sessionId)
                        }
                        MessageType.CHAT_TYPING_BROADCAST.code -> {
                            _uiState.update { it.copy(isTyping = true) }
                            viewModelScope.launch {
                                delay(3000)
                                _uiState.update { it.copy(isTyping = false) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeMessages(sessionId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            chatMessageDao.observeBySession(sessionId).collect { entities ->
                val models = entities.map { entity ->
                    ChatMessage(
                        msgId = entity.msgId,
                        sessionId = entity.sessionId,
                        senderId = entity.senderId,
                        senderName = entity.senderName,
                        senderAvatar = entity.senderAvatar,
                        content = entity.content,
                        contentType = entity.contentType,
                        seq = entity.seq,
                        sendTime = entity.sendTime,
                        isRead = entity.status == 2,
                        isRecalled = entity.status == 3,
                        replyToId = entity.replyToId,
                        replyToName = entity.replyToName,
                        replyToContent = entity.replyToContent,
                        extra = entity.extra
                    )
                }
                _uiState.update { it.copy(messages = models) }
            }
        }
    }

    fun loadMessages(sessionId: String) {
        this.sessionId = sessionId
        observeMessages(sessionId)
        markSessionAsRead(sessionId)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = chatApi.getHistory(sessionId)
                if (response.isSuccess && response.data != null) {
                    val entities = response.data.map { model ->
                        ChatMessageEntity(
                            msgId = model.msgId,
                            sessionId = model.sessionId,
                            senderId = model.senderId,
                            senderName = model.senderName,
                            senderAvatar = model.senderAvatar,
                            content = model.content,
                            contentType = model.contentType,
                            seq = model.seq,
                            sendTime = model.sendTime,
                            status = if (model.isRecalled) 3 else (if (model.isRead) 2 else 1),
                            replyToId = model.replyToId,
                            replyToName = model.replyToName,
                            replyToContent = model.replyToContent,
                            extra = model.extra
                        )
                    }
                    chatMessageDao.insertAll(entities)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun sendMessage(sessionId: String, content: String) {
        sendMediaMessage(sessionId, content, 1)
    }

    fun sendLocationMessage(sessionId: String, latitude: Double, longitude: Double, address: String) {
        sendMediaMessage(sessionId, "$latitude,$longitude,$address", 5)
    }

    private fun sendMediaMessage(sessionId: String, content: String, contentType: Int, extra: String? = null) {
        viewModelScope.launch {
            val msgId = UUID.randomUUID().toString()
            val reply = _uiState.value.replyTo
            
            val wsMessage = WebSocketMessage(
                type = MessageType.CHAT_SEND.code,
                roomId = sessionId,
                userId = currentUserId,
                clientTimestamp = System.currentTimeMillis(),
                data = mutableMapOf<String, Any>(
                    "msgId" to msgId,
                    "content" to content,
                    "contentType" to contentType
                ).apply {
                    if (reply != null) {
                        put("replyToId", reply.msgId)
                        put("replyToName", reply.senderName)
                        put("replyToContent", reply.content)
                    }
                    if (extra != null) {
                        put("extra", extra)
                    }
                }
            )

            val localMsg = ChatMessageEntity(
                msgId = msgId,
                sessionId = sessionId,
                senderId = currentUserId,
                senderName = "我",
                content = content,
                contentType = contentType,
                seq = 0,
                sendTime = System.currentTimeMillis(),
                status = 0, // Sending
                replyToId = reply?.msgId,
                replyToName = reply?.senderName,
                replyToContent = reply?.content,
                extra = extra
            )
            chatMessageDao.insert(localMsg)
            _uiState.update { it.copy(replyTo = null) }

            try {
                webSocket.send(wsMessage)
                chatApi.sendMessage(sessionId, content, contentType)
                chatMessageDao.insert(localMsg.copy(status = 1)) // Sent
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun uploadAndSendImage(sessionId: String, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            try {
                val file = compressImage(uri)
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val response = fileApi.uploadFile(body)
                if (response.isSuccess && response.data != null) {
                    val fileUrl = response.data["fileUrl"] ?: ""
                    sendMediaMessage(sessionId, fileUrl, 2)
                } else {
                    _uiState.update { it.copy(error = "上传图片失败: ${response.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "发送图片失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isUploading = false) }
            }
        }
    }

    private fun compressImage(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        val file = File(context.cacheDir, "img_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
        }
        inputStream?.close()
        return file
    }

    fun uploadAndSendFile(sessionId: String, uri: Uri, fileName: String, fileSize: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            try {
                val file = copyUriToFile(uri, fileName)
                val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val response = fileApi.uploadFile(body)
                if (response.isSuccess && response.data != null) {
                    val fileUrl = response.data["fileUrl"] ?: ""
                    val fileId = response.data["fileId"] ?: ""
                    sendMediaMessage(sessionId, fileUrl, 3, extra = "$fileName|$fileSize|$fileId")
                } else {
                    _uiState.update { it.copy(error = "上传文件失败: ${response.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "发送文件失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isUploading = false) }
            }
        }
    }

    private fun copyUriToFile(uri: Uri, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    fun startRecording() {
        try {
            recordFile = File(context.cacheDir, "voice_msg_${System.currentTimeMillis()}.m4a")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordFile?.absolutePath)
                prepare()
                start()
            }
            recordStartTime = System.currentTimeMillis()
            _uiState.update { it.copy(isRecording = true, recordingDuration = 0) }
            startRecordingTimer()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "录音失败: ${e.message}") }
        }
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { it.copy(recordingDuration = it.recordingDuration + 1) }
            }
        }
    }

    fun stopRecording(sessionId: String) {
        recordingTimerJob?.cancel()
        _uiState.update { it.copy(isRecording = false) }
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            val duration = ((System.currentTimeMillis() - recordStartTime) / 1000).toInt()
            val file = recordFile
            if (file != null && file.exists() && duration > 0) {
                uploadAndSendVoiceMessage(sessionId, file, duration)
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "停止录音失败: ${e.message}") }
        }
    }

    private fun uploadAndSendVoiceMessage(sessionId: String, file: File, durationSec: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            try {
                val requestFile = file.asRequestBody("audio/mpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val response = fileApi.uploadFile(body)
                if (response.isSuccess && response.data != null) {
                    val fileUrl = response.data["fileUrl"] ?: ""
                    sendMediaMessage(sessionId, fileUrl, 4, extra = durationSec.toString())
                } else {
                    _uiState.update { it.copy(error = "上传语音失败: ${response.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "发送语音失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isUploading = false) }
            }
        }
    }

    fun cancelRecording() {
        recordingTimerJob?.cancel()
        _uiState.update { it.copy(isRecording = false) }
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            recordFile?.delete()
            recordFile = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun recallMessage(message: ChatMessage) {
        viewModelScope.launch {
            try {
                val response = chatApi.recallMessage(message.msgId)
                if (response.isSuccess) {
                    chatMessageDao.recallMessage(message.msgId)
                } else {
                    _uiState.update { it.copy(error = "撤回失败: ${response.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "撤回发生错误: ${e.message}") }
            }
        }
    }

    fun forwardMessage(message: ChatMessage, targetSessionId: String) {
        sendMediaMessage(targetSessionId, message.content, message.contentType, message.extra)
    }

    fun replyTo(message: ChatMessage) {
        _uiState.update { it.copy(replyingTo = message) }
    }

    fun clearReply() {
        _uiState.update { it.copy(replyingTo = null) }
    }

    fun sendTyping(sessionId: String) {
        webSocket.send(WebSocketMessage(
            type = MessageType.CHAT_TYPING.code,
            roomId = sessionId,
            userId = currentUserId,
            clientTimestamp = System.currentTimeMillis()
        ))
    }

    fun markSessionAsRead(sessionId: String) {
        viewModelScope.launch {
            try {
                chatApi.markRead(sessionId)
                chatMessageDao.markAllAsRead(sessionId)
                webSocket.send(WebSocketMessage(
                    type = MessageType.CHAT_READ.code,
                    roomId = sessionId,
                    userId = currentUserId,
                    clientTimestamp = System.currentTimeMillis()
                ))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

