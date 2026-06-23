package com.example.healthapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.healthapp.ui.navigation.Screen
import com.example.healthapp.viewmodel.AuthTarget
import com.example.healthapp.viewmodel.AuthViewModel

@Composable
fun AuthGateScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.checkAuthState()
    }

    LaunchedEffect(uiState.target) {
        when (uiState.target) {
            AuthTarget.LOGIN -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.AuthGate.route) { inclusive = true }
                }
            }


            AuthTarget.HOME -> {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.AuthGate.route) { inclusive = true }
                }
            }

            null -> Unit
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}