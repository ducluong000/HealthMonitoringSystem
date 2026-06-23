package com.example.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthapp.data.model.HeartRateEntry
import com.example.healthapp.data.model.HeartRateType
import com.example.healthapp.ui.theme.*

@Composable
fun HeartRateCard(entry: HeartRateEntry) {
    val iconBg = Color(0xFFFFEBEE)
    val iconColor = ErrorRed
    val (tagBg, tagColor) = when (entry.type) {
        HeartRateType.HIGH -> listOf(Color(0xFFFFEBEE), ErrorRed)
        HeartRateType.LOW -> listOf(Color(0xFFFFF3E0), Color(0xFFEF6C00))
        HeartRateType.NORMAL -> listOf(Color(0xFFE8F5E9), SuccessGreen)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Favorite, null, tint = iconColor as Color, modifier = Modifier.size(26.dp))

            Spacer(modifier = Modifier.width(16.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(entry.bpm.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(" BPM", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextGray, modifier = Modifier.padding(bottom = 2.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(entry.time, fontSize = 14.sp, color = TextGray, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.width(16.dp))

            Surface(color = tagBg as Color, shape = RoundedCornerShape(12.dp)) {
                Text(entry.status, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tagColor as Color)
            }
        }
    }
}