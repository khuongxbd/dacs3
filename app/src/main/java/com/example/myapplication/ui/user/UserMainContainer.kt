package com.example.myapplication.ui.user

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
import com.example.myapplication.data.model.ClassModel
import com.example.myapplication.data.model.ClassQuiz // 🌟 Nhớ import ClassQuiz nếu chưa có nhé Khương

// Các Route nội bộ bên trong BottomBar của User
sealed class UserTab(val route: String, val title: String, val icon: ImageVector) {
    object Home : UserTab("u_home", "Trang chủ", Icons.Default.Home)
    object Classes : UserTab("u_classes", "Lớp học", Icons.Default.Star)
    object Search : UserTab("u_search", "Tìm kiếm", Icons.Default.Search)
    object Notifications : UserTab("u_notif", "Thông báo", Icons.Default.Notifications)
    object Profile : UserTab("u_profile", "Cá nhân", Icons.Default.Person)
}

@Composable
fun UserMainContainer(navController: NavHostController) {
    val subNavController = rememberNavController()
    val items = listOf(UserTab.Home, UserTab.Classes, UserTab.Search, UserTab.Notifications, UserTab.Profile)
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
            startDestination = UserTab.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(UserTab.Home.route) {
                UserHomeScreen(navController = navController)
            }

            composable(UserTab.Classes.route) {
                // Quản lý trạng thái màn hình con nội bộ
                var currentSubScreen by remember { mutableStateOf("list") }

                // QUẢN LÝ THÔNG MINH: Lưu trữ trực tiếp đối tượng lớp học và bài kiểm tra
                var selectedClass by remember { mutableStateOf<ClassModel?>(null) }
                var selectedQuiz by remember { mutableStateOf<ClassQuiz?>(null) } // 🌟 THÊM: State giữ bài kiểm tra đang chọn giải
                var targetSetId by remember { mutableStateOf("") }

                when (currentSubScreen) {
                    "list" -> {
                        UserClassScreen(onClassClick = { clickedClass ->
                            selectedClass = clickedClass
                            currentSubScreen = "detail"
                        })
                    }
                    "detail" -> {
                        if (selectedClass != null) {
                            ClassDetailScreen(
                                classObj = selectedClass!!,
                                onBack = {
                                    currentSubScreen = "list"
                                    selectedClass = null
                                },
                                onNavigateToFlashcard = { setId ->
                                    targetSetId = setId
                                    currentSubScreen = "study"
                                },
                                onNavigateToQuizPractice = { setId ->
                                    // 🚀 ĐÃ CẬP NHẬT: Lưu mã bộ từ vựng và mở màn luyện trắc nghiệm từ
                                    targetSetId = setId
                                    currentSubScreen = "quiz_practice"
                                },
                                onNavigateToTypingPractice = { setId ->
                                    // 🚀 ĐÃ CẬP NHẬT: Lưu mã bộ từ vựng và mở màn luyện gõ từ
                                    targetSetId = setId
                                    currentSubScreen = "typing_practice"
                                },
                                onNavigateToClassQuiz = { quizObj ->
                                    selectedQuiz = quizObj
                                    currentSubScreen = "take_quiz"
                                }
                            )
                        }
                    }
                    "study" -> {
                        if (selectedClass != null) {
                            FlashcardStudyScreen(
                                classId = selectedClass!!.classId,
                                setId = targetSetId,
                                onBack = { currentSubScreen = "detail" }
                            )
                        }
                    }
                    // 🌟 THÊM: Màn hình trắc nghiệm từ vựng tự do (Học phần được giao)
                    "quiz_practice" -> {
                        if (selectedClass != null) {
                            // Gọi đến màn hình trắc nghiệm từ vựng của cậu
                            // Truyền vào setId để màn hình đó tự lấy danh sách từ ra luyện tập
                            VocabQuizPracticeScreen(
                                setId = targetSetId,
                                onBack = { currentSubScreen = "detail" }
                            )
                        }
                    }
                    // 🌟 THÊM: Màn hình gõ từ vựng (Nhìn nghĩa viết từ)
                    "typing_practice" -> {
                        if (selectedClass != null) {
                            // Gọi đến màn hình gõ từ vựng của cậu
                            VocabTypingPracticeScreen(
                                setId = targetSetId,
                                onBack = { currentSubScreen = "detail" }
                            )
                        }
                    }
                    "take_quiz" -> {
                        if (selectedQuiz != null) {
                            StudentQuizScreen(
                                quiz = selectedQuiz!!,
                                onBack = {
                                    currentSubScreen = "detail"
                                    selectedQuiz = null
                                }
                            )
                        }
                    }
                }
            }

            composable(UserTab.Search.route) { UserSearchScreen() }

            composable(UserTab.Notifications.route) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Màn hình Thông báo học tập")
                }
            }

            composable(UserTab.Profile.route) {
                UserProfileScreen(rootNavController = navController)
            }
        }
    }
}