package com.example.healthapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthapp.viewmodel.AuthViewModel

import com.example.healthapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val uiState by authViewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HealthBackground)
    ) {
        // Decorative background circle
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = 220.dp, y = (-60).dp)
                .background(HealthPrimary.copy(alpha = 0.04f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.8f))

            // Logo + Brand
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(4.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(HealthPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = HealthPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = "HealthTrack",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = HealthPrimary,
                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
            )

            // Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(24.dp), spotColor = HealthPrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = HealthSurface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Chào mừng trở lại",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    Text(
                        text = "Tiếp tục hành trình sống khỏe cùng chúng tôi.",
                        fontSize = 13.sp,
                        color = HealthOutline,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Email Field
                    Text(
                        text = "EMAIL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HealthOutline,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("example@healthtrack.com", fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = InputBackground,
                            unfocusedContainerColor = InputBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = HealthOutline, modifier = Modifier.size(20.dp)) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    Text(
                        text = "MẬT KHẨU",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HealthOutline,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("••••••••", fontSize = 14.sp) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = InputBackground,
                            unfocusedContainerColor = InputBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = HealthOutline, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = HealthOutline,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true
                    )

                    // Forgot password
                    TextButton(
                        onClick = { /* Quên mật khẩu */ },
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Quên mật khẩu?", color = HealthPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Login Button
                    Button(
                        onClick = {
                            authViewModel.login(
                                email = email,
                                password = password,
                                onSuccess = onLoginSuccess
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = HealthPrimary)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Đăng nhập", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chưa có tài khoản?", color = HealthOutline, fontSize = 14.sp)
                TextButton(onClick = onNavigateToRegister) {
                    Text("Đăng ký", color = HealthSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "© 2024 HealthTrack International",
                fontSize = 10.sp,
                color = HealthOutline.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onNavigateToRegister = {}, onLoginSuccess = {})
}