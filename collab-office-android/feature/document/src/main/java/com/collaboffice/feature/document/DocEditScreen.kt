package com.collaboffice.feature.document

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocEditScreen(
    docId: String,
    docTitle: String = "文档",
    onNavigateBack: () -> Unit = {},
    viewModel: DocEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(docId) {
        viewModel.loadDocument(docId)
        viewModel.joinDocument(docId)
    }

    DisposableEffect(docId) {
        onDispose {
            viewModel.leaveDocument(docId)
        }
    }

    // Listen for remote operations and apply to WebView
    LaunchedEffect(uiState.pendingRemoteOps) {
        uiState.pendingRemoteOps.forEach { op ->
            webViewRef?.evaluateJavascript(
                "EditorAPI.applyRemoteOperation('${op.replace("'", "\\'")}');",
                null
            )
        }
        if (uiState.pendingRemoteOps.isNotEmpty()) {
            viewModel.clearPendingOps()
        }
    }

    // Listen for cursor updates
    LaunchedEffect(uiState.remoteCursors) {
        uiState.remoteCursors.forEach { (userId, cursor) ->
            webViewRef?.evaluateJavascript(
                "EditorAPI.showCursor('$userId', '${cursor.name}', '${cursor.color}', ${cursor.index}, ${cursor.length});",
                null
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val typeName = when (uiState.docType) {
                            1 -> "文档"
                            2 -> "表格"
                            3 -> "思维导图"
                            4 -> "白板"
                            else -> "文档"
                        }
                        Text(docTitle, fontWeight = FontWeight.SemiBold)
                        Text(
                            "$typeName · ${if (uiState.collaborators.isNotEmpty()) "${uiState.collaborators.size} 人在线协作" else "离线保存"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // Connection status indicator
                    val statusColor = if (uiState.isConnected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                    Icon(
                        if (uiState.isConnected) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.saveDocument(docId) }) {
                        Icon(Icons.Filled.Save, contentDescription = "保存")
                    }
                    IconButton(onClick = { /* version history */ }) {
                        Icon(Icons.Filled.History, contentDescription = "版本历史")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AndroidView(
                factory = { context ->
                    createEditorWebView(context, viewModel).also { webViewRef = it }
                },
                modifier = Modifier.fillMaxSize().padding(padding),
                update = { webView ->
                    // Set initial content when loaded
                    uiState.initialContent?.let { content ->
                        webView.evaluateJavascript(
                            "EditorAPI.initEditor(${uiState.docType}, '${content.replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r")}');",
                            null
                        )
                        viewModel.onContentApplied()
                    }
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createEditorWebView(
    context: android.content.Context,
    viewModel: DocEditViewModel
): WebView {
    return WebView(context).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
        }

        addJavascriptInterface(object {
            @JavascriptInterface
            fun onLocalOperation(operationJson: String) {
                viewModel.onLocalOperation(operationJson)
            }

            @JavascriptInterface
            fun onContentChanged(contentJson: String) {
                viewModel.onContentChanged(contentJson)
            }

            @JavascriptInterface
            fun onCursorChange(cursorJson: String) {
                viewModel.onCursorChange(cursorJson)
            }

            @JavascriptInterface
            fun onEditorReady() {
                viewModel.onEditorReady()
            }

            @JavascriptInterface
            fun onEditorError(errorJson: String) {
                android.util.Log.e("EditorWebView", "JS Error: $errorJson")
            }
        }, "AndroidBridge")

        webViewClient = WebViewClient()
        loadUrl("file:///android_asset/editor/index.html")
    }
}
