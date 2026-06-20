package com.example.myapplication.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.myapplication.ui.navigation.Screen
import kotlin.text.set

@Composable
fun RegisterScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val BackgroundBlack = Color(0xFF000000)
    val LimeGreen = Color(0xFFCCFF00)
    val SurfaceGray = Color(0xFF1C1C1E)

    Box(modifier = Modifier.fillMaxSize().background(BackgroundBlack)) {
        // Background Decorations
        Box(modifier = Modifier.offset(x = (-50).dp, y = (-50).dp).size(200.dp)
            .background(LimeGreen.copy(0.2f), CircleShape).blur(100.dp))
        Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 50.dp, y = 50.dp).size(250.dp)
            .background(LimeGreen.copy(0.15f), CircleShape).blur(120.dp))

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Create Account.", style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Sign up to get started.", color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 32.dp))

            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, LimeGreen.copy(0.3f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceGray.copy(0.5f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    GlassyTextField(value = name, onValueChange = { name = it }, label = "Họ và tên", accent = LimeGreen)
                    Spacer(modifier = Modifier.height(16.dp))
                    GlassyTextField(value = email, onValueChange = { email = it }, label = "Email", accent = LimeGreen)
                    Spacer(modifier = Modifier.height(16.dp))
                    GlassyTextField(value = password, onValueChange = { password = it }, label = "Mật khẩu", isPass = true, accent = LimeGreen)
                    Spacer(modifier = Modifier.height(16.dp))
                    GlassyTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Xác nhận mật khẩu", isPass = true, accent = LimeGreen)

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (password != confirmPassword) {
                                Toast.makeText(context, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener { authResult ->
                                    val uid = authResult.user?.uid ?: ""
                                    val userData = hashMapOf(
                                        "uid" to uid,
                                        "email" to email,
                                        "role" to "user",
                                        "name" to name
                                    )
                                    db.collection("users").document(uid).set(userData)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            Toast.makeText(context, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                            navController.navigate(Screen.UserMain.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            Toast.makeText(context, "Lưu thông tin thất bại: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    Toast.makeText(context, "Đăng ký thất bại: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LimeGreen)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("SIGN UP", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text("Đã có tài khoản? ", color = Color.White.copy(0.6f))
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("Đăng nhập", color = LimeGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


