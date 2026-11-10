package com.collaboffice.feature.meeting

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.collaboffice.android.service.ScreenShareService
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

data class Participant(
    val userId: String,
    val name: String,
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isHost: Boolean = false,
    val isHandRaised: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingRoomScreen(
    roomId: String,
    onLeaveMeeting: () -> Unit = {},
    viewModel: MeetingRoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val remoteTracks by viewModel.remoteTracks.collectAsState()
    val context = LocalContext.current

    val mediaProjectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    val screenShareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                ScreenShareService.start(context)
                viewModel.startScreenShare(result.data!!)
            }
        }
    )

    LaunchedEffect(roomId) { viewModel.joinMeeting(roomId) }
    DisposableEffect(roomId) { 
        onDispose { 
            viewModel.leaveMeeting(roomId) 
            ScreenShareService.stop(context)
        } 
    }

    Scaffold(
        containerColor = Color(0xFF1A1A2E)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.roomTitle,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                // Timer
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = formatDuration(uiState.elapsedSeconds),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Participant count
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text("${uiState.participants.size}")
                }
            }

            // Video grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (uiState.participants.size <= 2) 1 else 2),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.participants) { participant ->
                    val isLocal = participant.userId == viewModel.currentUserId
                    val track = if (isLocal) viewModel.getLocalVideoTrack() else remoteTracks[participant.userId]
                    
                    ParticipantTile(
                        participant = participant,
                        videoTrack = track,
                        eglContext = viewModel.getEglContext()
                    )
                }
            }

            // Bottom controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF16213E),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mic toggle
                    MeetingControlButton(
                        icon = if (uiState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        label = if (uiState.isMuted) "取消静音" else "静音",
                        isActive = !uiState.isMuted,
                        onClick = { viewModel.toggleMute() }
                    )

                    // Camera toggle
                    MeetingControlButton(
                        icon = if (uiState.isCameraOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                        label = if (uiState.isCameraOff) "开启摄像" else "关闭摄像",
                        isActive = !uiState.isCameraOff,
                        onClick = { viewModel.toggleCamera() }
                    )

                    // Switch camera (Android only)
                    MeetingControlButton(
                        icon = Icons.Filled.FlipCameraAndroid,
                        label = "切换",
                        isActive = true,
                        onClick = { viewModel.switchCamera() }
                    )

                    // Screen Share toggle
                    MeetingControlButton(
                        icon = if (uiState.isSharingScreen) Icons.Filled.StopScreenShare else Icons.Filled.ScreenShare,
                        label = if (uiState.isSharingScreen) "停止共享" else "屏幕共享",
                        isActive = uiState.isSharingScreen,
                        onClick = {
                            if (uiState.isSharingScreen) {
                                viewModel.stopScreenShare()
                                ScreenShareService.stop(context)
                            } else {
                                screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                            }
                        }
                    )

                    // Raise hand
                    MeetingControlButton(
                        icon = Icons.Filled.PanTool,
                        label = "举手",
                        isActive = uiState.isHandRaised,
                        onClick = { viewModel.toggleHandRaise() }
                    )

                    // Leave meeting
                    FilledIconButton(
                        onClick = {
                            viewModel.leaveMeeting(roomId)
                            ScreenShareService.stop(context)
                            onLeaveMeeting()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = "离开会议", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ParticipantTile(
    participant: Participant,
    videoTrack: VideoTrack?,
    eglContext: org.webrtc.EglBase.Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3460))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (participant.isCameraOff || videoTrack == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = Color(0xFF533483)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = participant.name.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                AndroidView(
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {
                            init(eglContext, null)
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                            setEnableHardwareScaler(true)
                            videoTrack.addSink(this)
                        }
                    },
                    onRelease = { view ->
                        videoTrack.removeSink(view)
                        view.release()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Name label
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (participant.isMuted) {
                    Icon(
                        Icons.Filled.MicOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = participant.name + if (participant.isHost) " (主持人)" else "",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Hand raised indicator
            if (participant.isHandRaised) {
                Text(
                    text = "✋",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

@Composable
fun MeetingControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color.White.copy(alpha = 0.1f)
                    else Color.White.copy(alpha = 0.05f)
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) Color.White else Color.White.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}
