package com.collaboffice.android.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.collaboffice.feature.document.DocListScreen

import com.collaboffice.feature.chat.ChatListScreen
import com.collaboffice.feature.task.TaskBoardScreen
import com.collaboffice.feature.file.FileListScreen
import com.collaboffice.feature.auth.ProfileScreen

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.CHAT, "消息", Icons.Filled.Chat, Icons.Outlined.Chat),
    BottomNavItem(Routes.DOCUMENT, "文档", Icons.Filled.Description, Icons.Outlined.Description),
    BottomNavItem(Routes.TASK, "任务", Icons.Filled.Task, Icons.Outlined.Task),
    BottomNavItem(Routes.FILE, "文件", Icons.Filled.Folder, Icons.Outlined.Folder),
    BottomNavItem(Routes.PROFILE, "我的", Icons.Filled.Person, Icons.Outlined.Person),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 退出登录: 清空回退栈并跳转到 AUTH 嵌套图（其 startDestination = LOGIN）
    val onLogout: () -> Unit = {
        navController.navigate(Routes.AUTH) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = Routes.CHAT,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.CHAT) {
                ChatListScreen(
                    onSessionClick = { sessionId ->
                        navController.navigate("chat/$sessionId")
                    }
                )
            }
            composable(Routes.DOCUMENT) {
                DocListScreen(
                    onDocClick = { docId ->
                        navController.navigate("doc/edit/$docId")
                    }
                )
            }
            composable(Routes.TASK) {
                TaskBoardScreen(
                    onTaskClick = { taskId ->
                        navController.navigate("task/$taskId")
                    }
                )
            }
            composable(Routes.FILE) {
                FileListScreen()
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onNavigateToEdit = { /* 后续接入编辑资料页 */ },
                    onNavigateToSettings = { /* 后续接入设置页 */ },
                    onNavigateToDevices = { /* 后续接入设备管理页 */ },
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Surface {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
        )
    }
}
