package com.example.myapplication.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ClassModel
import com.example.myapplication.data.model.ClassQuiz
import com.example.myapplication.data.model.FlashcardSet
import com.example.myapplication.data.model.QuizQuestion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuizScreen() {
    val db = FirebaseFirestore.getInstance()
    val adminId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val context = LocalContext.current

    var quizLists by remember { mutableStateOf(listOf<ClassQuiz>()) }
    var myClasses by remember { mutableStateOf(listOf<ClassModel>()) }
    var vocabLists by remember { mutableStateOf(listOf<FlashcardSet>()) }
    var isLoading by remember { mutableStateOf(true) }

    // State điều khiển Dialogs công cụ
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var showSelectVocabDropdown by remember { mutableStateOf(false) } // Điều khiển dialog chọn bộ từ vựng để điền nhanh

    var targetQuizForAssign by remember { mutableStateOf<ClassQuiz?>(null) }
    var targetQuizForEdit by remember { mutableStateOf<ClassQuiz?>(null) }

    // --- STATE PHỤC VỤ FORM ĐỀ THI ---
    var quizTitle by remember { mutableStateOf("") }
    var quizDuration by remember { mutableStateOf("15") }
    val tempQuestions = remember { mutableStateListOf<QuizQuestion>() }

    // State của từng câu hỏi con đang nhập liệu
    var qText by remember { mutableStateOf("") }
    var opA by remember { mutableStateOf("") }
    var opB by remember { mutableStateOf("") }
    var opC by remember { mutableStateOf("") }
    var opD by remember { mutableStateOf("") }
    var correctOp by remember { mutableStateOf("A") }
    var qExplanation by remember { mutableStateOf("") }
    var qImgUrl by remember { mutableStateOf("") }
    var editingQuestionIndex by remember { mutableStateOf(-1) }

    // Lắng nghe dữ liệu Realtime từ Firestore
    LaunchedEffect(adminId) {
        if (adminId.isBlank()) return@LaunchedEffect

        db.collection("quizzes")
            .whereEqualTo("adminId", adminId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) quizLists = snapshot.toObjects(ClassQuiz::class.java)
                isLoading = false
            }

        db.collection("classes").whereEqualTo("adminId", adminId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) myClasses = snapshot.toObjects(ClassModel::class.java)
            }

        db.collection("vocabularies").whereEqualTo("adminId", adminId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) vocabLists = snapshot.toObjects(FlashcardSet::class.java)
            }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kho Đề Kiểm Tra (Admin)") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Tạo Bài Kiểm Tra") },
                icon = { Icon(Icons.Default.Add, "Add") },
                onClick = {
                    quizTitle = ""; quizDuration =
                    "15"; tempQuestions.clear(); editingQuestionIndex = -1
                    qText = ""; opA = ""; opB = ""; opC = ""; opD = ""; qImgUrl = ""; qExplanation =
                    ""
                    showCreateDialog = true
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else if (quizLists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Kho trống. Hãy nhấn nút Tạo Bài Kiểm Tra ở góc dưới!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(quizLists) { quiz ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                targetQuizForEdit = quiz
                                quizTitle = quiz.title
                                quizDuration = quiz.duration.toString()
                                tempQuestions.clear()
                                tempQuestions.addAll(quiz.questions)
                                editingQuestionIndex = -1
                                qText = ""; opA = ""; opB = ""; opC = ""; opD = ""; qImgUrl =
                                ""; qExplanation = ""
                                showEditDialog = true
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    quiz.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Thời lượng: ${quiz.duration} phút | Số câu hỏi: ${quiz.questions.size} câu",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    targetQuizForAssign = quiz; showAssignDialog = true
                                }) {
                                    Icon(
                                        Icons.Default.Send,
                                        "Giao đề",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                IconButton(onClick = {
                                    val quizIdToDelete = quiz.quizId

                                    // Tạo WriteBatch để gom các lệnh xóa thực thi cùng một lúc
                                    val batch = db.batch()

                                    // 1. Duyệt qua toàn bộ danh sách lớp của Admin đang quản lý (đã có sẵn trong myClasses)
                                    myClasses.forEach { classItem ->
                                        // Trỏ thẳng tới vị trí ID đề thi nằm trong sub-collection class_quizzes của từng lớp
                                        val classQuizRef = db.collection("classes").document(classItem.classId)
                                            .collection("class_quizzes").document(quizIdToDelete)

                                        // Thêm lệnh xóa vào bộ gom (Cho dù lớp đó chưa được giao đề này, lệnh xóa vẫn chạy hợp lệ và không báo lỗi)
                                        batch.delete(classQuizRef)
                                    }

                                    // 2. Thêm lệnh xóa tài liệu đề thi gốc tại bảng /quizzes
                                    val quizRef = db.collection("quizzes").document(quizIdToDelete)
                                    batch.delete(quizRef)

                                    // 3. Thực thi chuỗi lệnh xóa đồng bộ
                                    batch.commit()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Đã xóa đề gốc và dọn sạch liên kết ở các lớp học! 🧹✨", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Lỗi khi xóa đề: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                }) {
                                    Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- HÀM LAYOUT FORM ĐIỀN CÂU HỎI (TÍCH HỢP LỰA CHỌN ĐIỀN NHANH TỪ VỰNG) ---
        @Composable
        fun QuizFormContent() {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quizTitle,
                    onValueChange = { quizTitle = it },
                    label = { Text("Tên tiêu đề bài kiểm tra") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quizDuration,
                    onValueChange = { quizDuration = it },
                    label = { Text("Thời gian làm bài (Phút)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // HÀNG CÔNG CỤ CHỌN CÁCH THÊM CÂU HỎI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (editingQuestionIndex == -1) "Soạn câu hỏi:" else "Đang chỉnh sửa câu hỏi:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // NÚT LỰA CHỌN: BỐC TỪ VỰNG ĐỂ ĐIỀN TỰ ĐỘNG
                    if (editingQuestionIndex == -1) {
                        Button(
                            onClick = {
                                if (vocabLists.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "Kho từ vựng của bạn đang trống!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    showSelectVocabDropdown = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bốc từ kho từ vựng", fontSize = 12.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = qText,
                    onValueChange = { qText = it },
                    label = { Text("Nội dung câu hỏi") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = qImgUrl,
                    onValueChange = { qImgUrl = it },
                    label = { Text("Link hình ảnh đính kèm (Nếu có)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = opA,
                    onValueChange = { opA = it },
                    label = { Text("Đáp án A") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = opB,
                    onValueChange = { opB = it },
                    label = { Text("Đáp án B") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = opC,
                    onValueChange = { opC = it },
                    label = { Text("Đáp án C") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = opD,
                    onValueChange = { opD = it },
                    label = { Text("Đáp án D") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Chọn đáp án đúng:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("A", "B", "C", "D").forEach { options ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (correctOp == options),
                                onClick = { correctOp = options })
                            Text(options)
                        }
                    }
                }
                OutlinedTextField(
                    value = qExplanation,
                    onValueChange = { qExplanation = it },
                    label = { Text("Giải thích chi tiết") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (editingQuestionIndex != -1) {
                        TextButton(onClick = {
                            editingQuestionIndex = -1; qText = ""; opA = ""; opB = ""; opC =
                            ""; opD = ""; qImgUrl = ""; qExplanation = ""
                        }) { Text("Hủy sửa") }
                    }
                    Button(onClick = {
                        if (qText.isBlank() || opA.isBlank() || opB.isBlank() || opC.isBlank() || opD.isBlank()) return@Button
                        val ques = QuizQuestion(
                            questionText = qText,
                            optionA = opA,
                            optionB = opB,
                            optionC = opC,
                            optionD = opD,
                            correctOption = correctOp,
                            explanation = qExplanation,
                            imageUrl = qImgUrl
                        )
                        if (editingQuestionIndex == -1) tempQuestions.add(ques) else tempQuestions[editingQuestionIndex] =
                            ques
                        editingQuestionIndex = -1
                        qText = ""; opA = ""; opB = ""; opC = ""; opD = ""; qImgUrl =
                        ""; qExplanation = ""
                    }) { Text(if (editingQuestionIndex == -1) "Thêm câu hỏi vào đề" else "Cập nhật câu hỏi") }
                }

                Text(
                    "Danh sách câu hỏi trong đề (${tempQuestions.size}):",
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                tempQuestions.forEachIndexed { idx, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (editingQuestionIndex == idx) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.4f
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp, 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${idx + 1}. ${item.questionText}",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Đúng: ${item.correctOption} | A: ${item.optionA} | B: ${item.optionB}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    editingQuestionIndex = idx; qText = item.questionText; opA =
                                    item.optionA; opB = item.optionB; opC = item.optionC; opD =
                                    item.optionD; correctOp = item.correctOption; qImgUrl =
                                    item.imageUrl; qExplanation = item.explanation
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Default.Edit,
                                        "Sửa",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    tempQuestions.removeAt(idx); if (editingQuestionIndex == idx) editingQuestionIndex =
                                    -1
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Xóa",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOG 1: SOẠN ĐỀ THỦ CÔNG ---
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Tạo Bài Kiểm Tra Mới") },
                text = { QuizFormContent() },
                confirmButton = {
                    Button(onClick = {
                        if (quizTitle.isBlank() || tempQuestions.isEmpty()) return@Button
                        val qId = UUID.randomUUID().toString()
                        val dataMap = hashMapOf(
                            "quizId" to qId, "title" to quizTitle,
                            "duration" to (quizDuration.toIntOrNull() ?: 15),
                            "questions" to tempQuestions.toList(), "adminId" to adminId
                        )
                        db.collection("quizzes").document(qId).set(dataMap)
                            .addOnSuccessListener {
                                showCreateDialog = false; Toast.makeText(
                                context,
                                "Đã lưu đề thi thành công!",
                                Toast.LENGTH_SHORT
                            ).show()
                            }
                    }) { Text("Lưu Đề") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCreateDialog = false
                    }) { Text("Hủy") }
                }
            )
        }

        // --- DIALOG 2: CHỈNH SỬA ĐỀ THI ---
        if (showEditDialog && targetQuizForEdit != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false; targetQuizForEdit = null },
                title = { Text("Chỉnh Sửa Bài Kiểm Tra") },
                text = { QuizFormContent() },
                confirmButton = {
                    Button(
                        enabled = editingQuestionIndex == -1,
                        onClick = {
                            if (quizTitle.isBlank() || tempQuestions.isEmpty()) return@Button
                            val dataMap = hashMapOf(
                                "quizId" to targetQuizForEdit!!.quizId, "title" to quizTitle,
                                "duration" to (quizDuration.toIntOrNull() ?: 15),
                                "questions" to tempQuestions.toList(), "adminId" to adminId
                            )
                            db.collection("quizzes").document(targetQuizForEdit!!.quizId)
                                .set(dataMap)
                                .addOnSuccessListener {
                                    showEditDialog = false; targetQuizForEdit =
                                    null; Toast.makeText(
                                    context,
                                    "Đã cập nhật bài thi thành công!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                }
                        }
                    ) { Text("Cập Nhật Đề") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEditDialog = false; targetQuizForEdit = null
                    }) { Text("Hủy") }
                }
            )
        }

        // --- POPUP LỰA CHỌN BỘ TỪ VỰNG ĐỂ AUTO-FILL ĐIỀN NHANH VÀO Ô ---
        // --- POPUP LỰA CHỌN BỘ TỪ VỰNG & SỐ LƯỢNG ĐỂ BỐC HÀNG LOẠT VÀO ĐỀ ---
        if (showSelectVocabDropdown) {
            // State quản lý bộ từ vựng đang được chọn trong popup và số lượng câu muốn bốc
            var chosenSet by remember { mutableStateOf<FlashcardSet?>(vocabLists.firstOrNull()) }
            var countInput by remember { mutableStateOf("5") }

            AlertDialog(
                onDismissRequest = { showSelectVocabDropdown = false },
                title = { Text("Bốc Câu Hỏi Từ Kho Từ Vựng") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Chọn chủ đề học phần và số lượng câu. Hệ thống sẽ tự động sinh câu hỏi trắc nghiệm Anh-Việt xoay vòng và cộng dồn vào đề hiện tại:",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Text(
                            "1. Chọn chủ đề nguồn:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        // Danh sách các bộ từ vựng để chọn
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 180.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(vocabLists) { vocabSet ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { chosenSet = vocabSet }
                                        .background(
                                            if (chosenSet?.setId == vocabSet.setId) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(8.dp)
                                ) {
                                    RadioButton(
                                        selected = (chosenSet?.setId == vocabSet.setId),
                                        onClick = { chosenSet = vocabSet })
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "${vocabSet.title} (${vocabSet.words.size} từ)",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        Text(
                            "2. Nhập số lượng câu muốn bốc:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        OutlinedTextField(
                            value = countInput,
                            onValueChange = { countInput = it },
                            label = { Text("Ví dụ: 1, 4, 5...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val currentSet = chosenSet ?: return@Button
                        val desiredCount = countInput.toIntOrNull() ?: 1

                        if (currentSet.words.size < 4) {
                            Toast.makeText(
                                context,
                                "Chủ đề '${currentSet.title}' phải có tối thiểu 4 từ để trộn đáp án nhiễu!",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }

                        if (desiredCount <= 0) {
                            Toast.makeText(
                                context,
                                "Vui lòng nhập số lượng câu hợp lệ!",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        // Xáo trộn danh sách từ vựng gốc để đảm bảo tính ngẫu nhiên mỗi lần bốc
                        val shuffledSourceWords = currentSet.words.shuffled()
                        // Giới hạn số lượng bốc không vượt quá số từ thực tế đang có trong kho
                        val finalLoopCount = minOf(desiredCount, shuffledSourceWords.size)

                        for (i in 0 until finalLoopCount) {
                            val targetWord = shuffledSourceWords[i]
                            val questionType = (0..1).random() // 50% hỏi xuôi, 50% hỏi ngược

                            val generatedQuestionText: String
                            val correctAnsString: String
                            val optionsList: List<String>

                            if (questionType == 0) {
                                generatedQuestionText =
                                    "Từ vựng '${targetWord.word}' có nghĩa là gì?"
                                correctAnsString = targetWord.meaning
                                val wrongs =
                                    currentSet.words.filter { it.meaning != correctAnsString }
                                        .map { it.meaning }.shuffled().take(3)
                                optionsList = (wrongs + correctAnsString).shuffled()
                            } else {
                                generatedQuestionText =
                                    "Nghĩa '${targetWord.meaning}' là của từ vựng nào dưới đây?"
                                correctAnsString = targetWord.word
                                val wrongs = currentSet.words.filter { it.word != correctAnsString }
                                    .map { it.word }.shuffled().take(3)
                                optionsList = (wrongs + correctAnsString).shuffled()
                            }

                            val correctLetter = when (optionsList.indexOf(correctAnsString)) {
                                0 -> "A"
                                1 -> "B"
                                2 -> "C"
                                else -> "D"
                            }

                            // Tạo object câu hỏi hoàn chỉnh
                            val autoQuestion = QuizQuestion(
                                questionText = generatedQuestionText,
                                optionA = optionsList.getOrNull(0) ?: "",
                                optionB = optionsList.getOrNull(1) ?: "",
                                optionC = optionsList.getOrNull(2) ?: "",
                                optionD = optionsList.getOrNull(3) ?: "",
                                correctOption = correctLetter,
                                explanation = "Đáp án đúng là $correctLetter. Từ: ${targetWord.word} -> Nghĩa: ${targetWord.meaning}",
                                imageUrl = targetWord.imageUrl
                            )

                            // TIẾN HÀNH CỘNG DỒN (APPEND) VÀO DANH SÁCH CÂU HỎI HIỆN TẠI CỦA ĐỀ
                            tempQuestions.add(autoQuestion)
                        }

                        Toast.makeText(
                            context,
                            "Đã bốc thêm thành công $finalLoopCount câu từ bộ '${currentSet.title}'!",
                            Toast.LENGTH_SHORT
                        ).show()
                        showSelectVocabDropdown = false // Đóng popup
                    }) {
                        Text("Bốc Vào Đề")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSelectVocabDropdown = false }) { Text("Hủy") }
                }
            )
        }

        // --- DIALOG 4: GIAO ĐỀ KIỂM TRA VÀO LỚP HỌC (CHỈ GỬI QUIZ ID) ---
        if (showAssignDialog && targetQuizForAssign != null) {
            AlertDialog(
                onDismissRequest = { showAssignDialog = false; targetQuizForAssign = null },
                title = { Text("Giao Bài Kiểm Tra Vào Lớp") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Chọn lớp học muốn giao bài thi '${targetQuizForAssign!!.title}':",
                            fontSize = 14.sp
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(myClasses) { classItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val quizIdToAssign = targetQuizForAssign!!.quizId

                                            val assignMap = hashMapOf(
                                                "quizId" to quizIdToAssign,
                                                "assignedAt" to System.currentTimeMillis()
                                            )

                                            db.collection("classes").document(classItem.classId)
                                                .collection("class_quizzes")
                                                .document(quizIdToAssign)
                                                .set(assignMap) // 🚀 Chỉ đẩy id lên lớp học
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        context,
                                                        "Đã giao mã đề vào lớp ${classItem.className}! 🎉",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    showAssignDialog = false
                                                    targetQuizForAssign = null
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(
                                                        context,
                                                        "Lỗi khi giao đề vào lớp!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(classItem.className)
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}