package com.collaboffice.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.collaboffice.feature.auth.LoginScreen
import com.collaboffice.feature.auth.RegisterScreen
import com.collaboffice.feature.document.DocEditScreen
import com.collaboffice.feature.chat.ChatDetailScreen
import com.collaboffice.feature.task.TaskDetailScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    // TODO: Check login state from TokenManager
    val startDestination = Routes.AUTH

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth flow
        navigation(startDestination = Routes.LOGIN, route = Routes.AUTH) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Routes.REGISTER)
                    }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Main flow with bottom navigation
        composable(Routes.MAIN) {
            MainScreen(navController = navController)
        }

        // Document edit flow
        composable(
            route = Routes.DOC_EDIT,
            arguments = listOf(navArgument("docId") { type = NavType.StringType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getString("docId") ?: ""
            DocEditScreen(
                docId = docId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Chat detail flow
        composable(
            route = Routes.CHAT_DETAIL,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ChatDetailScreen(
                sessionId = sessionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Task detail flow
        composable(
            route = Routes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            TaskDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
