package com.example.healthapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.healthapp.data.model.NotificationModel
import com.example.healthapp.ui.components.BottomNavBar
import com.example.healthapp.ui.components.BottomTab
import com.example.healthapp.ui.theme.CardBg
import com.example.healthapp.ui.theme.OnSurfaceColor
import com.example.healthapp.ui.theme.OnSurfaceVariant
import com.example.healthapp.ui.theme.PrimaryBlue
import com.example.healthapp.ui.theme.SuccessGreen
import com.example.healthapp.ui.theme.SurfaceColor
import com.example.healthapp.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val notifications by notificationViewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Thông báo",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                selected = BottomTab.NOTIFY,
                navController = navController
            )
        },
        containerColor = SurfaceColor
    ) { paddingValues ->

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có thông báo.",
                    color = OnSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(notifications) { item ->
                    NotificationItemCard(
                        item = item,
                        onClick = {
                            notificationViewModel.markAsRead(item.id)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    item: NotificationModel,
    onClick: () -> Unit
) {
    val color = notificationColor(item)
    val icon = notificationIcon(item)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = CardBg,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = color.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title.ifBlank { "Thông báo" },
                        color = OnSurfaceColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(PrimaryBlue, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = item.message,
                    color = OnSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatNotificationTime(item.createdAt),
                    color = OnSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun notificationColor(item: NotificationModel): Color {
    return when (item.level.lowercase()) {
        "warning" -> Color(0xFFFF9800)
        "success" -> SuccessGreen
        else -> PrimaryBlue
    }
}

private fun notificationIcon(item: NotificationModel): ImageVector {
    return when (item.level.lowercase()) {
        "warning" -> Icons.Default.Warning
        "success" -> Icons.Default.Favorite
        else -> Icons.Default.Notifications
    }
}

private fun formatNotificationTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""

    val millis = if (timestamp in 1..9_999_999_999L) {
        timestamp * 1000L
    } else {
        timestamp
    }

    return SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        .format(Date(millis))
}