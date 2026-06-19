package com.example.myapplication.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
                    title = { Text("QUẢN LÝ LỚP HỌC", fontWeight = FontWeight.Black) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BlackColor, titleContentColor = WhiteColor),
                    actions = {
                        IconButton(onClick = { FirebaseRepository().logout(); onLogoutSuccess() }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Thoát", tint = WhiteColor)
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("TẠO LỚP", fontWeight = FontWeight.ExtraBold) },
                    icon = { Icon(Icons.Default.Add, null) },
                    onClick = { showCreateDialog = true },
                    containerColor = LimeGreen,
                    contentColor = BlackColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(2.dp, BlackColor)
                )
            }
        ) { paddingValues ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(classList) { itemClass ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                            .clickable { selectedClass = itemClass; currentView = "detail" },
                        colors = CardDefaults.cardColors(containerColor = WhiteColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).background(LimeGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).border(2.dp, BlackColor, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Text(itemClass.avatarUrl, fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(itemClass.className, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text("Sĩ số: ${itemClass.memberIds.size}", fontWeight = FontWeight.Bold, color = Color.Gray)
                            }

                            if (itemClass.private && !itemClass.code.isNullOrBlank()) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Code", itemClass.code)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Đã sao chép mã: ${itemClass.code}", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Copy", tint = LimeGreen)
                                }
                            }

                            // Icon hành động
                            IconButton(onClick = { db.collection("classes").document(itemClass.classId).delete() }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                containerColor = WhiteColor,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(2.dp, BlackColor),
                title = { Text("TẠO LỚP HỌC MỚI", fontWeight = FontWeight.Black) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                            OutlinedTextField(value = className,
                                onValueChange = { className = it },
                                label = { Text("Tên lớp học") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BlackColor,
                                    unfocusedBorderColor = BlackColor),
                                    modifier = Modifier.fillMaxWidth())

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Lớp riêng tư (Hạn chế công khai)")
                                Switch(checked = isPrivateClass, onCheckedChange = {
                                    isPrivateClass = it
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
                            val initialMembers = listOf(adminId)

                            val newClass = ClassModel(
                                classId = classId,
                                className = className,
                                code = classCode,
                                adminId = adminId,
                                private = isPrivateClass,
                                avatarUrl = selectedAvatar,
                                maxMembers = limit,
                                memberIds = initialMembers
                            )
                            db.collection("classes").document(classId).set(newClass)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Đã tạo lớp thành công!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Lỗi khi tạo lớp", Toast.LENGTH_SHORT).show()
                                }

                            // Reset trạng thái
                            showCreateDialog = false
                            className = ""
                            isPrivateClass = false
                            classCode = null
                            isUnlimitedMembers = true
                            selectedAvatar = "📚"
                        },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LimeGreen, contentColor = BlackColor),
                            border = BorderStroke(2.dp, BlackColor)
                        )
                        { Text("XÁC NHẬN", fontWeight = FontWeight.ExtraBold) }
                    },
                dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Hủy") } }
            )
        }
    }
}