package com.collaboffice.android.ui.space

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.collaboffice.android.ui.components.AvatarImage
import com.collaboffice.android.ui.components.CollabTextField

data class SpaceMember(
    val userId: String = "",
    val username: String = "",
    val nickname: String = "",
    val avatar: String? = null,
    val role: String = "member", // owner, admin, member, guest
    val joinTime: String = ""
)

data class SpaceMemberUiState(
    val isLoading: Boolean = false,
    val members: List<SpaceMember> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceMemberScreen(
    spaceId: String,
    onNavigateBack: () -> Unit = {},
    viewModel: SpaceMemberViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf<SpaceMember?>(null) }
    var showRemoveDialog by remember { mutableStateOf<SpaceMember?>(null) }

    LaunchedEffect(spaceId) { viewModel.loadMembers(spaceId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("成员管理 (${uiState.members.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showInviteDialog = true }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "邀请成员")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            CollabTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearch(it) },
                label = "搜索成员...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                trailingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                }
            )

            // Role filter chips
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("全部 ${uiState.members.size}") }
                )
                val admins = uiState.members.count { it.role == "admin" || it.role == "owner" }
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("管理员 $admins") }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val filtered = if (uiState.searchQuery.isBlank()) uiState.members
                    else uiState.members.filter {
                        it.nickname.contains(uiState.searchQuery, true) ||
                                it.username.contains(uiState.searchQuery, true)
                    }

                    items(filtered, key = { it.userId }) { member ->
                        MemberItem(
                            member = member,
                            onChangeRole = { showRoleDialog = member },
                            onRemove = { showRemoveDialog = member }
                        )
                    }
                }
            }
        }
    }

    // Invite dialog
    if (showInviteDialog) {
        InviteMemberDialog(
            onDismiss = { showInviteDialog = false },
            onInvite = { username ->
                viewModel.inviteMember(spaceId, username)
                showInviteDialog = false
            }
        )
    }

    // Role change dialog
    showRoleDialog?.let { member ->
        RoleChangeDialog(
            member = member,
            onDismiss = { showRoleDialog = null },
            onChangeRole = { newRole ->
                viewModel.changeMemberRole(spaceId, member.userId, newRole)
                showRoleDialog = null
            }
        )
    }

    // Remove confirmation
    showRemoveDialog?.let { member ->
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            title = { Text("移除成员") },
            text = { Text("确定要将 ${member.nickname} 从空间中移除吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMember(spaceId, member.userId)
                        showRemoveDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun MemberItem(
    member: SpaceMember,
    onChangeRole: () -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarImage(url = member.avatar, name = member.nickname, size = 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.nickname, fontWeight = FontWeight.Medium)
                Text(
                    "@${member.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Role badge
            val roleLabel = when (member.role) {
                "owner" -> "拥有者"
                "admin" -> "管理员"
                "guest" -> "访客"
                else -> "成员"
            }
            val roleColor = when (member.role) {
                "owner" -> MaterialTheme.colorScheme.primary
                "admin" -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outline
            }
            SuggestionChip(
                onClick = {},
                label = { Text(roleLabel, style = MaterialTheme.typography.labelSmall) }
            )

            if (member.role != "owner") {
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("修改角色") },
                            onClick = { expanded = false; onChangeRole() },
                            leadingIcon = { Icon(Icons.Filled.ManageAccounts, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("移除成员") },
                            onClick = { expanded = false; onRemove() },
                            leadingIcon = { Icon(Icons.Filled.PersonRemove, null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onInvite: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("邀请成员") },
        text = {
            Column {
                Text("输入用户名或手机号邀请加入空间", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                CollabTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "用户名 / 手机号"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onInvite(username) },
                enabled = username.isNotBlank()
            ) { Text("邀请") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun RoleChangeDialog(
    member: SpaceMember,
    onDismiss: () -> Unit,
    onChangeRole: (String) -> Unit
) {
    val roles = listOf("admin" to "管理员", "member" to "成员", "guest" to "访客")
    var selected by remember { mutableStateOf(member.role) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改 ${member.nickname} 的角色") },
        text = {
            Column {
                roles.forEach { (roleKey, roleLabel) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = roleKey }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == roleKey, onClick = { selected = roleKey })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(roleLabel)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onChangeRole(selected) }) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
