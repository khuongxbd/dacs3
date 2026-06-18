package com.example.myapplication.ui.admin

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import com.example.myapplication.util.WordParserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var showSelectVocabDropdown by remember { mutableStateOf(false) }

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

    // Tạo scope để chạy bất đồng bộ đọc file Word
    val coroutineScope = rememberCoroutineScope()

    // --- LAUNCHER HỆ THỐNG: CHỌN FILE WORD THU GỌN ---
    val wordFilePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { selectedUri ->
                val appContext = context.applicationContext
                coroutineScope.launch(Dispatchers.IO) {
                    val importedQuestions = WordParserUtils.parseWordDocx(appContext, selectedUri)
                    withContext(Dispatchers.Main) {
                        if (importedQuestions.isNotEmpty()) {
                            tempQuestions.addAll(importedQuestions)
                            Toast.makeText(context, "Đã bốc thành công ${importedQuestions.size} câu vào danh sách nháp!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Không tìm thấy câu hỏi hoặc file lỗi định dạng!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    )

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
                    quizTitle = ""; quizDuration = "15"; tempQuestions.clear(); editingQuestionIndex = -1
                    qText = ""; opA = ""; opB = ""; opC = ""; opD = ""; qImgUrl = ""; qExplanation = ""
                    showCreateDialog = true
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else if (quizLists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Kho trống. Hãy nhấn nút Tạo Bài Kiểm Tra ở góc dưới!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
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
                                qText = ""; opA = ""; opB = ""; opC = ""; opD = ""; qImgUrl = ""; qExplanation = ""
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
                                Text(quiz.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Thời lượng: ${quiz.duration} phút | Số câu hỏi: ${quiz.questions.size} câu", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                            Row {
                                IconButton(onClick = { targetQuizForAssign = quiz; showAssignDialog = true }) {
                                    Icon(Icons.Default.Send, "Giao đề", tint = MaterialTheme.colorScheme.secondary)
                                }
                                IconButton(onClick = {
                                    val quizIdToDelete = quiz.quizId
                                    val batch = db.batch()
                                    myClasses.forEach { classItem ->
                                        val classQuizRef = db.collection("classes").document(classItem.classId)
                                            .collection("class_quizzes").document(quizIdToDelete)
                                        batch.delete(classQuizRef)
                                    }
                                    val quizRef = db.collection("quizzes").document(quizIdToDelete)
                                    batch.delete(quizRef)
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

        // --- DIALOG 1: SOẠN ĐỀ THỦ CÔNG & WORD ---
        if (showCreateDialog) {
            var showTimePickerPopup by remember { mutableStateOf(false) }
            var selectedStartTime by remember { mutableStateOf<Long?>(null) }
            var selectedEndTime by remember { mutableStateOf<Long?>(null) }

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Tạo Bài Kiểm Tra Mới") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Thời hạn làm bài:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Button(
                                onClick = { showTimePickerPopup = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Text(if (selectedStartTime != null && selectedEndTime != null) "Đã đặt thời hạn" else "Đặt thời hạn")
                            }
                        }

                        if (selectedStartTime != null && selectedEndTime != null) {
                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                            Text(
                                text = "⏱️ Từ: ${sdf.format(selectedStartTime)}\n⏱️ Đến: ${sdf.format(selectedEndTime)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider()

                        // Component nhập liệu form chính (ĐÃ FIX: Truyền đầy đủ tham số state)
                        QuizFormContent(
                            quizTitle = quizTitle, onQuizTitleChange = { quizTitle = it },
                            quizDuration = quizDuration, onQuizDurationChange = { quizDuration = it },
                            editingQuestionIndex = editingQuestionIndex,
                            onCancelEdit = { editingQuestionIndex = -1; qText = ""; opA = ""; opB = ""; opC = ""; opD = ""; qImgUrl = ""; qExplanation = "" },
                            onSelectVocabClick = { if (vocabLists.isEmpty()) Toast.makeText(context, "Kho từ vựng trống!", Toast.LENGTH_SHORT).show() else showSelectVocabDropdown = true },
                            qText = qText, onQTextChange = { qText = it },
                            qImgUrl = qImgUrl, onQImgUrlChange = { qImgUrl = it },
                            opA = opA, onOpAChange = { opA = it },
                            opB = opB, onOpBChange = { opB = it },
                            opC = opC, onOpCChange = { opC = it },
                            opD = opD, onOpDChange = { opD = it },
                            correctOp = correctOp, onCorrectOpChange = { correctOp = it },
                            qExplanation = qExplanation, onQExplanationChange = { qExplanation = it },
                            tempQuestions = tempQuestions,
                            wordFilePickerLauncher = wordFilePickerLauncher,
                            onAddOrUpdateQuestion = {
                                if (qText.isBlank() || opA.isBlank() || opB.isBlank() || opC.isBlank() || opD.isBlank()) return@QuizFormContent
                                val ques = QuizQuestion(qText, opA, opB, opC, opD, correctOp, qExplanation, qImgUrl)
                                if (editingQuestionIndex == -1) tempQuestions.add(ques) else tempQuestions[editingQuestionIndex] = ques
                                editingQuestionIndex = -1
                                qText = ""; opA = ""; opB = ""; opC = ""; opD = ""; qImgUrl = ""; qExplanation = ""
                            },
                            onEditQuestionClick = { idx, item ->
                                editingQuestionIndex = idx; qText = item.questionText; opA = item.optionA; opB = item.optionB; opC = item.optionC; opD = item.optionD; correctOp = item.correctOption; qImgUrl = item.imageUrl; qExplanation = item.explanation
                            },
                            onDeleteQuestionClick = { idx ->
                                tempQuestions.removeAt(idx)
                                if (editingQuestionIndex == idx) editingQuestionIndex = -1
                            },
                            context = context
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (quizTitle.isBlank() || tempQuestions.isEmpty()) return@Button
                        val qId = UUID.randomUUID().toString()

                        val dataMap = hashMapOf(
                            "quizId" to qId,
                            "classId" to "",
                            "title" to quizTitle,
                            "duration" to (quizDuration.toIntOrNull() ?: 15),
                            "startTime" to selectedStartTime,
                            "endTime" to selectedEndTime,
                            "questions" to tempQuestions.toList(),
                            "adminId" to adminId
                        )
                        db.collection("quizzes").document(qId).set(dataMap)
                            .addOnSuccessListener {
                                showCreateDialog = false; Toast.makeText(context, "Đã lưu đề thi thành công!", Toast.LENGTH_SHORT).show()
                            }
                    }) { Text("Lưu Đề") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Hủy") }
                }
            )

            // --- DIALOG CON PHỤC VỤ CHỌN THỜI GIAN (START / END TIME) ---
            if (showTimePickerPopup) {
                var durationHoursInput by remember { mutableStateOf("24") }

                AlertDialog(
                    onDismissRequest = { showTimePickerPopup = false },
                    title = { Text("Cấu Hình Thời Hạn Mở Đề") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Nhập số giờ hệ thống sẽ mở đề thi này kể từ bây giờ:", fontSize = 13.sp)
                            OutlinedTextField(
                                value = durationHoursInput,
                                onValueChange = { durationHoursInput = it },
                                label = { Text("Số giờ hiệu lực (Ví dụ: 2, 12, 24)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val hours = durationHoursInput.toLongOrNull() ?: 24
                            val now = System.currentTimeMillis()
                            selectedStartTime = now
                            selectedEndTime = now + (hours * 60 * 60 * 1000)
                            showTimePickerPopup = false
                        }) { Text("Xác Nhận") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePickerPopup = false }) { Text("Hủy") }
                    }
                )
            }
        }

        // --- DIALOG 2: CHỈNH SỬA ĐỀ THI ---
        if (showEditDialog && targetQuizForEdit != null) {
            var selectedStartTime by remember { mutableStateOf(targetQuizForEdit!!.startTime) }
            var selectedEndTime by remember { mutableStateOf(targetQuizForEdit!!.endTime) }
            var showTimePickerPopup by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showEditDialog = false; targetQuizForEdit = null },
                title = { Text("Chỉnh Sửa Bài Kiểm Tra") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Thời hạn làm bài:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Button(onClick = { showTimePickerPopup = true }) {
                                Text("Thay Đổi Hạn")
                            }
                        }

                        if (selectedStartTime != null && selectedEndTime != null) {
                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                            Text(
                                text = "⏱️ Từ: ${sdf.format(selectedStartTime)}\n⏱️ Đến: ${sdf.format(selectedEndTime)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider()

                        // Component nhập liệu form chính (ĐÃ FIX: Truyền đầy đủ tham số state)
                        QuizFormContent(
                            quizTitle = quizTitle, onQuizTitleChange = { quizTitle = it },
                            quizDuration = quizDuration, onQuizDurationChange = { quizDuration = it },
                            editingQuestionIndex = editingQuestionIndex,
                            onCancelEdit = { editingQuestionIndex = -1; qText = ""; opA = ""; opB = ""; opC = ""; opD = ""; qImgUrl = ""; qExplanation = "" },
                            onSelectVocabClick = { if (vocabLists.isEmpty()) Toast.makeText(context, "Kho từ vựng trống!", Toast.LENGTH_SHORT).show() else showSelectVocabDropdown = true },
                            qText = qText, onQTextChange = { qText = it },
                            qImgUrl = qImgUrl, onQImgUrlChange = { qImgUrl = it },
                            opA = opA, onOpAChange = { opA = it },
                            opB = opB, onOpBChange = { opB = it },
                            opC = opC, onOpCChange = { opC = it },
                            opD = opD, onOpDChange = { opD = it },
                            correctOp = correctOp, onCorrectOpChange = { correctOp = it },
                            qExplanation = qExplanation, onQExplanationChange = { qExplanation = it },
                            tempQuestions = tempQuestions,
                            wordFilePickerLauncher = wordFilePickerLauncher,
                            onAddOrUpdateQuestion = {
                                if (qText.isBlank() || opA.isBlank() || opB.isBlank() || opC.isBlank() || opD.isBlank()) return@QuizFormContent
                                val ques = QuizQuestion(qText, opA, opB, opC, opD, correctOp, qExplanation, qImgUrl)
                                if (editingQuestionIndex == -1) tempQuestions.add(ques) else tempQuestions[editingQuestionIndex] = ques
                                editingQuestionIndex = -1
                                qText = ""; opA = ""; opB = ""; opC = ""; opD = ""; qImgUrl = ""; qExplanation = ""
                            },
                            onEditQuestionClick = { idx, item ->
                                editingQuestionIndex = idx; qText = item.questionText; opA = item.optionA; opB = item.optionB; opC = item.optionC; opD = item.optionD; correctOp = item.correctOption; qImgUrl = item.imageUrl; qExplanation = item.explanation
                            },
                            onDeleteQuestionClick = { idx ->
                                tempQuestions.removeAt(idx)
                                if (editingQuestionIndex == idx) editingQuestionIndex = -1
                            },
                            context = context
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = editingQuestionIndex == -1,
                        onClick = {
                            if (quizTitle.isBlank() || tempQuestions.isEmpty()) return@Button
                            val dataMap = hashMapOf(
                                "quizId" to targetQuizForEdit!!.quizId,
                                "classId" to targetQuizForEdit!!.classId,
                                "title" to quizTitle,
                                "duration" to (quizDuration.toIntOrNull() ?: 15),
                                "startTime" to selectedStartTime,
                                "endTime" to selectedEndTime,
                                "questions" to tempQuestions.toList(),
                                "adminId" to adminId
                            )
                            db.collection("quizzes").document(targetQuizForEdit!!.quizId)
                                .set(dataMap)
                                .addOnSuccessListener {
                                    showEditDialog = false; targetQuizForEdit = null
                                    Toast.makeText(context, "Đã cập nhật bài thi thành công!", Toast.LENGTH_SHORT).show()
                                }
                        }
                    ) { Text("Cập Nhật Đề") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false; targetQuizForEdit = null }) { Text("Hủy") }
                }
            )

            if (showTimePickerPopup) {
                var durationHoursInput by remember { mutableStateOf("24") }
                AlertDialog(
                    onDismissRequest = { showTimePickerPopup = false },
                    title = { Text("Cấu Hình Thời Hạn Mở Đề") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = durationHoursInput,
                                onValueChange = { durationHoursInput = it },
                                label = { Text("Số giờ hiệu lực mới") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val hours = durationHoursInput.toLongOrNull() ?: 24
                            val now = System.currentTimeMillis()
                            selectedStartTime = now
                            selectedEndTime = now + (hours * 60 * 60 * 1000)
                            showTimePickerPopup = false
                        }) { Text("Cập Nhật") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePickerPopup = false }) { Text("Hủy") }
                    }
                )
            }
        }

        // --- DIALOG 3: POPUP LỰA CHỌN BỘ TỪ VỰNG ĐỂ AUTO-FILL ---
        if (showSelectVocabDropdown) {
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
                            text = "Chọn chủ đề học phần và số lượng câu. Hệ thống sẽ tự động sinh câu hỏi trắc nghiệm Anh-Việt xoay vòng và cộng dồn vào danh sách chờ hiện tại:",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Text("1. Chọn chủ đề nguồn:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

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

                        Text("2. Nhập số lượng câu muốn bốc:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                            Toast.makeText(context, "Chủ đề '${currentSet.title}' phải có tối thiểu 4 từ để trộn đáp án nhiễu!", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        if (desiredCount <= 0) {
                            Toast.makeText(context, "Vui lòng nhập số lượng câu hợp lệ!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val shuffledSourceWords = currentSet.words.shuffled()
                        val finalLoopCount = minOf(desiredCount, shuffledSourceWords.size)

                        for (i in 0 until finalLoopCount) {
                            val targetWord = shuffledSourceWords[i]
                            val questionType = (0..1).random()

                            val generatedQuestionText: String
                            val correctAnsString: String
                            val optionsList: List<String>

                            if (questionType == 0) {
                                generatedQuestionText = "Từ vựng '${targetWord.word}' có nghĩa là gì?"
                                correctAnsString = targetWord.meaning
                                val wrongs = currentSet.words.filter { it.meaning != correctAnsString }.map { it.meaning }.shuffled().take(3)
                                optionsList = (wrongs + correctAnsString).shuffled()
                            } else {
                                generatedQuestionText = "Nghĩa '${targetWord.meaning}' là của từ vựng nào dưới đây?"
                                correctAnsString = targetWord.word
                                val wrongs = currentSet.words.filter { it.word != correctAnsString }.map { it.word }.shuffled().take(3)
                                optionsList = (wrongs + correctAnsString).shuffled()
                            }

                            val correctLetter = when (optionsList.indexOf(correctAnsString)) {
                                0 -> "A"
                                1 -> "B"
                                2 -> "C"
                                else -> "D"
                            }

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
                            tempQuestions.add(autoQuestion)
                        }

                        Toast.makeText(context, "Đã bốc thêm thành công $finalLoopCount câu từ bộ '${currentSet.title}' vào danh sách nháp!", Toast.LENGTH_SHORT).show()
                        showSelectVocabDropdown = false
                    }) {
                        Text("Bốc Vào Đề")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSelectVocabDropdown = false }) { Text("Hủy") }
                }
            )
        }

        // --- DIALOG 4: GIAO ĐỀ KIỂM TRA VÀO LỚP HỌC ---
        if (showAssignDialog && targetQuizForAssign != null) {
            AlertDialog(
                onDismissRequest = { showAssignDialog = false; targetQuizForAssign = null },
                title = { Text("Giao Bài Kiểm Tra Vào Lớp") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Chọn lớp học muốn giao bài thi '${targetQuizForAssign!!.title}':", fontSize = 14.sp)
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(myClasses) { classItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val quizIdToAssign = targetQuizForAssign!!.quizId
                                            val assignMap = hashMapOf(
                                                "quizId" to quizIdToAssign,
                                                "classId" to classItem.classId,
                                                "title" to targetQuizForAssign!!.title,
                                                "duration" to targetQuizForAssign!!.duration,
                                                "startTime" to targetQuizForAssign!!.startTime,
                                                "endTime" to targetQuizForAssign!!.endTime,
                                                "questions" to targetQuizForAssign!!.questions,
                                                "assignedAt" to System.currentTimeMillis()
                                            )

                                            db.collection("classes").document(classItem.classId)
                                                .collection("class_quizzes")
                                                .document(quizIdToAssign)
                                                .set(assignMap)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Đã giao mã đề vào lớp ${classItem.className}! 🎉", Toast.LENGTH_SHORT).show()
                                                    showAssignDialog = false
                                                    targetQuizForAssign = null
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(context, "Lỗi khi giao đề vào lớp!", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

// =========================================================================
// COMPOSABLE COMPONENT ĐỘC LẬP: LAYOUT FORM ĐIỀN CÂU HỎI
// =========================================================================
@Composable
fun QuizFormContent(
    quizTitle: String,
    onQuizTitleChange: (String) -> Unit,
    quizDuration: String,
    onQuizDurationChange: (String) -> Unit,
    editingQuestionIndex: Int,
    onCancelEdit: () -> Unit,
    onSelectVocabClick: () -> Unit,
    qText: String,
    onQTextChange: (String) -> Unit,
    qImgUrl: String,
    onQImgUrlChange: (String) -> Unit,
    opA: String,
    onOpAChange: (String) -> Unit,
    opB: String,
    onOpBChange: (String) -> Unit,
    opC: String,
    onOpCChange: (String) -> Unit,
    opD: String,
    onOpDChange: (String) -> Unit,
    correctOp: String,
    onCorrectOpChange: (String) -> Unit,
    qExplanation: String,
    onQExplanationChange: (String) -> Unit,
    tempQuestions: SnapshotStateList<QuizQuestion>,
    wordFilePickerLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
    onAddOrUpdateQuestion: () -> Unit,
    onEditQuestionClick: (Int, QuizQuestion) -> Unit,
    onDeleteQuestionClick: (Int) -> Unit,
    context: Context
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(value = quizTitle, onValueChange = onQuizTitleChange, label = { Text("Tên tiêu đề bài kiểm tra") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = quizDuration, onValueChange = onQuizDurationChange, label = { Text("Thời gian làm bài (Phút)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (editingQuestionIndex == -1) "Soạn câu hỏi:" else "Đang chỉnh sửa câu hỏi:",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            if (editingQuestionIndex == -1) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { wordFilePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nhập file Word", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onSelectVocabClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bốc từ kho từ vựng", fontSize = 12.sp)
                    }
                }
            }
        }

        OutlinedTextField(value = qText, onValueChange = onQTextChange, label = { Text("Nội dung câu hỏi") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = qImgUrl, onValueChange = onQImgUrlChange, label = { Text("Link hình ảnh đính kèm (Nếu có)") }, modifier = Modifier.fillMaxWidth())

        OutlinedTextField(value = opA, onValueChange = onOpAChange, label = { Text("Đáp án A") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = opB, onValueChange = onOpBChange, label = { Text("Đáp án B") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = opC, onValueChange = onOpCChange, label = { Text("Đáp án C") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = opD, onValueChange = onOpDChange, label = { Text("Đáp án D") }, modifier = Modifier.fillMaxWidth())

        Text("Chọn đáp án đúng:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("A", "B", "C", "D").forEach { options ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = (correctOp == options), onClick = { onCorrectOpChange(options) })
                    Text(options)
                }
            }
        }
        OutlinedTextField(value = qExplanation, onValueChange = onQExplanationChange, label = { Text("Giải thích chi tiết") }, modifier = Modifier.fillMaxWidth())

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (editingQuestionIndex != -1) {
                TextButton(onClick = onCancelEdit) { Text("Hủy sửa") }
            }
            Button(onClick = onAddOrUpdateQuestion) {
                Text(if (editingQuestionIndex == -1) "Thêm câu hỏi vào đề" else "Cập nhật câu hỏi")
            }
        }

        Text("Danh sách câu hỏi trong đề (${tempQuestions.size}):", fontWeight = FontWeight.Bold, color = Color.DarkGray)
        tempQuestions.forEachIndexed { idx, item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                colors = CardDefaults.cardColors(containerColor = if (editingQuestionIndex == idx) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp, 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${idx + 1}. ${item.questionText}", fontWeight = FontWeight.SemiBold)
                        Text("Đúng: ${item.correctOption} | A: ${item.optionA} | B: ${item.optionB}", fontSize = 12.sp, color = Color.Gray)
                    }
                    Row {
                        IconButton(onClick = { onEditQuestionClick(idx, item) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, "Sửa", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDeleteQuestionClick(idx) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}