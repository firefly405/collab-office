package com.collaboffice.feature.task

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.collaboffice.core.model.task.KanbanColumn
import com.collaboffice.core.model.task.Task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBoardScreen(
    onTaskClick: (String) -> Unit,
    viewModel: TaskBoardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadKanban() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务看板") },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "新建任务")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.columns.forEachIndexed { index, column ->
                    KanbanColumnView(
                        column = column,
                        onTaskClick = onTaskClick,
                        onMoveLeft = if (index > 0) { taskId -> viewModel.updateTaskStatus(taskId, index - 1) } else null,
                        onMoveRight = if (index < uiState.columns.size - 1) { taskId -> viewModel.updateTaskStatus(taskId, index + 1) } else null
                    )
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCreateDialog = false
                    newTaskTitle = ""
                },
                title = { Text("新建任务") },
                text = {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("任务标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                viewModel.createTask(newTaskTitle)
                                showCreateDialog = false
                                newTaskTitle = ""
                            }
                        },
                        enabled = newTaskTitle.isNotBlank()
                    ) {
                        Text("创建")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCreateDialog = false
                        newTaskTitle = ""
                    }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun KanbanColumnView(
    column: KanbanColumn,
    onTaskClick: (String) -> Unit,
    onMoveLeft: ((String) -> Unit)?,
    onMoveRight: ((String) -> Unit)?
) {
    Card(
        modifier = Modifier.width(300.dp).fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = column.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Badge { Text("${column.tasks.size}") }
            }
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(column.tasks, key = { it.taskId }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onTaskClick(task.taskId) },
                        onMoveLeft = if (onMoveLeft != null) { { onMoveLeft(task.taskId) } } else null,
                        onMoveRight = if (onMoveRight != null) { { onMoveRight(task.taskId) } } else null
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onMoveLeft: (() -> Unit)?,
    onMoveRight: (() -> Unit)?
) {
    val priorityColor = when (task.priority) {
        1 -> MaterialTheme.colorScheme.error
        2 -> MaterialTheme.colorScheme.tertiary
        3 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = priorityColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = when (task.priority) { 1 -> "紧急"; 2 -> "高"; 3 -> "中"; else -> "低" },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = priorityColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                task.assigneeName?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            task.dueDate?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            // Quick status move actions
            if (onMoveLeft != null || onMoveRight != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMoveLeft != null) {
                        IconButton(onClick = onMoveLeft, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "左移", modifier = Modifier.size(18.dp))
                        }
                    }
                    if (onMoveRight != null) {
                        IconButton(onClick = onMoveRight, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "右移", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
