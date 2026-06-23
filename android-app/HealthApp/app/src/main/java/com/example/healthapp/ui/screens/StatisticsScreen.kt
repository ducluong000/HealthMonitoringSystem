package com.example.healthapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.healthapp.ui.components.BottomNavBar
import com.example.healthapp.ui.components.BottomTab
import com.example.healthapp.ui.components.DateSelector
import com.example.healthapp.ui.components.HeartRateRange
import com.example.healthapp.ui.components.TabSelector
import com.example.healthapp.ui.theme.CardBg
import com.example.healthapp.ui.theme.OnSurfaceColor
import com.example.healthapp.ui.theme.OnSurfaceVariant
import com.example.healthapp.ui.theme.PrimaryBlue
import com.example.healthapp.ui.theme.SuccessGreen
import com.example.healthapp.ui.theme.SurfaceColor
import com.example.healthapp.viewmodel.ComparisonItem
import com.example.healthapp.viewmodel.ComparisonTrend
import com.example.healthapp.viewmodel.GoalResultItem
import com.example.healthapp.viewmodel.StatisticsRange
import com.example.healthapp.viewmodel.StatisticsUiState
import com.example.healthapp.viewmodel.StatisticsViewModel

/* ───── Colors ───── */
private val WarningOrange = Color(0xFFFF9800)
private val DangerRed = Color(0xFFE53935)
private val GaugeBgColor = Color(0xFFE8EAED)
private val BarTrackColor = Color(0xFFECEFF1)
private val PreviousBarColor = Color(0xFFB0BEC5)
private val SuggestionBg = Color(0xFFE3F2FD)
private val SuggestionAccent = Color(0xFF1565C0)

