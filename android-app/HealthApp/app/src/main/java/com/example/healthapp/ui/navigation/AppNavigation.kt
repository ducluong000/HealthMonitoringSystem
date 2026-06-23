package com.example.healthapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.healthapp.ui.screens.HomeScreen
import com.example.healthapp.ui.screens.HeartRateDetailScreen
import com.example.healthapp.ui.screens.HeartRateHistoryScreen
import com.example.healthapp.ui.screens.ProfileScreen
import com.example.healthapp.ui.screens.SplashScreen
import com.example.healthapp.ui.screens.SpO2DetailScreen
import com.example.healthapp.ui.screens.SpO2HistoryScreen
import com.example.healthapp.ui.screens.StepsDetailScreen
import com.example.healthapp.ui.screens.StepsHistoryScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthapp.viewmodel.HomeViewModel
import com.example.healthapp.viewmodel.HeartRateViewModel
import com.example.healthapp.ui.screens.SleepDetailScreen
import com.example.healthapp.ui.screens.StatisticsScreen
import com.example.healthapp.viewmodel.Spo2ViewModel
import com.example.healthapp.ui.screens.NotificationScreen
import com.example.healthapp.ui.screens.LoginScreen
import com.example.healthapp.ui.screens.RegistrationScreen
import com.example.healthapp.ui.screens.AuthGateScreen
import com.example.healthapp.viewmodel.AuthViewModel

@Composable
fun AppNavigation() {

    // Bộ điều khiển navigation
    val navController = rememberNavController()
    val heartRateViewModel: HeartRateViewModel = viewModel()
    val spo2ViewModel: Spo2ViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    // NavHost = quản lý tất cả màn hình
    NavHost(
        navController = navController,
        startDestination = Screen.AuthGate.route
    ) {

        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }

        composable(Screen.AuthGate.route) {
            AuthGateScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    heartRateViewModel.refresh()
                    spo2ViewModel.refresh()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Screen.Register.route) {
            RegistrationScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    heartRateViewModel.refresh()
                    spo2ViewModel.refresh()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }


        // Home Screen
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel()

            HomeScreen(
                navController = navController,
                homeViewModel = homeViewModel
            )
        }

        // Statistics Screen
        composable(Screen.Statistics.route) {
            StatisticsScreen(navController)
        }

        // Notification Screen
        composable(Screen.Notification.route) {
            NotificationScreen(navController)
        }

        // Heart Rate Detail Screen
        composable(Screen.HeartRateDetail.route) {
            HeartRateDetailScreen(navController, heartRateViewModel)
        }

        // SpO2 Detail Screen
        composable(Screen.SpO2Detail.route) {
            SpO2DetailScreen(navController, spo2ViewModel)
        }

        // Step Detail Screen
        composable(Screen.StepsDetail.route) {
            StepsDetailScreen(navController)
        }

        composable(
            route = Screen.StepsHistory.route,
            arguments = listOf(
                navArgument("dateMillis") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val dateMillis = backStackEntry.arguments?.getLong("dateMillis") ?: 0L
            StepsHistoryScreen(navController, dateMillis)
        }

        // Sleep Detail Screen
        composable(Screen.SleepDetail.route) {
            SleepDetailScreen(navController)
        }

        // Heart Rate History Screen
        composable(Screen.HeartRateHistory.route) {
            HeartRateHistoryScreen(navController, heartRateViewModel)
        }

        // SpO2 History Screen
        composable(Screen.SpO2History.route) {
            SpO2HistoryScreen(navController, spo2ViewModel)
        }

        // Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }


    }
}