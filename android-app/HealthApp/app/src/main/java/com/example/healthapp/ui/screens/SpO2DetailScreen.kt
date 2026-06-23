package com.example.healthapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import com.example.healthapp.ui.theme.*


import com.example.healthapp.ui.navigation.Screen
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.healthapp.ui.components.CurrentMetricCard
import com.example.healthapp.ui.components.DateSelector
import com.example.healthapp.ui.components.TabSelector
import com.example.healthapp.ui.components.HeartRateRange
import com.example.healthapp.ui.components.addDays
import com.example.healthapp.ui.components.daysInMonth
import com.example.healthapp.ui.components.startOfMonth
import com.example.healthapp.ui.components.startOfWeek
import com.example.healthapp.data.model.MeasurementModel
import com.example.healthapp.viewmodel.Spo2ViewModel
import android.graphics.Paint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpO2DetailScreen(
    navController: NavController,
    spo2ViewModel: Spo2ViewModel = viewModel()
) {
    val currentHealth by spo2ViewModel.currentHealth.collectAsState()
    val measurements by spo2ViewModel.measurements.collectAsState()
    val selectedDateStartMillis by spo2ViewModel.selectedDateStartMillis.collectAsState()

    var selectedRange by remember { mutableStateOf(HeartRateRange.DAY) }
    var selectedMonthDayIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedRange, selectedDateStartMillis) {
        when (selectedRange) {
            HeartRateRange.WEEK -> {
                val weekStart = startOfWeek(selectedDateStartMillis)
                if (weekStart > 0L && weekStart != selectedDateStartMillis) {
                    spo2ViewModel.selectDate(weekStart)
                }
            }
            HeartRateRange.MONTH -> {
                val monthStart = startOfMonth(selectedDateStartMillis)
                if (monthStart > 0L && monthStart != selectedDateStartMillis) {
                    spo2ViewModel.selectDate(monthStart)
                }
            }
            else -> Unit
        }
    }

    val selectedDayMeasurements = remember(measurements, selectedDateStartMillis) {
        measurements
            .filter { it.isValidSpO2Measurement() }
            .filter { isMeasurementInDay(it, selectedDateStartMillis) }
            .sortedBy { measurementTimeMillis(it) }
    }

    val chartPoints = remember(selectedDayMeasurements) {
        selectedDayMeasurements.map {
            SpO2ChartPoint(
                timeMillis = measurementTimeMillis(it),
                value = it.spo2Avg
            )
        }
    }

    val weekStartMillis = remember(selectedDateStartMillis) {
        startOfWeek(selectedDateStartMillis)
    }

    val weeklyStats = remember(measurements, weekStartMillis) {
        calculateSpO2WeeklyStats(measurements, weekStartMillis)
    }

    val monthStartMillis = remember(selectedDateStartMillis) {
        startOfMonth(selectedDateStartMillis)
    }

    val monthlyStats = remember(measurements, monthStartMillis) {
        calculateSpO2MonthlyStats(measurements, monthStartMillis)
    }

    val weeklyRangeStats = remember(measurements, weekStartMillis) {
        val weekEnd = addDays(weekStartMillis, 7)
        calculateSpO2RangeStats(measurements, weekStartMillis, weekEnd)
    }

    val monthlyRangeStats = remember(measurements, monthStartMillis) {
        val monthEnd = addDays(monthStartMillis, daysInMonth(monthStartMillis))
        calculateSpO2RangeStats(measurements, monthStartMillis, monthEnd)
    }

    LaunchedEffect(monthStartMillis) {
        selectedMonthDayIndex = null
    }

    val selectedMonthlyStat = selectedMonthDayIndex?.let { index ->
        monthlyStats.getOrNull(index)
    }

    val dailyStats = remember(selectedDayMeasurements) {
        calculateSpO2DailyStats(selectedDayMeasurements)
    }

    val rangeStats = remember(selectedRange, dailyStats, weeklyRangeStats, monthlyRangeStats) {
        when (selectedRange) {
            HeartRateRange.WEEK -> weeklyRangeStats
            HeartRateRange.MONTH -> monthlyRangeStats
            else -> dailyStats
        }
    }

    val spo2Value = currentHealth.spo2
    val spo2Text = if (spo2Value > 0) spo2Value.toInt().toString() else "--"
    val status = spo2StatusUi(spo2Value)
    val lastMeasureText = formatLastMeasureTime(
        if (currentHealth.lastVitalMeasureTime > 0L) currentHealth.lastVitalMeasureTime else currentHealth.lastMeasureTime
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chi tiết SpO2",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryBlue)
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

            item {
                CurrentMetricCard(
                    title = "SpO2 hiện tại",
                    valueText = spo2Text,
                    unitText = "%",
                    statusLabel = status.label,
                    statusTextColor = status.textColor,
                    statusBgColor = status.bgColor,
                    statusBorderColor = status.borderColor,
                    lastMeasureText = lastMeasureText,
                    icon = Icons.Default.WaterDrop,
                    iconTint = PrimaryBlue,
                    valueColor = PrimaryBlue
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TabSelector(
                        selectedRange = selectedRange,
                        onRangeSelected = { selectedRange = it }
                    )

                    DateSelector(
                        selectedDateStartMillis = selectedDateStartMillis,
                        selectedRange = selectedRange,
                        onDateSelected = spo2ViewModel::selectDate
                    )
                }
            }

            item {
                SpO2ChartCard(
                    navController = navController,
                    range = selectedRange,
                    points = chartPoints,
                    weeklyStats = weeklyStats,
                    monthlyStats = monthlyStats,
                    onMonthDaySelected = { index ->
                        val stat = monthlyStats.getOrNull(index) ?: return@SpO2ChartCard
                        if (stat.count > 0) {
                            selectedMonthDayIndex = index
                        }
                    }
                )
            }

            if (selectedRange == HeartRateRange.MONTH && selectedMonthlyStat != null) {
                item {
                    SpO2MonthlyDayDetailCard(stat = selectedMonthlyStat)
                }
            }

            item {
                SpO2StatsGrid(stats = rangeStats)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InsightCard(
                        title = "NHẬN XÉT HÔM NAY",
                        content = buildSpO2Insight(rangeStats),
                        icon = Icons.Default.TrendingUp,
                        containerColor = Color(0xFFE3F2FD),
                        iconBg = Color(0xFF1976D2)
                    )
                    InsightCard(
                        title = "GỢI Ý CHO BẠN",
                        content = buildSpO2Suggestion(rangeStats),
                        icon = Icons.Default.Spa,
                        containerColor = Color(0xFFE8F5E9),
                        iconBg = Color(0xFF2E7D32)
                    )
                }
            }

            item {
                ExpertTipCard(
                    title = "Hiểu về SpO2",
                    description = "Cách cải thiện lượng oxy trong máu tự nhiên qua hít thở.",
                    icon = Icons.Default.WaterDrop
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

private data class SpO2StatusUi(
    val label: String,
    val textColor: Color,
    val bgColor: Color,
    val borderColor: Color
)

private fun spo2StatusUi(spo2: Double): SpO2StatusUi {
    return when {
        spo2 <= 0 -> SpO2StatusUi(
            label = "Chưa có",
            textColor = OnSurfaceVariant,
            bgColor = Color(0xFFE0E0E0),
            borderColor = Color(0xFFBDBDBD)
        )
        spo2 >= 95 -> SpO2StatusUi(
            label = "Ổn định",
            textColor = SuccessGreen,
            bgColor = Color(0xFFE8F5E9),
            borderColor = Color(0xFFC8E6C9)
        )
        spo2 >= 90 -> SpO2StatusUi(
            label = "Cần chú ý",
            textColor = Color(0xFFEF6C00),
            bgColor = Color(0xFFFFF3E0),
            borderColor = Color(0xFFFFE0B2)
        )
        else -> SpO2StatusUi(
            label = "Thấp",
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
fun SpO2ChartCard(
    navController: NavController,
    range: HeartRateRange,
    points: List<SpO2ChartPoint>,
    weeklyStats: List<WeeklySpO2DayStat>,
    monthlyStats: List<MonthlySpO2DayStat>,
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
                    text = "Biểu đồ SpO2",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceColor
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendItem(Color(0xFF4CAF50).copy(alpha = 0.2f), "Ổn định")
                    ChartLegendItem(Color(0xFFFF9800).copy(alpha = 0.2f), "Cần chú ý")
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
                            val yMin = 85f
                            val yMax = 100f
                            val warnThreshold = 90f
                            val stableThreshold = 95f

                            val left = 38.dp.toPx()
                            val right = size.width - 8.dp.toPx()
                            val top = 12.dp.toPx()
                            val bottom = size.height - 30.dp.toPx()

                            val chartWidth = right - left
                            val chartHeight = bottom - top

                            fun yForValue(value: Float): Float {
                                val ratio = ((value - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
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

                            val normalTop = yForValue(stableThreshold)
                            val normalBottom = yForValue(yMax)
                            val warnTop = yForValue(warnThreshold)
                            val warnBottom = yForValue(stableThreshold)
                            val lowTop = yForValue(yMin)
                            val lowBottom = yForValue(warnThreshold)

                            drawRect(
                                color = Color(0xFF4CAF50).copy(alpha = 0.08f),
                                topLeft = Offset(left, normalTop),
                                size = Size(chartWidth, normalBottom - normalTop)
                            )

                            drawRect(
                                color = Color(0xFFFF9800).copy(alpha = 0.08f),
                                topLeft = Offset(left, warnTop),
                                size = Size(chartWidth, warnBottom - warnTop)
                            )

                            drawRect(
                                color = Color(0xFFE53935).copy(alpha = 0.08f),
                                topLeft = Offset(left, lowTop),
                                size = Size(chartWidth, lowBottom - lowTop)
                            )

                            listOf(100, 95, 90, 85).forEach { value ->
                                val y = yForValue(value.toFloat())

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
                                color = Color(0xFF2E7D32),
                                start = Offset(left, yForValue(stableThreshold)),
                                end = Offset(right, yForValue(stableThreshold)),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )

                            drawLine(
                                color = Color(0xFFE53935),
                                start = Offset(left, yForValue(warnThreshold)),
                                end = Offset(right, yForValue(warnThreshold)),
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
                                    color = Color(0xFF1976D2),
                                    start = Offset(x, yForValue(max)),
                                    end = Offset(x, yForValue(min)),
                                    strokeWidth = 2.dp.toPx()
                                )

                                val pointColor = when {
                                    avg < 90f -> Color(0xFFE53935)
                                    avg < 95f -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }

                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = Offset(x, yForValue(avg))
                                )

                                drawCircle(
                                    color = pointColor,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(x, yForValue(avg))
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
                        val yMin = 85f
                        val yMax = 100f
                        val warnThreshold = 90f
                        val stableThreshold = 95f

                        val left = 38.dp.toPx()
                        val right = size.width - 8.dp.toPx()
                        val top = 12.dp.toPx()
                        val bottom = size.height - 30.dp.toPx()

                        val chartWidth = right - left
                        val chartHeight = bottom - top

                        fun yForValue(value: Float): Float {
                            val ratio = ((value - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
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

                        val normalTop = yForValue(stableThreshold)
                        val normalBottom = yForValue(yMax)
                        val warnTop = yForValue(warnThreshold)
                        val warnBottom = yForValue(stableThreshold)
                        val lowTop = yForValue(yMin)
                        val lowBottom = yForValue(warnThreshold)

                        drawRect(
                            color = Color(0xFF4CAF50).copy(alpha = 0.08f),
                            topLeft = Offset(left, normalTop),
                            size = Size(chartWidth, normalBottom - normalTop)
                        )

                        drawRect(
                            color = Color(0xFFFF9800).copy(alpha = 0.08f),
                            topLeft = Offset(left, warnTop),
                            size = Size(chartWidth, warnBottom - warnTop)
                        )

                        drawRect(
                            color = Color(0xFFE53935).copy(alpha = 0.08f),
                            topLeft = Offset(left, lowTop),
                            size = Size(chartWidth, lowBottom - lowTop)
                        )

                        listOf(100, 95, 90, 85).forEach { value ->
                            val y = yForValue(value.toFloat())

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
                            color = Color(0xFF2E7D32),
                            start = Offset(left, yForValue(stableThreshold)),
                            end = Offset(right, yForValue(stableThreshold)),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )

                        drawLine(
                            color = Color(0xFFE53935),
                            start = Offset(left, yForValue(warnThreshold)),
                            end = Offset(right, yForValue(warnThreshold)),
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
                                    color = Color(0xFF1976D2),
                                    start = Offset(x, yForValue(max)),
                                    end = Offset(x, yForValue(min)),
                                    strokeWidth = 2.dp.toPx()
                                )

                                val pointColor = when {
                                    avg < 90f -> Color(0xFFE53935)
                                    avg < 95f -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }

                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = Offset(x, yForValue(avg))
                                )

                                drawCircle(
                                    color = pointColor,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(x, yForValue(avg))
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
                                        y = yForValue(it.value.toFloat())
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
                                        color = Color(0xFF1976D2).copy(alpha = 0.12f)
                                    )

                                    drawPath(
                                        path = linePath,
                                        color = Color(0xFF1976D2),
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                }

                                offsets.forEachIndexed { index, offset ->
                                    val value = sortedPoints[index].value

                                    val pointColor = when {
                                        value < 90 -> Color(0xFFE53935)
                                        value < 95 -> Color(0xFFFF9800)
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
                navController.navigate(Screen.SpO2History.route)
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
fun SpO2MonthlyDayDetailCard(stat: MonthlySpO2DayStat) {
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
fun SpO2StatsGrid(stats: SpO2DailyStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SpO2StatItem(
            label = "Trung bình",
            value = stats.average?.toString() ?: "--",
            modifier = Modifier.weight(1f)
        )

        SpO2StatItem(
            label = "Cao nhất",
            value = stats.max?.toString() ?: "--",
            modifier = Modifier.weight(1f),
            valueColor = PrimaryBlue
        )

        SpO2StatItem(
            label = "Thấp nhất",
            value = stats.min?.toString() ?: "--",
            modifier = Modifier.weight(1f),
            valueColor = PrimaryBlue
        )
    }
}

@Composable
private fun SpO2StatItem(
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
                    text = "%",
                    fontSize = 11.sp,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

private fun buildSpO2Insight(stats: SpO2DailyStats): String {
    if (stats.count == 0 || stats.average == null) {
        return "Chưa có dữ liệu SpO2 cho ngày này."
    }

    return when {
        stats.average >= 95 -> "SpO2 trung bình ổn định trong vùng bình thường."
        stats.average >= 90 -> "SpO2 trung bình thấp hơn mức tối ưu, cần chú ý."
        else -> "SpO2 trung bình thấp, nên theo dõi thêm."
    }
}

private fun buildSpO2Suggestion(stats: SpO2DailyStats): String {
    if (stats.count == 0 || stats.average == null) {
        return "Hãy đo SpO2 ít nhất một lần để có gợi ý phù hợp."
    }

    return when {
        stats.average >= 95 -> "Duy trì nhịp thở sâu và đều đặn."
        stats.average >= 90 -> "Nghỉ ngơi và hít thở sâu để cải thiện SpO2."
        else -> "Nên nghỉ ngơi, hít thở sâu và theo dõi thêm."
    }
}

data class SpO2ChartPoint(
    val timeMillis: Long,
    val value: Double
)

data class WeeklySpO2DayStat(
    val dayStartMillis: Long,
    val average: Int? = null,
    val max: Int? = null,
    val min: Int? = null,
    val count: Int = 0
)

data class MonthlySpO2DayStat(
    val dayStartMillis: Long,
    val average: Int? = null,
    val max: Int? = null,
    val min: Int? = null,
    val count: Int = 0
)

data class SpO2DailyStats(
    val average: Int? = null,
    val max: Int? = null,
    val min: Int? = null,
    val count: Int = 0
)

private fun MeasurementModel.isValidSpO2Measurement(): Boolean {
    val completed = status.isBlank() || status.equals("completed", ignoreCase = true)

    return completed &&
        spo2Avg in 70.0..100.0 &&
        measurementTimeMillis(this) > 0L
}

private fun isMeasurementInDay(
    measurement: MeasurementModel,
    selectedDateStartMillis: Long
): Boolean {
    val time = measurementTimeMillis(measurement)
    val start = selectedDateStartMillis
    val end = addDays(start, 1)

    return time in start until end
}

private fun measurementTimeMillis(
    measurement: MeasurementModel
): Long {
    val rawTime = if (measurement.endTime > 0L) {
        measurement.endTime
    } else {
        measurement.startTime
    }

    return normalizeTimestampMillis(rawTime)
}

private fun normalizeTimestampMillis(timestamp: Long): Long {
    if (timestamp <= 0L) return 0L

    return if (timestamp in 1..9_999_999_999L) {
        timestamp * 1000L
    } else {
        timestamp
    }
}

private fun calculateSpO2DailyStats(
    measurements: List<MeasurementModel>
): SpO2DailyStats {
    if (measurements.isEmpty()) {
        return SpO2DailyStats()
    }

    val values = measurements.map { it.spo2Avg }

    return SpO2DailyStats(
        average = values.average().roundToInt(),
        max = values.maxOrNull()?.roundToInt(),
        min = values.minOrNull()?.roundToInt(),
        count = values.size
    )
}

private fun calculateSpO2WeeklyStats(
    measurements: List<MeasurementModel>,
    weekStartMillis: Long
): List<WeeklySpO2DayStat> {
    if (weekStartMillis <= 0L) {
        return List(7) { index ->
            WeeklySpO2DayStat(dayStartMillis = weekStartMillis + index)
        }
    }

    val valid = measurements.filter { it.isValidSpO2Measurement() }
    val weekEnd = addDays(weekStartMillis, 7)
    val inWeek = valid.filter {
        val time = measurementTimeMillis(it)
        time in weekStartMillis until weekEnd
    }

    return (0..6).map { dayIndex ->
        val dayStart = addDays(weekStartMillis, dayIndex)
        val dayMeasurements = inWeek.filter { isMeasurementInDay(it, dayStart) }

        if (dayMeasurements.isEmpty()) {
            WeeklySpO2DayStat(dayStartMillis = dayStart)
        } else {
            val values = dayMeasurements.map { it.spo2Avg }
            WeeklySpO2DayStat(
                dayStartMillis = dayStart,
                average = values.average().roundToInt(),
                max = values.maxOrNull()?.roundToInt(),
                min = values.minOrNull()?.roundToInt(),
                count = values.size
            )
        }
    }
}

private fun calculateSpO2MonthlyStats(
    measurements: List<MeasurementModel>,
    monthStartMillis: Long
): List<MonthlySpO2DayStat> {
    if (monthStartMillis <= 0L) {
        return emptyList()
    }

    val valid = measurements.filter { it.isValidSpO2Measurement() }
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
            MonthlySpO2DayStat(dayStartMillis = dayStart)
        } else {
            val values = dayMeasurements.map { it.spo2Avg }
            MonthlySpO2DayStat(
                dayStartMillis = dayStart,
                average = values.average().roundToInt(),
                max = values.maxOrNull()?.roundToInt(),
                min = values.minOrNull()?.roundToInt(),
                count = values.size
            )
        }
    }
}

private fun calculateSpO2RangeStats(
    measurements: List<MeasurementModel>,
    rangeStartMillis: Long,
    rangeEndMillis: Long
): SpO2DailyStats {
    if (rangeStartMillis <= 0L || rangeEndMillis <= 0L) {
        return SpO2DailyStats()
    }

    val values = measurements
        .asSequence()
        .filter { it.isValidSpO2Measurement() }
        .filter {
            val time = measurementTimeMillis(it)
            time in rangeStartMillis until rangeEndMillis
        }
        .map { it.spo2Avg }
        .toList()

    if (values.isEmpty()) {
        return SpO2DailyStats()
    }

    return SpO2DailyStats(
        average = values.average().roundToInt(),
        max = values.maxOrNull()?.roundToInt(),
        min = values.minOrNull()?.roundToInt(),
        count = values.size
    )
}
