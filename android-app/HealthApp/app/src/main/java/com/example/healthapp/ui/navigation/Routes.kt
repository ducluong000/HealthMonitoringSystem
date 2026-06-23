package com.example.healthapp.ui.navigation

// Sealed class đại diện cho tất cả màn hình trong app
sealed class Screen(val route: String) {

    // Splash
    object Splash : Screen("splash")

    // Auth
    object AuthGate : Screen("auth_gate")
    object Login : Screen("login")
    object Register : Screen("register")


    // Home
    object Home : Screen("home")

    // Statistics
    object Statistics : Screen("statistics")

    // Notification
    object Notification : Screen("notification")

    // Heart Rate
    object HeartRateDetail : Screen("heart_rate_detail")
    object HeartRateHistory : Screen("heart_rate_history")

    // SpO2
    object SpO2Detail : Screen("spo2_detail")
    object SpO2History : Screen("spo2_history")

    // Steps
    object StepsDetail : Screen("steps_detail")
    object StepsHistory : Screen("steps_history?dateMillis={dateMillis}") {
        fun createRoute(dateMillis: Long): String {
            return "steps_history?dateMillis=$dateMillis"
        }
    }

    // Sleep
    object SleepDetail : Screen("sleep_detail")

    // Profile
    object Profile : Screen("profile")


    // 🔥 (OPTIONAL - nâng cao) truyền dữ liệu
    object HeartRateDetailWithId : Screen("heart_rate_detail/{id}") {
        fun createRoute(id: Int): String {
            return "heart_rate_detail/$id"
        }
    }
}