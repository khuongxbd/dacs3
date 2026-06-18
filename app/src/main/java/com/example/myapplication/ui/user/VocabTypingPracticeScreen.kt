package com.example.myapplication.ui.user

import android.widget.Toast
import androidx.compose.foundation.background
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabTypingPracticeScreen(
    setId: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // State quản lý danh sách từ vựng lấy về từ Firestore
    var vocabWords by remember { mutableStateOf(listOf<com.example.myapplication.data.model.WordItem>()) }
    var isLoading by remember { mutableStateOf(true) }

    // --- STATE TRẠNG THÁI LÀM BÀI ---
    var currentWordIndex by remember { mutableStateOf(0) }
    // Ô nhập liệu cho từ hiện tại
    var currentInput by remember { mutableStateOf("") }
    // Lưu danh sách câu trả lời đã nhập: Key là vị trí câu, Value là chuỗi học sinh gõ
    val typedAnswers = remember { mutableStateMapOf<Int, String>() }
    var isSubmitted by remember { mutableStateOf(false) }

    // Tự động kéo bộ từ vựng về khi mở màn hình lên
    LaunchedEffect(setId) {
        if (setId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("vocabularies").document(setId)
            .get()
            .addOnSuccessListener { snapshot ->
                val vocabSet = snapshot.toObject(FlashcardSet::class.java)
                if (vocabSet != null && vocabSet.words.isNotEmpty()) {
                    // Trộn ngẫu nhiên danh sách từ vựng để tăng tính thử thách khi học
                    vocabWords = vocabSet.words.shuffled()
                } else {
                    Toast.makeText(context, "Học phần này hiện chưa có từ vựng nào!", Toast.LENGTH_LONG).show()
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
                title = { Text("Luyện Tập Gõ Từ Vựng (Viết)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
        } else if (vocabWords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Không có dữ liệu từ vựng.", color = Color.Gray)
            }
        } else {
            val currentWord = vocabWords[currentWordIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                if (!isSubmitted) {
                    // ==================== GIAO DIỆN ĐANG LUYỆN TẬP ====================
                    Text(
                        text = "Từ vựng ${currentWordIndex + 1} / ${vocabWords.size}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )

                    LinearProgressIndicator(
                        progress = { (currentWordIndex + 1).toFloat() / vocabWords.size },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Hiển thị Nghĩa tiếng Việt làm gợi ý cốt lõi
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Hãy viết từ tiếng Anh mang ý nghĩa:", fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentWord.meaning,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Ô để học sinh nhập từ tiếng Anh vào
                        OutlinedTextField(
                            value = currentInput,
                            onValueChange = {
                                currentInput = it
                                typedAnswers[currentWordIndex] = it // Lưu lại trực tiếp vào Map câu trả lời
                            },
                            label = { Text("Nhập từ tiếng Anh chính xác") },
                            placeholder = { Text("e.g. Apple") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // THANH ĐIỀU HƯỚNG TRƯỚC / SAU / NỘP BÀI
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                if (currentWordIndex > 0) {
                                    currentWordIndex--
                                    currentInput = typedAnswers[currentWordIndex] ?: "" // Phục hồi từ đã gõ câu trước
                                }
                            },
                            enabled = currentWordIndex > 0
                        ) { Text("Từ trước") }

                        if (currentWordIndex < vocabWords.size - 1) {
                            Button(
                                onClick = {
                                    currentWordIndex++
                                    currentInput = typedAnswers[currentWordIndex] ?: "" // Lấy từ đã nhập ở câu sau (nếu có)
                                }
                            ) { Text("Từ tiếp theo") }
                        } else {
                            Button(
                                onClick = { isSubmitted = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                            ) { Text("Hoàn thành 🏁") }
                        }
                    }
                } else {
                    // ==================== GIAO DIỆN SAU KHI NỘP BÀI (KẾT QUẢ) ====================

                    // Cơ chế kiểm tra: Ép chữ thường + Xóa khoảng trắng thừa 2 đầu để so khớp chuẩn xác
                    val correctCount = vocabWords.indices.count { idx ->
                        val original = vocabWords[idx].word.trim().lowercase(Locale.ROOT)
                        val typed = (typedAnswers[idx] ?: "").trim().lowercase(Locale.ROOT)
                        original == typed
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
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Kết Quả Luyện Viết Từ Vựng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = "Chính xác $correctCount / ${vocabWords.size} từ",
                                fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Danh sách chi tiết bài viết:", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                        androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(vocabWords.size) { idx ->
                                val w = vocabWords[idx]
                                val userAns = typedAnswers[idx] ?: "Chưa nhập từ"

                                val isCorrect = w.word.trim().lowercase(Locale.ROOT) == userAns.trim().lowercase(Locale.ROOT)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "Nghĩa gợi ý: ${w.meaning}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "• Từ của bạn gõ: $userAns", fontSize = 13.sp, color = if(isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828))
                                        Text(text = "• Từ chuẩn xác: ${w.word}", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
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