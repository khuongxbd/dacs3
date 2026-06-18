package com.example.myapplication.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ClassModel
import com.example.myapplication.data.model.ClassQuiz
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminClassDetailScreen(classObj: ClassModel, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Thành Viên", "Kho Bài Tập")

    val db = FirebaseFirestore.getInstance()
    var quizList by remember { mutableStateOf(listOf<ClassQuiz>()) }
    var studentEmails by remember { mutableStateOf(listOf<String>()) } // Hiển thị tạm Email các bạn đã vào lớp
    var isLoadingQuiz by remember { mutableStateOf(true) }

    // Load các bài tập/đề kiểm tra và thông tin thành viên của lớp này từ Firestore
    LaunchedEffect(classObj.classId) {
        // 1. Lấy danh sách đề thi thuộc lớp
        db.collection("classes").document(classObj.classId).collection("class_quizzes")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) quizList = snapshot.toObjects(ClassQuiz::class.java)
                isLoadingQuiz = false
            }

        // 2. Lấy Email thành viên học sinh trong mảng memberIds để hiển thị trực quan
        if (classObj.memberIds.isNotEmpty()) {
            db.collection("users").whereIn("uid", classObj.memberIds).get()
                .addOnSuccessListener { snap ->
                    studentEmails = snap.documents.map { it.getString("email") ?: "Ẩn danh" }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(classObj.className) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Thanh chuyển đổi Tab (TabRow) điều hướng mượt mà
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // --- DANH SÁCH THÀNH VIÊN ---
                    if (studentEmails.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Lớp học này hiện tại chưa có học sinh nào tham gia.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(studentEmails) { email ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(35.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                            Text("👤", fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(email, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // --- DANH SÁCH BÀI TẬP VÀ ĐỀ THI ĐÃ PHÁT HÀNH TRONG LỚP ---
                    if (isLoadingQuiz) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else if (quizList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Chưa đưa bài tập nào vào lớp này. (Hãy sang Tab Bài Kiểm Tra để tạo)", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(quizList) { quiz ->
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(quiz.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Thời lượng làm bài: ${quiz.duration} phút | Số lượng: ${quiz.questions.size} câu hỏi", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}