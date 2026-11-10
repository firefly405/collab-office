package com.collaboffice.feature.search

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

sealed class SearchResult {
    data class DocumentResult(
        val docId: String, val title: String, val snippet: String,
        val updatedAt: String, val docType: Int = 1
    ) : SearchResult()

    data class MessageResult(
        val messageId: String, val content: String, val senderName: String,
        val channelName: String, val timestamp: String
    ) : SearchResult()

    data class FileResult(
        val fileId: String, val fileName: String, val fileSize: Long,
        val filePath: String, val mimeType: String
    ) : SearchResult()

    data class UserResult(
        val userId: String, val username: String, val nickname: String,
        val avatar: String? = null
    ) : SearchResult()
}

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val activeTab: Int = 0, // 0=全部, 1=文档, 2=消息, 3=文件, 4=用户
    val documents: List<SearchResult.DocumentResult> = emptyList(),
    val messages: List<SearchResult.MessageResult> = emptyList(),
    val files: List<SearchResult.FileResult> = emptyList(),
    val users: List<SearchResult.UserResult> = emptyList(),
    val recentSearches: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit = {},
    onOpenDocument: (String) -> Unit = {},
    onOpenChat: (String) -> Unit = {},
    onOpenFile: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("全部", "文档", "消息", "文件", "用户")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.updateQuery(it) },
                        placeholder = { Text("搜索文档、消息、文件...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateQuery("") }) {
                                    Icon(Icons.Filled.Close, "清除")
                                }
                            }
                        },
                        leadingIcon = { Icon(Icons.Filled.Search, null) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.query.isNotEmpty()) {
                // Tab row
                ScrollableTabRow(
                    selectedTabIndex = uiState.activeTab,
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        val count = when (index) {
                            1 -> uiState.documents.size
                            2 -> uiState.messages.size
                            3 -> uiState.files.size
                            4 -> uiState.users.size
                            else -> uiState.documents.size + uiState.messages.size + uiState.files.size + uiState.users.size
                        }
                        Tab(
                            selected = uiState.activeTab == index,
                            onClick = { viewModel.setTab(index) },
                            text = { Text("$title ($count)") }
                        )
                    }
                }

                if (uiState.isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Documents
                        if (uiState.activeTab == 0 || uiState.activeTab == 1) {
                            if (uiState.documents.isNotEmpty()) {
                                item {
                                    if (uiState.activeTab == 0) {
                                        Text("文档", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                                items(uiState.documents) { doc ->
                                    DocumentResultItem(doc) { onOpenDocument(doc.docId) }
                                }
                            }
                        }

                        // Messages
                        if (uiState.activeTab == 0 || uiState.activeTab == 2) {
                            if (uiState.messages.isNotEmpty()) {
                                item {
                                    if (uiState.activeTab == 0) {
                                        Text("消息", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                                items(uiState.messages) { msg ->
                                    MessageResultItem(msg) { onOpenChat(msg.messageId) }
                                }
                            }
                        }

                        // Files
                        if (uiState.activeTab == 0 || uiState.activeTab == 3) {
                            if (uiState.files.isNotEmpty()) {
                                item {
                                    if (uiState.activeTab == 0) {
                                        Text("文件", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                                items(uiState.files) { file ->
                                    FileResultItem(file) { onOpenFile(file.fileId) }
                                }
                            }
                        }

                        // Users
                        if (uiState.activeTab == 0 || uiState.activeTab == 4) {
                            if (uiState.users.isNotEmpty()) {
                                item {
                                    if (uiState.activeTab == 0) {
                                        Text("用户", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                                items(uiState.users) { user ->
                                    UserResultItem(user)
                                }
                            }
                        }

                        // Empty state
                        if (!uiState.isSearching &&
                            uiState.documents.isEmpty() && uiState.messages.isEmpty() &&
                            uiState.files.isEmpty() && uiState.users.isEmpty()
                        ) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.SearchOff, null, modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("未找到相关结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Recent searches
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("最近搜索", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (uiState.recentSearches.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearRecentSearches() }) {
                                Text("清除")
                            }
                        }
                    }
                    uiState.recentSearches.forEach { query ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateQuery(query); viewModel.search() }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(query)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentResultItem(doc: SearchResult.DocumentResult, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = when (doc.docType) {
                1 -> Icons.Filled.Description
                2 -> Icons.Filled.TableChart
                3 -> Icons.Filled.AccountTree
                4 -> Icons.Filled.Draw
                else -> Icons.Filled.Description
            }
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(doc.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(doc.snippet, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(doc.updatedAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun MessageResultItem(msg: SearchResult.MessageResult, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Filled.Chat, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row {
                    Text(msg.senderName, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("# ${msg.channelName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Text(msg.content, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(msg.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun FileResultItem(file: SearchResult.FileResult, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.InsertDriveFile, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.fileName, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(file.filePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Text(formatFileSize(file.fileSize), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun UserResultItem(user: SearchResult.UserResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            com.collaboffice.android.ui.components.AvatarImage(url = user.avatar, name = user.nickname, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(user.nickname, fontWeight = FontWeight.Medium)
                Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.1f GB", gb)
}
