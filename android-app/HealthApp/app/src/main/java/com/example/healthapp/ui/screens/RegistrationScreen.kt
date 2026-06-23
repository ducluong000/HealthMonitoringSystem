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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthapp.viewmodel.AuthViewModel

import com.example.healthapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // Decorative background circle
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .background(PrimaryBlue.copy(alpha = 0.04f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            // Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonitorHeart,
                        contentDescription = "Logo",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
                Text(
                    text = "HealthTrack",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryBlue,
                    letterSpacing = (-0.5).sp
                )
            }

            Text(
                text = "Tạo tài khoản",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDark,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = "Bắt đầu hành trình chăm sóc sức khỏe của bạn.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Registration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 20.dp,
                        spotColor = PrimaryBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactInputField(
                        label = "TÊN",
                        icon = Icons.Default.Person,
                        placeholder = "Nguyễn Văn A",
                        value = name,
                        onValueChange = { name = it }
                    )

                    CompactInputField(
                        label = "EMAIL",
                        icon = Icons.Default.Mail,
                        placeholder = "example@healthtrack.com",
                        value = email,
                        onValueChange = { email = it }
                    )

                    CompactInputField(
                        label = "MẬT KHẨU",
                        icon = Icons.Default.Lock,
                        placeholder = "••••••••",
                        value = password,
                        onValueChange = { password = it },
                        isPassword = true
                    )

                    CompactInputField(
                        label = "XÁC NHẬN MẬT KHẨU",
                        icon = Icons.Default.VerifiedUser,
                        placeholder = "••••••••",
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        isPassword = true
                    )

                    // Terms text
                    Text(
                        text = buildAnnotatedString {
                            append("Nhấn Đăng ký là bạn đồng ý với ")
                            withStyle(style = SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold)) {
                                append("Điều khoản")
                            }
                            append(" và ")
                            withStyle(style = SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold)) {
                                append("Chính sách riêng tư")
                            }
                            append(".")
                        },
                        fontSize = 11.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Register Button
                    Button(
                        onClick = {
                            authViewModel.register(
                                email = email,
                                password = password,
                                confirmPassword = confirmPassword,
                                onSuccess = onRegisterSuccess
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Đăng ký", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Đã có tài khoản?", color = TextGray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onNavigateToLogin) {
                    Text("Đăng nhập", color = PrimaryBlue, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "© 2024 HealthTrack International",
                fontSize = 10.sp,
                color = TextGray.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactInputField(
    label: String,
    icon: ImageVector,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {

    var passwordVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextGray,
            letterSpacing = 1.sp
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            placeholder = { Text(placeholder, color = TextGray.copy(alpha = 0.4f), fontSize = 13.sp) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp)) },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )
    }
}

