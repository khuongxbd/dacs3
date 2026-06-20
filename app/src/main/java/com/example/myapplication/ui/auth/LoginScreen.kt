package com.example.myapplication.ui.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.myapplication.ui.navigation.Screen
import kotlin.text.get

@Composable
fun LoginScreen(navController: NavHostController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }


    val BackgroundBlack = Color(0xFF000000)
    val LimeGreen = Color(0xFFCCFF00) // Màu xanh lá mạ rực rỡ
    val SurfaceGray = Color(0xFF1C1C1E)


    Box(modifier = Modifier.fillMaxSize().background(BackgroundBlack)) {
        Box(
            modifier = Modifier.offset(x = (-50).dp, y = (-50).dp).size(200.dp)
                .background(LimeGreen.copy(0.2f), CircleShape).blur(100.dp)
        )
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 50.dp, y = 50.dp).size(250.dp)
                .background(LimeGreen.copy(0.15f), CircleShape).blur(120.dp)
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Sign In.", style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold, color = Color.White
            )

            Card(
                modifier = Modifier.fillMaxWidth().border(
                    1.dp,
                    LimeGreen.copy(0.3f),
                    RoundedCornerShape(24.dp)
                ),
                colors = CardDefaults.cardColors(containerColor = SurfaceGray.copy(0.5f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    GlassyTextField(
                        value = email, onValueChange = { email = it },
                        label = "Email", accent = LimeGreen
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GlassyTextField(
                        value = password, onValueChange = { password = it },
                        label = "Password", isPass = true, accent = LimeGreen
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Vui lòng nhập đầy đủ thông tin",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            isLoading = true
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener { authResult ->
                                    val uid = authResult.user?.uid ?: ""
                                    // Kiểm tra Role từ Firestore
                                    db.collection("users").document(uid).get()
                                        .addOnSuccessListener { doc ->
                                            isLoading = false
                                            val role = doc.getString("role") ?: "user"
                                            if (role == "admin") {
                                                navController.navigate(Screen.AdminMain.route) {
                                                    popUpTo(
                                                        Screen.Login.route
                                                    ) { inclusive = true }
                                                }
                                            } else {
                                                navController.navigate(Screen.UserMain.route) {
                                                    popUpTo(
                                                        Screen.Login.route
                                                    ) { inclusive = true }
                                                }
                                            }
                                        }
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Lỗi: ${it.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeGreen,
                            disabledContainerColor = LimeGreen.copy(alpha = 0.3f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "LOGIN",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Chưa có tài khoản? ", color = Color.White.copy(0.6f))
                        TextButton(
                            onClick = { navController.navigate(Screen.Register.route) },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                "Đăng ký ngay",
                                color = LimeGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPass: Boolean = false,
    accent: Color
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(0.6f)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (isPass && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPass) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = Color.White.copy(0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = accent
        ),
        singleLine = true
    )
}






