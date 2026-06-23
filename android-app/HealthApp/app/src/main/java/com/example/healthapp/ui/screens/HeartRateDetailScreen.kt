package com.example.healthapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.healthapp.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.healthapp.ui.theme.*


import com.example.healthapp.ui.components.CurrentMetricCard
import com.example.healthapp.ui.components.DateSelector
import com.example.healthapp.ui.components.TabSelector
import com.example.healthapp.ui.components.HeartRateRange
import com.example.healthapp.ui.components.addDays
import com.example.healthapp.ui.components.daysInMonth
import com.example.healthapp.ui.components.startOfMonth
import com.example.healthapp.ui.components.startOfWeek

import com.example.healthapp.data.model.MeasurementModel
import com.example.healthapp.viewmodel.HeartRateViewModel
import android.graphics.Paint
import java.util.Calendar
import kotlin.math.roundToInt
import com.example.healthapp.ui.utils.isMeasurementInDay
import com.example.healthapp.ui.utils.isValidHeartRateMeasurement
import com.example.healthapp.ui.utils.measurementTimeMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateDetailScreen(
    navController: NavController,
    heartRateViewModel: HeartRateViewModel = viewModel()
) {
    val currentHealth by heartRateViewModel.currentHealth.collectAsState()
    val measurements by heartRateViewModel.measurements.collectAsState()
    val selectedDateStartMillis by heartRateViewModel.selectedDateStartMillis.collectAsState()

    var selectedRange by remember { mutableStateOf(HeartRateRange.DAY) }
    var selectedMonthDayIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedRange, selectedDateStartMillis) {
        when (selectedRange) {
            HeartRateRange.WEEK -> {
                val weekStart = startOfWeek(selectedDateStartMillis)
                if (weekStart > 0L && weekStart != selectedDateStartMillis) {
                    heartRateViewModel.selectDate(weekStart)
                }
            }
            HeartRateRange.MONTH -> {
                val monthStart = startOfMonth(selectedDateStartMillis)
                if (monthStart > 0L && monthStart != selectedDateStartMillis) {
                    heartRateViewModel.selectDate(monthStart)
                }
            }
            else -> Unit
        }
    }

    val selectedDayMeasurements = remember(measurements, selectedDateStartMillis) {
        measurements
            .filter { it.isValidHeartRateMeasurement() }
            .filter { isMeasurementInDay(it, selectedDateStartMillis) }
            .sortedBy { measurementTimeMillis(it) }
    }

    val chartPoints = remember(selectedDayMeasurements) {
        selectedDayMeasurements.map {
            HeartRateChartPoint(
                timeMillis = measurementTimeMillis(it),
                bpm = it.heartRateAvg
            )
        }
    }

    val weekStartMillis = remember(selectedDateStartMillis) {
        startOfWeek(selectedDateStartMillis)
    }

    val weeklyStats = remember(measurements, weekStartMillis) {
        calculateHeartRateWeeklyStats(measurements, weekStartMillis)
    }

    val monthStartMillis = remember(selectedDateStartMillis) {
        startOfMonth(selectedDateStartMillis)
    }

    val monthlyStats = remember(measurements, monthStartMillis) {
        calculateHeartRateMonthlyStats(measurements, monthStartMillis)
    }

    val weeklyRangeStats = remember(measurements, weekStartMillis) {
        val weekEnd = addDays(weekStartMillis, 7)
        calculateHeartRateRangeStats(measurements, weekStartMillis, weekEnd)
    }

    val monthlyRangeStats = remember(measurements, monthStartMillis) {
        val monthEnd = addDays(monthStartMillis, daysInMonth(monthStartMillis))
        calculateHeartRateRangeStats(measurements, monthStartMillis, monthEnd)
    }

    LaunchedEffect(monthStartMillis) {
        selectedMonthDayIndex = null
    }

    val selectedMonthlyStat = selectedMonthDayIndex?.let { index ->
        monthlyStats.getOrNull(index)
    }

    val dailyStats = remember(selectedDayMeasurements) {
        calculateHeartRateDailyStats(selectedDayMeasurements)
    }
    val rangeStats = remember(selectedRange, dailyStats, weeklyRangeStats, monthlyRangeStats) {
        when (selectedRange) {
            HeartRateRange.WEEK -> weeklyRangeStats
            HeartRateRange.MONTH -> monthlyRangeStats
            else -> dailyStats
        }
    }
    val bpmValue = currentHealth.heartRate
    val bpmText = if (bpmValue > 0) bpmValue.toInt().toString() else "--"
    val status = heartRateStatusUi(bpmValue)
    val lastMeasureText = formatLastMeasureTime(
        if (currentHealth.lastVitalMeasureTime > 0L) currentHealth.lastVitalMeasureTime else currentHealth.lastMeasureTime
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chi tiết Nhịp tim",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryBlue)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        },
        /*
        bottomBar = {
            BottomNavBar(selected = BottomTab.STATS,navController = navController)
        },

         */
        containerColor = SurfaceColor


    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 1. Current Metric Card
            item {
                CurrentMetricCard(
                    title = "Nhịp tim hiện tại",
                    valueText = bpmText,
                    unitText = "bpm",
                    statusLabel = status.label,
                    statusTextColor = status.textColor,
                    statusBgColor = status.bgColor,
                    statusBorderColor = status.borderColor,
                    lastMeasureText = lastMeasureText,
                    icon = Icons.Default.Favorite,
                    iconTint = ErrorRed,
                    valueColor = PrimaryBlue
                )
            }

            // 2. Tabs & Date Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TabSelector(
                        selectedRange = selectedRange,
                        onRangeSelected = { selectedRange = it }
                    )

                    DateSelector(
                        selectedDateStartMillis = selectedDateStartMillis,
                        selectedRange = selectedRange,
                        onDateSelected = heartRateViewModel::selectDate
                    )
                }
            }

            // 3. Chart Section
            item {
                HeartRateChartCard(
                    navController = navController,
                    range = selectedRange,
                    points = chartPoints,
                    weeklyStats = weeklyStats,
                    monthlyStats = monthlyStats,
                    onMonthDaySelected = { index ->
                        val stat = monthlyStats.getOrNull(index) ?: return@HeartRateChartCard
                        if (stat.count > 0) {
                            selectedMonthDayIndex = index
                        }
                    }
                )
            }

            if (selectedRange == HeartRateRange.MONTH && selectedMonthlyStat != null) {
                item {
                    MonthlyDayDetailCard(stat = selectedMonthlyStat)
                }
            }

            // 4. Stats Grid
            item {
                StatsGrid(
                    stats = rangeStats
                )
            }

            // 5. Insights
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InsightCard(
                        title = "NHẬN XÉT HÔM NAY",
                        content = buildHeartRateInsight(rangeStats),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        containerColor = Color(0xFFE3F2FD),
                        iconBg = Color(0xFF1976D2)
                    )
                    InsightCard(
                        title = "GỢI Ý CHO BẠN",
                        content = buildHeartRateSuggestion(rangeStats),
                        icon = Icons.Default.Spa,
                        containerColor = Color(0xFFE8F5E9),
                        iconBg = Color(0xFF2E7D32)
                    )
                }
            }

            // 6. Expert Tip Card
            item { ExpertTipCard(
                title = "Cải thiện hồi phục tim",
                description = "Các bài tập cường độ thấp xen kẽ giúp tim phục hồi nhanh hơn.",
                icon = Icons.Default.Favorite
            ) }


            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

