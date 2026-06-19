package com.example.myapplication.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserNotificationScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var notifications by remember { mutableStateOf(listOf<NotificationModel>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId.isEmpty()) return@LaunchedEffect
        db.collection("classes").whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val classIds = snapshot.documents.map { it.id }
                if (classIds.isNotEmpty()) {
                    db.collection("notifications")
                        .addSnapshotListener { notiSnap, _ ->
                            if (notiSnap != null) {
                                val allNotis = notiSnap.toObjects(NotificationModel::class.java)
                                // Filter notifications for user's classes and sort by newest first
                                notifications = allNotis.filter { it.targetClassId in classIds }
                                    .sortedByDescending { it.createdAt }
                            }
                            isLoading = false
                        }
                } else {
                    isLoading = false
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông Báo", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Không có thông báo nào.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { noti ->
                    val isUnread = !noti.readBy.contains(userId)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isUnread) {
                                    // Đánh dấu là đã đọc
                                    db.collection("notifications").document(noti.notiId)
                                        .update("readBy", FieldValue.arrayUnion(userId))
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnread) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF5F5F5)
                        ),
                        elevation = CardDefaults.cardElevation(if (isUnread) 4.dp else 0.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (isUnread) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = noti.title,
                                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = noti.content,
                                    fontSize = 14.sp,
                                    color = if (isUnread) Color.Black else Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = formatTime(noti.createdAt),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timeInMillis: Long): String {
    val formatter = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(timeInMillis))
}
