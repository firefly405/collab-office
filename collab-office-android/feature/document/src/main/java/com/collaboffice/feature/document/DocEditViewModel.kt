package com.collaboffice.feature.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.database.dao.OfflineOpDao
import com.collaboffice.core.database.entity.OfflineOperationEntity
import com.collaboffice.core.model.ws.MessageType
import com.collaboffice.core.model.ws.WebSocketMessage
import com.collaboffice.core.network.api.DocumentApi
import com.collaboffice.core.network.auth.TokenManager
import com.collaboffice.core.network.websocket.CollabWebSocket
import com.collaboffice.core.network.websocket.ConnectionState
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CursorInfo(
    val name: String = "",
    val color: String = "#666",
    val index: Int = 0,
    val length: Int = 0
)

data class Collaborator(
    val userId: String,
    val name: String,
    val color: String
)

data class DocEditUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val isEditorReady: Boolean = false,
    val docType: Int = 1,
    val initialContent: String? = null,
    val pendingRemoteOps: List<String> = emptyList(),
    val remoteCursors: Map<String, CursorInfo> = emptyMap(),
    val collaborators: List<Collaborator> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DocEditViewModel @Inject constructor(
    private val documentApi: DocumentApi,
    private val webSocket: CollabWebSocket,
    private val tokenManager: TokenManager,
    private val offlineOpDao: OfflineOpDao,
    private val moshi: Moshi
) : ViewModel() {

    private val mapAdapter = moshi.adapter(Map::class.java)
    private var lastContentJson: String? = null

    private val _uiState = MutableStateFlow(DocEditUiState())
    val uiState: StateFlow<DocEditUiState> = _uiState.asStateFlow()

    private var currentDocId: String? = null
    private var localSeq: Long = 0
    private val currentUserId get() = tokenManager.userId.toString()

    init {
        // Monitor connection state
        viewModelScope.launch {
            webSocket.connectionState.collect { state ->
                _uiState.update { it.copy(isConnected = state == ConnectionState.CONNECTED) }

                // When reconnected, re-join doc and sync offline ops
                if (state == ConnectionState.CONNECTED) {
                    currentDocId?.let { docId ->
                        joinDocument(docId)
                        syncOfflineOps(docId)
                    }
                }
            }
        }

        // Listen for incoming WebSocket messages
        viewModelScope.launch {
            webSocket.messages.collect { msg ->
                handleWebSocketMessage(msg)
            }
        }
    }

    fun loadDocument(docId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Fetch document metadata to get document type
                var docType = 1
                try {
                    val docDetailResponse = documentApi.getDocumentDetail(docId)
                    if (docDetailResponse.isSuccess) {
                        docType = docDetailResponse.data?.type ?: 1
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. Fetch document content
                val response = documentApi.getContent(docId)
                if (response.isSuccess) {
                    _uiState.update { it.copy(
                        isLoading = false,
                        docType = docType,
                        initialContent = response.data
                    )}
                } else {
                    _uiState.update { it.copy(isLoading = false, docType = docType) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun joinDocument(docId: String) {
        currentDocId = docId
        webSocket.send(WebSocketMessage(
            type = MessageType.DOC_JOIN.code,
            roomId = docId,
            userId = currentUserId,
            clientTimestamp = System.currentTimeMillis()
        ))
    }

    fun leaveDocument(docId: String) {
        webSocket.send(WebSocketMessage(
            type = MessageType.DOC_LEAVE.code,
            roomId = docId,
            userId = currentUserId,
            clientTimestamp = System.currentTimeMillis()
        ))
        currentDocId = null
    }

    fun onLocalOperation(operationJson: String) {
        val docId = currentDocId ?: return
        localSeq++

        val msgType = when (_uiState.value.docType) {
            2 -> MessageType.SHEET_CELL_UPDATE
            3 -> MessageType.MINDMAP_NODE_UPDATE
            4 -> MessageType.WHITEBOARD_DRAW
            else -> MessageType.DOC_OPERATION
        }

        if (_uiState.value.isConnected) {
            // Send via WebSocket
            val dataMap = if (_uiState.value.docType == 1) {
                mapOf("operations" to operationJson, "version" to localSeq)
            } else {
                try {
                    @Suppress("UNCHECKED_CAST")
                    mapAdapter.fromJson(operationJson) as Map<String, Any?>
                } catch (e: Exception) {
                    mapOf("operation" to operationJson)
                }
            }

            webSocket.send(WebSocketMessage(
                type = msgType.code,
                roomId = docId,
                userId = currentUserId,
                clientTimestamp = System.currentTimeMillis(),
                seq = localSeq,
                data = dataMap
            ))
        } else {
            // Store offline
            viewModelScope.launch {
                offlineOpDao.enqueue(OfflineOperationEntity(
                    docId = docId,
                    seq = localSeq,
                    operationJson = operationJson
                ))
            }
        }
    }

    fun onContentChanged(contentJson: String) {
        lastContentJson = contentJson
    }

    fun onCursorChange(cursorJson: String) {
        val docId = currentDocId ?: return
        webSocket.send(WebSocketMessage(
            type = MessageType.DOC_CURSOR.code,
            roomId = docId,
            userId = currentUserId,
            clientTimestamp = System.currentTimeMillis(),
            data = mapOf("cursor" to cursorJson)
        ))
    }

    fun onEditorReady() {
        _uiState.update { it.copy(isEditorReady = true) }
    }

    fun onContentApplied() {
        _uiState.update { it.copy(initialContent = null) }
    }

    fun clearPendingOps() {
        _uiState.update { it.copy(pendingRemoteOps = emptyList()) }
    }

    fun saveDocument(docId: String) {
        val content = lastContentJson ?: return
        viewModelScope.launch {
            try {
                documentApi.saveContent(docId, mapOf("content" to content))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存失败: ${e.message}") }
            }
        }
    }

    private fun handleWebSocketMessage(msg: WebSocketMessage) {
        when (msg.type) {
            MessageType.DOC_OPERATION_BROADCAST.code -> {
                if (msg.userId != currentUserId) {
                    val ops = msg.data?.get("operations")?.toString() ?: return
                    _uiState.update { it.copy(pendingRemoteOps = it.pendingRemoteOps + ops) }
                }
            }
            MessageType.SHEET_CELL_BROADCAST.code,
            MessageType.MINDMAP_NODE_BROADCAST.code,
            MessageType.WHITEBOARD_DRAW_BROADCAST.code -> {
                if (msg.userId != currentUserId) {
                    val data = msg.data ?: return
                    try {
                        val json = mapAdapter.toJson(data)
                        _uiState.update { it.copy(pendingRemoteOps = it.pendingRemoteOps + json) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            MessageType.DOC_CURSOR_BROADCAST.code -> {
                if (msg.userId != currentUserId) {
                    val data = msg.data ?: return
                    val cursor = CursorInfo(
                        name = data["userName"]?.toString() ?: "",
                        color = data["color"]?.toString() ?: "#666",
                        index = (data["index"] as? Number)?.toInt() ?: 0,
                        length = (data["length"] as? Number)?.toInt() ?: 0
                    )
                    _uiState.update { it.copy(
                        remoteCursors = it.remoteCursors + (msg.userId!! to cursor)
                    )}
                }
            }
            MessageType.DOC_AWARENESS.code -> {
                val data = msg.data ?: return
                @Suppress("UNCHECKED_CAST")
                val users = data["users"] as? List<Map<String, Any>> ?: return
                val collaborators = users.map { user ->
                    Collaborator(
                        userId = user["userId"]?.toString() ?: "",
                        name = user["name"]?.toString() ?: "",
                        color = user["color"]?.toString() ?: "#666"
                    )
                }.filter { it.userId != currentUserId }
                _uiState.update { it.copy(collaborators = collaborators) }
            }
            MessageType.DOC_SYNC.code -> {
                val content = msg.data?.get("content")?.toString() ?: return
                _uiState.update { it.copy(initialContent = content) }
            }
        }
    }

    private suspend fun syncOfflineOps(docId: String) {
        val pendingOps = offlineOpDao.getPending()
        val docOps = pendingOps.filter { it.docId == docId }
        docOps.forEach { op ->
            webSocket.send(WebSocketMessage(
                type = MessageType.DOC_OPERATION.code,
                roomId = docId,
                userId = currentUserId,
                clientTimestamp = System.currentTimeMillis(),
                seq = op.seq,
                data = mapOf("operations" to op.operationJson, "version" to op.seq)
            ))
        }
        offlineOpDao.markSynced(docOps.map { it.id })
        offlineOpDao.deleteSynced()
    }
}
