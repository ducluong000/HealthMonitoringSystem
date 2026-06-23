package com.example.healthapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthapp.ui.theme.OnSurfaceVariant
import com.example.healthapp.ui.theme.PrimaryBlue

enum class HeartRateRange {
    DAY,
    WEEK,
    MONTH
}

@Composable
fun TabSelector(
    selectedRange: HeartRateRange,
    onRangeSelected: (HeartRateRange) -> Unit
) {
    Surface(
        color = androidx.compose.ui.graphics.Color(0xFFF3F4F5),
        shape = CircleShape,
        modifier = Modifier.width(280.dp)
    ) {
        Row(Modifier.padding(4.dp)) {
            listOf(
                HeartRateRange.DAY to "Ngày",
                HeartRateRange.WEEK to "Tuần",
                HeartRateRange.MONTH to "Tháng"
            ).forEach { (range, label) ->
                val isSelected = selectedRange == range
                Surface(
                    modifier = Modifier.weight(1f).clickable { onRangeSelected(range) },
                    color = if (isSelected) PrimaryBlue else androidx.compose.ui.graphics.Color.Transparent,
                    shape = CircleShape,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        color = if (isSelected) androidx.compose.ui.graphics.Color.White else OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