/* ═══════════════════════════════════════════════
   Main Screen
   ═══════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    statisticsViewModel: StatisticsViewModel = viewModel()
) {
    val uiState by statisticsViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Thống kê cá nhân",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = PrimaryBlue
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                selected = BottomTab.STATS,
                navController = navController
            )
        },
        containerColor = SurfaceColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            /* ── Header spacer ── */
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            /* ── Tab + Date selector ── */
            item {
                RangeSection(
                    uiState = uiState,
                    onRangeSelected = { statisticsViewModel.selectRange(it) },
                    onDateSelected = { statisticsViewModel.selectDate(it) }
                )
            }

            /* ── Health Score ── */
            item { HealthScoreCard(uiState) }

            /* ── Comparison ── */
            item { ComparisonCard(uiState.previousPeriodTitle, uiState.comparisons) }

            /* ── Goal ── */
            item { GoalCard(uiState.goalResults) }

            /* ── Factors ── */
            item { FactorsCard(uiState.positiveFactors, uiState.improvementFactors) }

            /* ── Suggestion ── */
            item {
                SuggestionCard(
                    uiState.suggestionTitle,
                    uiState.suggestionContent,
                    uiState.suggestedStepGoal,
                    uiState.suggestedSleepGoalSeconds
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/* ═══════════════════════════════════════════════
   1 · Range / Date selector
   ═══════════════════════════════════════════════ */

@Composable
private fun RangeSection(
    uiState: StatisticsUiState,
    onRangeSelected: (StatisticsRange) -> Unit,
    onDateSelected: (Long) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TabSelector(
            selectedRange = uiState.selectedRange.toHeartRateRange(),
            onRangeSelected = { onRangeSelected(it.toStatisticsRange()) }
        )
        DateSelector(
            selectedDateStartMillis = uiState.selectedDateMillis,
            selectedRange = uiState.selectedRange.toHeartRateRange(),
            onDateSelected = onDateSelected
        )
    }
}

/* ═══════════════════════════════════════════════
   2 · Health Score Card
   ═══════════════════════════════════════════════ */

@Composable
private fun HealthScoreCard(uiState: StatisticsUiState) {
    val score = uiState.healthScore
    val scoreColor = scoreColor(score)
    var infoExpanded by remember { mutableStateOf(false) }
    var scoreExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        shadowElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            /* ℹ icon – top right: general explanation */
            IconButton(
                onClick = { infoExpanded = !infoExpanded; if (infoExpanded) scoreExpanded = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Cách tính điểm",
                    tint = if (infoExpanded) PrimaryBlue else OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Điểm sức khỏe cá nhân",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceColor
                )

                Spacer(Modifier.height(10.dp))

                /* Gauge – tap to show actual scores */
                ScoreGauge(
                    score = score,
                    color = scoreColor,
                    modifier = Modifier
                        .size(90.dp)
                        .clickable { scoreExpanded = !scoreExpanded; if (scoreExpanded) infoExpanded = false }
                )

                Spacer(Modifier.height(8.dp))

                Surface(shape = CircleShape, color = scoreColor.copy(alpha = 0.12f)) {
                    Text(
                        text = uiState.healthScoreLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))

                DiffBadge(uiState.healthScoreDiff, uiState.previousPeriodTitle)

                /* ── Section A: actual scores per component ── */
                AnimatedVisibility(visible = scoreExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = "Chi tiết điểm kỳ này",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceColor
                        )
                        Spacer(Modifier.height(8.dp))
                        ScoreDetailRow("Nhịp tim", uiState.heartRateScore, 25)
                        Spacer(Modifier.height(6.dp))
                        ScoreDetailRow("SpO2", uiState.spo2Score, 25)
                        Spacer(Modifier.height(6.dp))
                        ScoreDetailRow("Bước chân", uiState.stepScore, 25)
                        Spacer(Modifier.height(6.dp))
                        ScoreDetailRow("Giấc ngủ", uiState.sleepScore, 25)
                    }
                }

                /* ── Section B: general explanation ── */
                AnimatedVisibility(visible = infoExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = "Cách tính điểm sức khỏe",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceColor
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Điểm tổng (tối đa 100) được chia đều cho 4 chỉ số, mỗi chỉ số chiếm tối đa 25 điểm. Công thức tính cụ thể như sau:\n\n" +
                                    "1. Nhịp tim (25đ): Điểm = (Số lần đo nằm trong khoảng bình thường 60–100 bpm / Tổng số lần đo) × 25.\n\n" +
                                    "2. SpO2 (25đ): Điểm = (Số lần đo có nồng độ ≥ 95% / Tổng số lần đo) × 25.\n\n" +
                                    "3. Bước chân (25đ):\n" +
                                    "  • Tab Ngày: Điểm = (Số bước đã đi / Mục tiêu) × 25.\n" +
                                    "  • Tab Tuần/Tháng: Điểm = (Số ngày hoàn thành mục tiêu / Tổng số ngày) × 25.\n\n" +
                                    "4. Giấc ngủ (25đ):\n" +
                                    "  • Tab Ngày: Điểm = (Thời gian ngủ / Mục tiêu) × 25.\n" +
                                    "  • Tab Tuần/Tháng: Điểm = (Số ngày hoàn thành mục tiêu / Tổng số ngày) × 25.",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreGauge(score: Int, color: Color, modifier: Modifier = Modifier) {
    val progress = (score / 100f).coerceIn(0f, 1f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 10.dp.toPx()
            val d = size.minDimension - sw
            val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val s = Size(d, d)
            drawArc(GaugeBgColor, 0f, 360f, false, tl, s, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * progress, false, tl, s, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toString(), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = OnSurfaceColor)
            Text("/100", fontSize = 12.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DiffBadge(diff: Int, previousTitle: String) {
    val color = when {
        diff > 0 -> SuccessGreen
        diff < 0 -> WarningOrange
        else -> PrimaryBlue
    }
    val icon: ImageVector = when {
        diff > 0 -> Icons.Default.KeyboardArrowUp
        diff < 0 -> Icons.Default.KeyboardArrowDown
        else -> Icons.Default.CheckCircle
    }
    val text = when {
        diff > 0 -> "Tăng $diff điểm so với $previousTitle"
        diff < 0 -> "Giảm ${-diff} điểm so với $previousTitle"
        else -> "Ổn định so với $previousTitle"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ScoreDetailRow(label: String, score: Int, maxPoints: Int) {
    val ratio = (score.toFloat() / maxPoints).coerceIn(0f, 1f)
    val barColor = when {
        ratio >= 0.8f -> SuccessGreen
        ratio >= 0.5f -> PrimaryBlue
        else -> WarningOrange
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = OnSurfaceColor, modifier = Modifier.width(72.dp))
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(BarTrackColor)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(ratio)
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(barColor)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("$score/$maxPoints", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = barColor)
    }
}

/* ═══════════════════════════════════════════════
   3 · Comparison Card
   ═══════════════════════════════════════════════ */

@Composable
private fun ComparisonCard(periodTitle: String, comparisons: List<ComparisonItem>) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "So sánh với $periodTitle",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceColor
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = OnSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(Modifier.height(14.dp))

                if (comparisons.isEmpty()) {
                    EmptyHint("Chưa đủ dữ liệu để so sánh.")
                } else {
                    comparisons.forEachIndexed { idx, item ->
                        ComparisonRow(item)
                        if (idx < comparisons.lastIndex) Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(item: ComparisonItem) {
    val color = trendColor(item.trend)
    val trendIcon: ImageVector = when (item.trend) {
        ComparisonTrend.POSITIVE -> Icons.Default.KeyboardArrowUp
        ComparisonTrend.NEGATIVE -> Icons.Default.KeyboardArrowDown
        ComparisonTrend.NEUTRAL -> Icons.Default.CheckCircle
        ComparisonTrend.UNKNOWN -> Icons.Default.Info
    }

    Column {
        /* Title + badge */
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceColor, modifier = Modifier.weight(1f))
            Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(trendIcon, null, tint = color, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(cleanChangeText(item.changeText), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        /* Current bar */
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Hiện tại", fontSize = 11.sp, color = OnSurfaceVariant, modifier = Modifier.width(52.dp))
            Spacer(Modifier.width(6.dp))
            HBar(ratio = item.currentBarRatio, color = color, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(item.currentValue, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurfaceColor, modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
        }

        Spacer(Modifier.height(4.dp))

        /* Previous bar */
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Trước", fontSize = 11.sp, color = OnSurfaceVariant, modifier = Modifier.width(52.dp))
            Spacer(Modifier.width(6.dp))
            HBar(ratio = item.previousBarRatio, color = PreviousBarColor, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(item.previousValue, fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun HBar(ratio: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(10.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(BarTrackColor)
    ) {
        Box(
            Modifier
                .fillMaxWidth(ratio.coerceIn(0.02f, 1f))
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(color)
        )
    }
}

/* ═══════════════════════════════════════════════
   4 · Goal Card
   ═══════════════════════════════════════════════ */

@Composable
private fun GoalCard(items: List<GoalResultItem>) {
    CardWrapper {
        SectionTitle("Mức độ đạt mục tiêu")
        Spacer(Modifier.height(14.dp))

        if (items.isEmpty()) {
            EmptyHint("Chưa có dữ liệu mục tiêu.")
        } else {
            items.forEachIndexed { idx, item ->
                GoalRow(item)
                if (idx < items.lastIndex) Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun GoalRow(item: GoalResultItem) {
    val barColor = when {
        item.progress >= 0.75f -> SuccessGreen
        item.progress >= 0.4f -> PrimaryBlue
        else -> WarningOrange
    }
    val icon = when {
        item.progress >= 0.75f -> Icons.Default.CheckCircle
        item.progress >= 0.4f -> Icons.Default.Star
        else -> Icons.Default.Warning
    }

    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = barColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceColor, modifier = Modifier.weight(1f))
            Surface(shape = CircleShape, color = barColor.copy(alpha = 0.12f)) {
                Text(
                    item.resultText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = barColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(BarTrackColor)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(item.progress.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(barColor)
            )
        }
    }
}

/* ═══════════════════════════════════════════════
   5 · Factors Card
   ═══════════════════════════════════════════════ */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FactorsCard(positive: List<String>, improvement: List<String>) {
    CardWrapper {
        SectionTitle("Yếu tố ảnh hưởng")
        Spacer(Modifier.height(14.dp))

        /* Positive */
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ThumbUp, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Tích cực", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            positive.forEach { Chip(it, SuccessGreen) }
        }

        Spacer(Modifier.height(16.dp))

        /* Improvement */
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = WarningOrange, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Cần cải thiện", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            improvement.forEach { Chip(it, WarningOrange) }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.10f)) {
        Text(
            text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/* ═══════════════════════════════════════════════
   6 · Suggestion Card
   ═══════════════════════════════════════════════ */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionCard(
    title: String,
    content: String,
    suggestedStepGoal: Long?,
    suggestedSleepGoalSeconds: Long?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SuggestionBg,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = SuggestionAccent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    title.ifBlank { "Gợi ý cá nhân hóa" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuggestionAccent
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                content.ifBlank { "Tiếp tục theo dõi thêm dữ liệu để có gợi ý phù hợp hơn." },
                color = OnSurfaceColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            if (suggestedStepGoal != null || suggestedSleepGoalSeconds != null) {
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestedStepGoal?.let {
                        SuggestionBadge("Đề xuất: ${formatNumber(it)} bước/ngày")
                    }
                    suggestedSleepGoalSeconds?.let {
                        SuggestionBadge("Đề xuất: ${formatSleepDuration(it)}/ngày")
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionBadge(text: String) {
    Surface(shape = CircleShape, color = SuggestionAccent.copy(alpha = 0.14f)) {
        Text(
            text,
            color = SuggestionAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/* ═══════════════════════════════════════════════
   Shared helpers
   ═══════════════════════════════════════════════ */

@Composable
private fun CardWrapper(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurfaceColor)
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, color = OnSurfaceVariant, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

/* ── Mappers ── */

private fun StatisticsRange.toHeartRateRange(): HeartRateRange = when (this) {
    StatisticsRange.DAY -> HeartRateRange.DAY
    StatisticsRange.WEEK -> HeartRateRange.WEEK
    StatisticsRange.MONTH -> HeartRateRange.MONTH
}

private fun HeartRateRange.toStatisticsRange(): StatisticsRange = when (this) {
    HeartRateRange.DAY -> StatisticsRange.DAY
    HeartRateRange.WEEK -> StatisticsRange.WEEK
    HeartRateRange.MONTH -> StatisticsRange.MONTH
}

/* ── Color helpers ── */

private fun scoreColor(score: Int): Color = when {
    score >= 85 -> SuccessGreen
    score >= 70 -> PrimaryBlue
    score >= 50 -> WarningOrange
    else -> DangerRed
}

private fun trendColor(trend: ComparisonTrend): Color = when (trend) {
    ComparisonTrend.POSITIVE -> SuccessGreen
    ComparisonTrend.NEGATIVE -> WarningOrange
    ComparisonTrend.NEUTRAL -> PrimaryBlue
    ComparisonTrend.UNKNOWN -> OnSurfaceVariant
}

/* ── Format helpers ── */

private fun cleanChangeText(text: String): String =
    text.replace(Regex("[↑↓↗↘→←]"), "").trim()

private fun formatNumber(value: Long): String =
    "%,d".format(value).replace(",", ".")

