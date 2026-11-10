package com.collaboffice.feature.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.collaboffice.core.model.chat.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    sessionId: String,
    sessionName: String = "聊天",
    onNavigateBack: () -> Unit = {},
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }
    var messageToForward by remember { mutableStateOf<ChatMessage?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadAndSendImage(sessionId, it) }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, it)
            val fileSize = getFileSize(context, it)
            viewModel.uploadAndSendFile(sessionId, it, fileName, fileSize)
        }
    }

    LaunchedEffect(sessionId) {
        viewModel.loadMessages(sessionId)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(sessionName, fontWeight = FontWeight.SemiBold)
                        if (uiState.isTyping) {
                            Text(
                                "对方正在输入...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* More details */ }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                uiState.replyingTo?.let { replyMsg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Reply,
                                contentDescription = "回复",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "回复 ${replyMsg.senderName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = replyMsg.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { viewModel.clearReply() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "取消")
                            }
                        }
                    }
                }

                Surface(
                    shadowElevation = 4.dp,
                    tonalElevation = 2.dp
                ) {
                    if (uiState.isRecording) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "录音中",
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "录音中: ${uiState.recordingDuration}秒",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { viewModel.cancelRecording() }) {
                                Text("取消")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.stopRecording(sessionId) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("发送")
                            }
                        }
                    } else {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.startRecording() }) {
                                    Icon(Icons.Filled.Mic, contentDescription = "语音")
                                }
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = {
                                        inputText = it
                                        viewModel.sendTyping(sessionId)
                                    },
                                    placeholder = { Text("输入消息...") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp),
                                    maxLines = 4
                                )
                                IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                                    Icon(Icons.Filled.AddCircleOutline, contentDescription = "附件")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                FilledIconButton(
                                    onClick = {
                                        if (inputText.isNotBlank()) {
                                            viewModel.sendMessage(sessionId, inputText.trim())
                                            inputText = ""
                                        }
                                    },
                                    enabled = inputText.isNotBlank()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                                }
                            }

                            AnimatedVisibility(
                                visible = showAttachmentMenu,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    AttachmentOptionItem(
                                        icon = Icons.Filled.Image,
                                        label = "图片",
                                        onClick = {
                                            showAttachmentMenu = false
                                            imagePicker.launch("image/*")
                                        }
                                    )
                                    AttachmentOptionItem(
                                        icon = Icons.Filled.AttachFile,
                                        label = "文件",
                                        onClick = {
                                            showAttachmentMenu = false
                                            filePicker.launch("*/*")
                                        }
                                    )
                                    AttachmentOptionItem(
                                        icon = Icons.Filled.LocationOn,
                                        label = "位置",
                                        onClick = {
                                            showAttachmentMenu = false
                                            showLocationPicker = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading && uiState.messages.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.messages, key = { it.msgId }) { message ->
                        MessageBubble(
                            message = message,
                            isOwnMessage = message.senderId == viewModel.currentUserId,
                            onReplyClick = { viewModel.replyTo(message) },
                            onRecallClick = { viewModel.recallMessage(message) },
                            onForwardClick = {
                                messageToForward = message
                                showForwardDialog = true
                            },
                            onImageClick = { previewImageUrl = it }
                        )
                    }
                }
            }

            if (uiState.isUploading) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在上传媒体文件...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    previewImageUrl?.let { url ->
        Dialog(
            onDismissRequest = { previewImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { previewImageUrl = null }
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = "图片预览",
                    modifier = Modifier.fillMaxSize().align(Alignment.Center)
                )
                IconButton(
                    onClick = { previewImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White)
                }
            }
        }
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            onDismiss = { showLocationPicker = false },
            onLocationSelected = { lat, lng, addr ->
                showLocationPicker = false
                viewModel.sendLocationMessage(sessionId, lat, lng, addr)
            }
        )
    }

    if (showForwardDialog && messageToForward != null) {
        ForwardDialog(
            onDismiss = {
                showForwardDialog = false
                messageToForward = null
            },
            onSelectSession = { targetId ->
                showForwardDialog = false
                messageToForward?.let { viewModel.forwardMessage(it, targetId) }
                messageToForward = null
                Toast.makeText(context, "转发成功", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AttachmentOptionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isOwnMessage: Boolean,
    onReplyClick: () -> Unit,
    onRecallClick: () -> Unit,
    onForwardClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (message.isRecalled) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isOwnMessage) "你撤回了一条消息" else "${message.senderName} 撤回了一条消息",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            Surface(
                modifier = Modifier.size(36.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = message.senderName.take(1).uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box {
            Column(
                horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                if (!isOwnMessage) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isOwnMessage) 16.dp else 4.dp,
                        topEnd = if (isOwnMessage) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    color = if (isOwnMessage)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (message.contentType == 2) {
                                onImageClick(message.content)
                            }
                        },
                        onLongClick = { expandedMenu = true }
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (message.replyToId != null) {
                            Column(
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.05f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = "${message.replyToName}:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = message.replyToContent ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        when (message.contentType) {
                            1 -> {
                                Text(
                                    text = message.content,
                                    color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            2 -> {
                                AsyncImage(
                                    model = message.content,
                                    contentDescription = "图片",
                                    modifier = Modifier
                                        .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                            3 -> {
                                val parts = message.extra?.split("|") ?: emptyList()
                                val fileName = parts.getOrNull(0) ?: "未知文件"
                                val fileSize = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        Toast.makeText(context, "开始下载 $fileName...", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Description,
                                        contentDescription = "文件",
                                        tint = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatSize(fileSize),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                            4 -> {
                                val duration = message.extra?.toIntOrNull() ?: 0
                                var isPlaying by remember { mutableStateOf(false) }
                                val mediaPlayer = remember { android.media.MediaPlayer() }

                                DisposableEffect(Unit) {
                                    onDispose { mediaPlayer.release() }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            if (isPlaying) {
                                                mediaPlayer.stop()
                                                mediaPlayer.reset()
                                                isPlaying = false
                                            } else {
                                                try {
                                                    mediaPlayer.reset()
                                                    mediaPlayer.setDataSource(message.content)
                                                    mediaPlayer.prepare()
                                                    mediaPlayer.setOnCompletionListener {
                                                        isPlaying = false
                                                    }
                                                    mediaPlayer.start()
                                                    isPlaying = true
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "无法播放音频", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                        contentDescription = "播放",
                                        tint = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${duration}'' 语音消息",
                                        color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            5 -> {
                                val coords = message.content.split(",")
                                val lat = coords.getOrNull(0) ?: "0"
                                val lng = coords.getOrNull(1) ?: "0"
                                val addr = coords.getOrNull(2) ?: "未知位置"

                                Column(
                                    modifier = Modifier.clickable {
                                        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng($addr)"))
                                        context.startActivity(mapIntent)
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.LocationOn,
                                            contentDescription = "位置",
                                            tint = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = addr,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = formatTime(message.sendTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    if (isOwnMessage) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                            contentDescription = if (message.isRead) "已读" else "已发送",
                            tint = if (message.isRead) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("回复") },
                    onClick = {
                        expandedMenu = false
                        onReplyClick()
                    },
                    leadingIcon = { Icon(Icons.Filled.Reply, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("转发") },
                    onClick = {
                        expandedMenu = false
                        onForwardClick()
                    },
                    leadingIcon = { Icon(Icons.Filled.Forward, contentDescription = null) }
                )
                if (isOwnMessage && System.currentTimeMillis() - message.sendTime < 120000) {
                    DropdownMenuItem(
                        text = { Text("撤回") },
                        onClick = {
                            expandedMenu = false
                            onRecallClick()
                        },
                        leadingIcon = { Icon(Icons.Filled.Undo, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
fun LocationPickerDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double, String) -> Unit
) {
    val locations = listOf(
        Triple(39.9042, 116.4074, "北京市 天安门广场"),
        Triple(31.2304, 121.4737, "上海市 东方明珠广播电视塔"),
        Triple(22.5431, 114.0579, "深圳市 腾讯大厦"),
        Triple(23.1291, 113.2644, "广州市 越秀公园")
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择位置") },
        text = {
            Column {
                locations.forEach { loc ->
                    ListItem(
                        headlineContent = { Text(loc.third) },
                        supportingContent = { Text("经度: ${loc.second}, 纬度: ${loc.first}") },
                        leadingContent = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.Red) },
                        modifier = Modifier.clickable { onLocationSelected(loc.first, loc.second, loc.third) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun ForwardDialog(
    onDismiss: () -> Unit,
    onSelectSession: (String) -> Unit
) {
    val mockSessions = listOf(
        "session_1" to "产品开发组",
        "session_2" to "运营反馈群",
        "session_3" to "张三 (私聊)",
        "session_4" to "李四 (私聊)"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择转发的聊天") },
        text = {
            Column {
                mockSessions.forEach { item ->
                    ListItem(
                        headlineContent = { Text(item.second) },
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(item.second.take(1))
                                }
                            }
                        },
                        modifier = Modifier.clickable { onSelectSession(item.first) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "未知文件"
}

private fun getFileSize(context: Context, uri: Uri): Long {
    var result: Long = 0L
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (index != -1) result = cursor.getLong(index)
            }
        } finally {
            cursor?.close()
        }
    }
    return result
}
