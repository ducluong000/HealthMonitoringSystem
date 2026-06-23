package com.example.healthapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.healthapp.data.model.StepRecordModel
import com.example.healthapp.ui.components.BottomNavBar
import com.example.healthapp.ui.components.BottomTab
import com.example.healthapp.ui.theme.BackgroundGray
import com.example.healthapp.ui.theme.CardBg
import com.example.healthapp.ui.theme.OnSurfaceVariant
import com.example.healthapp.ui.theme.PrimaryBlue
import com.example.healthapp.ui.theme.TextDark
import com.example.healthapp.ui.theme.TextGray
import com.example.healthapp.viewmodel.StepsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class StepHistoryEntry(
    val id: String,
    val steps: Long,
    val delta: Long,
    val time: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsHistoryScreen(
    navController: NavController,
    selectedDateStartMillis: Long,
    stepsViewModel: StepsViewModel = viewModel()
) {
    val stepsHistory by stepsViewModel.stepsHistory.collectAsState()

    val resolvedDateStartMillis = remember(selectedDateStartMillis) {
        if (selectedDateStartMillis > 0L) {
            startOfDayMillis(selectedDateStartMillis)
        } else {
            startOfDayMillis(System.currentTimeMillis())
        }
    }

    val dateKey = remember(resolvedDateStartMillis) {
        formatDateKey(resolvedDateStartMillis)
    }

    val dayHistory = stepsHistory[dateKey]

    val historyRecords = remember(dayHistory) {
        val records = dayHistory?.records?.values?.toMutableList() ?: mutableListOf()

        records.sortedBy { record ->
            normalizeTimestampMillis(record.updatedAt.takeIf { time -> time > 0L } ?: record.timestamp)
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val entries = historyRecords.mapIndexed { index, record ->
        val timeMillis = normalizeTimestampMillis(record.updatedAt.takeIf { it > 0L } ?: record.timestamp)
        val previousSteps = historyRecords.getOrNull(index - 1)?.steps ?: 0L
        val delta = (record.steps - previousSteps).coerceAtLeast(0L)

        StepHistoryEntry(
            id = index.toString(),
            steps = record.steps,
            delta = delta,
            time = if (timeMillis > 0L) timeFormatter.format(Date(timeMillis)) else "--:--"
        )
    }.asReversed()

    val headerDateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val headerDateText = remember(resolvedDateStartMillis) {
        "Ngày ${headerDateFormatter.format(Date(resolvedDateStartMillis))}"
    }

    val totalSteps = remember(dayHistory) {
        dayHistory?.totalSteps ?: 0L
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Lịch sử bước chân",
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryBlue,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = TextGray)
                    }
                }
            )
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
                    Text(
                        headerDateText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        "Tổng: ${formatNumber(totalSteps)} bước",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (entries.isEmpty()) {
                item {
                    Text(
                        "Chưa có dữ liệu cho ngày này",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        color = TextGray
                    )
                }
            } else {
                items(entries) { entry ->
                    StepsHistoryCard(entry)
                }
            }

            item { Spacer(modifier = Modifier.padding(bottom = 24.dp)) }
        }
    }
}

@Composable
private fun StepsHistoryCard(entry: StepHistoryEntry) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = CardBg,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DirectionsWalk,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.padding(start = 16.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    formatNumber(entry.delta),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    " bước",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(entry.time, fontSize = 14.sp, color = TextGray, fontWeight = FontWeight.Medium)
        }
    }
}

private fun formatDateKey(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        .format(Date(timeMillis))
}

private fun formatNumber(value: Long): String {
    return "%,d".format(value).replace(",", ".")
}

private fun normalizeTimestampMillis(timestamp: Long): Long {
    if (timestamp <= 0L) return 0L

    return if (timestamp in 1..9_999_999_999L) {
        timestamp * 1000L
    } else {
        timestamp
    }
}

private fun startOfDayMillis(timeMillis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}