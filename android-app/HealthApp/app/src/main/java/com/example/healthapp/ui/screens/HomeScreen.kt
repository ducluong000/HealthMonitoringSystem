package com.example.healthapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import com.example.healthapp.ui.theme.*

import com.example.healthapp.ui.components.BottomNavBar
import com.example.healthapp.ui.components.BottomTab
import com.example.healthapp.ui.navigation.Screen

import com.example.healthapp.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel
) {
    val currentHealth by homeViewModel.currentHealth.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val errorMessage by homeViewModel.errorMessage.collectAsState()
    val userProfile by homeViewModel.userProfile.collectAsState()

    val heartRate = currentHealth.heartRate
    val spo2 = currentHealth.spo2
    val steps = currentHealth.steps
    val calories = calculateCaloriesFromSteps(
        steps = steps,
        weightKg = userProfile.weight,
        heightCm = userProfile.height
    )
    val stepGoal = if (userProfile.stepGoal > 0L) userProfile.stepGoal else 10000L
    val calorieGoal = if (userProfile.calorieGoal > 0L) userProfile.calorieGoal else 500L
    val sleepGoalSeconds = if (userProfile.sleepGoalSeconds > 0L) {
        userProfile.sleepGoalSeconds
    } else {
        28800L
    }
    val sleepSeconds = currentHealth.sleep?.totalSleepSeconds ?: 0L
    val sleepText = formatSleepDuration(sleepSeconds)
    val sleepProgress = calculateSleepProgress(
        totalSeconds = sleepSeconds,
        sleepGoalSeconds = sleepGoalSeconds
    )



    val lastVitalTime = currentHealth.lastVitalMeasureTime
        .takeIf { it > 0L }
        ?: currentHealth.lastMeasureTime
    Scaffold(
        bottomBar = {
            BottomNavBar(selected = BottomTab.HOME,navController = navController)
        },
        containerColor = Color(0xFFE2EEF8) // Darker background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Header() }

            if (isLoading) {
                item {
                    Text(
                        text = "Đang tải dữ liệu từ Firebase...",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = "Lỗi Firebase: $errorMessage",
                        fontSize = 12.sp,
                        color = Color.Red
                    )
                }
            }

            item {
                StepsCard(
                    navController = navController,
                    steps = steps,
                    stepGoal = stepGoal
                )
            }

            item {
                IoTCard(
                    navController = navController,
                    heartRate = heartRate,
                    spo2 = spo2,
                    lastMeasureTime = lastVitalTime
                )
            }

            item {
                GaugesRow(
                    navController = navController,
                    calories = calories,
                    calorieGoal = calorieGoal,
                    sleepText = sleepText,
                    sleepProgress = sleepProgress
                )
            }
            item { InsightsSection() }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun Header() {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Chào buổi sáng"
        hour < 18 -> "Chào buổi chiều"
        else -> "Chào buổi tối"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(greeting, fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Sức Khỏe Của Bạn", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
        }
        IconButton(onClick = { }) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.Gray)
        }
    }
}

