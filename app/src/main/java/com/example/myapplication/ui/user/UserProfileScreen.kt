package com.example.myapplication.ui.user

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.data.repository.FirebaseRepository
import com.example.myapplication.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class đại diện cho cấu trúc lịch sử bài kiểm tra
data class QuizHistoryItem(
    val id: String = "",
    val quizTitle: String = "",
    val score: Double = 0.0,
    val totalQuestions: Int = 0,
    val timestamp: Long = 0L,
    val selectedAnswers: Map<String, String> = emptyMap(), // Câu hỏi index -> Đáp án đã chọn
    val correctAnswers: Map<String, String> = emptyMap(),  // Câu hỏi index -> Đáp án đúng
    val questionsText: Map<String, String> = emptyMap()    // Câu hỏi index -> Nội dung câu hỏi
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(rootNavController: NavHostController) {
    val repo = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    val currentUser = auth.currentUser
    val userEmail = currentUser?.email ?: "Chưa cập nhật Email"
    var userName by remember { mutableStateOf(currentUser?.displayName ?: "Học viên xuất sắc") }
    val userId = currentUser?.uid ?: ""

    // State lưu trữ chỉ số Streak thực tế từ Firebase
    var currentStreak by remember { mutableStateOf(0) }
    var highestStreak by remember { mutableStateOf(0) }

    // State chứa danh sách lịch sử làm bài
    var quizHistoryList by remember { mutableStateOf(listOf<QuizHistoryItem>()) }
    var isLoadingHistory by remember { mutableStateOf(true) }

    // ID của bài tập đang được nhấn mở rộng để xem chi tiết câu hỏi
    var expandedHistoryId by remember { mutableStateOf<String?> (null) }

    // Fetch dữ liệu từ Firestore
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            // 1. Lấy dữ liệu Streak và Name
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        currentStreak = document.getLong("currentStreak")?.toInt() ?: 0
                        highestStreak = document.getLong("highestStreak")?.toInt() ?: 0
                        val name = document.getString("name")
                        if (!name.isNullOrBlank()) {
                            userName = name
                        }
                    }
                }

            // 2. Lấy dữ liệu Lịch sử làm bài (Giả định lưu tại users/{userId}/quiz_history)
            db.collection("users").document(userId).collection("quiz_history")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    val history = mutableListOf<QuizHistoryItem>()
                    for (doc in snapshot.documents) {
                        val id = doc.id
                        val title = doc.getString("quizTitle") ?: "Bài kiểm tra vô danh"
                        val score = doc.getDouble("score") ?: 0.0
                        val total = doc.getLong("totalQuestions")?.toInt() ?: 0
                        val time = doc.getLong("timestamp") ?: 0L

                        val selected = doc.get("selectedAnswers") as? Map<String, String> ?: emptyMap()
                        val correct = doc.get("correctAnswers") as? Map<String, String> ?: emptyMap()
                        val qTexts = doc.get("questionsText") as? Map<String, String> ?: emptyMap()

                        history.add(QuizHistoryItem(id, title, score, total, time, selected, correct, qTexts))
                    }
                    quizHistoryList = history
                    isLoadingHistory = false
                }
                .addOnFailureListener {
                    isLoadingHistory = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ Sơ & Thống Kê", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==================== 1. KHU VỰC AVATAR & THÔNG TIN CHÍNH ====================
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.size(90.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = userName.take(1).uppercase(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = userEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)

            Spacer(modifier = Modifier.height(20.dp))

            // ==================== 2. THẺ STREAK RỰC LỬA ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFFE65100), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Chuỗi học tập hiện tại", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), fontSize = 13.sp)
                        Text(
                            text = if (currentStreak > 0) "$currentStreak Ngày liên tục! 🔥" else "Chưa có chuỗi ngày học nào",
                            fontWeight = FontWeight.ExtraBold, color = Color(0xFFB71C1C), fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thống kê kỷ lục Streak nhanh
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                ProfileItemRow(icon = Icons.Default.EmojiEvents, iconTint = Color(0xFFFFB300), label = "Kỷ lục Streak cao nhất", value = "$highestStreak Ngày")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==================== 3. MỤC LỊCH SỬ LÀM BÀI CHI TIẾT ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lịch sử làm bài gần đây", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoadingHistory) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (quizHistoryList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Bạn chưa thực hiện bài kiểm tra nào gần đây. 📑", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                // Render danh sách lịch sử dạng Expandable Card
                quizHistoryList.forEach { historyItem ->
                    val isExpanded = expandedHistoryId == historyItem.id
                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(historyItem.timestamp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { expandedHistoryId = if (isExpanded) null else historyItem.id },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Dòng tiêu đề và điểm số tổng quan
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = historyItem.quizTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = dateStr, fontSize = 12.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${historyItem.score} Điểm",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (historyItem.score >= 5.0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                            }

                            // Khối chứa danh sách chi tiết từng câu hỏi khi được nhấn mở rộng (Expanded)
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                    Text("Chi tiết bài làm:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)

                                    // Duyệt qua toàn bộ số câu hỏi đã làm
                                    for (i in 0 until historyItem.totalQuestions) {
                                        val key = i.toString()
                                        val questionTitle = historyItem.questionsText[key] ?: "Câu hỏi không có nội dung"
                                        val userAns = historyItem.selectedAnswers[key] ?: "Không chọn"
                                        val correctAns = historyItem.correctAnswers[key] ?: ""
                                        val isUserCorrect = userAns == correctAns

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isUserCorrect) Color(0xFFE8F5E9).copy(alpha = 0.6f)
                                                else Color(0xFFFFEBEE).copy(alpha = 0.6f)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(text = "Câu ${i + 1}: $questionTitle", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (isUserCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                        contentDescription = null,
                                                        tint = if (isUserCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Bạn chọn: $userAns | Đáp án đúng: $correctAns",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isUserCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                    )
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

            Spacer(modifier = Modifier.height(28.dp))

            // ==================== 4. NÚT ĐĂNG XUẤT AN TOÀN ====================
            Button(
                onClick = {
                    repo.logout()
                    rootNavController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng Xuất Tài Khoản", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ✅ ĐÃ ĐƯA RA NGOÀI: Hàm bổ trợ vẽ dòng thông tin không bị lồng Composable
@Composable
fun ProfileItemRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}