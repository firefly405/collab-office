package com.collaboffice.feature.task

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.collaboffice.core.model.task.TaskComment
import com.collaboffice.core.model.task.TaskDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var commentText by remember { mutableStateOf("") }
    var showAssigneeDropdown by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.uploadAttachment(it, context) }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.taskDetail == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val task = uiState.taskDetail
            if (task == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("未找到任务详情")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Task info panel
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            Text(
                                text = task.description ?: "无描述信息",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Info grid
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Status
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("状态：", fontWeight = FontWeight.SemiBold)
                                        Text(when (task.status) {
                                            0 -> "待办"
                                            1 -> "进行中"
                                            2 -> "已完成"
                                            else -> "待办"
                                        })
                                    }

                                    // Priority
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.PriorityHigh, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("优先级：", fontWeight = FontWeight.SemiBold)
                                        Text(when (task.priority) {
                                            1 -> "紧急"
                                            2 -> "高"
                                            3 -> "中"
                                            else -> "低"
                                        })
                                    }

                                    // Assignee
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("指派给：", fontWeight = FontWeight.SemiBold)
                                        
                                        val assigneeName = uiState.members.find { it.id == task.assigneeId }?.nickname 
                                            ?: uiState.members.find { it.id == task.assigneeId }?.username 
                                            ?: "未指派"
                                        
                                        Box {
                                            Text(
                                                text = assigneeName,
                                                modifier = Modifier.clickable { showAssigneeDropdown = true },
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            DropdownMenu(
                                                expanded = showAssigneeDropdown,
                                                onDismissRequest = { showAssigneeDropdown = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("未指派") },
                                                    onClick = {
                                                        viewModel.updateTask(mapOf("assigneeId" to null))
                                                        showAssigneeDropdown = false
                                                    }
                                                )
                                                uiState.members.forEach { user ->
                                                    DropdownMenuItem(
                                                        text = { Text(user.nickname ?: user.username) },
                                                        onClick = {
                                                            viewModel.updateTask(mapOf("assigneeId" to user.id))
                                                            showAssigneeDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Due Date
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("截止日期：", fontWeight = FontWeight.SemiBold)
                                        Text(task.dueDate ?: "无截止日期")
                                    }
                                }
                            }
                        }

                        // Comments Section Title
                        item {
                            Text(
                                text = "讨论与附件",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Comments List
                        items(task.comments) { comment ->
                            CommentItem(comment = comment)
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Upload Progress Bar
                    if (uiState.isUploading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Add comment bar
                    Surface(
                        tonalElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                                .imePadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { filePicker.launch("*/*") }) {
                                Icon(Icons.Filled.AttachFile, contentDescription = "添加附件")
                            }
                            TextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("说点什么...") },
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        viewModel.addComment(commentText)
                                        commentText = ""
                                    }
                                },
                                enabled = commentText.isNotBlank()
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = "发送", tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: TaskComment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(36.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = (comment.userName ?: "匿").take(1).uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.userName ?: "匿名用户",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.createTime?.substringBefore(".")?.replace("T", " ") ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            val content = comment.content
            if (content.startsWith("[附件:") && content.contains("](") && content.endsWith(")")) {
                // Render file link beautifully
                val fileName = content.substringAfter("[附件: ").substringBefore("](")
                val fileUrl = content.substringAfter("](").substringBeforeLast(")")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(fileName, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            Text("已成功关联为任务附件", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
