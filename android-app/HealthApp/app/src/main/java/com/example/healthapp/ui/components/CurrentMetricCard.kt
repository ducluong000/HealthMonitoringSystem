package com.example.healthapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthapp.ui.theme.CardBg
import com.example.healthapp.ui.theme.OnSurfaceVariant
import com.example.healthapp.ui.theme.PrimaryBlue

@Composable
fun CurrentMetricCard(
    title: String,
    valueText: String,
    unitText: String,
    statusLabel: String,
    statusTextColor: Color,
    statusBgColor: Color,
    statusBorderColor: Color,
    lastMeasureText: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = PrimaryBlue,
    valueColor: Color = PrimaryBlue
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(valueText, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
                Spacer(Modifier.width(4.dp))
                Text(
                    unitText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (statusLabel.isNotBlank()) {
                    Surface(
                        color = statusBgColor,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, statusBorderColor)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(statusTextColor))
                            Spacer(Modifier.width(6.dp))
                            Text(statusLabel, color = statusTextColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
                if (lastMeasureText.isNotBlank()) {
                    Text(lastMeasureText, color = OnSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
    }
}
