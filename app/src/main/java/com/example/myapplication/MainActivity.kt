package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.navigation.SetupNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

                    // Nếu đã login trước đó, tạm thời chuyển thẳng ra luồng UserMain để test cho nhanh
                    val startDestination = if (currentUser != null) Screen.UserMain.route else Screen.Login.route

                    SetupNavGraph(navController = navController, startDestination = startDestination)
                }
            }
        }
    }
}