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
import androidx.compose.ui.draw.shadow
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
    val BlackColor = Color.Black
    val WhiteColor = Color.White

    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var quizQuestions by remember { mutableStateOf(listOf<VocabQuizQuestion>()) }
    var isLoading by remember { mutableStateOf(true) }

    // --- STATE TRẠNG THÁI LÀM BÀI ---
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val selectedAnswers = remember { mutableStateMapOf<Int, String>() }
    var isSubmitted by remember { mutableStateOf(false) }

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
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("LUYỆN TẬP TRẮC NGHIỆM", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlackColor, titleContentColor = WhiteColor, navigationIconContentColor = WhiteColor)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.Black)
            }
        } else if (quizQuestions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Không có dữ liệu câu hỏi.", fontWeight = FontWeight.Bold)
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
                        text = "CÂU HỎI ${currentQuestionIndex + 1} / ${quizQuestions.size}",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    LinearProgressIndicator(
                        progress = { (currentQuestionIndex + 1).toFloat() / quizQuestions.size },
                        modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 8.dp).border(2.dp, Color.Black),
                        color = Color(0xFF00C853), // LimeGreen
                        trackColor = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = currentQuestion.questionText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

                        currentQuestion.options.forEach { optionText ->
                            val isSelected = studentChoice == optionText
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color.Black)
                                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                                    .clickable { selectedAnswers[currentQuestionIndex] = optionText },
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF00C853) else Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = optionText,
                                    modifier = Modifier.padding(20.dp),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                            enabled = currentQuestionIndex > 0,
                            modifier = Modifier.weight(1f).border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                        ) { Text("QUAY LẠI", fontWeight = FontWeight.ExtraBold) }

                        Button(
                            onClick = { if (currentQuestionIndex < quizQuestions.size - 1) currentQuestionIndex++ else isSubmitted = true },
                            modifier = Modifier.weight(1f).border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (currentQuestionIndex == quizQuestions.size - 1) Color(0xFF00C853) else Color.Black)
                        ) { Text(if (currentQuestionIndex == quizQuestions.size - 1) "NỘP BÀI" else "TIẾP THEO", fontWeight = FontWeight.ExtraBold) }
                    }
                } else {
                    // ==================== GIAO DIỆN SAU KHI NỘP BÀI ====================
                    val correctCount = quizQuestions.indices.count { selectedAnswers[it] == quizQuestions[it].correctOption }

                    Card(
                        modifier = Modifier.fillMaxWidth().border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("KẾT QUẢ", fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text("$correctCount / ${quizQuestions.size}", fontWeight = FontWeight.Black, fontSize = 48.sp, color = Color(0xFF00C853))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("CHI TIẾT KẾT QUẢ:", fontWeight = FontWeight.Black)

                    Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                        androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(quizQuestions.size) { idx ->
                                val q = quizQuestions[idx]
                                val ans = selectedAnswers[idx] ?: "Chưa trả lời"
                                val isCorrect = ans == q.correctOption

                                Card(
                                    modifier = Modifier.fillMaxWidth().border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = q.questionText, fontWeight = FontWeight.Bold)
                                        Text("Đáp án của bạn: $ans", color = if (isCorrect) Color(0xFF2E7D32) else Color.Red, fontWeight = FontWeight.Bold)
                                        Text("Đáp án đúng: ${q.correctOption}", fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text("TRỞ LẠI LỚP HỌC", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}