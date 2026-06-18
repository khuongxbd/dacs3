package com.example.myapplication.ui.user

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.FlashcardSet
import com.example.myapplication.data.model.WordItem
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardStudyScreen(classId: String, setId: String, onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var flashcardSet by remember { mutableStateOf<FlashcardSet?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // 0: Chế độ học Flashcard, 1: Chế độ trắc nghiệm nhanh
    var studyMode by remember { mutableStateOf(0) }

    // 🔥 ĐÃ SỬA: Thay đổi đường dẫn truy vấn trỏ thẳng vào gốc "vocabularies" để kéo data
    LaunchedEffect(setId) {
        if (setId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("vocabularies").document(setId).get()
            .addOnSuccessListener { doc ->
                if (doc != null) {
                    flashcardSet = doc.toObject(FlashcardSet::class.java)
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(flashcardSet?.title ?: "Học Từ Vựng") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (flashcardSet == null || flashcardSet!!.words.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Không có dữ liệu chuỗi từ vựng.") }
        } else {
            val words = flashcardSet!!.words

            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                // Thanh lựa chọn chế độ học
                TabRow(selectedTabIndex = studyMode, modifier = Modifier.padding(bottom = 16.dp)) {
                    Tab(selected = studyMode == 0, onClick = { studyMode = 0 }, text = { Text("Lật Thẻ (Flashcard)") })
                    Tab(selected = studyMode == 1, onClick = { studyMode = 1 }, text = { Text("Trắc Nghiệm Nghĩa") })
                }

                if (studyMode == 0) {
                    FlashcardView(words = words)
                } else {
                    QuizVocabView(words = words)
                }
            }
        }
    }
}

// --- HÀM 1: GIAO DIỆN LẬT FLASHCARD ---
@Composable
fun FlashcardView(words: List<WordItem>) {
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) } // false: Tiếng Anh, true: Tiếng Việt

    // Hiệu ứng xoay mượt mà khi bấm lật thẻ
    val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f, label = "cardFlip")

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Từ số: ${currentIndex + 1}/${words.size}", style = MaterialTheme.typography.titleMedium)

        // Thẻ Flashcard có hiệu ứng 3D xoay lật
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 8 * density
                }
                .clickable { isFlipped = !isFlipped },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (rotation <= 90f) {
                    // Mặt trước: Tiếng Anh
                    Text(
                        text = words[currentIndex].word,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Mặt sau: Tiếng Việt (Xoay ngược lại 180 độ chữ để không bị lật ngược mắt nhìn)
                    Text(
                        text = words[currentIndex].meaning,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer { rotationY = 180f }
                    )
                }
            }
        }

        // Nút bấm chuyển đổi từ vựng trước/sau
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { if (currentIndex > 0) { currentIndex--; isFlipped = false } },
                enabled = currentIndex > 0
            ) { Text("Từ trước") }

            Button(
                onClick = { if (currentIndex < words.size - 1) { currentIndex++; isFlipped = false } },
                enabled = currentIndex < words.size - 1
            ) { Text("Từ tiếp theo") }
        }
    }
}

// --- HÀM 2: GIAO DIỆN TRẮC NGHIỆM TỪ VỰNG ---
@Composable
fun QuizVocabView(words: List<WordItem>) {
    var currentIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf("") }
    var isAnswered by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }

    val currentWord = words[currentIndex]

    // Tạo danh sách 4 đáp án (gồm 1 đáp án đúng và 3 đáp án ngẫu nhiên khác)
    val options = remember(currentIndex) {
        val wrongOptions = words.filter { it.meaning != currentWord.meaning }.map { it.meaning }.shuffled().take(3)
        (wrongOptions + currentWord.meaning).shuffled()
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Hãy chọn nghĩa đúng của từ:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = currentWord.word,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Danh sách các nút đáp án trắc nghiệm
            options.forEach { option ->
                val buttonColor = when {
                    !isAnswered -> ButtonDefaults.buttonColors()
                    option == currentWord.meaning -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    option == selectedAnswer -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    else -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = {
                        if (!isAnswered) {
                            selectedAnswer = option
                            isAnswered = true
                            if (option == currentWord.meaning) score++
                        }
                    },
                    colors = buttonColor,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(option, fontSize = 18.sp)
                }
            }
        }

        // Điều hướng câu trắc nghiệm tiếp theo
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text("Điểm số hiện tại: $score/${words.size}", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (isAnswered) {
                Button(
                    onClick = {
                        if (currentIndex < words.size - 1) {
                            currentIndex++
                            isAnswered = false
                            selectedAnswer = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (currentIndex == words.size - 1) "Hoàn thành bài học!" else "Tiếp tục")
                }
            }
        }
    }
}