
package com.example.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.healthapp.ui.navigation.Screen
import com.example.healthapp.ui.theme.PrimaryBlue
import com.example.healthapp.ui.theme.OnSurfaceVariant

@Composable
fun BottomNavBar(
    selected: BottomTab,
    navController: NavController
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp),
        color = Color.White,
        shadowElevation = 16.dp
    ) {

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            NavItem(
                icon = Icons.Default.Home,
                active = selected == BottomTab.HOME,
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate(Screen.Home.route)
            }



            NavItem(
                icon = Icons.Default.Leaderboard,
                active = selected == BottomTab.STATS,
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate(Screen.Statistics.route)
            }

            NavItem(
                icon = Icons.Default.Notifications,
                active = selected == BottomTab.NOTIFY,
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate(Screen.Notification.route)
            }

            NavItem(
                icon = Icons.Default.Person,
                active = selected == BottomTab.PROFILE,
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate(Screen.Profile.route)
            }
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Box(
        modifier = modifier
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (active)
                        PrimaryBlue.copy(alpha = 0.1f)
                    else
                        Color.Transparent
                )
                .padding(12.dp)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) PrimaryBlue else OnSurfaceVariant
            )
        }
    }
}

enum class BottomTab {
    HOME,

    STATS,
    NOTIFY,
    PROFILE
}