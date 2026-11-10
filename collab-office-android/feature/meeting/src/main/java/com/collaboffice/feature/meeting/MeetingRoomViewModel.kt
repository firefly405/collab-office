package com.collaboffice.feature.meeting

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.model.ws.MessageType
import com.collaboffice.core.model.ws.WebSocketMessage
import com.collaboffice.core.network.api.MeetingApi
import com.collaboffice.core.network.auth.TokenManager
import com.collaboffice.core.network.websocket.CollabWebSocket
import com.collaboffice.feature.meeting.webrtc.WebRTCClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack
import javax.inject.Inject

data class MeetingRoomUiState(
    val roomTitle: String = "会议",
    val participants: List<Participant> = emptyList(),
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isHandRaised: Boolean = false,
    val isSharingScreen: Boolean = false,
    val elapsedSeconds: Int = 0,
    val error: String? = null
)

@HiltViewModel
class MeetingRoomViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meetingApi: MeetingApi,
    private val webSocket: CollabWebSocket,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeetingRoomUiState())
    val uiState: StateFlow<MeetingRoomUiState> = _uiState.asStateFlow()

    val currentUserId get() = tokenManager.userId.toString()

    private val _remoteTracks = MutableStateFlow<Map<String, VideoTrack>>(emptyMap())
    val remoteTracks: StateFlow<Map<String, VideoTrack>> = _remoteTracks.asStateFlow()

    private val webRtcClient = WebRTCClient(context, webSocket) { userId, track ->
        _remoteTracks.update { it + (userId to track) }
    }

    init {
        // Timer
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }

        // WebSocket message handling
        viewModelScope.launch {
            webSocket.messages.collect { msg ->
                when (msg.type) {
                    MessageType.MEETING_SIGNAL.code -> {
                        val data = msg.data ?: return@collect
                        val signalType = data["signalType"]?.toString() ?: ""
                        val fromUserId = msg.userId ?: ""
                        val roomId = msg.roomId ?: ""
                        when (signalType) {
                            "offer" -> {
                                val sdp = data["sdp"]?.toString() ?: ""
                                webRtcClient.handleOffer(roomId, fromUserId, sdp)
                            }
                            "answer" -> {
                                val sdp = data["sdp"]?.toString() ?: ""
                                webRtcClient.handleAnswer(sdp)
                            }
                            "candidate" -> {
                                val sdpMid = data["sdpMid"]?.toString() ?: ""
                                val sdpMLineIndex = (data["sdpMLineIndex"] as? Number)?.toInt() ?: 0
                                val candidate = data["candidate"]?.toString() ?: ""
                                webRtcClient.addIceCandidate(sdpMid, sdpMLineIndex, candidate)
                            }
                        }
                    }
                    MessageType.MEETING_MEDIA.code -> {
                        val userId = msg.userId ?: return@collect
                        val data = msg.data ?: return@collect
                        _uiState.update { state ->
                            state.copy(participants = state.participants.map { p ->
                                if (p.userId == userId) {
                                    p.copy(
                                        isMuted = data["isMuted"] as? Boolean ?: p.isMuted,
                                        isCameraOff = data["isCameraOff"] as? Boolean ?: p.isCameraOff
                                    )
                                } else p
                             })
                        }
                    }
                    MessageType.MUTE_ALL.code -> {
                        _uiState.update { it.copy(isMuted = true) }
                        webRtcClient.toggleMicrophone(false)
                    }
                    MessageType.KICK_USER.code -> {
                        if (msg.data?.get("targetUserId")?.toString() == currentUserId) {
                            // Kicked from meeting
                            _uiState.update { it.copy(error = "您已被主持人移出会议") }
                        }
                    }
                }
            }
        }
    }

    fun getLocalVideoTrack(): VideoTrack? = webRtcClient.getLocalVideoTrack()
    fun getEglContext() = webRtcClient.eglBase.eglBaseContext

    fun joinMeeting(roomId: String) {
        viewModelScope.launch {
            try {
                meetingApi.joinMeeting(roomId)
            } catch (_: Exception) {}
        }

        webSocket.send(WebSocketMessage(
            type = MessageType.MEETING_JOIN.code,
            roomId = roomId,
            userId = currentUserId,
            clientTimestamp = System.currentTimeMillis()
        ))
    }

    fun leaveMeeting(roomId: String) {
        webSocket.send(WebSocketMessage(
            type = MessageType.MEETING_LEAVE.code,
            roomId = roomId,
            userId = currentUserId,
            clientTimestamp = System.currentTimeMillis()
        ))

        viewModelScope.launch {
            try { meetingApi.leaveMeeting(roomId) } catch (_: Exception) {}
        }
    }

    fun toggleMute() {
        val nextState = !_uiState.value.isMuted
        _uiState.update { it.copy(isMuted = nextState) }
        webRtcClient.toggleMicrophone(!nextState)
        broadcastMediaState()
    }

    fun toggleCamera() {
        val nextState = !_uiState.value.isCameraOff
        _uiState.update { it.copy(isCameraOff = nextState) }
        webRtcClient.toggleCamera(!nextState)
        broadcastMediaState()
    }

    fun switchCamera() {
        webRtcClient.switchCamera()
    }

    fun startScreenShare(intent: Intent) {
        _uiState.update { it.copy(isSharingScreen = true) }
        webRtcClient.startScreenShare(intent)
    }

    fun stopScreenShare() {
        _uiState.update { it.copy(isSharingScreen = false) }
        webRtcClient.stopScreenShare()
    }

    fun toggleHandRaise() {
        _uiState.update { it.copy(isHandRaised = !it.isHandRaised) }
    }

    private fun broadcastMediaState() {
        val state = _uiState.value
        webSocket.send(WebSocketMessage(
            type = MessageType.MEETING_MEDIA.code,
            userId = currentUserId,
            clientTimestamp = System.currentTimeMillis(),
            data = mapOf(
                "isMuted" to state.isMuted,
                "isCameraOff" to state.isCameraOff
            )
        ))
    }

    override fun onCleared() {
        super.onCleared()
        webRtcClient.release()
    }
}
