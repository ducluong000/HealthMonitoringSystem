package com.example.healthapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.healthapp.data.model.HeartRateEntry
import com.example.healthapp.data.model.HeartRateType
import com.example.healthapp.ui.components.HeartRateCard
import com.example.healthapp.ui.theme.BackgroundGray
import com.example.healthapp.ui.theme.PrimaryBlue
import com.example.healthapp.viewmodel.HeartRateViewModel
import com.example.healthapp.ui.utils.isMeasurementInDay
import com.example.healthapp.ui.utils.isValidHeartRateMeasurement
import com.example.healthapp.ui.utils.measurementTimeMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

import com.example.healthapp.ui.components.BottomNavBar
import com.example.healthapp.ui.components.BottomTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateHistoryScreen(
    navController: NavController,
    heartRateViewModel: HeartRateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val measurements by heartRateViewModel.measurements.collectAsState()
    val selectedDateStartMillis by heartRateViewModel.selectedDateStartMillis.collectAsState()

    val dayMeasurements = measurements
        .filter { it.isValidHeartRateMeasurement() }
        .filter { isMeasurementInDay(it, selectedDateStartMillis) }
        .sortedByDescending { measurementTimeMillis(it) }

    val headerDateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val headerDateText = remember(selectedDateStartMillis) {
        if (selectedDateStartMillis > 0L) {
            "Ngày ${headerDateFormatter.format(Date(selectedDateStartMillis))}"
        } else {
            "Gần đây"
        }
    }

    val timeFormatter = rememberHistoryTimeFormatter()
    val entries = dayMeasurements.mapIndexed { index, measurement ->
        val bpm = measurement.heartRateAvg.roundToInt()
        val type = when {
            bpm < 60 -> HeartRateType.LOW
            bpm > 100 -> HeartRateType.HIGH
            else -> HeartRateType.NORMAL
        }
        val status = when (type) {
            HeartRateType.LOW -> "Thấp"
            HeartRateType.HIGH -> "Cao"
            HeartRateType.NORMAL -> "Bình thường"
        }
        val timeLabel = timeFormatter.format(Date(measurementTimeMillis(measurement)))

        HeartRateEntry(
            id = index,
            bpm = bpm,
            status = status,
            time = timeLabel,
            type = type
        )
    }

    Scaffold(

        containerColor = BackgroundGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lịch sử nhịp tim", fontWeight = FontWeight.ExtraBold, color = PrimaryBlue, fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = {}) { Icon(Icons.Default.DateRange, null, tint = Color.Gray) } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 24.dp)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(headerDateText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Text("Số lần đo: ${entries.size}", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }

            if (entries.isEmpty()) {
                item {
                    Text(
                        "Chưa có dữ liệu cho ngày này",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        color = Color.Gray
                    )
                }
            } else {
                items(entries) { HeartRateCard(it) }
            }
        }
    }
}

@Composable
private fun rememberHistoryTimeFormatter(): SimpleDateFormat {
    return remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
}

/*
@Preview(showBackground = true)
@Composable
fun HeartRateHistoryScreenPreview() {
    HeartRateHistoryScreen()
}*/
