package com.example.myapplication.ui.user

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import java.text.SimpleDateFormat
import java.util.Locale

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
    val tabs = listOf("Từ vựng", "Kiểm tra", "Thành viên")

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
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = classDetailState?.className ?: classObj.className,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Sĩ số: ${classDetailState?.memberIds?.size ?: 0} học viên",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // TabRow tùy chỉnh viền đen
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color.Black,
                        height = 4.dp // Làm dày vạch chỉ báo
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if(selectedTab == index) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nội dung chính
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Black
                    )
                } else {
                    when (selectedTab) {
                        0 -> VocabTabContent(
                            assignedVocabularies,
                            onNavigateToFlashcard,
                            onNavigateToQuizPractice,
                            onNavigateToTypingPractice
                        )
                        1 -> QuizTabContent(classQuizzes, onNavigateToClassQuiz)
                        2 -> MembersTabContent(memberList, classDetailState?.adminId ?: "")
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
            Text("Chưa có bộ từ vựng nào được giao!", fontWeight = FontWeight.Medium, color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 20.dp), // Thêm khoảng trống dưới cùng
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(vocabSets) { vocabSet ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color.Black)
                        .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Tiêu đề bộ từ vựng
                        Text(
                            text = vocabSet.title.uppercase(), // Chữ hoa tạo cảm giác mạnh mẽ
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )

                        // Thông tin phụ
                        Surface(
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                            color = Color.Black.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Gồm ${vocabSet.words.size} từ vựng",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Nhóm các nút bấm
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Hàm tạo nút bấm gọn gàng để tránh lặp code
                            VocabActionButton(
                                onClick = { onFlashcardClick(vocabSet.setId) },
                                label = "Flashcard",
                                icon = Icons.Default.Style,
                                containerColor = LimeGreen,
                                modifier = Modifier.weight(1f)
                            )
                            VocabActionButton(
                                onClick = { onQuizPracticeClick(vocabSet.setId) },
                                label = "Trắc nghiệm",
                                icon = Icons.Default.Assignment,
                                containerColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            VocabActionButton(
                                onClick = { onTypingPracticeClick(vocabSet.setId) },
                                label = "Gõ từ",
                                icon = Icons.Default.Keyboard,
                                containerColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper Composable để các nút bấm trông đồng bộ
@Composable
fun VocabActionButton(
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            // Thêm shadow nhẹ để nút trông nổi khối hơn
            .shadow(4.dp, RoundedCornerShape(8.dp), spotColor = Color.Black)
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(8.dp),
        // Chỉnh sửa Padding để chữ không bị sát mép
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
        // Loại bỏ elevation mặc định của Button để không bị xung đột với .shadow() tự định nghĩa
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

// ======================== TAB 2: DANH SÁCH BÀI KIỂM TRA TRONG LỚP ========================
@Composable
fun QuizTabContent(quizzes: List<ClassQuiz>, onQuizClick: (quiz: ClassQuiz) -> Unit) {
    if (quizzes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Hiện tại lớp không có bài kiểm tra nào!", fontWeight = FontWeight.Medium, color = Color.Gray)
        }
    } else {
        val now = System.currentTimeMillis()
        val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(quizzes) { quiz ->
                val isStarted = quiz.startTime == null || now >= quiz.startTime!!
                val isEnded = quiz.endTime != null && now > quiz.endTime!!
                val isValidTime = isStarted && !isEnded

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color.Black)
                        .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                        .clickable(enabled = isValidTime) { onQuizClick(quiz) },
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Tiêu đề bài thi
                            Text(
                                text = quiz.title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (isValidTime) Color.Black else Color.Gray
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Thông tin thời gian & số câu
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${quiz.duration} phút • ${quiz.questions.size} câu", fontSize = 12.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Badge trạng thái
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    !isStarted -> Color(0xFFFFCC80) // Cam nhạt
                                    isEnded -> Color(0xFFEF9A9A)    // Đỏ nhạt
                                    else -> LimeGreen               // LimeGreen chủ đạo
                                }.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = when {
                                        !isStarted -> "⏱️ Chưa mở"
                                        isEnded -> "❌ Đã hết hạn"
                                        else -> "🟢 Đang mở"
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Icon mũi tên
                        if (isValidTime) {
                            Box(
                                modifier = Modifier.size(40.dp).border(2.dp, Color.Black, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== TAB 3: HIỂN THỊ THÀNH VIÊN VÀ AVATAR ========================
@Composable
fun MembersTabContent(members: List<User>, adminId: String) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(members) { user ->
            val isAdmin = user.uid == adminId

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    // Bóng đổ cứng chuẩn Neobrutalism
                    .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color.Black)
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar với viền đen
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(if (isAdmin) LimeGreen else Color(0xFFEEEEEE), CircleShape)
                            .border(2.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Thông tin thành viên
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(8.dp))
                                // Badge Admin màu LimeGreen nổi bật
                                Surface(
                                    color = LimeGreen,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, Color.Black)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = user.email,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}