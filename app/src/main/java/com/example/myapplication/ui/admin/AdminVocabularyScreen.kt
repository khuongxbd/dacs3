package com.example.myapplication.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
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
import com.example.myapplication.data.model.ClassModel
import com.example.myapplication.data.model.FlashcardSet
import com.example.myapplication.data.model.WordItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVocabularyScreen() {
    val db = FirebaseFirestore.getInstance()
    val adminId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val context = LocalContext.current

    var vocabLists by remember { mutableStateOf(listOf<FlashcardSet>()) }
    var myClasses by remember { mutableStateOf(listOf<ClassModel>()) }
    var isLoading by remember { mutableStateOf(true) }

    // State điều khiển Dialogs
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }

    // Đối tượng được chọn để Sửa hoặc Giao bài
    var targetSetForAssign by remember { mutableStateOf<FlashcardSet?>(null) }
    var targetSetForEdit by remember { mutableStateOf<FlashcardSet?>(null) }

    // State dùng chung cho Form nhập liệu (Tạo mới & Chỉnh sửa)
    var setTitle by remember { mutableStateOf("") }
    val tempWords = remember { mutableStateListOf<WordItem>() }
    var inputWord by remember { mutableStateOf("") }
    var inputMeaning by remember { mutableStateOf("") }
    var inputImgUrl by remember { mutableStateOf("") }
    var editingWordIndex by remember { mutableStateOf(-1) }

    // Tải dữ liệu Realtime từ Firestore
    LaunchedEffect(adminId) {
        if (adminId.isBlank()) return@LaunchedEffect

        db.collection("vocabularies")
            .whereEqualTo("adminId", adminId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) vocabLists = snapshot.toObjects(FlashcardSet::class.java)
                isLoading = false
            }

        db.collection("classes")
            .whereEqualTo("adminId", adminId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) myClasses = snapshot.toObjects(ClassModel::class.java)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KHO TỪ VỰNG", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlackColor, titleContentColor = WhiteColor)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("TẠO HỌC PHẦN", fontWeight = FontWeight.ExtraBold) },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = {
                    setTitle = ""; tempWords.clear(); editingWordIndex = -1
                    showCreateDialog = true
                },
                containerColor = LimeGreen,
                contentColor = BlackColor,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(2.dp, BlackColor)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (vocabLists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Kho trống. Nhấn nút dưới góc để thêm bộ từ vựng!", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(vocabLists) { vocabSet ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                            .clickable {
                                targetSetForEdit = vocabSet
                                setTitle = vocabSet.title
                                tempWords.clear()
                                tempWords.addAll(vocabSet.words)
                                editingWordIndex = -1
                                inputWord = ""; inputMeaning = ""; inputImgUrl = ""
                                showEditDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = WhiteColor),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(vocabSet.title, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text("Số từ: ${vocabSet.words.size}", fontWeight = FontWeight.Bold, color = Color.Gray)
                                if (vocabSet.assignedClassIds.isNotEmpty()) {
                                    Text("📢 Đang giao cho ${vocabSet.assignedClassIds.size} lớp học", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                }
                            }

                            Row {
                                IconButton(onClick = {
                                    targetSetForAssign = vocabSet
                                    showAssignDialog = true
                                }) { Icon(Icons.Default.Send, null, tint = LimeGreen) }

                                IconButton(onClick = {
                                    db.collection("vocabularies").document(vocabSet.setId).delete()
                                        .addOnSuccessListener { Toast.makeText(context, "Đã xóa học phần", Toast.LENGTH_SHORT).show() }
                                }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                            }
                        }
                    }
                }
            }
        }

        // --- HÀM COMPOSABLE REUSE CHO FORM CHỨA DANH SÁCH TỪ TẠM THỜI (DÙNG CHUNG CHO CẢ CREATE & EDIT) ---
        @Composable
        fun VocabularyFormContent() {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tên học phần
                OutlinedTextField(
                    value = setTitle,
                    onValueChange = { setTitle = it },
                    label = { Text("Tên chủ đề học phần") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlackColor,
                        unfocusedBorderColor = BlackColor,
                        focusedLabelColor = BlackColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Phần nhập từ vựng
                Text(
                    text = if (editingWordIndex == -1) "THÊM TỪ VỰNG MỚI:" else "ĐANG SỬA TỪ:",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = BlackColor
                )

                OutlinedTextField(value = inputWord, onValueChange = { inputWord = it }, label = { Text("Từ tiếng Anh") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BlackColor, unfocusedBorderColor = BlackColor), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = inputMeaning, onValueChange = { inputMeaning = it }, label = { Text("Nghĩa tiếng Việt") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BlackColor, unfocusedBorderColor = BlackColor), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = inputImgUrl, onValueChange = { inputImgUrl = it }, label = { Text("Link ảnh minh họa") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BlackColor, unfocusedBorderColor = BlackColor), shape = RoundedCornerShape(8.dp))

                // Nút Thêm/Cập nhật
                Button(
                    onClick = {
                        if (inputWord.isBlank() || inputMeaning.isBlank()) return@Button
                        if (editingWordIndex == -1) {
                            tempWords.add(WordItem(word = inputWord, meaning = inputMeaning, imageUrl = inputImgUrl))
                        } else {
                            tempWords[editingWordIndex] = WordItem(word = inputWord, meaning = inputMeaning, imageUrl = inputImgUrl)
                            editingWordIndex = -1
                        }
                        inputWord = ""; inputMeaning = ""; inputImgUrl = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).border(2.dp, BlackColor),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LimeGreen, contentColor = BlackColor)
                ) {
                    Text(if (editingWordIndex == -1) "THÊM VÀO BỘ" else "CẬP NHẬT TỪ", fontWeight = FontWeight.ExtraBold)
                }

                // Danh sách từ
                Text("DANH SÁCH TỪ HIỆN TẠI (${tempWords.size}):", fontWeight = FontWeight.Black, fontSize = 14.sp, color = BlackColor)

                tempWords.forEachIndexed { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().border(2.dp, BlackColor, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (editingWordIndex == index) LimeGreen.copy(alpha = 0.2f) else WhiteColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${index + 1}. ${item.word}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Text("Nghĩa: ${item.meaning}", fontSize = 13.sp, color = Color.DarkGray)
                            }
                            Row {
                                IconButton(onClick = {
                                    editingWordIndex = index
                                    inputWord = item.word
                                    inputMeaning = item.meaning
                                    inputImgUrl = item.imageUrl
                                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Sửa", tint = BlackColor) }

                                IconButton(onClick = {
                                    tempWords.removeAt(index)
                                    if (editingWordIndex == index) editingWordIndex = -1
                                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Xóa", tint = Color.Red) }
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOG 1: TẠO MỚI HỌC PHẦN ---
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                containerColor = WhiteColor, // Đảm bảo nền trắng
                shape = RoundedCornerShape(8.dp), // Bo góc vuông vức
                modifier = Modifier.border(2.dp, BlackColor), // Viền đen 2dp đặc trưng
                title = {
                    Text(
                        "TẠO HỌC PHẦN MỚI",
                        fontWeight = FontWeight.Black,
                        color = BlackColor
                    )
                },
                text = {
                    VocabularyFormContent()
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (setTitle.isBlank() || tempWords.isEmpty()) return@Button
                            val setId = UUID.randomUUID().toString()
                            val newSet = FlashcardSet(
                                setId = setId,
                                title = setTitle,
                                words = tempWords.toList(),
                                adminId = adminId
                            )

                            db.collection("vocabularies").document(setId).set(newSet)
                                .addOnSuccessListener {
                                    showCreateDialog = false
                                    Toast.makeText(context, "Tạo thành công!", Toast.LENGTH_SHORT).show()
                                }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlackColor,
                            contentColor = WhiteColor
                        ),
                        border = BorderStroke(2.dp, BlackColor)
                    ) {
                        Text("LƯU HỌC PHẦN", fontWeight = FontWeight.ExtraBold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCreateDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = BlackColor)
                    ) {
                        Text("HỦY", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // --- DIALOG 2: CHỈNH SỬA HỌC PHẦN ĐÃ CÓ TRÊN FIRESTORE (KHI NHẤN VÀO ITEM DANH SÁCH) ---
        if (showEditDialog && targetSetForEdit != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false; targetSetForEdit = null },
                containerColor = WhiteColor,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(2.dp, BlackColor),
                title = {
                    Text(
                        "CHỈNH SỬA HỌC PHẦN",
                        fontWeight = FontWeight.Black,
                        color = BlackColor
                    )
                },
                text = {
                    VocabularyFormContent()
                },
                confirmButton = {
                    Button(
                        enabled = editingWordIndex == -1, // Chỉ cho phép lưu khi không ở trạng thái đang sửa từ con
                        onClick = {
                            if (setTitle.isBlank() || tempWords.isEmpty()) return@Button

                            val updatedSet = targetSetForEdit!!.copy(
                                title = setTitle,
                                words = tempWords.toList()
                            )

                            db.collection("vocabularies").document(targetSetForEdit!!.setId)
                                .set(updatedSet)
                                .addOnSuccessListener {
                                    showEditDialog = false
                                    targetSetForEdit = null
                                    Toast.makeText(context, "Đã cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlackColor,
                            contentColor = WhiteColor,
                            disabledContainerColor = Color.Gray, // Màu xám khi disabled
                            disabledContentColor = Color.LightGray
                        ),
                        border = BorderStroke(2.dp, BlackColor)
                    ) {
                        Text("CẬP NHẬT HỌC PHẦN", fontWeight = FontWeight.ExtraBold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEditDialog = false; targetSetForEdit = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = BlackColor)
                    ) {
                        Text("HỦY", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // --- DIALOG 3: GỬI HỌC PHẦN VÀO LỚP ---
        if (showAssignDialog && targetSetForAssign != null) {
            AlertDialog(
                onDismissRequest = { showAssignDialog = false; targetSetForAssign = null },
                containerColor = WhiteColor,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(2.dp, BlackColor),
                title = {
                    Text("GỬI BỘ TỪ VỰNG", fontWeight = FontWeight.Black, color = BlackColor)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chọn các lớp học để áp dụng học phần này:", fontSize = 14.sp)

                        // LazyColumn hiển thị danh sách lớp với Checkbox tùy chỉnh
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 250.dp)
                                .border(1.dp, BlackColor, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            items(myClasses) { classItem ->
                                val isAssigned = targetSetForAssign!!.assignedClassIds.contains(classItem.classId)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val currentList = targetSetForAssign!!.assignedClassIds.toMutableList()
                                            if (isAssigned) currentList.remove(classItem.classId) else currentList.add(classItem.classId)
                                            db.collection("vocabularies").document(targetSetForAssign!!.setId)
                                                .update("assignedClassIds", currentList)
                                                .addOnSuccessListener {
                                                    targetSetForAssign = targetSetForAssign!!.copy(assignedClassIds = currentList)
                                                }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isAssigned,
                                        onCheckedChange = null, // Đã xử lý tại Row click
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = LimeGreen,
                                            uncheckedColor = BlackColor,
                                            checkmarkColor = BlackColor
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(classItem.className, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAssignDialog = false; targetSetForAssign = null },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LimeGreen, contentColor = BlackColor),
                        border = BorderStroke(2.dp, BlackColor)
                    ) {
                        Text("XONG", fontWeight = FontWeight.ExtraBold)
                    }
                }
            )
        }
    }
}