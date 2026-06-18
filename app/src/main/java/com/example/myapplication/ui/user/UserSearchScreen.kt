package com.example.myapplication.ui.user

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen() {
    val db = FirebaseFirestore.getInstance()
    val studentId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val context = LocalContext.current

    var allClasses by remember { mutableStateOf(listOf<ClassModel>()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var memberFilterType by remember { mutableStateOf(0) } // 0: Mặc định, 1: Nhiều thành viên, 2: Ít thành viên
    var showFilterMenu by remember { mutableStateOf(false) }

    var showJoinDialog by remember { mutableStateOf(false) }
    var targetClassToJoin by remember { mutableStateOf<ClassModel?>(null) }
    var inputClassCode by remember { mutableStateOf("") }

    // Lấy danh sách lớp học Realtime từ Firestore
    LaunchedEffect(Unit) {
        db.collection("classes")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allClasses = snapshot.toObjects(ClassModel::class.java)
                }
                isLoading = false
            }
    }

    // Lọc theo tên và sắp xếp theo số thành viên
    val filteredAndSortedClasses = remember(allClasses, searchQuery, memberFilterType) {
        var result = allClasses.filter {
            it.className.contains(searchQuery, ignoreCase = true)
        }

        result = when (memberFilterType) {
            1 -> result.sortedByDescending { it.memberIds.size }
            2 -> result.sortedBy { it.memberIds.size }
            else -> result
        }
        result
    }

    // Hàm xử lý đẩy trực tiếp Member ID lên Firestore (Gia nhập lớp)
    fun joinClassDirectly(targetClass: ClassModel) {
        val currentMembers = targetClass.memberIds.toMutableList()
        if (!currentMembers.contains(studentId)) {
            currentMembers.add(studentId)

            db.collection("classes").document(targetClass.classId)
                .update("memberIds", currentMembers)
                .addOnSuccessListener {
                    Toast.makeText(context, "Gia nhập lớp ${targetClass.className} thành công! 🎯", Toast.LENGTH_SHORT).show()
                    showJoinDialog = false
                    targetClassToJoin = null
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Lỗi kết nối database!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // HÀM MỚI: Xử lý xóa Member ID khỏi Firestore (Hủy tham gia / Rời lớp)
    fun leaveClass(targetClass: ClassModel) {
        val currentMembers = targetClass.memberIds.toMutableList()
        if (currentMembers.contains(studentId)) {
            currentMembers.remove(studentId) // Xóa ID của user hiện tại ra khỏi mảng

            db.collection("classes").document(targetClass.classId)
                .update("memberIds", currentMembers)
                .addOnSuccessListener {
                    Toast.makeText(context, "Đã hủy tham gia lớp ${targetClass.className} ❌", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Lỗi kết nối database khi rời lớp!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tìm Kiếm Lớp Học", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // --- CÔNG CỤ TÌM KIẾM ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Nhập tên lớp học cần tìm...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    modifier = Modifier.weight(1f)
                )

                // NÚT BỘ LỌC THÀNH VIÊN
                Box {
                    IconButton(
                        onClick = { showFilterMenu = !showFilterMenu },
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .size(52.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Hiển thị mặc định") },
                            onClick = { memberFilterType = 0; showFilterMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Nhiều thành viên nhất 👥🔥") },
                            onClick = { memberFilterType = 1; showFilterMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Ít thành viên nhất 👥❄️") },
                            onClick = { memberFilterType = 2; showFilterMenu = false }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = memberFilterType != 0) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = if (memberFilterType == 1) "Đang lọc: Nhiều thành viên nhất" else "Đang lọc: Ít thành viên nhất",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // --- DANH SÁCH LỚP HỌC KẾT QUẢ ---
            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredAndSortedClasses.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy kết quả phù hợp!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredAndSortedClasses) { classItem ->
                        val totalStudents = classItem.memberIds.size
                        val isJoined = classItem.memberIds.contains(studentId)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = classItem.className,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = if (classItem.private) "🔒 Riêng tư" else "🌐 Công khai",
                                            fontSize = 11.sp,
                                            color = if (classItem.private) Color.Red else Color(0xFF007A33),
                                            modifier = Modifier
                                                .background(
                                                    color = (if (classItem.private) Color.Red else Color(0xFF007A33)).copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Group,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$totalStudents học viên",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                // --- THAY ĐỔI LOGIC NÚT BẤM DỰA VÀO TRẠNG THÁI THAM GIA ---
                                if (isJoined) {
                                    // NẾU ĐÃ THAM GIA -> Nhấn vào để Hủy tham gia (Rời lớp)
                                    Button(
                                        onClick = { leaveClass(classItem) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Hủy tham gia", fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    // NẾU CHƯA THAM GIA -> Nhấn vào để xin vào lớp (Check công khai/riêng tư)
                                    Button(
                                        onClick = {
                                            if (classItem.private) {
                                                targetClassToJoin = classItem
                                                inputClassCode = ""
                                                showJoinDialog = true
                                            } else {
                                                joinClassDirectly(classItem)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Vào Lớp")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOG CHECK MÃ CODE (CHỈ BẬT KHI LỚP LÀ PRIVATE) ---
        if (showJoinDialog && targetClassToJoin != null) {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false; targetClassToJoin = null },
                title = { Text("Yêu Cầu Nhập Mã Lớp") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Lớp '${targetClassToJoin!!.className}' đang ở chế độ riêng tư. Vui lòng nhập đúng mã lớp để gia nhập:",
                            fontSize = 14.sp
                        )
                        OutlinedTextField(
                            value = inputClassCode,
                            onValueChange = { inputClassCode = it },
                            label = { Text("Mã bảo mật lớp") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val targetClass = targetClassToJoin!!
                            if (inputClassCode.trim() == (targetClass.code ?: "")) {
                                joinClassDirectly(targetClass)
                            } else {
                                Toast.makeText(context, "Mã lớp không hợp lệ! Vui lòng kiểm tra lại.", Toast.LENGTH_LONG).show()
                            }
                        }
                    ) {
                        Text("Xác Nhận")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinDialog = false; targetClassToJoin = null }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}