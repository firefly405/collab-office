package com.collaboffice.feature.meeting

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingListScreen(
    onCreateMeeting: () -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("会议") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateMeeting,
                icon = { Icon(Icons.Filled.VideoCall, contentDescription = null) },
                text = { Text("发起会议") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.VideoCall, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Text("暂无会议", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("点击下方按钮发起新会议", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
