package com.example.healthapp.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.healthapp.data.model.SleepSessionModel
import com.example.healthapp.ui.components.CurrentMetricCard
import com.example.healthapp.ui.components.DateSelector
import com.example.healthapp.ui.components.HeartRateRange
import com.example.healthapp.ui.components.TabSelector
import com.example.healthapp.ui.theme.CardBg
import com.example.healthapp.ui.theme.OnSurfaceColor
import com.example.healthapp.ui.theme.OnSurfaceVariant
import com.example.healthapp.ui.theme.PrimaryBlue
import com.example.healthapp.ui.theme.SuccessGreen
import com.example.healthapp.ui.theme.SurfaceColor
import com.example.healthapp.viewmodel.SleepViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepDetailScreen(
    navController: NavController,
    sleepViewModel: SleepViewModel = viewModel()
) {
    val currentHealth by sleepViewModel.currentHealth.collectAsState()
    val userProfile by sleepViewModel.userProfile.collectAsState()
    val sleepSessions by sleepViewModel.sleepSessions.collectAsState()

    var selectedRange by remember {
        mutableStateOf(HeartRateRange.DAY)
    }

    var selectedDateStartMillis by remember {
        mutableStateOf(startOfDayMillis(System.currentTimeMillis()))
    }

    val sleepGoalSeconds = if (userProfile.sleepGoalSeconds > 0L) {
        userProfile.sleepGoalSeconds
    } else {
        28800L
    }

    val currentSleep = currentHealth.sleep
    val latestSleepSeconds = currentSleep?.totalSleepSeconds ?: 0L
    val latestSleepStatus = currentSleep?.status ?: ""

    val rangeStartMillis = when (selectedRange) {
        HeartRateRange.DAY -> selectedDateStartMillis
        HeartRateRange.WEEK -> startOfWeekMillis(selectedDateStartMillis)
        HeartRateRange.MONTH -> startOfMonthMillis(selectedDateStartMillis)
    }

    val rangeEndMillis = when (selectedRange) {
        HeartRateRange.DAY -> addDays(rangeStartMillis, 1)
        HeartRateRange.WEEK -> addDays(rangeStartMillis, 7)
        HeartRateRange.MONTH -> addMonths(rangeStartMillis, 1)
    }

    val selectedSessions = remember(sleepSessions, rangeStartMillis, rangeEndMillis) {
        sleepSessions.filter { session ->
            val time = sessionGroupTimeMillis(session)
            time in rangeStartMillis until rangeEndMillis
        }
    }

    val chartPoints = when (selectedRange) {
        HeartRateRange.DAY -> buildDaySleepPoints(selectedSessions)
        HeartRateRange.WEEK -> buildWeeklySleepPoints(
            sessions = sleepSessions,
            selectedDateMillis = selectedDateStartMillis
        )

        HeartRateRange.MONTH -> buildMonthlySleepPoints(
            sessions = sleepSessions,
            selectedDateMillis = selectedDateStartMillis
        )
    }

    val stats = when (selectedRange) {
        HeartRateRange.DAY -> buildSleepStats(
            points = chartPoints,
            sleepGoalSeconds = sleepGoalSeconds,
            wakeCount = selectedSessions.sumOf { it.wakeCount }
        )

        HeartRateRange.WEEK -> buildSleepStats(
            points = chartPoints,
            sleepGoalSeconds = sleepGoalSeconds,
            wakeCount = selectedSessions.sumOf { it.wakeCount }
        )

        HeartRateRange.MONTH -> buildSleepStats(
            points = chartPoints,
            sleepGoalSeconds = sleepGoalSeconds,
            wakeCount = selectedSessions.sumOf { it.wakeCount }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chi tiết giấc ngủ",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryBlue
                        )
                    }
                }
            )
        },

        containerColor = SurfaceColor
    ) { paddingValues ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                CurrentMetricCard(
                    title = "Giấc ngủ gần nhất",
                    valueText = formatSleepDuration(latestSleepSeconds),
                    unitText = "",
                    statusLabel = sleepStatusText(
                        status = latestSleepStatus,
                        totalSeconds = latestSleepSeconds,
                        goalSeconds = sleepGoalSeconds
                    ),
                    statusTextColor = sleepStatusColor(
                        status = latestSleepStatus,
                        totalSeconds = latestSleepSeconds,
                        goalSeconds = sleepGoalSeconds
                    ),
                    statusBgColor = sleepStatusColor(
                        status = latestSleepStatus,
                        totalSeconds = latestSleepSeconds,
                        goalSeconds = sleepGoalSeconds
                    ).copy(alpha = 0.12f),
                    statusBorderColor = sleepStatusColor(
                        status = latestSleepStatus,
                        totalSeconds = latestSleepSeconds,
                        goalSeconds = sleepGoalSeconds
                    ).copy(alpha = 0.35f),
                    lastMeasureText = "Cập nhật: ${
                        formatTimeFromSeconds(
                            currentSleep?.endTime?.takeIf { it > 0L }
                                ?: currentSleep?.startTime
                                ?: 0L
                        )
                    }",
                    icon = Icons.Default.Bedtime,
                    iconTint = Color(0xFF3F51B5),
                    valueColor = Color(0xFF3F51B5)
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TabSelector(
                        selectedRange = selectedRange,
                        onRangeSelected = { selectedRange = it }
                    )

                    DateSelector(
                        selectedDateStartMillis = selectedDateStartMillis,
                        selectedRange = selectedRange,
                        onDateSelected = {
                            selectedDateStartMillis = startOfDayMillis(it)
                        }
                    )
                }
            }

            item {
                SleepChartCard(
                    selectedRange = selectedRange,
                    points = chartPoints,
                    sleepGoalSeconds = sleepGoalSeconds
                )
            }

            item {
                SleepSessionList(
                    sessions = selectedSessions.take(5)
                )
            }

            item {
                SleepStatsGrid(
                    selectedRange = selectedRange,
                    stats = stats
                )
            }

            item {
                SleepInfoCard(
                    title = "NHẬN XÉT",
                    content = buildSleepInsight(stats),
                    icon = Icons.Default.CheckCircle,
                    containerColor = Color(0xFFE3F2FD),
                    iconBg = PrimaryBlue
                )
            }

            item {
                SleepInfoCard(
                    title = "GỢI Ý CHO BẠN",
                    content = buildSleepSuggestion(stats),
                    icon = Icons.Default.Lightbulb,
                    containerColor = Color(0xFFE8F5E9),
                    iconBg = SuccessGreen
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SleepChartCard(
    selectedRange: HeartRateRange,
    points: List<SleepBarPoint>,
    sleepGoalSeconds: Long
) {
    val title = when (selectedRange) {
        HeartRateRange.DAY -> "Biểu đồ phiên ngủ trong ngày"
        HeartRateRange.WEEK -> "Biểu đồ giấc ngủ theo tuần"
        HeartRateRange.MONTH -> "Biểu đồ giấc ngủ theo tháng"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (selectedRange) {
                    HeartRateRange.DAY -> "Mỗi cột thể hiện một phiên ngủ."
                    HeartRateRange.WEEK -> "Mỗi cột thể hiện tổng thời gian ngủ mỗi ngày."
                    HeartRateRange.MONTH -> "Kéo ngang để xem các ngày trong tháng."
                },
                fontSize = 12.sp,
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedRange == HeartRateRange.MONTH) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    SleepBarChart(
                        points = points,
                        sleepGoalSeconds = sleepGoalSeconds,
                        modifier = Modifier
                            .width((points.size * 44).dp)
                            .height(240.dp)
                    )
                }
            } else {
                SleepBarChart(
                    points = points,
                    sleepGoalSeconds = sleepGoalSeconds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }
        }
    }
}

