package com.example.myapplication.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.navigation.Screen

sealed class AdminTab(val route: String, val title: String, val icon: ImageVector) {
    object Classes : AdminTab("a_classes", "Lớp", Icons.Default.Home)
    object Vocabulary : AdminTab("a_vocab", "Từ vựng", Icons.Default.List)
    object Quiz : AdminTab("a_quiz", "Bài kiểm tra", Icons.Default.CheckCircle)
}

@Composable
fun AdminMainContainer(navController: NavHostController) {
    val subNavController = rememberNavController()
    val items = listOf(AdminTab.Classes, AdminTab.Vocabulary, AdminTab.Quiz)
    var selectedItem by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            subNavController.navigate(tab.route) {
                                popUpTo(subNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = subNavController,
            startDestination = AdminTab.Classes.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // DÒNG MỚI ĐÃ FIX:
            composable(AdminTab.Classes.route) {
                AdminClassScreen(onLogoutSuccess = {
                    // Khi Admin bấm đăng xuất thành công, điều hướng quay về màn hình Login gốc
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
            composable(AdminTab.Vocabulary.route) { AdminVocabularyScreen() }
            composable(AdminTab.Quiz.route) {
                AdminQuizScreen()
            }
        }
    }
}