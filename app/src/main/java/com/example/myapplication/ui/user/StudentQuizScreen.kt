package com.example.myapplication.ui.user

import android.os.CountDownTimer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ClassQuiz
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQuizScreen(
    quiz: ClassQuiz,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val questions = quiz.questions

    // Nếu đề không có câu hỏi nào thì quay xe luôn
    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Đề thi này chưa có câu hỏi nào!")
        }
        return
    }

    // --- STATE QUẢN LÝ TRẠNG THÁI LÀM BÀI ---
    var currentQuestionIndex by remember { mutableStateOf(0) }
    // Lưu đáp án học sinh chọn: Key là vị trí câu hỏi (0, 1, 2...), Value là "A", "B", "C", "D"
    val selectedAnswers = remember { mutableStateMapOf<Int, String>() }
    var isSubmitted by remember { mutableStateOf(false) }

    // --- STATE ĐẾM NGƯỢC THỜI GIAN ---
    var timeRemaining by remember { mutableStateOf(quiz.duration * 60 * 1000L) }
    var timerText by remember { mutableStateOf("") }

    // Khởi chạy đồng hồ đếm ngược
    DisposableEffect(key1 = quiz.quizId) {
        val timer = object : CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                timerText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                if (!isSubmitted) {
                    isSubmitted = true
                    Toast.makeText(context, "Hết giờ làm bài! Hệ thống tự động nộp bài.", Toast.LENGTH_LONG).show()
                }
            }
        }
        if (!isSubmitted) timer.start()

        onDispose { timer.cancel() }
    }

    val currentQuestion = questions[currentQuestionIndex]
    val studentChoice = selectedAnswers[currentQuestionIndex]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz.title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (!isSubmitted) {
                        // Hiển thị đồng hồ bấm giờ
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(timerText, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (!isSubmitted) {
                // ==================== GIAO DIỆN ĐANG LÀM BÀI ====================

                // Tiến độ làm bài (Ví dụ: Câu 1 / 5)
                Text(
                    text = "Câu hỏi ${currentQuestionIndex + 1} / ${questions.size}",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )

                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nội dung câu hỏi
                    Text(text = currentQuestion.questionText, fontSize = 18.sp, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Danh sách 4 đáp án A, B, C, D
                    val options = listOf(
                        "A" to currentQuestion.optionA,
                        "B" to currentQuestion.optionB,
                        "C" to currentQuestion.optionC,
                        "D" to currentQuestion.optionD
                    )

                    options.forEach { (letter, text) ->
                        val isSelected = studentChoice == letter
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAnswers[currentQuestionIndex] = letter }
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isSelected, onClick = { selectedAnswers[currentQuestionIndex] = letter })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "$letter. $text", fontSize = 15.sp)
                            }
                        }
                    }
                }

                // THANH ĐIỀU HƯỚNG TRƯỚC / SAU / NỘP BÀI
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                        enabled = currentQuestionIndex > 0
                    ) { Text("Câu trước") }

                    if (currentQuestionIndex < questions.size - 1) {
                        Button(onClick = { currentQuestionIndex++ }) { Text("Câu tiếp theo") }
                    } else {
                        Button(
                            onClick = { isSubmitted = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007A33))
                        ) { Text("Nộp bài 🏁") }
                    }
                }

            } else {
                // ==================== GIAO DIỆN SAU KHI NỘP BÀI (KẾT QUẢ) ====================

                // Tính toán số câu đúng
                val correctCount = questions.indices.count { idx ->
                    selectedAnswers[idx] == questions[idx].correctOption
                }
                val score = (correctCount.toFloat() / questions.size) * 10

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF007A33), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Kết Quả Làm Bài", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = String.format(Locale.getDefault(), "Điểm số: %.1f / 10", score),
                            fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "Đúng $correctCount / ${questions.size} câu", fontSize = 14.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Xem lại đáp án chi tiết:", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                // Danh sách câu hỏi kèm theo kết quả đúng sai và lời giải thích
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(questions.size) { idx ->
                        val q = questions[idx]
                        val ans = selectedAnswers[idx] ?: "Chưa chọn"
                        val isCorrect = ans == q.correctOption

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Câu ${idx + 1}: ${q.questionText}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "• Đáp án của bạn: $ans", fontSize = 13.sp, color = if(isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828))
                                Text(text = "• Đáp án chính xác: ${q.correctOption}", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)

                                if (q.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "💡 Giải thích: ${q.explanation}",
                                        fontSize = 13.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) { Text("Thoát ra màn hình lớp") }
            }
        }
    }
}