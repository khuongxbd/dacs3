package com.example.myapplication.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ClassModel
import com.example.myapplication.data.model.ClassQuiz
import com.google.firebase.firestore.FirebaseFirestore

data class StudentDisplay(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val highestStreak: Int = 0,
    val currentStreak: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminClassDetailScreen(classObj: ClassModel, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Thành Viên", "Kho Bài Tập", "Bảng Xếp Hạng")

    val db = FirebaseFirestore.getInstance()
    var quizList by remember { mutableStateOf(listOf<ClassQuiz>()) }
    var students by remember { mutableStateOf(listOf<StudentDisplay>()) }
    var isLoadingQuiz by remember { mutableStateOf(true) }

LaunchedEffect(classObj.classId) {
        db.collection("classes").document(classObj.classId).collection("class_quizzes")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) quizList = snapshot.toObjects(ClassQuiz::class.java)
                isLoadingQuiz = false
            }

        if (classObj.memberIds.isNotEmpty()) {
            db.collection("users").whereIn("uid", classObj.memberIds).get()
                .addOnSuccessListener { snap ->
                    val list = snap.documents.map { doc ->
                        StudentDisplay(
                            uid = doc.getString("uid") ?: "",
                            name = doc.getString("name") ?: "Học viên xuất sắc",
                            email = doc.getString("email") ?: "Ẩn danh",
                            highestStreak = doc.getLong("highestStreak")?.toInt() ?: 0,
                            currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0
                        )
                    }
                    students = list
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(classObj.className.uppercase(), fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlackColor, titleContentColor = WhiteColor),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = WhiteColor) }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(WhiteColor)) {

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = WhiteColor,
                contentColor = BlackColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = LimeGreen,
                        height = 4.dp
                    )
                },
                modifier = Modifier.border(0.dp, BlackColor).shadow(2.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(students) { student ->
                            Card(
                                modifier = Modifier.fillMaxWidth().border(2.dp, BlackColor, RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = WhiteColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(35.dp).background(LimeGreen, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                                        Text("👤", fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Text(student.email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(quizList) { quiz ->
                            Card(
                                modifier = Modifier.fillMaxWidth().border(2.dp, BlackColor, RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = WhiteColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(quiz.title, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Thời lượng: ${quiz.duration} phút | ${quiz.questions.size} câu hỏi", fontWeight = FontWeight.Bold, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    val rankedStudents = students.sortedByDescending { it.highestStreak }
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(rankedStudents.size) { index ->
                            val student = rankedStudents[index]
                            val medal = when(index) {
                                0 -> "🏆"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "🏅"
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth().border(2.dp, BlackColor, RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = if (index < 3) Color(0xFFFFF9C4) else WhiteColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(medal, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            Text(student.email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${student.highestStreak} Ngày", fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
                                        Text("Kỷ lục Streak", fontSize = 12.sp, color = Color.Gray)
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