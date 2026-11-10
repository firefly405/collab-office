package com.collaboffice.feature.file

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.collaboffice.core.model.file.FileItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    viewModel: FileListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFileForMenu by remember { mutableStateOf<FileItem?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.uploadFile(it, context) }
        }
    )

    LaunchedEffect(uiState.currentFolderId) {
        viewModel.loadFiles(uiState.currentFolderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.currentFolderId == null) "文件" else "文件夹内容") },
                navigationIcon = {
                    if (uiState.currentFolderId != null) {
                        IconButton(onClick = { viewModel.loadFiles(null) }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "返回根目录")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { filePicker.launch("*/*") }) {
                Icon(Icons.Filled.Upload, contentDescription = "上传")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Upload Progress
            if (uiState.isUploading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("正在上传文件...", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = uiState.uploadProgress ?: 0f,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            if (uiState.isLoading && uiState.files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("没有文件或文件夹", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.files, key = { it.fileId }) { file ->
                        FileListItem(
                            file = file,
                            onClick = {
                                if (file.isFolder) {
                                    viewModel.openFile(file)
                                } else {
                                    selectedFileForMenu = file
                                }
                            }
                        )
                    }
                }
            }
        }

        // Context actions bottom sheet / dialog
        selectedFileForMenu?.let { file ->
            AlertDialog(
                onDismissRequest = { selectedFileForMenu = null },
                title = { Text(file.fileName) },
                text = { Text("选择您想执行的操作：") },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = {
                                selectedFileForMenu = null
                                val url = file.downloadUrl ?: "http://10.0.2.2:8080/api/v1/file/download/${file.fileId}"
                                if (file.fileType?.contains("image") == true) {
                                    previewImageUrl = url
                                } else {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }
                            }
                        ) {
                            Text("预览")
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(
                            onClick = {
                                viewModel.downloadFile(file, context)
                                selectedFileForMenu = null
                            }
                        ) {
                            Text("下载")
                        }

                        TextButton(
                            onClick = { selectedFileForMenu = null }
                        ) {
                            Text("取消")
                        }
                    }
                }
            )
        }

        // Image Preview Dialog
        previewImageUrl?.let { imageUrl ->
            Dialog(onDismissRequest = { previewImageUrl = null }) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(400.dp).padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "图片预览",
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { previewImageUrl = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileListItem(file: FileItem, onClick: () -> Unit) {
    val icon = when {
        file.isFolder -> Icons.Filled.Folder
        file.fileType?.contains("image") == true -> Icons.Filled.Image
        file.fileType?.contains("pdf") == true -> Icons.Filled.PictureAsPdf
        file.fileType?.contains("video") == true -> Icons.Filled.VideoFile
        else -> Icons.Filled.InsertDriveFile
    }
    val iconColor = when {
        file.isFolder -> MaterialTheme.colorScheme.primary
        file.fileType?.contains("image") == true -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ListItem(
        headlineContent = {
            Text(file.fileName, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            if (!file.isFolder) {
                Text(formatFileSize(file.fileSize), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        leadingContent = { Icon(icon, contentDescription = null, tint = iconColor) },
        modifier = Modifier.clickable { onClick() }
    )
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "${bytes} B"
    bytes < 1048576 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / 1048576.0)
}
