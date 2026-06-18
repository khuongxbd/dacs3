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
        topBar = { TopAppBar(title = { Text("Kho Từ Vựng Gốc (Admin)") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Tạo Học Phần") },
                icon = { Icon(Icons.Default.Add, "Add") },
                onClick = {
                    setTitle = ""; tempWords.clear(); editingWordIndex = -1
                    showCreateDialog = true
                }
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(vocabLists) { vocabSet ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // KHI NHẤN VÀO ITEM: Đổ dữ liệu cũ vào các State để tiến hành SỬA
                                targetSetForEdit = vocabSet
                                setTitle = vocabSet.title
                                tempWords.clear()
                                tempWords.addAll(vocabSet.words)
                                editingWordIndex = -1
                                inputWord = ""; inputMeaning = ""; inputImgUrl = ""
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
                                Text(vocabSet.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Số lượng: ${vocabSet.words.size} từ vựng", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                if (vocabSet.assignedClassIds.isNotEmpty()) {
                                    Text("📢 Đang giao cho ${vocabSet.assignedClassIds.size} lớp học", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                }
                            }

                            Row {
                                IconButton(onClick = {
                                    targetSetForAssign = vocabSet
                                    showAssignDialog = true
                                }) {
                                    Icon(Icons.Default.Send, contentDescription = "Giao", tint = MaterialTheme.colorScheme.secondary)
                                }

                                IconButton(onClick = {
                                    db.collection("vocabularies").document(vocabSet.setId).delete()
                                        .addOnSuccessListener { Toast.makeText(context, "Đã xóa học phần", Toast.LENGTH_SHORT).show() }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- HÀM COMPOSABLE REUSE CHO FORM CHỨA DANH SÁCH TỪ TẠM THỜI (DÙNG CHUNG CHO CẢ CREATE & EDIT) ---
        @Composable
        fun VocabularyFormContent(isEditMode: Boolean) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = setTitle, onValueChange = { setTitle = it }, label = { Text("Tên chủ đề học phần") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Text(if (editingWordIndex == -1) "Thêm từ vựng mới:" else "Đang sửa từ vựng:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(value = inputWord, onValueChange = { inputWord = it }, label = { Text("Từ tiếng Anh") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = inputMeaning, onValueChange = { inputMeaning = it }, label = { Text("Nghĩa tiếng Việt") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = inputImgUrl, onValueChange = { inputImgUrl = it }, label = { Text("Link ảnh minh họa") }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (editingWordIndex != -1) {
                        TextButton(onClick = { editingWordIndex = -1; inputWord = ""; inputMeaning = ""; inputImgUrl = "" }) { Text("Hủy sửa từ") }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Button(onClick = {
                        if (inputWord.isBlank() || inputMeaning.isBlank()) return@Button
                        if (editingWordIndex == -1) {
                            tempWords.add(WordItem(word = inputWord, meaning = inputMeaning, imageUrl = inputImgUrl))
                        } else {
                            tempWords[editingWordIndex] = WordItem(word = inputWord, meaning = inputMeaning, imageUrl = inputImgUrl)
                            editingWordIndex = -1
                        }
                        inputWord = ""; inputMeaning = ""; inputImgUrl = ""
                    }) { Text(if (editingWordIndex == -1) "Thêm vào bộ" else "Cập nhật từ") }
                }

                Text("Danh sách từ hiện tại (${tempWords.size}):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
                tempWords.forEachIndexed { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = if(editingWordIndex == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp, 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${index + 1}. ${item.word}", fontWeight = FontWeight.Bold)
                                Text("Nghĩa: ${item.meaning}", fontSize = 13.sp, color = Color.Gray)
                            }
                            Row {
                                IconButton(onClick = {
                                    editingWordIndex = index
                                    inputWord = item.word
                                    inputMeaning = item.meaning
                                    inputImgUrl = item.imageUrl
                                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Sửa", tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = {
                                    tempWords.removeAt(index)
                                    if(editingWordIndex == index) editingWordIndex = -1
                                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error) }
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
                title = { Text("Tạo Học Phần Mới") },
                text = { VocabularyFormContent(isEditMode = false) },
                confirmButton = {
                    Button(onClick = {
                        if (setTitle.isBlank() || tempWords.isEmpty()) return@Button
                        val setId = UUID.randomUUID().toString()
                        val newSet = FlashcardSet(setId = setId, title = setTitle, words = tempWords.toList(), adminId = adminId)
                        db.collection("vocabularies").document(setId).set(newSet)
                            .addOnSuccessListener { showCreateDialog = false; Toast.makeText(context, "Tạo thành công!", Toast.LENGTH_SHORT).show() }
                    }) { Text("Lưu Lên Hệ Thống") }
                },
                dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Hủy") } }
            )
        }

        // --- DIALOG 2: CHỈNH SỬA HỌC PHẦN ĐÃ CÓ TRÊN FIRESTORE (KHI NHẤN VÀO ITEM DANH SÁCH) ---
        if (showEditDialog && targetSetForEdit != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false; targetSetForEdit = null },
                title = { Text("Chỉnh Sửa Học Phần") },
                text = { VocabularyFormContent(isEditMode = true) },
                confirmButton = {
                    Button(
                        enabled = editingWordIndex == -1, // Đang sửa từ con thì bắt cập nhật từ trước khi lưu tổng
                        onClick = {
                            if (setTitle.isBlank() || tempWords.isEmpty()) return@Button

                            // Tiến hành cập nhật đè (Update) dữ liệu mới lên Document cũ trên Firestore
                            val updatedSet = targetSetForEdit!!.copy(
                                title = setTitle,
                                words = tempWords.toList()
                            )

                            db.collection("vocabularies").document(targetSetForEdit!!.setId)
                                .set(updatedSet) // set() đè đối tượng đã copy dữ liệu mới
                                .addOnSuccessListener {
                                    showEditDialog = false
                                    targetSetForEdit = null
                                    Toast.makeText(context, "Đã cập nhật thay đổi thành công!", Toast.LENGTH_SHORT).show()
                                }
                        }
                    ) { Text("Cập Nhật Học Phần") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false; targetSetForEdit = null }) { Text("Hủy") }
                }
            )
        }

        // --- DIALOG 3: GỬI HỌC PHẦN VÀO LỚP ---
        if (showAssignDialog && targetSetForAssign != null) {
            AlertDialog(
                onDismissRequest = { showAssignDialog = false },
                title = { Text("Gửi Học Phần Vào Lớp") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chọn lớp học nhận bộ từ vựng:", fontSize = 14.sp)
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(myClasses) { classItem ->
                                val isAssigned = targetSetForAssign!!.assignedClassIds.contains(classItem.classId)
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val currentList = targetSetForAssign!!.assignedClassIds.toMutableList()
                                        if (isAssigned) currentList.remove(classItem.classId) else currentList.add(classItem.classId)
                                        db.collection("vocabularies").document(targetSetForAssign!!.setId).update("assignedClassIds", currentList)
                                            .addOnSuccessListener { targetSetForAssign = targetSetForAssign!!.copy(assignedClassIds = currentList) }
                                    }.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = isAssigned, onCheckedChange = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(classItem.className)
                                }
                            }
                        }
                    }
                },
                confirmButton = { Button(onClick = { showAssignDialog = false; targetSetForAssign = null }) { Text("Xong") } }
            )
        }
    }
}