package com.example.healthapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.healthapp.ui.components.BottomNavBar
import com.example.healthapp.ui.components.BottomTab
import com.example.healthapp.ui.theme.*
import com.example.healthapp.viewmodel.Spo2ViewModel
import com.example.healthapp.ui.utils.isMeasurementInDay
import com.example.healthapp.ui.utils.isValidSpO2Measurement
import com.example.healthapp.ui.utils.measurementTimeMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class SpO2Reading(
    val id: String,
    val value: Int,
    val status: String,
    val statusLabel: String,
    val time: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpO2HistoryScreen(
    navController: NavController,
    spo2ViewModel: Spo2ViewModel = viewModel()
) {
    val measurements by spo2ViewModel.measurements.collectAsState()
    val selectedDateStartMillis by spo2ViewModel.selectedDateStartMillis.collectAsState()

    val dayMeasurements = measurements
        .filter { it.isValidSpO2Measurement() }
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
    val readings = dayMeasurements.mapIndexed { index, measurement ->
        val value = measurement.spo2Avg.roundToInt()
        val status = when {
            value >= 95 -> "stable"
            value >= 90 -> "warn"
            else -> "low"
        }
        val statusLabel = when (status) {
            "stable" -> "Ổn định"
            "warn" -> "Cần chú ý"
            else -> "Thấp"
        }
        val timeLabel = timeFormatter.format(Date(measurementTimeMillis(measurement)))

        SpO2Reading(
            id = index.toString(),
            value = value,
            status = status,
            statusLabel = statusLabel,
            time = timeLabel
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Lịch sử SpO2",
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryBlue,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundGray)
            )
        },
        bottomBar = {
            BottomNavBar(selected = BottomTab.STATS,navController = navController)
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(headerDateText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Text(
                        "Số lần đo: ${readings.size}",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (readings.isEmpty()) {
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
                items(readings) { reading ->
                    SpO2Card(reading)
                }
            }
        }
    }
}

@Composable
private fun rememberHistoryTimeFormatter(): SimpleDateFormat {
    return remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
}

@Composable
fun SpO2Card(reading: SpO2Reading) {
    val (statusColor, statusBg) = when (reading.status) {
        "low" -> LowRed to LowRedBg
        "warn" -> Color(0xFFEF6C00) to Color(0xFFFFF3E0)
        else -> StableGreen to StableGreenBg
    }
    val iconColor = PrimaryBlue

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
            Icon(Icons.Default.WaterDrop, null, tint = iconColor, modifier = Modifier.size(26.dp))

            Spacer(modifier = Modifier.width(16.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(reading.value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(" %", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextGray, modifier = Modifier.padding(bottom = 2.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(reading.time, fontSize = 14.sp, color = TextGray, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.width(16.dp))

            Surface(color = statusBg, shape = RoundedCornerShape(12.dp)) {
                Text(reading.statusLabel, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }
        }
    }
}
/*
@Preview(showBackground = true)
@Composable
fun SpO2HistoryScreenPreview() {
    SpO2HistoryScreen()
}*/