private data class HeartRateStatusUi(
    val label: String,
    val textColor: Color,
    val bgColor: Color,
    val borderColor: Color
)

private fun heartRateStatusUi(bpm: Double): HeartRateStatusUi {
    return when {
        bpm <= 0 -> HeartRateStatusUi(
            label = "Chưa có dữ liệu",
            textColor = OnSurfaceVariant,
            bgColor = Color(0xFFE0E0E0),
            borderColor = Color(0xFFBDBDBD)
        )
        bpm < 60 -> HeartRateStatusUi(
            label = "Thấp",
            textColor = Color(0xFFEF6C00),
            bgColor = Color(0xFFFFF3E0),
            borderColor = Color(0xFFFFE0B2)
        )
        bpm <= 100 -> HeartRateStatusUi(
            label = "Bình thường",
            textColor = SuccessGreen,
            bgColor = Color(0xFFE8F5E9),
            borderColor = Color(0xFFC8E6C9)
        )
        else -> HeartRateStatusUi(
            label = "Cao",
            textColor = ErrorRed,
            bgColor = Color(0xFFFFEBEE),
            borderColor = Color(0xFFFFCDD2)
        )
    }
}

private fun formatLastMeasureTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "Chưa có dữ liệu"
    val normalizedMillis = if (epochMillis in 1..9_999_999_999L) epochMillis * 1000L else epochMillis
    val formatter = SimpleDateFormat("HH:mm, dd/MM", Locale.getDefault())
    return "Đo lần cuối: ${formatter.format(Date(normalizedMillis))}"
}

