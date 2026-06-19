package com.example.myapplication.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
// Định nghĩa các màu chủ đạo
val LimeGreen = Color(0xFF00C853)
val BlackColor = Color.Black
val WhiteColor = Color.White

@Composable
fun AdminMainContainer(navController: NavHostController) {
    val subNavController = rememberNavController()
    val items = listOf(AdminTab.Classes, AdminTab.Vocabulary, AdminTab.Quiz)
    var selectedItem by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            // Tùy chỉnh thanh điều hướng cứng cáp
            NavigationBar(
                containerColor = WhiteColor,
                modifier = Modifier
                    .border(2.dp, BlackColor) // Viền đen toàn thanh điều hướng
                    .shadow(8.dp) // Tạo độ nổi 3D
            ) {
                items.forEachIndexed { index, tab ->
                    val isSelected = selectedItem == index
                    NavigationBarItem(
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) BlackColor else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                color = BlackColor
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            selectedItem = index
                            subNavController.navigate(tab.route) {
                                popUpTo(subNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = LimeGreen // Màu nền khi chọn là Xanh lá mạ
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = subNavController,
            startDestination = AdminTab.Classes.route,
            modifier = Modifier.padding(paddingValues).background(WhiteColor) // Đảm bảo nền trắng
        ) {
            composable(AdminTab.Classes.route) {
                AdminClassScreen(onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
            composable(AdminTab.Vocabulary.route) { AdminVocabularyScreen() }
            composable(AdminTab.Quiz.route) { AdminQuizScreen() }
        }
    }
}