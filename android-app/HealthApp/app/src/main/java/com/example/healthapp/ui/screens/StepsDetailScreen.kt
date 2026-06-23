/*
package com.example.healthapp.ui.screens


import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import com.example.healthapp.ui.theme.*
import com.example.healthapp.ui.components.BottomNavBar
import com.example.healthapp.ui.components.BottomTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsDetailScreen(navController: NavController) {

    Scaffold(

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chi tiết bước chân",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = PrimaryBlue)
                    }
                }
            )
        },

        bottomBar = {
            BottomNavBar(selected = BottomTab.STATS,navController = navController)
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

            item { CurrentStepsCard() }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    //TabSelector()
                   // DateSelector()
                }
            }

            item { StepsChartCard() }

            item { StepsProgressCard() }

            item {

                InsightCard(
                    title = "NHẬN XÉT HÔM NAY",
                    content = "Bạn đã hoàn thành mục tiêu hôm nay",
                    icon = Icons.Default.CheckCircle,
                    containerColor = Color(0xFFE3F2FD),
                    iconBg = PrimaryBlue
                )

            }

            item {

                ExpertTipCard(
                    title = "GỢI Ý DÀNH CHO BẠN",
                    description = "Tiếp tục duy trì thói quen vận động để cải thiện sức khỏe tim mạch.",
                    icon = Icons.Default.Lightbulb
                )

            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

        }

    }

}


@Composable
fun CurrentStepsCard() {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {

        Surface(
            shape = CircleShape,
            color = PrimaryBlue.copy(alpha = 0.1f)
        ) {
            Icon(
                Icons.Default.DirectionsWalk,
                null,
                tint = PrimaryBlue,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.Bottom) {

            Text(
                "8.432",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                "bước",
                color = OnSurfaceVariant
            )

        }

        Text(
            "Mục tiêu: 10.000 bước",
            fontSize = 12.sp,
            color = OnSurfaceVariant
        )

    }

}



@Composable
fun StepsChartCard() {

    Column {



    }

}


@Composable
fun StepsProgressCard() {

    val progress = 0.84f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardBg
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                "TIẾN ĐỘ HIỆN TẠI",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    "8.432 / 10.000 bước",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "84%",
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen,
                    fontSize = 20.sp
                )

            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.LightGray)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    PrimaryBlue,
                                    SuccessGreen
                                )
                            )
                        )
                )

            }

        }

    }

}
/*
@Preview(showBackground = true)
@Composable
fun StepsDetailPreview() {
    StepsDetailScreen()
}*/
*/
package com.example.healthapp.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.healthapp.data.model.StepHistoryModel
import com.example.healthapp.ui.components.CurrentMetricCard
import com.example.healthapp.ui.components.DateSelector
import com.example.healthapp.ui.components.HeartRateRange
import com.example.healthapp.ui.components.TabSelector
import com.example.healthapp.ui.navigation.Screen
import com.example.healthapp.ui.theme.CardBg
import com.example.healthapp.ui.theme.OnSurfaceColor
import com.example.healthapp.ui.theme.OnSurfaceVariant
import com.example.healthapp.ui.theme.PrimaryBlue
import com.example.healthapp.ui.theme.SuccessGreen
import com.example.healthapp.ui.theme.SurfaceColor
import com.example.healthapp.viewmodel.StepsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsDetailScreen(
    navController: NavController,
    stepsViewModel: StepsViewModel = viewModel()
) {
    val currentHealth by stepsViewModel.currentHealth.collectAsState()
    val userProfile by stepsViewModel.userProfile.collectAsState()
    val stepsHistory by stepsViewModel.stepsHistory.collectAsState()

    var selectedRange by rememberSaveable {
        mutableStateOf(HeartRateRange.DAY)
    }

    val selectedDateRaw by stepsViewModel.selectedDateMillis.collectAsState()
    val selectedDateStartMillis = remember(selectedDateRaw) {
        startOfDayMillis(selectedDateRaw)
    }

    val stepGoal = if (userProfile.stepGoal > 0L) {
        userProfile.stepGoal
    } else {
        10000L
    }

    val selectedDateKey = formatDateKey(selectedDateStartMillis)

    val selectedDaySteps = getStepsForDate(
        dateKey = selectedDateKey,
        stepsHistory = stepsHistory,
        currentSteps = currentHealth.steps,
        selectedDateMillis = selectedDateStartMillis
    )

    val chartPoints = when (selectedRange) {
        HeartRateRange.DAY -> buildHourlyStepPoints(
            dateKey = selectedDateKey,
            stepsHistory = stepsHistory
        )

        HeartRateRange.WEEK -> buildWeeklyStepPoints(
            selectedDateMillis = selectedDateStartMillis,
            stepsHistory = stepsHistory,
            currentSteps = currentHealth.steps
        )

        HeartRateRange.MONTH -> buildMonthlyStepPoints(
            selectedDateMillis = selectedDateStartMillis,
            stepsHistory = stepsHistory,
            currentSteps = currentHealth.steps
        )
    }

    val stats = when (selectedRange) {
        HeartRateRange.DAY -> buildDayStepStats(
            steps = selectedDaySteps,
            stepGoal = stepGoal
        )

        HeartRateRange.WEEK -> buildPeriodStepStats(
            points = chartPoints,
            stepGoal = stepGoal
        )

        HeartRateRange.MONTH -> buildPeriodStepStats(
            points = chartPoints,
            stepGoal = stepGoal
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chi tiết bước chân",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
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
                    title = "Bước chân hôm nay",
                    valueText = formatNumber(currentHealth.steps),
                    unitText = "bước",
                    statusLabel = "Đạt ${stats.progressPercent}%",
                    statusTextColor = if (stats.progressPercent >= 100) SuccessGreen else PrimaryBlue,
                    statusBgColor = if (stats.progressPercent >= 100) {
                        SuccessGreen.copy(alpha = 0.12f)
                    } else {
                        PrimaryBlue.copy(alpha = 0.12f)
                    },
                    statusBorderColor = if (stats.progressPercent >= 100) {
                        SuccessGreen.copy(alpha = 0.35f)
                    } else {
                        PrimaryBlue.copy(alpha = 0.35f)
                    },
                    lastMeasureText = "Cập nhật: ${formatTimeFromSeconds(currentHealth.lastStepUpdateTime)}",
                    icon = Icons.Default.DirectionsWalk,
                    iconTint = PrimaryBlue,
                    valueColor = PrimaryBlue
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
                        onDateSelected = { stepsViewModel.selectDate(it) }
                    )
                }
            }

            item {
                StepsChartCard(
                    selectedRange = selectedRange,
                    points = chartPoints,
                    stepGoal = stepGoal
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OutlinedButton(
                        onClick = {
                            navController.navigate(Screen.StepsHistory.createRoute(selectedDateStartMillis))
                        },
                        shape = CircleShape,
                        border = BorderStroke(2.dp, PrimaryBlue)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
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

            item {
                StepsStatsGrid(
                    selectedRange = selectedRange,
                    stats = stats
                )
            }

            item {
                InsightCard(
                    title = "NHẬN XÉT HÔM NAY",
                    content = buildStepsInsight(stats),
                    icon = Icons.Default.CheckCircle,
                    containerColor = Color(0xFFE3F2FD),
                    iconBg = PrimaryBlue
                )
            }

            item {
                ExpertTipCard(
                    title = "GỢI Ý DÀNH CHO BẠN",
                    description = buildStepsSuggestion(stats),
                    icon = Icons.Default.Lightbulb
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StepsChartCard(
    selectedRange: HeartRateRange,
    points: List<StepBarPoint>,
    stepGoal: Long
) {
    val title = when (selectedRange) {
        HeartRateRange.DAY -> "Biểu đồ bước chân theo giờ"
        HeartRateRange.WEEK -> "Biểu đồ bước chân theo tuần"
        HeartRateRange.MONTH -> "Biểu đồ bước chân theo tháng"
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
                    HeartRateRange.DAY -> "Mỗi cột thể hiện số bước tăng trong từng giờ."
                    HeartRateRange.WEEK -> "Mỗi cột thể hiện tổng bước của từng ngày."
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
                    StepsBarChart(
                        points = points,
                        stepGoal = stepGoal,
                        showGoalLine = true,
                        modifier = Modifier
                            .width((points.size * 44).dp)
                            .height(240.dp)
                    )
                }
            } else {
                StepsBarChart(
                    points = points,
                    stepGoal = stepGoal,
                    showGoalLine = selectedRange != HeartRateRange.DAY,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }
        }
    }
}

@Composable
fun StepsBarChart(
    points: List<StepBarPoint>,
    stepGoal: Long,
    showGoalLine: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val left = 42.dp.toPx()
        val right = size.width - 10.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 34.dp.toPx()

        val chartWidth = right - left
        val chartHeight = bottom - top

        val maxPointValue = points.maxOfOrNull { it.steps } ?: 0L
        val maxValue = maxOf(maxPointValue, if (showGoalLine) stepGoal else 0L, 1L).toFloat()

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
                formatCompactNumber(value.toLong()),
                left - 8.dp.toPx(),
                y + 4.dp.toPx(),
                labelPaint
            )
        }

        if (showGoalLine && stepGoal > 0L) {
            val goalY = yForValue(stepGoal.toFloat())

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

        if (points.all { it.steps <= 0L }) {
            drawContext.canvas.nativeCanvas.drawText(
                "Chưa có dữ liệu bước chân",
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
                val barTop = yForValue(point.steps.toFloat())
                val barBottom = bottom

                val barColor = when {
                    showGoalLine && point.steps >= stepGoal -> SuccessGreen
                    else -> PrimaryBlue
                }

                if (point.steps > 0L) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(centerX - barWidth / 2f, barTop),
                        size = androidx.compose.ui.geometry.Size(
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
fun StepsStatsGrid(
    selectedRange: HeartRateRange,
    stats: StepStats
) {
    val items = when (selectedRange) {
        HeartRateRange.DAY -> listOf(
            StepStatUi("Tổng bước", formatNumber(stats.totalSteps), "bước"),
            StepStatUi("Mục tiêu", formatNumber(stats.goal), "bước"),
            StepStatUi("Còn lại", formatNumber(stats.remainingSteps), "bước")
        )

        HeartRateRange.WEEK -> listOf(
            StepStatUi("Tổng tuần", formatNumber(stats.totalSteps), "bước"),
            StepStatUi("TB/ngày", formatNumber(stats.averagePerDay), "bước"),
            StepStatUi("Đạt mục tiêu", "${stats.reachedGoalDays}/7", "ngày")
        )

        HeartRateRange.MONTH -> listOf(
            StepStatUi("Tổng tháng", formatNumber(stats.totalSteps), "bước"),
            StepStatUi("TB/ngày", formatNumber(stats.averagePerDay), "bước"),
            StepStatUi("Đạt mục tiêu", stats.reachedGoalDays.toString(), "ngày")
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            StepStatCard(
                item = item,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StepStatCard(
    item: StepStatUi,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE8EAED))
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
                color = PrimaryBlue
            )

            Text(
                text = item.unit,
                fontSize = 10.sp,
                color = OnSurfaceVariant
            )
        }
    }
}

data class StepBarPoint(
    val label: String,
    val steps: Long,
    val showLabel: Boolean = true
)

data class StepStats(
    val totalSteps: Long,
    val goal: Long,
    val remainingSteps: Long,
    val progress: Float,
    val progressPercent: Int,
    val averagePerDay: Long,
    val reachedGoalDays: Int
)

data class StepStatUi(
    val label: String,
    val value: String,
    val unit: String
)

private fun buildDayStepStats(
    steps: Long,
    stepGoal: Long
): StepStats {
    val progress = if (stepGoal > 0) {
        (steps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    return StepStats(
        totalSteps = steps,
        goal = stepGoal,
        remainingSteps = (stepGoal - steps).coerceAtLeast(0L),
        progress = progress,
        progressPercent = (progress * 100).roundToInt(),
        averagePerDay = steps,
        reachedGoalDays = if (steps >= stepGoal) 1 else 0
    )
}

private fun buildPeriodStepStats(
    points: List<StepBarPoint>,
    stepGoal: Long
): StepStats {
    val total = points.sumOf { it.steps }
    val days = points.size.coerceAtLeast(1)
    val progress = if (stepGoal > 0) {
        (total.toFloat() / (stepGoal * days).toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    return StepStats(
        totalSteps = total,
        goal = stepGoal,
        remainingSteps = ((stepGoal * days) - total).coerceAtLeast(0L),
        progress = progress,
        progressPercent = (progress * 100).roundToInt(),
        averagePerDay = total / days,
        reachedGoalDays = points.count { it.steps >= stepGoal }
    )
}

private fun buildHourlyStepPoints(
    dateKey: String,
    stepsHistory: Map<String, StepHistoryModel>
): List<StepBarPoint> {
    val dayData = stepsHistory[dateKey]
    val hourSteps = LongArray(24) { 0L }

    val records = dayData
        ?.records
        ?.values
        ?.sortedBy { normalizeTimestampMillis(it.updatedAt.takeIf { time -> time > 0L } ?: it.timestamp) }
        ?: emptyList()

    var previousTotal = 0L

    records.forEach { record ->
        val currentTotal = record.steps
        val delta = (currentTotal - previousTotal).coerceAtLeast(0L)

        val timeMillis = normalizeTimestampMillis(
            record.updatedAt.takeIf { it > 0L } ?: record.timestamp
        )

        val hour = hourFromMillis(timeMillis)

        if (hour in 0..23) {
            hourSteps[hour] += delta
        }

        previousTotal = currentTotal
    }

    return (0..23).map { hour ->
        StepBarPoint(
            label = "${hour}h",
            steps = hourSteps[hour],
            showLabel = hour % 6 == 0
        )
    }
}

private fun buildWeeklyStepPoints(
    selectedDateMillis: Long,
    stepsHistory: Map<String, StepHistoryModel>,
    currentSteps: Long
): List<StepBarPoint> {
    val weekStart = startOfWeekMillis(selectedDateMillis)

    return (0..6).map { index ->
        val dayStart = addDays(weekStart, index)
        val dateKey = formatDateKey(dayStart)

        StepBarPoint(
            label = shortWeekday(dayStart),
            steps = getStepsForDate(
                dateKey = dateKey,
                stepsHistory = stepsHistory,
                currentSteps = currentSteps,
                selectedDateMillis = dayStart
            ),
            showLabel = true
        )
    }
}

private fun buildMonthlyStepPoints(
    selectedDateMillis: Long,
    stepsHistory: Map<String, StepHistoryModel>,
    currentSteps: Long
): List<StepBarPoint> {
    val monthStart = startOfMonthMillis(selectedDateMillis)
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = monthStart
    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    return (1..maxDay).map { day ->
        val dayStart = dayStartOfMonth(monthStart, day)
        val dateKey = formatDateKey(dayStart)

        StepBarPoint(
            label = day.toString().padStart(2, '0'),
            steps = getStepsForDate(
                dateKey = dateKey,
                stepsHistory = stepsHistory,
                currentSteps = currentSteps,
                selectedDateMillis = dayStart
            ),
            showLabel = true
        )
    }
}

private fun getStepsForDate(
    dateKey: String,
    stepsHistory: Map<String, StepHistoryModel>,
    currentSteps: Long,
    selectedDateMillis: Long
): Long {
    val historySteps = stepsHistory[dateKey]?.totalSteps ?: 0L

    return if (isToday(selectedDateMillis)) {
        maxOf(historySteps, currentSteps)
    } else {
        historySteps
    }
}

private fun buildStepsInsight(
    stats: StepStats
): String {
    return when {
        stats.totalSteps <= 0L -> "Chưa có dữ liệu bước chân trong khoảng thời gian đã chọn."
        stats.progressPercent >= 100 -> "Bạn đã hoàn thành mục tiêu bước chân. Rất tốt!"
        stats.progressPercent >= 70 -> "Bạn đã hoàn thành ${stats.progressPercent}% mục tiêu, hãy cố gắng thêm một chút nữa."
        else -> "Bạn mới hoàn thành ${stats.progressPercent}% mục tiêu bước chân."
    }
}

private fun buildStepsSuggestion(
    stats: StepStats
): String {
    return when {
        stats.totalSteps <= 0L -> "Hãy bắt đầu vận động nhẹ để ghi nhận dữ liệu bước chân."
        stats.remainingSteps > 0 -> "Bạn còn khoảng ${formatNumber(stats.remainingSteps)} bước để đạt mục tiêu."
        else -> "Tiếp tục duy trì thói quen vận động đều đặn mỗi ngày."
    }
}
/*
private fun formatTimeFromSeconds(timestamp: Long): String {
    if (timestamp <= 0L) return "Chưa có"

    val millis = normalizeTimestampMillis(timestamp)
    return SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        .format(Date(millis))
}*/

private fun formatDateKey(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        .format(Date(timeMillis))
}

private fun formatNumber(value: Long): String {
    return "%,d".format(value).replace(",", ".")
}

private fun formatCompactNumber(value: Long): String {
    return when {
        value >= 1000 -> "${value / 1000}k"
        else -> value.toString()
    }
}

private fun normalizeTimestampMillis(timestamp: Long): Long {
    if (timestamp <= 0L) return 0L

    return if (timestamp in 1..9_999_999_999L) {
        timestamp * 1000L
    } else {
        timestamp
    }
}

private fun hourFromMillis(timeMillis: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    return calendar.get(Calendar.HOUR_OF_DAY)
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

private fun isToday(timeMillis: Long): Boolean {
    val today = Calendar.getInstance()
    val target = Calendar.getInstance()
    target.timeInMillis = timeMillis

    return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
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
