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
import com.example.myapplication.data.model.ClassQuiz

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

    // 🔥 STATE GIỮ BÀI THI ĐANG CHỌN ĐỂ TRUYỀN SANG ROUTE ĐỘC LẬP
    var globalSelectedQuiz by remember { mutableStateOf<ClassQuiz?>(null) }

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
            // ==================== ROUTE 1: TRANG CHỦ ====================
            composable(UserTab.Home.route) {
                UserHomeScreen(
                    navController = navController,
                    onNavigateToQuiz = { _, quizObj ->
                        globalSelectedQuiz = quizObj
                        subNavController.navigate("student_quiz_screen") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToReviewWrongs = {
                        subNavController.navigate("review_wrongs_screen") { launchSingleTop = true }
                    }
                )
            }

            // ==================== ROUTE 2: LỚP HỌC ====================
            composable(UserTab.Classes.route) {
                var currentSubScreen by remember { mutableStateOf("list") }
                var selectedClass by remember { mutableStateOf<ClassModel?>(null) }
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
                                    targetSetId = setId
                                    currentSubScreen = "quiz_practice"
                                },
                                onNavigateToTypingPractice = { setId ->
                                    targetSetId = setId
                                    currentSubScreen = "typing_practice"
                                },
                                onNavigateToClassQuiz = { quizObj ->
                                    // 🔥 SỬA ĐỂ ĐỒNG BỘ: Gán dữ liệu vào state chung và nhảy sang Route độc lập
                                    globalSelectedQuiz = quizObj
                                    subNavController.navigate("student_quiz_screen") {
                                        launchSingleTop = true
                                    }
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
                    "quiz_practice" -> {
                        if (selectedClass != null) {
                            VocabQuizPracticeScreen(
                                setId = targetSetId,
                                onBack = { currentSubScreen = "detail" }
                            )
                        }
                    }
                    "typing_practice" -> {
                        if (selectedClass != null) {
                            VocabTypingPracticeScreen(
                                setId = targetSetId,
                                onBack = { currentSubScreen = "detail" }
                            )
                        }
                    }
                }
            }

            // ==================== 🔥 ROUTE: MÀN HÌNH LÀM BÀI ĐỘC LẬP ====================
            composable("student_quiz_screen") {
                if (globalSelectedQuiz != null) {
                    StudentQuizScreen(
                        quiz = globalSelectedQuiz!!,
                        onBack = {
                            globalSelectedQuiz = null
                            subNavController.popBackStack()

                            // Đồng bộ lại chỉ số highlight mục BottomBar dựa vào màn hình trước đó
                            val currentRoute = subNavController.currentDestination?.route
                            selectedItem = if (currentRoute == UserTab.Home.route) 0 else 1
                        }
                    )
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

            // ==================== 🔥 ROUTE: MÀN HÌNH ÔN TẬP CÂU SAI ĐỘC LẬP ====================
            composable("review_wrongs_screen") {
                ReviewWrongQuestionsScreen(
                    onBack = {
                        subNavController.popBackStack()
                        // Trả màu Highlight BottomBar về lại Home (0) sau khi thoát ôn tập
                        selectedItem = 0
                    }
                )
            }
        }
    }
}