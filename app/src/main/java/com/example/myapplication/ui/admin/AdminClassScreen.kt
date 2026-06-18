package com.example.myapplication.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ClassModel
import com.example.myapplication.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminClassScreen(onLogoutSuccess: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val adminId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val context = LocalContext.current

    var currentView by remember { mutableStateOf("list") }
    var selectedClass by remember { mutableStateOf<ClassModel?>(null) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var className by remember { mutableStateOf("") }

    var classCode by remember { mutableStateOf<String?>(null) }
    var isPrivateClass by remember { mutableStateOf(false) }

    var isUnlimitedMembers by remember { mutableStateOf(true) }
    var maxMembersText by remember { mutableStateOf("50") }
    var selectedAvatar by remember { mutableStateOf("📚") }
    val avatars = listOf("📚", "💻", "🇬🇧", "🎨", "🔬", "🧮")

    var classList by remember { mutableStateOf(listOf<ClassModel>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(adminId) {
        if (adminId.isBlank()) return@LaunchedEffect
        db.collection("classes")
            .whereEqualTo("adminId", adminId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) classList = snapshot.toObjects(ClassModel::class.java)
                isLoading = false
            }
    }

    if (currentView == "detail" && selectedClass != null) {
        AdminClassDetailScreen(
            classObj = selectedClass!!,
            onBack = { currentView = "list"; selectedClass = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Lớp Học Quản Lý") },
                    actions = {
                        IconButton(onClick = {
                            FirebaseRepository().logout()
                            onLogoutSuccess()
                        }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Thoát", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Tạo Lớp") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Tạo") },
                    onClick = { showCreateDialog = true }
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (classList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("Danh sách trống. Hãy nhấn nút Tạo Lớp dưới góc màn hình!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(classList) { itemClass ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedClass = itemClass
                                    currentView = "detail"
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier.size(45.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(itemClass.avatarUrl.ifBlank { "📚" }, fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(itemClass.className, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Sĩ số: ${itemClass.memberIds.size}/${if (itemClass.maxMembers == -1) "Không giới hạn" else itemClass.maxMembers}",
                                            style = MaterialTheme.typography.bodySmall, color = Color.Gray
                                        )
                                    }
                                }

                                Row {
                                    if (itemClass.private && !itemClass.code.isNullOrBlank()) {
                                        IconButton(onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Code", itemClass.code)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Đã sao chép mã: ${itemClass.code}", Toast.LENGTH_SHORT).show()
                                        }) { Icon(Icons.Default.Share, "Copy", tint = MaterialTheme.colorScheme.primary) }
                                    }
                                    IconButton(onClick = {
                                        db.collection("classes").document(itemClass.classId).delete()
                                    }) { Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }

            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text("Cấu Hình Tạo Lớp Học") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Chọn ảnh đại diện lớp:", fontSize = 14.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(avatars) { avatar ->
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp).clip(CircleShape)
                                            .background(if (selectedAvatar == avatar) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedAvatar = avatar },
                                        contentAlignment = Alignment.Center
                                    ) { Text(avatar, fontSize = 18.sp) }
                                }
                            }

                            OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Tên lớp học") }, modifier = Modifier.fillMaxWidth())

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Lớp riêng tư (Hạn chế công khai)")
                                Switch(checked = isPrivateClass, onCheckedChange = {
                                    isPrivateClass = it
                                    // NẾU BẬT PRIVATE THÌ SINH MÃ, NẾU TẮT THÌ CHO THÀNH NULL LUÔN THEO Ý KHƯƠNG
                                    classCode = if (it) UUID.randomUUID().toString().take(6).uppercase() else null
                                })
                            }

                            if (isPrivateClass) {
                                OutlinedTextField(
                                    value = classCode ?: "",
                                    onValueChange = { classCode = it },
                                    label = { Text("Mã tham gia lớp") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Thành viên không giới hạn")
                                Switch(checked = isUnlimitedMembers, onCheckedChange = { isUnlimitedMembers = it })
                            }
                            if (!isUnlimitedMembers) {
                                OutlinedTextField(value = maxMembersText, onValueChange = { maxMembersText = it }, label = { Text("Số lượng tối đa") }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (className.isBlank()) return@Button
                            val classId = UUID.randomUUID().toString()
                            val limit = if (isUnlimitedMembers) -1 else (maxMembersText.toIntOrNull() ?: 50)

                            val newClass = ClassModel(
                                classId = classId,
                                className = className,
                                code = classCode, // Sẽ truyền chuỗi code hoặc null thẳng lên Firestore
                                adminId = adminId,
                                private = isPrivateClass, // Map chuẩn đét với biến private mới
                                avatarUrl = selectedAvatar,
                                maxMembers = limit
                            )
                            db.collection("classes").document(classId).set(newClass)
                            showCreateDialog = false
                            className = ""; isPrivateClass = false; classCode = null; isUnlimitedMembers = true
                        }) { Text("Xác Nhận Tạo") }
                    },
                    dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Hủy") } }
                )
            }
        }
    }
}