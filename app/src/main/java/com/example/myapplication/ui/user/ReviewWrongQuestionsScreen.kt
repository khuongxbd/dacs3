package com.example.myapplication.ui.user

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.WrongQuestionSnapshot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewWrongQuestionsScreen(onBack : () -> Unit) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val userId = auth.currentUser?.uid ?: ""

    var wrongDocs by remember { mutableStateOf(listOf<HomeReviewState>()) }
    var currentIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedOption by remember { mutableStateOf("") }
    var isChecked by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isBlank()) { isLoading = false; return@LaunchedEffect }

        db.collection("users").document(userId)
            .collection("spaced_repetition_wrongs")
            .whereLessThanOrEqualTo("nextReviewTime", System.currentTimeMillis())
            .get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<HomeReviewState>()
                for (doc in snapshot.documents) {
                    val classId = doc.getString("classId") ?: ""
                    val streak = doc.getLong("streak")?.toInt() ?: 0
                    val nextReviewTime = doc.getLong("nextReviewTime") ?: 0L

                    val snapMap = doc.get("snapshot") as? Map<String, Any>
                    if (snapMap != null) {
                        val snapshotObj = WrongQuestionSnapshot(
                            questionText = snapMap["questionText"] as? String ?: "",
                            optionA = snapMap["optionA"] as? String ?: "",
                            optionB = snapMap["optionB"] as? String ?: "",
                            optionC = snapMap["optionC"] as? String ?: "",
                            optionD = snapMap["optionD"] as? String ?: "",
                            correctOption = snapMap["correctOption"] as? String ?: "",
                            userSelected = snapMap["userSelected"] as? String ?: "",
                            explanation = snapMap["explanation"] as? String ?: ""
                        )
                        list.add(HomeReviewState(doc.id, classId, snapshotObj, streak, nextReviewTime))
                    }
                }
                wrongDocs = list
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LUYỆN LẠI CÂU SAI", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlackColor, titleContentColor = WhiteColor, navigationIconContentColor = WhiteColor)
            )
        }
    )
    { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (wrongDocs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("🎉 Bạn không còn câu hỏi sai nào cần ôn tập hiện tại.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            val currentData = wrongDocs[currentIndex]
            val question = currentData.snapshot

            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                LinearProgressIndicator(progress = { (currentIndex + 1).toFloat() / wrongDocs.size }, modifier = Modifier.fillMaxWidth().height(8.dp), color = LimeGreen)
                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = question.questionText, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    val options = listOf(
                        "A" to question.optionA,
                        "B" to question.optionB,
                        "C" to question.optionC,
                        "D" to question.optionD
                    )

                    options.forEach { (letter, text) ->
                        val isSelected = selectedOption == letter
                        val isCorrect = letter == question.correctOption

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                .clickable(enabled = !isChecked) { selectedOption = letter },
                            colors = CardDefaults.cardColors(containerColor = if (isChecked && isCorrect) LimeGreen.copy(alpha = 0.2f) else if (isChecked && isSelected && !isCorrect) Color(0xFFFFEBEE) else WhiteColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = BlackColor))
                                Text(text = "$letter. $text", fontSize = 16.sp, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    if (isChecked && question.explanation.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .border(2.dp, BlackColor, RoundedCornerShape(8.dp)), // Viền đen cứng cáp
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("💡 GIẢI THÍCH:", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Green)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = question.explanation, fontSize = 15.sp, color = BlackColor)
                            }
                        }
                    }

                }

                Button(
                    onClick = {
                        if (!isChecked) {
                            if (selectedOption.isBlank()) Toast.makeText(context, "Chọn đáp án đi!", Toast.LENGTH_SHORT).show()
                            else isChecked = true
                        } else {
                            if (currentIndex < wrongDocs.size - 1) { currentIndex++; selectedOption = ""; isChecked = false }
                            else { Toast.makeText(context, "Hoàn thành!", Toast.LENGTH_SHORT).show(); onBack() }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isChecked) BlackColor else LimeGreen,
                        contentColor = if (!isChecked) WhiteColor else BlackColor // Chữ trắng trên nền đen, chữ Đen trên nền Xanh
                    ),
                    border = BorderStroke(2.dp, BlackColor)
                ) {
                    Text(
                        text = if (!isChecked) "KIỂM TRA ĐÁP ÁN" else if (currentIndex < wrongDocs.size - 1) "CÂU TIẾP THEO" else "HOÀN THÀNH",
                        fontWeight = FontWeight.ExtraBold
                    )
                }


            }
        }
    }
}