package com.collaboffice.feature.meeting.webrtc

import android.content.Context
import android.content.Intent
import com.collaboffice.core.model.ws.MessageType
import com.collaboffice.core.model.ws.WebSocketMessage
import com.collaboffice.core.network.websocket.CollabWebSocket
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer

class WebRTCClient(
    private val context: Context,
    private val webSocket: CollabWebSocket,
    private val onRemoteTrackReceived: (userId: String, track: VideoTrack) -> Unit
) {
    var eglBase: EglBase = EglBase.create()
        private set

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    
    private var isInitialized = false
    private var isFrontCamera = true

    init {
        initialize()
    }

    fun initialize() {
        if (isInitialized) return
        
        // 1. Initialize factory options
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )

        val options = PeerConnectionFactory.Options()
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()

        // 2. Create audio track
        audioSource = factory?.createAudioSource(MediaConstraints())
        localAudioTrack = factory?.createAudioTrack("local_audio", audioSource)

        // 3. Create video track with camera capturer
        videoCapturer = createCameraCapturer()
        videoSource = factory?.createVideoSource(videoCapturer?.isScreencast ?: false)
        videoCapturer?.initialize(
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext),
            context,
            videoSource?.capturerObserver
        )
        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack = factory?.createVideoTrack("local_video", videoSource)

        isInitialized = true
    }

    fun getLocalVideoTrack(): VideoTrack? = localVideoTrack

    fun createPeerConnection(roomId: String, targetUserId: String) {
        val iceServers = listOf(
            IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        
        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    // Send candidate via WebSocket
                    webSocket.send(WebSocketMessage(
                        type = MessageType.MEETING_SIGNAL.code,
                        roomId = roomId,
                        data = mapOf(
                            "signalType" to "candidate",
                            "targetUserId" to targetUserId,
                            "sdpMid" to it.sdpMid,
                            "sdpMLineIndex" to it.sdpMLineIndex.toDouble(),
                            "candidate" to it.sdp
                        )
                    ))
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is VideoTrack) {
                    onRemoteTrackReceived(targetUserId, track)
                }
            }
        }

        peerConnection = factory?.createPeerConnection(rtcConfig, observer)
        
        // Add local tracks
        localAudioTrack?.let { peerConnection?.addTrack(it) }
        localVideoTrack?.let { peerConnection?.addTrack(it) }
    }

    fun createOffer(roomId: String, targetUserId: String) {
        createPeerConnection(roomId, targetUserId)
        val constraints = MediaConstraints()
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    peerConnection?.setLocalDescription(this, it)
                    // Send offer via WebSocket
                    webSocket.send(WebSocketMessage(
                        type = MessageType.MEETING_SIGNAL.code,
                        roomId = roomId,
                        data = mapOf(
                            "signalType" to "offer",
                            "targetUserId" to targetUserId,
                            "sdp" to it.description
                        )
                    ))
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    fun handleOffer(roomId: String, fromUserId: String, sdp: String) {
        createPeerConnection(roomId, fromUserId)
        
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {}
            override fun onSetSuccess() {
                // Create Answer
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        desc?.let {
                            peerConnection?.setLocalDescription(this, it)
                            // Send Answer
                            webSocket.send(WebSocketMessage(
                                type = MessageType.MEETING_SIGNAL.code,
                                roomId = roomId,
                                data = mapOf(
                                    "signalType" to "answer",
                                    "targetUserId" to fromUserId,
                                    "sdp" to it.description
                                )
                            ))
                        }
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(error: String?) {}
                    override fun onSetFailure(error: String?) {}
                }, MediaConstraints())
            }
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, SessionDescription(SessionDescription.Type.OFFER, sdp))
    }

    fun handleAnswer(sdp: String) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    fun addIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        peerConnection?.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
    }

    fun addIceCandidate(candidateJson: String) {
        // Fallback or overload if json parsing is handled at VM level
    }

    fun startScreenShare(mediaProjectionIntent: Intent) {
        // 1. Stop current camera capture
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (e: Exception) {}

        // 2. Create screen capturer
        val callback = object : MediaProjection.Callback() {}
        videoCapturer = ScreenCapturerAndroid(mediaProjectionIntent, callback)
        videoSource = factory?.createVideoSource(true)
        
        videoCapturer?.initialize(
            SurfaceTextureHelper.create("ScreenCaptureThread", eglBase.eglBaseContext),
            context,
            videoSource?.capturerObserver
        )
        videoCapturer?.startCapture(1280, 720, 30)

        // 3. Update local track with new source
        localVideoTrack?.dispose()
        localVideoTrack = factory?.createVideoTrack("local_video", videoSource)
    }

    fun stopScreenShare() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (e: Exception) {}

        // Re-initialize camera capture
        videoCapturer = createCameraCapturer()
        videoSource = factory?.createVideoSource(false)
        videoCapturer?.initialize(
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext),
            context,
            videoSource?.capturerObserver
        )
        videoCapturer?.startCapture(1280, 720, 30)
        
        localVideoTrack?.dispose()
        localVideoTrack = factory?.createVideoTrack("local_video", videoSource)
    }

    fun switchCamera() {
        if (videoCapturer is CameraVideoCapturer) {
            val cameraVideoCapturer = videoCapturer as CameraVideoCapturer
            cameraVideoCapturer.switchCamera(null)
            isFrontCamera = !isFrontCamera
        }
    }

    fun toggleMicrophone(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun toggleCamera(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun release() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (e: Exception) {}
        
        peerConnection?.close()
        factory?.dispose()
        eglBase.release()
        isInitialized = false
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        
        // Find front camera first
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        
        // Back camera fallback
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        
        return null
    }
}
