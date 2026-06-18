package com.example.myapplication.ui.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.ClassModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserClassScreen(onClassClick: (ClassModel) -> Unit) { // Nhận vào Lambda 1 tham số ClassModel chuẩn chỉnh
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var joinedClasses by remember { mutableStateOf(listOf<ClassModel>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Lắng nghe danh sách các lớp mà học sinh này đã tham gia
    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect
        db.collection("classes")
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    joinedClasses = snapshot.toObjects(ClassModel::class.java)
                }
                isLoading = false
            }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lớp Học Của Tôi") }) }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (joinedClasses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Bạn chưa tham gia lớp học nào. Hãy qua mục Tìm kiếm nhé!")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(joinedClasses) { cls ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                // ✅ ĐÃ SỬA: Truyền nguyên đối tượng cls (ClassModel) ra ngoài thay vì truyền (id, name)
                                onClassClick(cls)
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(cls.className, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Mã lớp: ${cls.code ?: "Không có mã"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}