@Composable
fun StepsCard(
    navController: NavController,
    steps: Long,
    stepGoal: Long
) {
    val progress = if (stepGoal > 0L) {
        (steps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val stepsText = "%,d".format(steps).replace(",", ".")
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable {
            navController.navigate(Screen.StepsDetail.route)
        }
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = SecondaryGreen,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "SỐ BƯỚC",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryGreen
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.Bottom
                ) {

                    Text(
                        text = stepsText,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "/ ${stepGoal / 1000}k",
                        fontSize = 18.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            CircularProgress(
                percentage = progress,
                color = SecondaryGreen,
                trackColor = SecondaryGreen.copy(alpha = 0.15f),
                size = 72,
                showText = true
            )
        }
    }
}
@Composable
fun IoTCard(
    navController: NavController,
    heartRate: Double,
    spo2: Double,
    lastMeasureTime: Long
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "ĐO LƯỜNG IOT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )

                Text(
                    "Lần cuối: ${formatTimeFromSeconds(lastMeasureTime)}",
                    fontSize = 9.sp,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                MetricItem(
                    icon = Icons.Default.Favorite,
                    label = "NHỊP TIM",
                    value = if (heartRate > 0) heartRate.toInt().toString() else "--",
                    unit = "bpm",
                    status = heartRateStatus(heartRate),
                    color = Color.Red,
                    modifier = Modifier.weight(1f).clickable {
                        navController.navigate(Screen.HeartRateDetail.route)
                    }
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.LightGray)
                )

                MetricItem(
                    icon = Icons.Default.WaterDrop,
                    label = "SPO2",
                    value = if (spo2 > 0) spo2.toInt().toString() else "--",
                    unit = "%",
                    status = spo2Status(spo2),
                    color = Color.Blue,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 20.dp).clickable {
                            navController.navigate(Screen.SpO2Detail.route)
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                )
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Đo đồng thời", fontSize = 13.sp)
            }
        }
    }
}
@Composable
fun MetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    status: String,
    color: Color,
    modifier: Modifier
) {

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color.copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(icon, null, tint = color)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {

            Text(label, fontSize = 8.sp, color = Color.Gray)

            Row(verticalAlignment = Alignment.Bottom) {

                Text(
                    value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    unit,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Text(
                status,
                fontSize = 9.sp,
                color = Color.Green
            )
        }
    }
}

@Composable
fun GaugesRow(
    navController: NavController,
    calories: Int,
    calorieGoal: Long,
    sleepText: String,
    sleepProgress: Float
) {

    val calorieProgress = if (calorieGoal > 0L) {
        (calories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        GaugeCard(
            "CALORIES",
            calories.toString(),
            "kcal",
            Icons.Default.LocalFireDepartment,
            Color(0xFFFF9800),
            calorieProgress,
            Modifier.weight(1f)
        )

        GaugeCard(
            "GIẤC NGỦ",
            sleepText,
            "",
            Icons.Default.Bedtime,
            Color(0xFF3F51B5),
            sleepProgress,
            Modifier
                .weight(1f)
                .clickable {
                    navController.navigate(Screen.SleepDetail.route)
                }
        )
    }
}

@Composable
fun GaugeCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color,
    progress: Float,
    modifier: Modifier
) {

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = modifier
    ) {

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            CircularProgress(
                percentage = progress,
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            ) {
                Icon(icon, null, tint = color)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(label, fontSize = 9.sp, color = Color.Gray)

            Row(verticalAlignment = Alignment.Bottom) {

                Text(
                    value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                if (unit.isNotEmpty()) {
                    Text(
                        unit,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
data class HealthArticle(
    val id: Int,
    val title: String,
    val summary: String,
    val content: String,
    val icon: ImageVector,
    val category: String,
    val readTime: String,
    val accentColor: Color,
    val bgColor: Color
)

private fun getHealthArticles() = listOf(
    HealthArticle(
        id = 1,
        title = "7 cách cải thiện giấc ngủ",
        summary = "Ngủ đủ giấc giúp cơ thể phục hồi và tăng cường miễn dịch.",
        content = "1. Đi ngủ và thức dậy đúng giờ mỗi ngày.\n\n2. Tránh caffeine sau 2h chiều.\n\n3. Tắt màn hình điện tử 30 phút trước khi ngủ.\n\n4. Giữ phòng ngủ tối, mát và yên tĩnh.\n\n5. Tập thể dục đều đặn nhưng không tập sát giờ ngủ.\n\n6. Tránh ăn no trước khi đi ngủ.\n\n7. Thực hành thở sâu hoặc thiền trước giờ ngủ.",
        icon = Icons.Default.Bedtime,
        category = "Giấc ngủ",
        readTime = "3 phút",
        accentColor = Color(0xFF3F51B5),
        bgColor = Color(0xFFB3BEF0) // Darker
    ),
    HealthArticle(
        id = 2,
        title = "Nhịp tim khỏe mạnh",
        summary = "Hiểu về nhịp tim và cách giữ trái tim luôn khỏe.",
        content = "Nhịp tim bình thường khi nghỉ ngơi: 60-100 bpm.\n\nNhịp tim thấp hơn cho thấy tim hoạt động hiệu quả.\n\nCách giữ tim khỏe:\n• Tập cardio 150 phút/tuần\n• Ăn nhiều rau xanh, cá, hạt\n• Giảm muối và đường\n• Kiểm soát stress\n• Không hút thuốc\n• Ngủ đủ 7-8 tiếng",
        icon = Icons.Default.Favorite,
        category = "Tim mạch",
        readTime = "4 phút",
        accentColor = Color(0xFFE53935),
        bgColor = Color(0xFFFFB5BC) // Darker
    ),
    HealthArticle(
        id = 3,
        title = "Lợi ích của việc đi bộ",
        summary = "10.000 bước mỗi ngày thay đổi sức khỏe của bạn.",
        content = "Đi bộ 10.000 bước/ngày giúp:\n\n• Giảm nguy cơ bệnh tim mạch 30%\n• Kiểm soát cân nặng hiệu quả\n• Cải thiện tâm trạng, giảm stress\n• Tăng cường sức mạnh xương khớp\n• Cải thiện tiêu hóa\n\nMẹo: Bắt đầu từ 5.000 bước và tăng dần 500 bước mỗi tuần.",
        icon = Icons.Default.DirectionsWalk,
        category = "Vận động",
        readTime = "3 phút",
        accentColor = Color(0xFF2E7D32),
        bgColor = Color(0xFFB5E3BE) // Darker
    ),
    HealthArticle(
        id = 4,
        title = "SpO2 là gì? Tại sao quan trọng?",
        summary = "Tìm hiểu về nồng độ oxy trong máu và sức khỏe.",
        content = "SpO2 (Oxygen Saturation) đo lượng oxy trong máu.\n\n• Bình thường: 95-100%\n• Cần chú ý: 90-94%\n• Nguy hiểm: dưới 90%\n\nKhi nào cần lo lắng:\n- SpO2 dưới 94% kéo dài\n- Khó thở, chóng mặt\n- Môi hoặc đầu ngón tay tím\n\nCách cải thiện:\n• Tập thở sâu hàng ngày\n• Tập thể dục đều đặn\n• Tránh ô nhiễm không khí",
        icon = Icons.Default.WaterDrop,
        category = "Oxy máu",
        readTime = "3 phút",
        accentColor = Color(0xFF1565C0),
        bgColor = Color(0xFFADD7FF) // Darker
    ),
    HealthArticle(
        id = 5,
        title = "Dinh dưỡng cho người tập luyện",
        summary = "Ăn đúng cách để tối ưu hiệu quả tập luyện.",
        content = "Trước tập (1-2h): Carb phức hợp + protein nhẹ\nVí dụ: Bánh mì nguyên cám + chuối\n\nSau tập (30 phút): Protein + carb\nVí dụ: Sữa chua Hy Lạp + trái cây\n\nNguyên tắc:\n• Uống đủ 2-3 lít nước/ngày\n• Chia nhỏ 4-5 bữa ăn\n• Protein: 1.5-2g/kg cân nặng\n• Ăn nhiều rau xanh, trái cây",
        icon = Icons.Default.Restaurant,
        category = "Dinh dưỡng",
        readTime = "5 phút",
        accentColor = Color(0xFFFF9800),
        bgColor = Color(0xFFFFD18A) // Darker
    )
)

@Composable
fun InsightsSection() {
    var selectedArticle by remember { mutableStateOf<HealthArticle?>(null) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Thông tin hữu ích",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        getHealthArticles().forEach { article ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = article.bgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { selectedArticle = article }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            article.icon, null,
                            tint = article.accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            article.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            article.summary,
                            fontSize = 12.sp,
                            color = Color(0xFF555555),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.White,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    article.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = article.accentColor
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                Icons.Default.Schedule, null,
                                tint = article.accentColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                article.readTime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = article.accentColor.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight, null,
                        tint = article.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (selectedArticle != null) {
        val article = selectedArticle!!
        AlertDialog(
            onDismissRequest = { selectedArticle = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(article.bgColor, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(article.icon, null, tint = article.accentColor, modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text(
                    article.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
            },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(article.accentColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(article.category, fontSize = 10.sp, color = article.accentColor, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${article.readTime} đọc", fontSize = 10.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        article.content,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF333333)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedArticle = null }) {
                    Text("Đóng", color = article.accentColor, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}
@Composable
fun CircularProgress(
    percentage: Float,
    color: Color,
    trackColor: Color,
    size: Int = 80,
    showText: Boolean = false,
    content: @Composable (() -> Unit)? = null
) {

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size.dp)
    ) {

        Canvas(
            modifier = Modifier.size(size.dp)
        ) {

            drawCircle(
                color = trackColor,
                style = Stroke(width = 18f)
            )

            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360 * percentage,
                useCenter = false,
                style = Stroke(
                    width = 18f,
                    cap = StrokeCap.Round
                )
            )
        }
        if(showText) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        content?.invoke()
    }
}
/*
@Preview(showBackground = true,showSystemUi = true)
@Composable
fun PreviewHome() {
    HomeScreen(navController)
}*/
fun formatTimeFromSeconds(timestamp: Long): String {
    if (timestamp <= 0L) return "Chưa có"

    val millis = timestamp * 1000
    val sdf = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun heartRateStatus(heartRate: Double): String {
    return when {
        heartRate <= 0 -> "Chưa có"
        heartRate < 60 -> "Thấp"
        heartRate <= 100 -> "Bình thường"
        else -> "Cao"
    }
}

fun spo2Status(spo2: Double): String {
    return when {
        spo2 <= 0 -> "Chưa có"
        spo2 >= 95 -> "Ổn định"
        spo2 >= 90 -> "Cần chú ý"
        else -> "Thấp"
    }
}
fun calculateCaloriesFromSteps(
    steps: Long,
    weightKg: Int,
    heightCm: Int
): Int {
    if (steps <= 0L) return 0

    val safeWeight = if (weightKg > 0) weightKg.toDouble() else 60.0
    val safeHeight = if (heightCm > 0) heightCm.toDouble() else 165.0

    val stepLengthMeter = safeHeight * 0.414 / 100.0
    val distanceKm = steps * stepLengthMeter / 1000.0

    val calories = safeWeight * distanceKm * 0.5

    return calories.toInt()
}
fun formatSleepDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "Chưa có"

    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

fun calculateSleepProgress(
    totalSeconds: Long,
    sleepGoalSeconds: Long
): Float {
    if (totalSeconds <= 0L || sleepGoalSeconds <= 0L) return 0f

    return (totalSeconds.toFloat() / sleepGoalSeconds.toFloat())
        .coerceIn(0f, 1f)
}

