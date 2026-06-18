package com.example.myapplication.ui.user

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.FlashcardSet
import com.google.firebase.firestore.FirebaseFirestore

// Model phụ vụ cấu trúc một câu hỏi trắc nghiệm từ vựng tự sinh
data class VocabQuizQuestion(
    val questionText: String,
    val options: List<String>,
    val correctOption: String,
    val originalWord: String,
    val originalMeaning: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabQuizPracticeScreen(
    setId: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var quizQuestions by remember { mutableStateOf(listOf<VocabQuizQuestion>()) }
    var isLoading by remember { mutableStateOf(true) }

    // --- STATE TRẠNG THÁI LÀM BÀI ---
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val selectedAnswers = remember { mutableStateMapOf<Int, String>() }
    var isSubmitted by remember { mutableStateOf(false) }

    // Tự động kéo bộ từ vựng và sinh bộ đề trắc nghiệm ngẫu nhiên
    LaunchedEffect(setId) {
        if (setId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("vocabularies").document(setId)
            .get()
            .addOnSuccessListener { snapshot ->
                val vocabSet = snapshot.toObject(FlashcardSet::class.java)
                if (vocabSet != null && vocabSet.words.size >= 4) {
                    val generatedList = mutableListOf<VocabQuizQuestion>()
                    val allWords = vocabSet.words

                    // Duyệt qua từng từ vựng để biến nó thành một câu hỏi trắc nghiệm
                    allWords.forEach { target ->
                        val questionType = (0..1).random() // 50% hỏi từ chọn nghĩa, 50% hỏi nghĩa chọn từ
                        val qText: String
                        val correctAns: String
                        val wrongOptions: List<String>

                        if (questionType == 0) {
                            qText = "Từ vựng '${target.word}' có nghĩa là gì?"
                            correctAns = target.meaning
                            // Lấy ngẫu nhiên 3 nghĩa của các từ khác để làm đáp án nhiễu
                            wrongOptions = allWords.filter { it.meaning != correctAns }
                                .map { it.meaning }.shuffled().take(3)
                        } else {
                            qText = "Nghĩa '${target.meaning}' là của từ vựng nào dưới đây?"
                            correctAns = target.word
                            // Lấy ngẫu nhiên 3 từ khác để làm đáp án nhiễu
                            wrongOptions = allWords.filter { it.word != correctAns }
                                .map { it.word }.shuffled().take(3)
                        }

                        // Trộn đáp án đúng và 3 đáp án sai lại với nhau
                        val fullOptions = (wrongOptions + correctAns).shuffled()

                        generatedList.add(
                            VocabQuizQuestion(
                                questionText = qText,
                                options = fullOptions,
                                correctOption = correctAns,
                                originalWord = target.word,
                                originalMeaning = target.meaning
                            )
                        )
                    }

                    quizQuestions = generatedList.shuffled() // Xáo trộn thứ tự các câu hỏi trong đề
                } else {
                    Toast.makeText(context, "Học phần phải có tối thiểu 4 từ vựng để tạo bài trắc nghiệm!", Toast.LENGTH_LONG).show()
                    onBack()
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Không thể tải dữ liệu từ vựng!", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Luyện Tập Trắc Nghiệm Từ Vựng", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại") }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (quizQuestions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Không có dữ liệu câu hỏi.", color = Color.Gray)
            }
        } else {
            val currentQuestion = quizQuestions[currentQuestionIndex]
            val studentChoice = selectedAnswers[currentQuestionIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                if (!isSubmitted) {
                    // ==================== GIAO DIỆN ĐANG LÀM BÀI ====================
                    Text(
                        text = "Câu hỏi ${currentQuestionIndex + 1} / ${quizQuestions.size}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )

                    LinearProgressIndicator(
                        progress = { (currentQuestionIndex + 1).toFloat() / quizQuestions.size },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = currentQuestion.questionText, fontSize = 18.sp, fontWeight = FontWeight.Medium)

                        Spacer(modifier = Modifier.height(8.dp))

                        currentQuestion.options.forEach { optionText ->
                            val isSelected = studentChoice == optionText
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAnswers[currentQuestionIndex] = optionText }
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
                                    RadioButton(selected = isSelected, onClick = { selectedAnswers[currentQuestionIndex] = optionText })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = optionText, fontSize = 15.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                            enabled = currentQuestionIndex > 0
                        ) { Text("Câu trước") }

                        if (currentQuestionIndex < quizQuestions.size - 1) {
                            Button(onClick = { currentQuestionIndex++ }) { Text("Câu tiếp") }
                        } else {
                            Button(
                                onClick = { isSubmitted = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007A33))
                            ) { Text("Nộp bài 🏁") }
                        }
                    }
                } else {
                    // ==================== GIAO DIỆN SAU KHI NỘP BÀI (KẾT QUẢ) ====================
                    val correctCount = quizQuestions.indices.count { idx ->
                        selectedAnswers[idx] == quizQuestions[idx].correctOption
                    }

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
                            Text("Hoàn Thành Ôn Luyện!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                text = "Đúng $correctCount / ${quizQuestions.size} từ vựng",
                                fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Chi tiết kết quả ôn tập:", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                        androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(quizQuestions.size) { idx ->
                                val q = quizQuestions[idx]
                                val ans = selectedAnswers[idx] ?: "Chưa chọn đáp án"
                                val isCorrect = ans == q.correctOption

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = q.questionText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "• Lựa chọn của bạn: $ans", fontSize = 13.sp, color = if(isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828))
                                        Text(text = "• Đáp án đúng: ${q.correctOption}", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                                        Text(text = "👉 Ghi nhớ: ${q.originalWord} nghĩa là ${q.originalMeaning}", fontSize = 12.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }

                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Text("Trở lại lớp học")
                    }
                }
            }
        }
    }
}