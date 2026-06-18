package com.example.myapplication.ui.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.model.ClassQuiz
import com.example.myapplication.data.repository.FirebaseRepository
import com.example.myapplication.ui.navigation.Screen
import androidx.compose.material3.ExperimentalMaterial3Api
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(navController: NavHostController) {
    val repo = remember { FirebaseRepository() }
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Đổi kiểu dữ liệu từ Quiz thành ClassQuiz cho đồng bộ
    var allQuizzesWithClass by remember { mutableStateOf(listOf<Pair<String, ClassQuiz>>()) } // Lưu cặp: Pair(classId, classQuiz)
    var isLoading by remember { mutableStateOf(true) }

    // Logic nâng cao: Lấy bài kiểm tra từ tất cả các lớp học sinh này đã tham gia
    LaunchedEffect(userId) {
        if (userId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("classes")
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { classSnapshot, _ ->
                if (classSnapshot != null && !classSnapshot.isEmpty) {
                    val tempList = mutableListOf<Pair<String, ClassQuiz>>()
                    var loadedClassesCount = 0
                    val totalClasses = classSnapshot.size()

                    for (classDoc in classSnapshot.documents) {
                        val classId = classDoc.id
                        classDoc.reference.collection("class_quizzes").get()
                            .addOnSuccessListener { quizSnapshot ->
                                val quizzes = quizSnapshot.toObjects(ClassQuiz::class.java)
                                quizzes.forEach { quiz ->
                                    tempList.add(Pair(classId, quiz))
                                }
                                loadedClassesCount++
                                // Khi đã quét qua hết các lớp, cập nhật dữ liệu lên UI
                                if (loadedClassesCount == totalClasses) {
                                    allQuizzesWithClass = tempList
                                    isLoading = false
                                }
                            }
                            .addOnFailureListener {
                                loadedClassesCount++
                                if (loadedClassesCount == totalClasses) {
                                    isLoading = false
                                }
                            }
                    }
                } else {
                    isLoading = false
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bài Kiểm Tra Của Bạn") },
                actions = {
                    IconButton(onClick = {
                        repo.logout()
                        // Quay về màn hình Login gốc và xóa toàn bộ stack cũ
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Text("Thoát", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (allQuizzesWithClass.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Hiện tại chưa có bài kiểm tra nào từ các lớp bạn tham gia.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(allQuizzesWithClass) { (classId, quiz) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                // Điều hướng sang màn hình làm bài thi, truyền cả classId và quizId
                                // Nếu dùng dạng Route cũ: navController.navigate("exam_screen/$classId/${quiz.quizId}")
                                // Hoặc điều hướng linh hoạt tùy thuộc cấu trúc NavGraph hiện tại của bạn:
                                navController.navigate("exam_screen/$classId/${quiz.quizId}")
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(quiz.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Thời gian: ${quiz.duration} phút", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}