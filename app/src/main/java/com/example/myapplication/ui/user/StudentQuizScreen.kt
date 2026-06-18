package com.example.myapplication.ui.user

import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ClassQuiz
import com.example.myapplication.data.model.QuizResult
import com.example.myapplication.data.model.WrongQuestionSnapshot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQuizScreen(
    quiz: ClassQuiz,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val questions = quiz.questions

    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: ""

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Đề thi này chưa có câu hỏi nào!")
        }
        return
    }

    // --- STATE QUẢN LÝ TRẠNG THÁI LÀM BÀI ---
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val selectedAnswers = remember { mutableStateMapOf<Int, String>() }
    var isSubmitted by remember { mutableStateOf(false) }

    // --- STATE ĐIỀU KHIỂN CÁC DIALOG CẢNH BÁO ---
    var showExitWarningDialog by remember { mutableStateOf(false) }
    var showSubmitConfirmDialog by remember { mutableStateOf(false) }
    var showQuestionsMenuBottomSheet by remember { mutableStateOf(false) }

    // --- STATE ĐẾM NGƯỢC THỜI GIAN ---
    var timeRemaining by remember { mutableStateOf(quiz.duration * 60 * 1000L) }
    var timerText by remember { mutableStateOf("") }

    // --- LOGIC HÀM XỬ LÝ NỘP BÀI THỰC TẾ ---
    val submitQuizLogic = {
        if (!isSubmitted) {
            isSubmitted = true
            val wrongQuestionsList = mutableListOf<WrongQuestionSnapshot>()
            var correctCount = 0

            questions.forEachIndexed { idx, question ->
                val userAns = selectedAnswers[idx] ?: "Chưa chọn"
                if (userAns.trim().uppercase() == question.correctOption.trim().uppercase()) {
                    correctCount++
                } else {
                    wrongQuestionsList.add(
                        WrongQuestionSnapshot(
                            questionText = question.questionText,
                            optionA = question.optionA,
                            optionB = question.optionB,
                            optionC = question.optionC,
                            optionD = question.optionD,
                            correctOption = question.correctOption,
                            userSelected = userAns,
                            explanation = question.explanation
                        )
                    )
                }
            }

            val resultId = UUID.randomUUID().toString()
            val quizResult = QuizResult(
                resultId = resultId,
                userId = currentUserId,
                classId = quiz.classId,
                quizId = quiz.quizId,
                quizTitle = quiz.title,
                score = correctCount,
                totalQuestions = questions.size,
                completedAt = System.currentTimeMillis(),
                wrongAnswers = wrongQuestionsList
            )

            if (currentUserId.isNotBlank()) {
                val batch = db.batch()

                // Tính điểm số
                val calculatedScore = (correctCount.toFloat() / questions.size) * 10
                val finalScoreDouble = String.format(Locale.US, "%.1f", calculatedScore).toDouble()

                val formattedResult = quizResult.copy(score = correctCount)

                // 1. Lưu kết quả thi vào bảng chung lịch sử (Giữ nguyên)
                val resultRef = db.collection("quiz_results").document(resultId)
                batch.set(resultRef, formattedResult)

                // 2. 🔥 SỬA TẠI ĐÂY: Lưu thêm 1 bản ghi vào đúng sub-collection mà UserProfileScreen đang đọc
                val questionsTextMap = mutableMapOf<String, String>()
                val selectedAnswersMap = mutableMapOf<String, String>()
                val correctAnswersMap = mutableMapOf<String, String>()

                questions.forEachIndexed { index, q ->
                    val key = index.toString()
                    questionsTextMap[key] = q.questionText
                    selectedAnswersMap[key] = selectedAnswers[index] ?: "Chưa chọn"
                    correctAnswersMap[key] = q.correctOption
                }

                val userHistoryRef = db.collection("users").document(currentUserId)
                    .collection("quiz_history").document(resultId)

                val historyData = mapOf(
                    "quizTitle" to quiz.title,
                    "score" to finalScoreDouble,
                    "totalQuestions" to questions.size,
                    "timestamp" to System.currentTimeMillis(),
                    "questionsText" to questionsTextMap,
                    "selectedAnswers" to selectedAnswersMap,
                    "correctAnswers" to correctAnswersMap
                )
                batch.set(userHistoryRef, historyData)

                // 3. Đẩy các câu hỏi sai vào kho Spaced Repetition (Giữ nguyên logic cũ của cậu)
                wrongQuestionsList.forEach { wrongSnapshot ->
                    val questionDocId = UUID.nameUUIDFromBytes(wrongSnapshot.questionText.toByteArray()).toString()
                    val sRRef = db.collection("users").document(currentUserId)
                        .collection("spaced_repetition_wrongs").document(questionDocId)

                    val srData = mapOf(
                        "classId" to quiz.classId,
                        "quizId" to quiz.quizId,
                        "snapshot" to wrongSnapshot,
                        "streak" to 0,
                        "nextReviewTime" to 0L
                    )
                    batch.set(sRRef, srData)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Đã nộp bài và đồng bộ lịch sử thành công!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Lưu kết quả thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // --- CẢNH BÁO NÚT BACK HỆ THỐNG CỦA ĐIỆN THOẠI ---
    BackHandler(enabled = !isSubmitted) {
        showExitWarningDialog = true
    }

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
                    submitQuizLogic()
                    Toast.makeText(context, "Hết giờ làm bài! Hệ thống tự động nộp bài.", Toast.LENGTH_LONG).show()
                }
            }
        }
        if (!isSubmitted) timer.start()
        onDispose { timer.cancel() }
    }

    val currentQuestion = questions[currentQuestionIndex]
    val studentChoice = selectedAnswers[currentQuestionIndex]

    // Đảm bảo bạn có các màu này:
    val LimeGreen = Color(0xFF00C853)
    val BlackColor = Color.Black
    val WhiteColor = Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz.title, fontWeight = FontWeight.Black, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BlackColor,
                    titleContentColor = WhiteColor,
                    navigationIconContentColor = WhiteColor,
                    actionIconContentColor = WhiteColor
                ),
                navigationIcon = {
                    IconButton(onClick = { if (isSubmitted) onBack() else showExitWarningDialog = true }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (!isSubmitted) {
                        IconButton(onClick = { showQuestionsMenuBottomSheet = true }) {
                            Icon(Icons.Default.GridOn, contentDescription = "Mục lục")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = LimeGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(timerText, color = WhiteColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            if (!isSubmitted) {
                // --- THANH TIẾN TRÌNH ---
                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = LimeGreen
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- CÂU HỎI ---
                Text(text = currentQuestion.questionText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(16.dp))

                // --- OPTIONS ---
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val options = listOf("A" to currentQuestion.optionA, "B" to currentQuestion.optionB, "C" to currentQuestion.optionC, "D" to currentQuestion.optionD)
                    options.forEach { (letter, text) ->
                        val isSelected = studentChoice == letter
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(8.dp))
                                .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                .clickable { selectedAnswers[currentQuestionIndex] = letter },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) LimeGreen else WhiteColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "$letter. $text", fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal, color = BlackColor)
                            }
                        }
                    }
                }

                // --- NÚT ĐIỀU HƯỚNG 3D ---
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                        modifier = Modifier.weight(1f).height(55.dp).shadow(4.dp, RoundedCornerShape(8.dp)),
                        enabled = currentQuestionIndex > 0,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhiteColor, contentColor = BlackColor),
                        border = BorderStroke(2.dp, BlackColor)
                    ) { Text("TRƯỚC", fontWeight = FontWeight.ExtraBold) }

                    if (currentQuestionIndex < questions.size - 1) {
                        Button(
                            onClick = { currentQuestionIndex++ },
                            modifier = Modifier.weight(1f).height(55.dp).shadow(4.dp, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlackColor, contentColor = WhiteColor),
                            border = BorderStroke(2.dp, BlackColor)
                        ) { Text("TIẾP", fontWeight = FontWeight.ExtraBold) }
                    } else {
                        Button(
                            onClick = { showSubmitConfirmDialog = true },
                            modifier = Modifier.weight(1f).height(55.dp).shadow(4.dp, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LimeGreen, contentColor = BlackColor),
                            border = BorderStroke(2.dp, BlackColor)
                        ) { Text("NỘP BÀI", fontWeight = FontWeight.ExtraBold) }
                    }
                }
            } else {
                val correctCount = questions.indices.count { idx -> selectedAnswers[idx] == questions[idx].correctOption }
                val score = (correctCount.toFloat() / questions.size) * 10

                // 1. CARD KẾT QUẢ TỔNG (Style 3D cứng cáp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .border(2.dp, BlackColor, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = WhiteColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LimeGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("KẾT QUẢ", fontWeight = FontWeight.Black, fontSize = 20.sp, color = BlackColor)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f / 10", score),
                            fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, color = LimeGreen
                        )
                        Text(text = "Đúng $correctCount / ${questions.size} câu", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("XEM LẠI CHI TIẾT:", fontWeight = FontWeight.Black, fontSize = 16.sp, color = BlackColor)

                // 2. LIST CÂU HỎI CHI TIẾT
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(questions.size) { idx ->
                        val q = questions[idx]
                        val ans = selectedAnswers[idx] ?: "Chưa chọn"
                        val isCorrect = ans == q.correctOption

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, BlackColor, RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = WhiteColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Câu ${idx + 1}: ${q.questionText}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(text = "• Bạn chọn: $ans", color = if(isCorrect) LimeGreen else Color(0xFFC62828), fontWeight = FontWeight.ExtraBold)
                                Text(text = "• Đáp án đúng: ${q.correctOption}", color = LimeGreen, fontWeight = FontWeight.ExtraBold)

                                if (q.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = BlackColor, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("💡 GIẢI THÍCH:", fontWeight = FontWeight.Black, fontSize = 13.sp, color = LimeGreen)
                                    Text(text = q.explanation, fontSize = 14.sp, color = BlackColor)
                                }
                            }
                        }
                    }
                }

                // 3. NÚT THOÁT 3D (Đồng bộ nút ở màn hình làm bài)
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .padding(top = 16.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlackColor, contentColor = WhiteColor),
                    border = BorderStroke(2.dp, BlackColor)
                ) {
                    Text("THOÁT RA MÀN HÌNH LỚP", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    // --- CÁC DIALOG CẢNH BÁO GIỮ NGUYÊN ---
    if (showExitWarningDialog) {
        AlertDialog(
            onDismissRequest = { showExitWarningDialog = false },
            title = { Text("Cảnh Báo Thoát Bài Thi") },
            text = { Text("Bài làm của bạn đang diễn ra và CHƯA được lưu lại. Bạn có chắc muốn rời đi?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showExitWarningDialog = false
                        onBack()
                    }
                ) { Text("Rời khỏi") }
            },
            dismissButton = {
                TextButton(onClick = { showExitWarningDialog = false }) { Text("Tiếp tục làm bài") }
            }
        )
    }

    if (showSubmitConfirmDialog) {
        val unansweredCount = questions.size - selectedAnswers.size
        AlertDialog(
            onDismissRequest = { showSubmitConfirmDialog = false },
            title = { Text("Xác Nhận Nộp Bài") },
            text = {
                Column {
                    if (unansweredCount > 0) {
                        Text(
                            text = "⚠️ Chú ý: Bạn còn $unansweredCount câu hỏi CHƯA hoàn thành đáp án!",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text("Bạn có chắc chắn muốn kết thúc bài thi và nộp kết quả ngay bây giờ?")
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007A33)),
                    onClick = {
                        showSubmitConfirmDialog = false
                        submitQuizLogic()
                    }
                ) { Text("Xác nhận Nộp") }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmDialog = false }) { Text("Xem lại") }
            }
        )
    }

    if (showQuestionsMenuBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuestionsMenuBottomSheet = false },
            containerColor = WhiteColor, // Nền trắng
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
            ) {
                Text(
                    text = "MỤC LỤC CÂU HỎI",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = BlackColor,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 260.dp)
                ) {
                    items(questions.size) { index ->
                        val isAnswered = selectedAnswers.containsKey(index)
                        val isCurrent = currentQuestionIndex == index

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                // Bỏ CircleShape, dùng RoundedCornerShape(8.dp) cho đồng bộ
                                .background(
                                    color = when {
                                        isCurrent -> BlackColor // Hiện tại thì nền đen
                                        isAnswered -> LimeGreen // Đã làm thì nền xanh
                                        else -> WhiteColor // Chưa làm thì nền trắng
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 2.dp,
                                    color = BlackColor, // Luôn có viền đen cứng cáp
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    currentQuestionIndex = index
                                    showQuestionsMenuBottomSheet = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    isCurrent -> WhiteColor // Chữ trắng trên nền đen
                                    isAnswered -> BlackColor // Chữ đen trên nền xanh
                                    else -> BlackColor // Chữ đen trên nền trắng
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}