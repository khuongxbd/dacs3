package com.example.myapplication.ui.user

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ClassModel
import com.example.myapplication.data.model.FlashcardSet
import com.example.myapplication.data.model.ClassQuiz
import com.example.myapplication.data.model.User
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classObj: ClassModel,
    onBack: () -> Unit,
    onNavigateToFlashcard: (setId: String) -> Unit,
    onNavigateToQuizPractice: (setId: String) -> Unit,
    onNavigateToTypingPractice: (setId: String) -> Unit,
    onNavigateToClassQuiz: (quiz: ClassQuiz) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Từ vựng 📚", "Kiểm tra 📝", "Thành viên 👥")

    var classDetailState by remember { mutableStateOf<ClassModel?>(classObj) }
    var assignedVocabularies by remember { mutableStateOf(listOf<FlashcardSet>()) }
    var classQuizzes by remember { mutableStateOf(listOf<ClassQuiz>()) }
    var memberList by remember { mutableStateOf(listOf<User>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(classObj.classId) {
        if (classObj.classId.isBlank()) return@LaunchedEffect

        // 1. Đồng bộ realtime chi tiết lớp học
        db.collection("classes").document(classObj.classId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val updatedClass = snapshot.toObject(ClassModel::class.java)
                    classDetailState = updatedClass

                    // 2. Lấy thông tin thành viên
                    val memberIds = updatedClass?.memberIds ?: emptyList()
                    if (memberIds.isNotEmpty()) {
                        db.collection("users")
                            .whereIn("uid", memberIds)
                            .addSnapshotListener { userSnapshot, _ ->
                                if (userSnapshot != null) {
                                    memberList = userSnapshot.toObjects(User::class.java)
                                }
                                isLoading = false
                            }
                    } else {
                        memberList = emptyList()
                        isLoading = false
                    }
                }
            }

        // 3. Lấy các bộ từ vựng được giao đến lớp học này
        db.collection("vocabularies")
            .whereArrayContains("assignedClassIds", classObj.classId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    assignedVocabularies = snapshot.toObjects(FlashcardSet::class.java)
                }
            }

        // 4. 🔥 TỐI ƯU TRUY VẤN 2 CHẶNG: Lấy danh sách ID đề thi được giao rồi quét nội dung gốc
        db.collection("classes").document(classObj.classId)
            .collection("class_quizzes")
            .addSnapshotListener { assignSnapshot, _ ->
                if (assignSnapshot == null || assignSnapshot.isEmpty) {
                    classQuizzes = emptyList() // Nếu lớp trống đề, gán list rỗng luôn
                    return@addSnapshotListener
                }

                // Gom tất cả Document ID (chính là quizId) từ sub-collection về
                val quizIds = assignSnapshot.documents.map { it.id }

                if (quizIds.isNotEmpty()) {
                    // Quét ngược ra bảng quizzes gốc để bốc nội dung chi tiết bài thi
                    db.collection("quizzes")
                        .whereIn("quizId", quizIds)
                        .addSnapshotListener { quizSnapshot, _ ->
                            if (quizSnapshot != null) {
                                classQuizzes = quizSnapshot.toObjects(ClassQuiz::class.java)
                            }
                        }
                } else {
                    classQuizzes = emptyList()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(classDetailState?.className ?: classObj.className, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Sĩ số: ${classDetailState?.memberIds?.size ?: 0} học viên", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp) }
                    )
                }
            }

            if (isLoading || classDetailState == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> VocabTabContent(
                            vocabSets = assignedVocabularies,
                            onFlashcardClick = onNavigateToFlashcard,
                            onQuizPracticeClick = onNavigateToQuizPractice,
                            onTypingPracticeClick = onNavigateToTypingPractice
                        )
                        1 -> QuizTabContent(
                            quizzes = classQuizzes,
                            onQuizClick = onNavigateToClassQuiz
                        )
                        2 -> MembersTabContent(
                            members = memberList,
                            adminId = classDetailState?.adminId ?: ""
                        )
                    }
                }
            }
        }
    }
}

// ======================== TAB 1: BỘ TỪ VỰNG & CHẾ ĐỘ ÔN LUYỆN ========================
@Composable
fun VocabTabContent(
    vocabSets: List<FlashcardSet>,
    onFlashcardClick: (setId: String) -> Unit,
    onQuizPracticeClick: (setId: String) -> Unit,
    onTypingPracticeClick: (setId: String) -> Unit
) {
    if (vocabSets.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Lớp học chưa được giao bộ từ vựng nào!", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(vocabSets) { vocabSet ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = vocabSet.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Số lượng: ${vocabSet.words.size} từ vựng",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onFlashcardClick(vocabSet.setId) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text("Flashcard", fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = { onQuizPracticeClick(vocabSet.setId) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007A33)),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text("Trắc nghiệm", fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = { onTypingPracticeClick(vocabSet.setId) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text("Gõ từ", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== TAB 2: DANH SÁCH BÀI KIỂM TRA TRONG LỚP ========================
// 🌟 ĐÃ SỬA: Thay đổi (quizId: String) thành (quiz: ClassQuiz)
@Composable
fun QuizTabContent(quizzes: List<ClassQuiz>, onQuizClick: (quiz: ClassQuiz) -> Unit) {
    if (quizzes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Hiện tại lớp không có bài kiểm tra nào!", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(quizzes) { quiz ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // 🚀 TRUYỀN NGUYÊN OBJECT: Đẩy toàn bộ dữ liệu đề thi ra ngoài để làm bài luôn
                            onQuizClick(quiz)
                        },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = quiz.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "${quiz.duration} phút | ${quiz.questions.size} câu hỏi", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
        }
    }
}

// ======================== TAB 3: HIỂN THỊ THÀNH VIÊN VÀ AVATAR ========================
@Composable
fun MembersTabContent(members: List<User>, adminId: String) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(members) { user ->
            val isAdmin = user.uid == adminId

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAdmin) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Giáo viên ⭐",
                                    fontSize = 10.sp,
                                    color = Color(0xFFD4AF37),
                                    modifier = Modifier
                                        .background(Color(0xFFD4AF37).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(text = user.email, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}