@Composable
private fun SleepBarChart(
    points: List<SleepBarPoint>,
    sleepGoalSeconds: Long,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val left = 42.dp.toPx()
        val right = size.width - 10.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 34.dp.toPx()

        val chartWidth = right - left
        val chartHeight = bottom - top

        val maxPointValue = points.maxOfOrNull { it.seconds } ?: 0L
        val maxValue = maxOf(maxPointValue, sleepGoalSeconds, 36000L).toFloat()

        fun yForValue(value: Float): Float {
            val ratio = (value / maxValue).coerceIn(0f, 1f)
            return bottom - ratio * chartHeight
        }

        val labelPaint = Paint().apply {
            color = android.graphics.Color.rgb(100, 106, 120)
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val xLabelPaint = Paint().apply {
            color = android.graphics.Color.rgb(100, 106, 120)
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val emptyPaint = Paint().apply {
            color = android.graphics.Color.rgb(100, 106, 120)
            textSize = 13.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val gridValues = listOf(
            0f,
            maxValue * 0.25f,
            maxValue * 0.5f,
            maxValue * 0.75f,
            maxValue
        )

        gridValues.forEach { value ->
            val y = yForValue(value)

            drawLine(
                color = Color(0xFFE0E0E0),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )

            drawContext.canvas.nativeCanvas.drawText(
                formatSleepDuration(value.toLong()),
                left - 8.dp.toPx(),
                y + 4.dp.toPx(),
                labelPaint
            )
        }

        if (sleepGoalSeconds > 0L) {
            val goalY = yForValue(sleepGoalSeconds.toFloat())

            drawLine(
                color = SuccessGreen,
                start = Offset(left, goalY),
                end = Offset(right, goalY),
                strokeWidth = 2.dp.toPx()
            )

            drawContext.canvas.nativeCanvas.drawText(
                "Mục tiêu",
                right,
                goalY - 6.dp.toPx(),
                Paint().apply {
                    color = android.graphics.Color.rgb(76, 175, 80)
                    textSize = 10.sp.toPx()
                    textAlign = Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
        }

        if (points.all { it.seconds <= 0L }) {
            drawContext.canvas.nativeCanvas.drawText(
                "Chưa có dữ liệu giấc ngủ",
                size.width / 2f,
                size.height / 2f,
                emptyPaint
            )
        }

        if (points.isNotEmpty()) {
            val gap = chartWidth / points.size
            val barWidth = (gap * 0.48f).coerceAtMost(18.dp.toPx())

            points.forEachIndexed { index, point ->
                val centerX = left + gap * index + gap / 2f
                val barTop = yForValue(point.seconds.toFloat())
                val barBottom = bottom

                val barColor = if (point.seconds >= sleepGoalSeconds) {
                    SuccessGreen
                } else {
                    Color(0xFF3F51B5)
                }

                if (point.seconds > 0L) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(centerX - barWidth / 2f, barTop),
                        size = Size(
                            width = barWidth,
                            height = barBottom - barTop
                        ),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }

                if (point.showLabel) {
                    drawContext.canvas.nativeCanvas.drawText(
                        point.label,
                        centerX,
                        bottom + 22.dp.toPx(),
                        xLabelPaint
                    )
                }
            }
        }
    }
}



@Composable
private fun SleepStatsGrid(
    selectedRange: HeartRateRange,
    stats: SleepStats
) {
    val items = when (selectedRange) {
        HeartRateRange.DAY -> listOf(
            SleepStatUi("Tổng ngủ", formatSleepDuration(stats.totalSeconds), ""),
            SleepStatUi("Mục tiêu", formatSleepDuration(stats.goalSeconds), ""),
            SleepStatUi("Thức giấc", stats.wakeCount.toString(), "lần")
        )

        HeartRateRange.WEEK -> listOf(
            SleepStatUi("Tổng tuần", formatSleepDuration(stats.totalSeconds), ""),
            SleepStatUi("TB/ngày", formatSleepDuration(stats.averagePerDay), ""),
            SleepStatUi("Đạt mục tiêu", "${stats.reachedGoalDays}/7", "ngày")
        )

        HeartRateRange.MONTH -> listOf(
            SleepStatUi("Tổng tháng", formatSleepDuration(stats.totalSeconds), ""),
            SleepStatUi("TB/ngày", formatSleepDuration(stats.averagePerDay), ""),
            SleepStatUi("Đạt mục tiêu", stats.reachedGoalDays.toString(), "ngày")
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            SleepStatCard(
                item = item,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SleepStatCard(
    item: SleepStatUi,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3F51B5)
            )

            if (item.unit.isNotBlank()) {
                Text(
                    text = item.unit,
                    fontSize = 10.sp,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SleepSessionList(
    sessions: List<SleepSessionModel>
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Phiên ngủ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceColor
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = OnSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                if (sessions.isEmpty()) {
                    Text(
                        text = "Chưa có phiên ngủ trong khoảng thời gian này.",
                        fontSize = 13.sp,
                        color = OnSurfaceVariant
                    )
                } else {
                    sessions.forEach { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = formatSleepTimeRange(session),
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceColor,
                                    fontSize = 14.sp
                                )

                                Text(
                                    text = "Thức giấc: ${session.wakeCount} lần",
                                    color = OnSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                text = formatSleepDuration(session.totalSleepSeconds),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF3F51B5),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepInfoCard(
    title: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    iconBg: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(16.dp),
                color = iconBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    color = iconBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = content,
                    color = OnSurfaceColor,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private data class SleepBarPoint(
    val label: String,
    val seconds: Long,
    val showLabel: Boolean = true
)

private data class SleepStats(
    val totalSeconds: Long,
    val goalSeconds: Long,
    val progress: Float,
    val progressPercent: Int,
    val averagePerDay: Long,
    val maxSeconds: Long,
    val wakeCount: Int,
    val reachedGoalDays: Int
)

private data class SleepStatUi(
    val label: String,
    val value: String,
    val unit: String
)

private fun buildDaySleepPoints(
    sessions: List<SleepSessionModel>
): List<SleepBarPoint> {
    if (sessions.isEmpty()) {
        return listOf(
            SleepBarPoint("L1", 0L)
        )
    }

    return sessions.sortedBy { it.startTime }.mapIndexed { index, session ->
        SleepBarPoint(
            label = "L${index + 1}",
            seconds = session.totalSleepSeconds
        )
    }
}

private fun buildWeeklySleepPoints(
    sessions: List<SleepSessionModel>,
    selectedDateMillis: Long
): List<SleepBarPoint> {
    val weekStart = startOfWeekMillis(selectedDateMillis)

    return (0..6).map { index ->
        val dayStart = addDays(weekStart, index)
        val dayEnd = addDays(dayStart, 1)

        val totalSeconds = sessions
            .filter {
                val time = sessionGroupTimeMillis(it)
                time in dayStart until dayEnd
            }
            .sumOf { it.totalSleepSeconds }

        SleepBarPoint(
            label = shortWeekday(dayStart),
            seconds = totalSeconds
        )
    }
}

private fun buildMonthlySleepPoints(
    sessions: List<SleepSessionModel>,
    selectedDateMillis: Long
): List<SleepBarPoint> {
    val monthStart = startOfMonthMillis(selectedDateMillis)

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = monthStart
    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    return (1..maxDay).map { day ->
        val dayStart = dayStartOfMonth(monthStart, day)
        val dayEnd = addDays(dayStart, 1)

        val totalSeconds = sessions
            .filter {
                val time = sessionGroupTimeMillis(it)
                time in dayStart until dayEnd
            }
            .sumOf { it.totalSleepSeconds }

        SleepBarPoint(
            label = day.toString().padStart(2, '0'),
            seconds = totalSeconds
        )
    }
}

private fun buildSleepStats(
    points: List<SleepBarPoint>,
    sleepGoalSeconds: Long,
    wakeCount: Int
): SleepStats {
    val totalSeconds = points.sumOf { it.seconds }
    val count = points.size.coerceAtLeast(1)

    val progress = if (sleepGoalSeconds > 0L) {
        (totalSeconds.toFloat() / (sleepGoalSeconds * count).toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    return SleepStats(
        totalSeconds = totalSeconds,
        goalSeconds = sleepGoalSeconds,
        progress = progress,
        progressPercent = (progress * 100).roundToInt(),
        averagePerDay = totalSeconds / count,
        maxSeconds = points.maxOfOrNull { it.seconds } ?: 0L,
        wakeCount = wakeCount,
        reachedGoalDays = points.count { it.seconds >= sleepGoalSeconds }
    )
}

private fun sessionGroupTimeMillis(session: SleepSessionModel): Long {
    val rawTime = if (session.endTime > 0L) {
        session.endTime
    } else {
        session.startTime
    }

    return normalizeTimestampMillis(rawTime)
}
/*
private fun formatSleepDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "0m"

    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}*/

private fun formatSleepTimeRange(session: SleepSessionModel): String {
    val start = formatTimeOnly(session.startTime)
    val end = formatTimeOnly(session.endTime)

    return "$start - $end"
}

private fun sleepStatusText(
    status: String,
    totalSeconds: Long,
    goalSeconds: Long
): String {
    return when {
        status.equals("sleeping", ignoreCase = true) -> "Đang ngủ"
        totalSeconds <= 0L -> "Chưa có"
        totalSeconds >= goalSeconds -> "Đạt mục tiêu"
        else -> "Hoàn thành"
    }
}

private fun sleepStatusColor(
    status: String,
    totalSeconds: Long,
    goalSeconds: Long
): Color {
    return when {
        status.equals("sleeping", ignoreCase = true) -> PrimaryBlue
        totalSeconds >= goalSeconds -> SuccessGreen
        totalSeconds > 0L -> Color(0xFF3F51B5)
        else -> OnSurfaceVariant
    }
}

private fun buildSleepInsight(stats: SleepStats): String {
    return when {
        stats.totalSeconds <= 0L -> "Chưa có dữ liệu giấc ngủ trong khoảng thời gian đã chọn."
        stats.progressPercent >= 100 -> "Thời gian ngủ đã đạt mục tiêu đề ra."
        stats.progressPercent >= 70 -> "Thời gian ngủ gần đạt mục tiêu, nên duy trì giờ ngủ ổn định hơn."
        else -> "Thời gian ngủ còn thấp so với mục tiêu đề ra."
    }
}

private fun buildSleepSuggestion(stats: SleepStats): String {
    return when {
        stats.totalSeconds <= 0L -> "Hãy theo dõi giấc ngủ để có dữ liệu đánh giá chính xác hơn."
        stats.progressPercent >= 100 -> "Tiếp tục duy trì thói quen ngủ đều đặn mỗi ngày."
        else -> "Nên ngủ đủ giờ, hạn chế thức khuya và duy trì lịch ngủ ổn định."
    }
}
/*
private fun formatTimeFromSeconds(timestamp: Long): String {
    if (timestamp <= 0L) return "Chưa có"

    val millis = normalizeTimestampMillis(timestamp)
    return SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        .format(Date(millis))
}*/

private fun formatTimeOnly(timestamp: Long): String {
    if (timestamp <= 0L) return "--:--"

    val millis = normalizeTimestampMillis(timestamp)
    return SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(millis))
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

private fun startOfWeekMillis(timeMillis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.firstDayOfWeek = Calendar.MONDAY

    while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        calendar.add(Calendar.DAY_OF_MONTH, -1)
    }

    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    return calendar.timeInMillis
}

private fun startOfMonthMillis(timeMillis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun dayStartOfMonth(
    monthStartMillis: Long,
    dayOfMonth: Int
): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = monthStartMillis
    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun addDays(
    timeMillis: Long,
    days: Int
): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.add(Calendar.DAY_OF_MONTH, days)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun addMonths(
    timeMillis: Long,
    months: Int
): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.add(Calendar.MONTH, months)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun shortWeekday(timeMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis

    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "T2"
        Calendar.TUESDAY -> "T3"
        Calendar.WEDNESDAY -> "T4"
        Calendar.THURSDAY -> "T5"
        Calendar.FRIDAY -> "T6"
        Calendar.SATURDAY -> "T7"
        else -> "CN"
    }
}