package com.example.myapplication.ui.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.data.model.ClassQuiz
import com.example.myapplication.data.model.WrongQuestionSnapshot
import com.example.myapplication.data.repository.FirebaseRepository
import com.example.myapplication.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip

// Trạng thái lưu trữ thuật toán lặp lại ngắt quãng
data class HomeReviewState(
    val id: String = "",
    val classId: String = "",
    val snapshot: WrongQuestionSnapshot = WrongQuestionSnapshot(),
    val streak: Int = 0,
    val nextReviewTime: Long = 0L
)

val LimeGreen = Color(0xFFB9FF66)
val BlackColor = Color.Black
val WhiteColor = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(navController: NavHostController,
                   onNavigateToQuiz: (String, ClassQuiz) -> Unit,
                   onNavigateToReviewWrongs: () -> Unit) {
    val repo = remember { FirebaseRepository() }
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    var userName by remember { mutableStateOf("Đang tải...") }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    userName = document.getString("name") ?: "Học viên"
                }
                .addOnFailureListener {
                    userName = "Học viên"
                }
        }
    }

    val globalContext = LocalContext.current

    var allQuizzesWithClass by remember { mutableStateOf(listOf<Pair<String, ClassQuiz>>()) }
    var enrolledClassesCount by remember { mutableStateOf(0) }
    var wrongQuestionsCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("classes")
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { classSnapshot, error ->
                if (error != null || classSnapshot == null || classSnapshot.isEmpty) {
                    enrolledClassesCount = 0
                    allQuizzesWithClass = emptyList()
                    isLoading = false
                    return@addSnapshotListener
                }

                enrolledClassesCount = classSnapshot.size()
                val totalClasses = classSnapshot.size()
                var processedClasses = 0
                val masterQuizList = mutableListOf<Pair<String, ClassQuiz>>()

                for (classDoc in classSnapshot.documents) {
                    val classId = classDoc.id

                    classDoc.reference.collection("class_quizzes")
                        .addSnapshotListener { assignSnapshot, _ ->
                            if (assignSnapshot != null && !assignSnapshot.isEmpty) {
                                val quizIds = assignSnapshot.documents.map { it.id }

                                db.collection("quizzes")
                                    .whereIn("quizId", quizIds)
                                    .get()
                                    .addOnSuccessListener { quizSnapshot ->
                                        if (quizSnapshot != null) {
                                            for (qDoc in quizSnapshot.documents) {
                                                val quiz = qDoc.toObject(ClassQuiz::class.java)
                                                if (quiz != null) {
                                                    // 🔥 FIX LỖI GỐC: Ép ID tài liệu thực tế của qDoc vào quizId để chuỗi Route không bị rỗng
                                                    val finalQuiz = quiz.copy(
                                                        quizId = qDoc.id,
                                                        classId = classId
                                                    )
                                                    masterQuizList.removeAll { it.second.quizId == qDoc.id && it.first == classId }
                                                    masterQuizList.add(Pair(classId, finalQuiz))
                                                }
                                            }
                                        }

                                        if (++processedClasses >= totalClasses) {
                                            allQuizzesWithClass = masterQuizList.toList()
                                            isLoading = false
                                        }
                                    }
                                    .addOnFailureListener {
                                        if (++processedClasses >= totalClasses) {
                                            allQuizzesWithClass = masterQuizList.toList()
                                            isLoading = false
                                        }
                                    }
                            } else {
                                if (++processedClasses >= totalClasses) {
                                    allQuizzesWithClass = masterQuizList.toList()
                                    isLoading = false
                                }
                            }
                        }
                }
            }

        // Đếm tổng số câu sai đang chờ được giải quyết bằng thuật toán Spaced Repetition
        db.collection("users").document(userId)
            .collection("spaced_repetition_wrongs")
            .whereLessThanOrEqualTo("nextReviewTime", System.currentTimeMillis())
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    wrongQuestionsCount = snapshot.size()
                }
            }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Xin chào,", fontSize = 12.sp, color = Color.Gray)
                        Text(userName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        repo.logout()
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Đăng xuất")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // ==================== 1. STATS DASHBOARD ====================
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ElevatedCard(
                            modifier = Modifier.weight(1f).border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = LimeGreen)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Lớp học", fontWeight = FontWeight.Bold)
                                Text("$enrolledClassesCount Lớp", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        ElevatedCard(
                            modifier = Modifier.weight(1f).border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Bài tập", fontWeight = FontWeight.Bold)
                                Text("${allQuizzesWithClass.size} Đề", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                // ==================== 2. MỤC ÔN TẬP ====================
                if (wrongQuestionsCount > 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Ôn tập kiến thức", fontWeight = FontWeight.ExtraBold)
                                    Text("$wrongQuestionsCount câu sai cần luyện lại.", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { onNavigateToReviewWrongs() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Ôn ngay", color = LimeGreen)
                                }
                            }
                        }
                    }
                }

                // ==================== 3. DANH SÁCH BÀI KIỂM TRA ====================
                item {
                    Text("Bài kiểm tra được giao", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }

                items(allQuizzesWithClass) { pair ->
                    val (classId, quiz) = pair
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                            .clickable { onNavigateToQuiz(classId, quiz) },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Kiểm tra", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(quiz.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("${quiz.duration} phút • ${quiz.questions.size} câu", fontSize = 12.sp, color = Color.Gray)
                            }
                            Box(
                                modifier = Modifier.size(40.dp).border(2.dp, Color.Black, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}