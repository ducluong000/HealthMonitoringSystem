package com.example.healthapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthapp.ui.theme.*
import androidx.navigation.NavController
import com.example.healthapp.ui.navigation.Screen

@Composable
fun SplashScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF0F7FF), Color(0xFFF8F9FA)),
                    radius = 2000f
                )
            )
    ) {
        // Decorative Floating Orbs
        FloatingOrb(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-100).dp, y = (-100).dp),
            color = Primary.copy(alpha = 0.05f)
        )

        FloatingOrb(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp),
            color = Color(0xFF1B6D24).copy(alpha = 0.05f) // Secondary color
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Central Branding Cluster
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Logo
                Box(contentAlignment = Alignment.Center) {
                    // Background layers
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .rotate(6f)
                            .background(Primary.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .rotate(-3f)
                            .background(PrimaryContainer.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    )
                    // Main Icon Box
                    Surface(
                        modifier = Modifier.size(96.dp),
                        color = Primary,
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 12.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxSize()
                        )
                    }
                }

                // Identity
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "HealthTrack",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Primary,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "Theo dõi sức khỏe mỗi ngày",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Action Area
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Pagination Dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(5) { index ->
                        val alpha = when (index) {
                            2 -> 1f
                            1, 3 -> 0.4f
                            else -> 0.2f
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = alpha))
                        )
                    }
                }

                // Primary Button
                Button(
                    onClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(32.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bắt đầu",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    }
                }

                // Footer
                Text(
                    text = "MATERIAL DESIGN 3 • PREMIUM HEALTH SYSTEMS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant.copy(alpha = 0.4f),
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun FloatingOrb(modifier: Modifier = Modifier, color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .size(250.dp)
            .scale(scale)
            .blur(60.dp)
            .background(color, CircleShape)
    )
}
/*
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(onStartClick = {})
}*/