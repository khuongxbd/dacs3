package com.example.myapplication.ui.user

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.model.ClassModel
import com.example.myapplication.data.model.ClassQuiz
import com.example.myapplication.data.model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    var globalSelectedQuiz by remember { mutableStateOf<ClassQuiz?>(null) }

    var unreadCount by remember { mutableStateOf(0) }
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        if (userId.isEmpty()) return@LaunchedEffect
        db.collection("classes").whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val classIds = snapshot.documents.map { it.id }
                if (classIds.isNotEmpty()) {
                    db.collection("notifications")
                        .addSnapshotListener { notiSnap, _ ->
                            if (notiSnap != null) {
                                val notis = notiSnap.toObjects(NotificationModel::class.java)
                                unreadCount = notis.count { it.targetClassId in classIds && !it.readBy.contains(userId) }
                            }
                        }
                } else {
                    unreadCount = 0
                }
            }
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F0F0F),
                tonalElevation = 8.dp,
                modifier = Modifier.border(0.5.dp, Color.White.copy(0.1f))
            ) {
                items.forEachIndexed { index, tab ->
                    val isSelected = selectedItem == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            selectedItem = index
                            subNavController.navigate(tab.route) {
                                popUpTo(subNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Color(0xFFCCFF00) else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                color = if (isSelected) Color(0xFFCCFF00) else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent // Bỏ cái hình tròn nền mặc định
                        )
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

            composable("student_quiz_screen") {
                if (globalSelectedQuiz != null) {
                    StudentQuizScreen(
                        quiz = globalSelectedQuiz!!,
                        onBack = {
                            globalSelectedQuiz = null
                            subNavController.popBackStack()

                            val currentRoute = subNavController.currentDestination?.route
                            selectedItem = if (currentRoute == UserTab.Home.route) 0 else 1
                        }
                    )
                }
            }

            composable(UserTab.Search.route) { UserSearchScreen() }

            composable(UserTab.Notifications.route) {
                UserNotificationScreen(onBack = {
                    subNavController.popBackStack()
                })
            }

            composable(UserTab.Profile.route) {
                UserProfileScreen(rootNavController = navController)
            }

            composable("review_wrongs_screen") {
                ReviewWrongQuestionsScreen(
                    onBack = {
                        subNavController.popBackStack()
                        selectedItem = 0
                    }
                )
            }
        }
    }
}