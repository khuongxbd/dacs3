package com.example.myapplication.ui.user

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.FlashcardSet
import com.example.myapplication.data.model.WordItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardStudyScreen(classId: String, setId: String, onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var flashcardSet by remember { mutableStateOf<FlashcardSet?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val BlackColor = Color.Black
    val WhiteColor = Color.White

    // 0: Chế độ học Flashcard, 1: Chế độ trắc nghiệm nhanh
    var studyMode by remember { mutableStateOf(0) }

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
                title = { Text(flashcardSet?.title ?: "Học Từ Vựng", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlackColor, titleContentColor = WhiteColor, navigationIconContentColor = WhiteColor)

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
                    FlashcardView(words = words, onFinish = onBack)
                } else {
                    QuizVocabView(words = words, onFinish = onBack)
                }
            }
        }
    }
}

// --- HÀM 1: GIAO DIỆN LẬT FLASHCARD ---
@Composable
fun FlashcardView(words: List<WordItem>, onFinish: () -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f, label = "cardFlip")
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Progress Bar kiểu Neobrutalism
        Text("TỪ VỰNG ${currentIndex + 1}/${words.size}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)

        Card(
            modifier = Modifier
                .padding(vertical = 20.dp)
                .fillMaxWidth(0.9f)
                .height(350.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color.Black)
                .border(3.dp, Color.Black, RoundedCornerShape(16.dp))
                .graphicsLayer { rotationY = rotation; cameraDistance = 8 * density }
                .clickable { isFlipped = !isFlipped },
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (rotation <= 90f) words[currentIndex].word else words[currentIndex].meaning,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp).graphicsLayer { if (rotation > 90f) rotationY = 180f }
                )
            }
        }

        // Action Buttons đồng bộ với thiết kế màn hình ClassDetail
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { if (currentIndex > 0) { currentIndex--; isFlipped = false } },
                modifier = Modifier.weight(1f).border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) { Text("BACK", fontWeight = FontWeight.ExtraBold) }

            Button(
                onClick = { 
                    if (currentIndex < words.size - 1) { 
                        currentIndex++; isFlipped = false 
                    } else {
                        coroutineScope.launch {
                            com.example.myapplication.utils.FirebaseUtils.updateUserStreak()
                        }
                        Toast.makeText(context, "Hoàn thành ôn tập!", Toast.LENGTH_SHORT).show()
                        onFinish()
                    }
                },
                modifier = Modifier.weight(1f).border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LimeGreen, contentColor = Color.Black)
            ) { Text(if (currentIndex < words.size - 1) "NEXT" else "HOÀN THÀNH", fontWeight = FontWeight.ExtraBold) }
        }
    }
}

// --- HÀM 2: GIAO DIỆN TRẮC NGHIỆM TỪ VỰNG ---
@Composable
fun QuizVocabView(words: List<WordItem>, onFinish: () -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf("") }
    var isAnswered by remember { mutableStateOf(false) }
    val currentWord = words[currentIndex]
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val options = remember(currentIndex) {
        (words.filter { it.meaning != currentWord.meaning }.map { it.meaning }.shuffled().take(3) + currentWord.meaning).shuffled()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(currentWord.word.uppercase(), fontSize = 40.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(30.dp))

        options.forEach { option ->
            val isCorrect = option == currentWord.meaning
            val isSelected = option == selectedAnswer

            Button(
                onClick = { if (!isAnswered) { selectedAnswer = option; isAnswered = true } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        !isAnswered -> Color.White
                        isCorrect -> LimeGreen
                        isSelected -> Color(0xFFFF5252)
                        else -> Color(0xFFF0F0F0)
                    },
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(option, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(8.dp))
            }
        }

        if (isAnswered) {
            Button(
                onClick = { 
                    if (currentIndex < words.size - 1) { 
                        currentIndex++; isAnswered = false; selectedAnswer = "" 
                    } else {
                        coroutineScope.launch {
                            com.example.myapplication.utils.FirebaseUtils.updateUserStreak()
                        }
                        Toast.makeText(context, "Hoàn thành bài tập!", Toast.LENGTH_SHORT).show()
                        onFinish()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
            ) { Text(if (currentIndex < words.size - 1) "TIẾP THEO" else "HOÀN TẤT", fontWeight = FontWeight.ExtraBold) }
        }
    }
}