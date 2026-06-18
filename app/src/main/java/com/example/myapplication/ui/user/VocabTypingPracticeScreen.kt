package com.example.myapplication.ui.user

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabTypingPracticeScreen(
    setId: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Gam màu: Đen, Trắng, Xanh lá mạ
    val LimeGreen = Color(0xFF00C853)
    val BlackColor = Color.Black
    val WhiteColor = Color.White

    var vocabWords by remember { mutableStateOf(listOf<com.example.myapplication.data.model.WordItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentWordIndex by remember { mutableStateOf(0) }
    var currentInput by remember { mutableStateOf("") }
    val typedAnswers = remember { mutableStateMapOf<Int, String>() }
    var isSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(setId) {
        if (setId.isBlank()) { isLoading = false; return@LaunchedEffect }
        db.collection("vocabularies").document(setId).get()
            .addOnSuccessListener { snapshot ->
                val vocabSet = snapshot.toObject(FlashcardSet::class.java)
                if (vocabSet != null && vocabSet.words.isNotEmpty()) { vocabWords = vocabSet.words.shuffled() }
                else { Toast.makeText(context, "Chưa có từ vựng!", Toast.LENGTH_SHORT).show(); onBack() }
                isLoading = false
            }.addOnFailureListener { isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LUYỆN TẬP GÕ TỪ", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlackColor, titleContentColor = WhiteColor, navigationIconContentColor = WhiteColor)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LimeGreen) }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                if (!isSubmitted) {
                    LinearProgressIndicator(progress = { (currentWordIndex + 1).toFloat() / vocabWords.size }, modifier = Modifier.fillMaxWidth().height(8.dp), color = LimeGreen)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Card 3D
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Nghĩa:", fontSize = 14.sp)
                            Text(vocabWords[currentWordIndex].meaning, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = currentInput,
                        onValueChange = { currentInput = it; typedAnswers[currentWordIndex] = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nhập từ tiếng Anh...") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LimeGreen, focusedLabelColor = LimeGreen)
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Nút BACK (Trước)
                        Button(
                            onClick = { if(currentWordIndex > 0) { currentWordIndex--; currentInput = typedAnswers[currentWordIndex] ?: "" }},
                            enabled = currentWordIndex > 0,
                            modifier = Modifier
                                .weight(1f)
                                .height(55.dp)
                                .shadow(4.dp, RoundedCornerShape(8.dp)), // Đổ bóng 3D
                            shape = RoundedCornerShape(8.dp), // Chữ nhật bo góc 8dp
                            colors = ButtonDefaults.buttonColors(containerColor = WhiteColor, contentColor = BlackColor),
                            border = androidx.compose.foundation.BorderStroke(2.dp, BlackColor) // Viền đen cứng cáp
                        ) {
                            Text("TRƯỚC", fontWeight = FontWeight.ExtraBold)
                        }

                        // Nút TIẾP / HOÀN THÀNH
                        Button(
                            onClick = {
                                if (currentWordIndex < vocabWords.size - 1) {
                                    currentWordIndex++
                                    currentInput = typedAnswers[currentWordIndex] ?: ""
                                } else {
                                    isSubmitted = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(55.dp)
                                .shadow(4.dp, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),
                            // Logic: Nếu là câu cuối thì màu Xanh, còn không thì màu Đen
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentWordIndex == vocabWords.size - 1) LimeGreen else BlackColor,
                                contentColor = if (currentWordIndex == vocabWords.size - 1) BlackColor else WhiteColor
                            ),
                            border = androidx.compose.foundation.BorderStroke(2.dp, BlackColor)
                        ) {
                            Text(
                                text = if (currentWordIndex == vocabWords.size - 1) "HOÀN THÀNH" else "TIẾP",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                } else {
                    val correctCount = vocabWords.indices.count { i -> vocabWords[i].word.trim().lowercase(Locale.ROOT) == (typedAnswers[i] ?: "").trim().lowercase(Locale.ROOT) }
                    Text("Kết quả: $correctCount/${vocabWords.size}", style = MaterialTheme.typography.headlineMedium, color = LimeGreen, fontWeight = FontWeight.Black)

                    LazyColumn(modifier = Modifier.padding(top = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vocabWords.size) { i ->
                            val isCorrect = vocabWords[i].word.trim().lowercase(Locale.ROOT) == (typedAnswers[i] ?: "").trim().lowercase(Locale.ROOT)
                            Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = if(isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))) {
                                ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent), headlineContent = { Text(vocabWords[i].meaning, fontWeight = FontWeight.Bold) }, supportingContent = { Text("Bạn gõ: ${typedAnswers[i] ?: ""} | Đúng: ${vocabWords[i].word}") })
                            }
                        }
                    }
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .padding(top = 8.dp)
                            .shadow(4.dp, RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlackColor, contentColor = WhiteColor)
                    ) {
                        Text("TRỞ VỀ", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}