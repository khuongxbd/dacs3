package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

// Định nghĩa các Route tĩnh cho luồng chính của hệ thống
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object UserMain : Screen("user_main")
    object AdminMain : Screen("admin_main")
}

@Composable
fun SetupNavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {

        // 1. Màn hình Đăng nhập / Đăng ký gốc
        composable(Screen.Login.route) {
            com.example.myapplication.ui.auth.LoginScreen(navController = navController)
        }

        composable(Screen.Register.route) {
            com.example.myapplication.ui.auth.RegisterScreen(navController = navController)
        }

        // 2. Điểm đầu của luồng Giao diện Học sinh (Chứa Bottom Bar)
        composable(Screen.UserMain.route) {
            com.example.myapplication.ui.user.UserMainContainer(navController = navController)
        }

        // 3. Điểm đầu của luồng Giao diện Giáo viên/Admin (Chứa Bottom Bar)
        composable(Screen.AdminMain.route) {
            com.example.myapplication.ui.admin.AdminMainContainer(navController = navController)
        }
    }
}