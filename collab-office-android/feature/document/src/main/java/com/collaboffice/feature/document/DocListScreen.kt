package com.collaboffice.feature.document

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.collaboffice.core.model.doc.Document

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocListScreen(
    onDocClick: (String) -> Unit = {},
    viewModel: DocListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadDocuments() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文档") },
                actions = {
                    IconButton(onClick = { /* search */ }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateMenu = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新建")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.documents, key = { it.docId }) { doc ->
                    DocItem(doc = doc, onClick = { onDocClick(doc.docId) })
                }
            }
        }
    }

    if (showCreateMenu) {
        CreateDocBottomSheet(
            onDismiss = { showCreateMenu = false },
            onCreate = { type, title ->
                viewModel.createDocument(type, title)
                showCreateMenu = false
            }
        )
    }
}

@Composable
fun DocItem(doc: Document, onClick: () -> Unit) {
    val icon = when (doc.type) {
        1 -> Icons.Filled.Description
        2 -> Icons.Filled.TableChart
        3 -> Icons.Filled.AccountTree
        4 -> Icons.Filled.Draw
        else -> Icons.Filled.InsertDriveFile
    }
    val typeName = when (doc.type) {
        1 -> "文档"
        2 -> "表格"
        3 -> "思维导图"
        4 -> "白板"
        else -> "文件"
    }
    ListItem(
        headlineContent = {
            Text(doc.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text("$typeName · ${doc.creatorName ?: ""} · ${doc.updateTime ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDocBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (type: Int, title: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableIntStateOf(1) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("新建", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(1 to "文档", 2 to "表格", 3 to "思维导图", 4 to "白板").forEach { (type, name) ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onCreate(selectedType, title.ifBlank { "未命名" }) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("创建")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