@Composable
fun HeartRateChartCard(
    navController: NavController,
    range: HeartRateRange,
    points: List<HeartRateChartPoint>,
    weeklyStats: List<WeeklyHeartRateDayStat>,
    monthlyStats: List<MonthlyHeartRateDayStat>,
    onMonthDaySelected: (Int) -> Unit
) {
    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = CardBg,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Biểu đồ nhịp tim",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceColor
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendItem(Color(0xFF4CAF50).copy(alpha = 0.2f), "Bình thường")
                    ChartLegendItem(Color(0xFFFF9800).copy(alpha = 0.2f), "Cao")
                    ChartLegendItem(Color(0xFFE53935).copy(alpha = 0.2f), "Thấp")
                }

                Spacer(Modifier.height(12.dp))

                if (range == HeartRateRange.MONTH) {
                    val scrollState = rememberScrollState()
                    val dayCount = monthlyStats.size.coerceAtLeast(1)
                    val dayStep = when {
                        dayCount >= 31 -> 10.dp
                        dayCount >= 30 -> 11.dp
                        else -> 10.dp
                    }
                    val canvasWidth = (dayStep * (dayCount - 1)) + 46.dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .width(canvasWidth)
                                .height(220.dp)
                                .pointerInput(monthlyStats, scrollState.value, dayStep) {
                                    detectTapGestures { tapOffset ->
                                        val leftPx = 38.dp.toPx()
                                        val dayStepPx = dayStep.toPx()
                                        val adjustedX = tapOffset.x + scrollState.value
                                        val index = ((adjustedX - leftPx) / dayStepPx).roundToInt()
                                        if (index in 0 until dayCount) {
                                            onMonthDaySelected(index)
                                        }
                                    }
                                }
                        ) {
                            val yMin = 40f
                            val yMax = 140f
                            val lowThreshold = 60f
                            val highThreshold = 100f

                            val left = 38.dp.toPx()
                            val right = size.width - 8.dp.toPx()
                            val top = 12.dp.toPx()
                            val bottom = size.height - 30.dp.toPx()

                            val chartWidth = right - left
                            val chartHeight = bottom - top

                            fun yForBpm(bpm: Float): Float {
                                val ratio = ((bpm - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
                                return bottom - ratio * chartHeight
                            }

                            val labelPaint = Paint().apply {
                                color = android.graphics.Color.rgb(100, 106, 120)
                                textSize = 11.sp.toPx()
                                textAlign = Paint.Align.RIGHT
                                isAntiAlias = true
                            }

                            val emptyTextPaint = Paint().apply {
                                color = android.graphics.Color.rgb(140, 146, 160)
                                textSize = 12.sp.toPx()
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                            }

                            val normalTop = yForBpm(highThreshold)
                            val normalBottom = yForBpm(lowThreshold)
                            val highTop = yForBpm(yMax)
                            val highBottom = yForBpm(highThreshold)
                            val lowTop = yForBpm(lowThreshold)
                            val lowBottom = yForBpm(yMin)

                            drawRect(
                                color = Color(0xFF4CAF50).copy(alpha = 0.08f),
                                topLeft = Offset(left, normalTop),
                                size = Size(chartWidth, normalBottom - normalTop)
                            )

                            drawRect(
                                color = Color(0xFFFF9800).copy(alpha = 0.08f),
                                topLeft = Offset(left, highTop),
                                size = Size(chartWidth, highBottom - highTop)
                            )

                            drawRect(
                                color = Color(0xFFE53935).copy(alpha = 0.08f),
                                topLeft = Offset(left, lowTop),
                                size = Size(chartWidth, lowBottom - lowTop)
                            )

                            // Grid ngang + label bpm
                            listOf(40, 60, 80, 100, 120, 140).forEach { value ->
                                val y = yForBpm(value.toFloat())

                                drawLine(
                                    color = Color(0xFFE0E0E0),
                                    start = Offset(left, y),
                                    end = Offset(right, y),
                                    strokeWidth = 1.dp.toPx()
                                )

                                drawContext.canvas.nativeCanvas.drawText(
                                    value.toString(),
                                    left - 8.dp.toPx(),
                                    y + 4.dp.toPx(),
                                    labelPaint
                                )
                            }

                            drawLine(
                                color = Color(0xFFFF9800),
                                start = Offset(left, yForBpm(highThreshold)),
                                end = Offset(right, yForBpm(highThreshold)),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )

                            drawLine(
                                color = Color(0xFF1976D2),
                                start = Offset(left, yForBpm(lowThreshold)),
                                end = Offset(right, yForBpm(lowThreshold)),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )

                            val dayStepPx = dayStep.toPx()
                            val labeledDays = setOf(1, 5, 10, 15, 20, 25, 30)

                            monthlyStats.forEachIndexed { index, stat ->
                                val x = left + dayStepPx * index
                                val dayNumber = index + 1

                                if (labeledDays.contains(dayNumber)) {
                                    val dayLabel = String.format(Locale.getDefault(), "%02d", dayNumber)
                                    drawContext.canvas.nativeCanvas.drawText(
                                        dayLabel,
                                        x,
                                        bottom + 22.dp.toPx(),
                                        labelPaint
                                    )
                                }

                                if (stat.count == 0) {
                                    if (labeledDays.contains(dayNumber)) {
                                        drawContext.canvas.nativeCanvas.drawText(
                                            "--",
                                            x,
                                            bottom + 36.dp.toPx(),
                                            emptyTextPaint
                                        )
                                    }
                                    return@forEachIndexed
                                }

                                val min = stat.min?.toFloat() ?: return@forEachIndexed
                                val max = stat.max?.toFloat() ?: return@forEachIndexed
                                val avg = stat.average?.toFloat() ?: return@forEachIndexed

                                drawLine(
                                    color = Color(0xFFE53935),
                                    start = Offset(x, yForBpm(max)),
                                    end = Offset(x, yForBpm(min)),
                                    strokeWidth = 2.dp.toPx()
                                )

                                val pointColor = when {
                                    avg < 60f -> Color(0xFFE53935)
                                    avg > 100f -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }

                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = Offset(x, yForBpm(avg))
                                )

                                drawCircle(
                                    color = pointColor,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(x, yForBpm(avg))
                                )
                            }
                        }
                    }
                } else {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        val yMin = 40f
                        val yMax = 140f
                        val lowThreshold = 60f
                        val highThreshold = 100f

                        val left = 38.dp.toPx()
                        val right = size.width - 8.dp.toPx()
                        val top = 12.dp.toPx()
                        val bottom = size.height - 30.dp.toPx()

                        val chartWidth = right - left
                        val chartHeight = bottom - top

                        fun yForBpm(bpm: Float): Float {
                            val ratio = ((bpm - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
                            return bottom - ratio * chartHeight
                        }

                        val labelPaint = Paint().apply {
                            color = android.graphics.Color.rgb(100, 106, 120)
                            textSize = 11.sp.toPx()
                            textAlign = Paint.Align.RIGHT
                            isAntiAlias = true
                        }

                        val emptyTextPaint = Paint().apply {
                            color = android.graphics.Color.rgb(140, 146, 160)
                            textSize = 12.sp.toPx()
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        // Grid ngang + label bpm
                        listOf(40, 60, 80, 100, 120, 140).forEach { value ->
                            val y = yForBpm(value.toFloat())

                            drawLine(
                                color = Color(0xFFE0E0E0),
                                start = Offset(left, y),
                                end = Offset(right, y),
                                strokeWidth = 1.dp.toPx()
                            )

                            drawContext.canvas.nativeCanvas.drawText(
                                value.toString(),
                                left - 8.dp.toPx(),
                                y + 4.dp.toPx(),
                                labelPaint
                            )
                        }

                        // Ngưỡng cao 100 bpm
                        drawLine(
                            color = Color(0xFFFF9800),
                            start = Offset(left, yForBpm(highThreshold)),
                            end = Offset(right, yForBpm(highThreshold)),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )

                        // Ngưỡng thấp 60 bpm
                        drawLine(
                            color = Color(0xFF1976D2),
                            start = Offset(left, yForBpm(lowThreshold)),
                            end = Offset(right, yForBpm(lowThreshold)),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )

                        if (range == HeartRateRange.WEEK) {
                            val labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                            val step = if (chartWidth > 0f) chartWidth / 6f else 0f

                            weeklyStats.forEachIndexed { index, stat ->
                                val x = left + step * index

                                drawContext.canvas.nativeCanvas.drawText(
                                    labels.getOrNull(index) ?: "",
                                    x,
                                    bottom + 22.dp.toPx(),
                                    labelPaint
                                )

                                if (stat.count == 0) {
                                    drawContext.canvas.nativeCanvas.drawText(
                                        "--",
                                        x,
                                        bottom + 38.dp.toPx(),
                                        labelPaint
                                    )
                                    return@forEachIndexed
                                }

                                val min = stat.min?.toFloat() ?: return@forEachIndexed
                                val max = stat.max?.toFloat() ?: return@forEachIndexed
                                val avg = stat.average?.toFloat() ?: return@forEachIndexed

                                drawLine(
                                    color = Color(0xFFE53935),
                                    start = Offset(x, yForBpm(max)),
                                    end = Offset(x, yForBpm(min)),
                                    strokeWidth = 2.dp.toPx()
                                )

                                val pointColor = when {
                                    avg < 60f -> Color(0xFFE53935)
                                    avg > 100f -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }

                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = Offset(x, yForBpm(avg))
                                )

                                drawCircle(
                                    color = pointColor,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(x, yForBpm(avg))
                                )
                            }
                        } else {
                            fun xForTime(timeMillis: Long): Float {
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = timeMillis

                                val secondsInDay =
                                    calendar.get(Calendar.HOUR_OF_DAY) * 3600 +
                                    calendar.get(Calendar.MINUTE) * 60 +
                                    calendar.get(Calendar.SECOND)

                                val ratio = secondsInDay / 86400f
                                return left + ratio * chartWidth
                            }

                            // Grid dọc + label giờ
                            listOf(0, 6, 12, 18, 24).forEach { hour ->
                                val x = left + (hour / 24f) * chartWidth

                                drawLine(
                                    color = Color(0xFFF0F0F0),
                                    start = Offset(x, top),
                                    end = Offset(x, bottom),
                                    strokeWidth = 1.dp.toPx()
                                )

                                drawContext.canvas.nativeCanvas.drawText(
                                    "${hour}h",
                                    x,
                                    bottom + 22.dp.toPx(),
                                    labelPaint
                                )
                            }

                            if (points.isEmpty()) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    "Chưa có dữ liệu cho ngày này",
                                    size.width / 2f,
                                    size.height / 2f,
                                    emptyTextPaint
                                )
                            } else {
                                val sortedPoints = points.sortedBy { it.timeMillis }

                                val offsets = sortedPoints.map {
                                    Offset(
                                        x = xForTime(it.timeMillis),
                                        y = yForBpm(it.bpm.toFloat())
                                    )
                                }

                                if (offsets.size >= 2) {
                                    val linePath = Path().apply {
                                        moveTo(offsets.first().x, offsets.first().y)
                                        offsets.drop(1).forEach {
                                            lineTo(it.x, it.y)
                                        }
                                    }

                                    val fillPath = Path().apply {
                                        moveTo(offsets.first().x, bottom)
                                        lineTo(offsets.first().x, offsets.first().y)
                                        offsets.drop(1).forEach {
                                            lineTo(it.x, it.y)
                                        }
                                        lineTo(offsets.last().x, bottom)
                                        close()
                                    }

                                    drawPath(
                                        path = fillPath,
                                        color = Color(0xFFE53935).copy(alpha = 0.12f)
                                    )

                                    drawPath(
                                        path = linePath,
                                        color = Color(0xFFE53935),
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                }

                                offsets.forEachIndexed { index, offset ->
                                    val bpm = sortedPoints[index].bpm

                                    val pointColor = when {
                                        bpm < 60 -> Color(0xFFE53935)
                                        bpm > 100 -> Color(0xFFFF9800)
                                        else -> Color(0xFF4CAF50)
                                    }

                                    drawCircle(
                                        color = Color.White,
                                        radius = 5.dp.toPx(),
                                        center = offset
                                    )

                                    drawCircle(
                                        color = pointColor,
                                        radius = 3.5.dp.toPx(),
                                        center = offset
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                navController.navigate(Screen.HeartRateHistory.route)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = CircleShape,
            border = BorderStroke(2.dp, PrimaryBlue)
        ) {
            Icon(
                Icons.Default.History,
                null,
                modifier = Modifier.size(18.dp),
                tint = PrimaryBlue
            )

            Spacer(Modifier.width(8.dp))

            Text(
                "Xem lịch sử dữ liệu",
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        }
    }
}

@Composable
fun MonthlyDayDetailCard(stat: MonthlyHeartRateDayStat) {
    val dateText = if (stat.dayStartMillis > 0L) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(stat.dayStartMillis))
    } else {
        "--"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBg,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE8EAED))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Chi tiết ngày $dateText",
                fontWeight = FontWeight.Bold,
                color = OnSurfaceColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Trung bình", color = OnSurfaceVariant, fontSize = 12.sp)
                Text(stat.average?.toString() ?: "--", fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Cao nhất", color = OnSurfaceVariant, fontSize = 12.sp)
                Text(stat.max?.toString() ?: "--", fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Thấp nhất", color = OnSurfaceVariant, fontSize = 12.sp)
                Text(stat.min?.toString() ?: "--", fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Số lần đo", color = OnSurfaceVariant, fontSize = 12.sp)
                Text(stat.count.toString(), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ChartLegendItem(
    color: Color,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(Modifier.width(4.dp))

        Text(
            text = text,
            fontSize = 10.sp,
            color = OnSurfaceVariant
        )
    }
}

@Composable
fun StatsGrid(
    stats: HeartRateDailyStats
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatItem(
            label = "Trung bình",
            value = stats.average?.toString() ?: "--",
            modifier = Modifier.weight(1f)
        )

        StatItem(
            label = "Cao nhất",
            value = stats.max?.toString() ?: "--",
            modifier = Modifier.weight(1f),
            valueColor = PrimaryBlue
        )

        StatItem(
            label = "Thấp nhất",
            value = stats.min?.toString() ?: "--",
            modifier = Modifier.weight(1f),
            valueColor = PrimaryBlue
        )
    }
}


@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier,
    valueColor: Color = OnSurfaceColor
) {
    Surface(
        modifier = modifier.height(82.dp),
        color = CardBg,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFE8EAED)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = valueColor
                )

                Spacer(Modifier.width(3.dp))

                Text(
                    text = "bpm",
                    fontSize = 11.sp,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
fun InsightCard(title: String, content: String, icon: ImageVector, containerColor: Color, iconBg: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, iconBg.copy(alpha = 0.2f))
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = iconBg) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Column {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = iconBg.copy(alpha = 0.8f))
                Spacer(Modifier.height(4.dp))
                Text(content, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OnSurfaceColor)
            }
        }
    }
}
/*
@Composable
fun ExpertTipCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PrimaryBlue,
        shape = RoundedCornerShape(24.dp)
    ) {
        Box {
            Column(Modifier.padding(24.dp).fillMaxWidth(0.7f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape) {
                    Icon(Icons.Default.Lightbulb, null, tint = Color.White, modifier = Modifier.padding(6.dp).size(20.dp))
                }
                Text("Cải thiện hồi phục tim", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Các bài tập cường độ thấp xen kẽ giúp tim phục hồi nhanh hơn sau khi gắng sức.", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.White), contentPadding = PaddingValues(horizontal = 20.dp)) {
                    Text("Đọc chi tiết", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            // Image placeholder
            Icon(
                Icons.Default.Person, null,
                modifier = Modifier.align(Alignment.BottomEnd).size(120.dp).offset(x = 10.dp, y = 10.dp),
                tint = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}
*/
@Composable
fun ExpertTipCard(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Lightbulb,
    image: ImageVector = Icons.Default.Person
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PrimaryBlue,
        shape = RoundedCornerShape(24.dp)
    ) {
        Box {
            Column(
                Modifier
                    .padding(24.dp)
                    .fillMaxWidth(0.7f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Icon(
                        icon,
                        null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp)
                    )
                }

                Text(
                    title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    description,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )

                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Text(
                        "Đọc chi tiết",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                image,
                null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(120.dp)
                    .offset(x = 10.dp, y = 10.dp),
                tint = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}

data class HeartRateChartPoint(
    val timeMillis: Long,
    val bpm: Double
)

data class WeeklyHeartRateDayStat(
    val dayStartMillis: Long,
    val average: Int? = null,
    val max: Int? = null,
    val min: Int? = null,
    val count: Int = 0
)

data class MonthlyHeartRateDayStat(
    val dayStartMillis: Long,
    val average: Int? = null,
    val max: Int? = null,
    val min: Int? = null,
    val count: Int = 0
)

data class HeartRateDailyStats(
    val average: Int? = null,
    val max: Int? = null,
    val min: Int? = null,
    val count: Int = 0
)

private fun calculateHeartRateDailyStats(
    measurements: List<MeasurementModel>
): HeartRateDailyStats {
    if (measurements.isEmpty()) {
        return HeartRateDailyStats()
    }

    val values = measurements.map { it.heartRateAvg }

    return HeartRateDailyStats(
        average = values.average().roundToInt(),
        max = values.maxOrNull()?.roundToInt(),
        min = values.minOrNull()?.roundToInt(),
        count = values.size
    )
}

private fun calculateHeartRateWeeklyStats(
    measurements: List<MeasurementModel>,
    weekStartMillis: Long
): List<WeeklyHeartRateDayStat> {
    if (weekStartMillis <= 0L) {
        return List(7) { index ->
            WeeklyHeartRateDayStat(dayStartMillis = weekStartMillis + index)
        }
    }

    val valid = measurements.filter { it.isValidHeartRateMeasurement() }
    val weekEnd = addDays(weekStartMillis, 7)
    val inWeek = valid.filter {
        val time = measurementTimeMillis(it)
        time in weekStartMillis until weekEnd
    }

    return (0..6).map { dayIndex ->
        val dayStart = addDays(weekStartMillis, dayIndex)
        val dayMeasurements = inWeek.filter { isMeasurementInDay(it, dayStart) }

        if (dayMeasurements.isEmpty()) {
            WeeklyHeartRateDayStat(dayStartMillis = dayStart)
        } else {
            val values = dayMeasurements.map { it.heartRateAvg }
            WeeklyHeartRateDayStat(
                dayStartMillis = dayStart,
                average = values.average().roundToInt(),
                max = values.maxOrNull()?.roundToInt(),
                min = values.minOrNull()?.roundToInt(),
                count = values.size
            )
        }
    }
}

private fun calculateHeartRateMonthlyStats(
    measurements: List<MeasurementModel>,
    monthStartMillis: Long
): List<MonthlyHeartRateDayStat> {
    if (monthStartMillis <= 0L) {
        return emptyList()
    }

    val valid = measurements.filter { it.isValidHeartRateMeasurement() }
    val dayCount = daysInMonth(monthStartMillis)
    val monthEnd = addDays(monthStartMillis, dayCount)
    val inMonth = valid.filter {
        val time = measurementTimeMillis(it)
        time in monthStartMillis until monthEnd
    }

    return (0 until dayCount).map { dayIndex ->
        val dayStart = addDays(monthStartMillis, dayIndex)
        val dayMeasurements = inMonth.filter { isMeasurementInDay(it, dayStart) }

        if (dayMeasurements.isEmpty()) {
            MonthlyHeartRateDayStat(dayStartMillis = dayStart)
        } else {
            val values = dayMeasurements.map { it.heartRateAvg }
            MonthlyHeartRateDayStat(
                dayStartMillis = dayStart,
                average = values.average().roundToInt(),
                max = values.maxOrNull()?.roundToInt(),
                min = values.minOrNull()?.roundToInt(),
                count = values.size
            )
        }
    }
}

private fun calculateHeartRateRangeStats(
    measurements: List<MeasurementModel>,
    rangeStartMillis: Long,
    rangeEndMillis: Long
): HeartRateDailyStats {
    if (rangeStartMillis <= 0L || rangeEndMillis <= 0L) {
        return HeartRateDailyStats()
    }

    val values = measurements
        .asSequence()
        .filter { it.isValidHeartRateMeasurement() }
        .filter {
            val time = measurementTimeMillis(it)
            time in rangeStartMillis until rangeEndMillis
        }
        .map { it.heartRateAvg }
        .toList()

    if (values.isEmpty()) {
        return HeartRateDailyStats()
    }

    return HeartRateDailyStats(
        average = values.average().roundToInt(),
        max = values.maxOrNull()?.roundToInt(),
        min = values.minOrNull()?.roundToInt(),
        count = values.size
    )
}

private fun buildHeartRateInsight(stats: HeartRateDailyStats): String {
    if (stats.count == 0 || stats.average == null) {
        return "Chưa có dữ liệu nhịp tim cho ngày này."
    }

    return when {
        stats.average < 60 -> "Nhịp tim trung bình thấp hơn mức bình thường."
        stats.average > 100 -> "Nhịp tim trung bình cao hơn mức bình thường."
        else -> "Nhịp tim trung bình nằm trong vùng ổn định."
    }
}

private fun buildHeartRateSuggestion(stats: HeartRateDailyStats): String {
    if (stats.count == 0 || stats.average == null) {
        return "Hãy đo nhịp tim ít nhất một lần để có gợi ý phù hợp."
    }

    return when {
        stats.average < 60 -> "Khởi động nhẹ và theo dõi thêm trước khi vận động mạnh."
        stats.average > 100 -> "Nghỉ ngơi, bổ sung nước và giảm cường độ hoạt động."
        else -> "Duy trì thói quen vận động và nghỉ ngơi cân bằng."
    }
}

/*
@Preview(showBackground = true)
@Composable
fun HeartRateDetailScreenPreview() {
    HeartRateDetailScreen()
}*/
