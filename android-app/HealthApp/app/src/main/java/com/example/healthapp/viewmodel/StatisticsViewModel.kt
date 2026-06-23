package com.example.healthapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.model.MeasurementModel
import com.example.healthapp.data.model.SleepSessionModel
import com.example.healthapp.data.model.StepHistoryModel
import com.example.healthapp.data.model.UserProfileModel
import com.example.healthapp.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class StatisticsViewModel : ViewModel() {

    private val repository = HealthRepository()

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState

    private var userProfile = UserProfileModel()
    private var measurements = emptyList<MeasurementModel>()
    private var stepsHistory = emptyMap<String, StepHistoryModel>()
    private var sleepSessions = emptyList<SleepSessionModel>()

    private var selectedRange = StatisticsRange.DAY
    private var selectedDateMillis = startOfDayMillis(System.currentTimeMillis())

    init {
        observeUserProfile()
        observeMeasurements()
        observeStepsHistory()
        observeSleepSessions()
    }

    fun selectRange(range: StatisticsRange) {
        selectedRange = range
        updateUiState()
    }

    fun selectDate(timeMillis: Long) {
        selectedDateMillis = startOfDayMillis(timeMillis)
        updateUiState()
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            repository.observeUserProfile()
                .catch { }
                .collect { data ->
                    userProfile = data ?: UserProfileModel()
                    updateUiState()
                }
        }
    }

    private fun observeMeasurements() {
        viewModelScope.launch {
            repository.observeMeasurements()
                .catch { }
                .collect { data ->
                    measurements = data
                    updateUiState()
                }
        }
    }

    private fun observeStepsHistory() {
        viewModelScope.launch {
            repository.observeStepsHistory()
                .catch { }
                .collect { data ->
                    stepsHistory = data
                    updateUiState()
                }
        }
    }

    private fun observeSleepSessions() {
        viewModelScope.launch {
            repository.observeSleepSessions()
                .catch { }
                .collect { data ->
                    sleepSessions = data
                    updateUiState()
                }
        }
    }

    private fun updateUiState() {
        val currentRange = buildCurrentRange(selectedRange, selectedDateMillis)
        val previousRange = buildPreviousRange(selectedRange, currentRange)

        val currentData = buildPeriodData(currentRange)
        val previousData = buildPeriodData(previousRange)

        val currentScore = calculateHealthScore(currentData)
        val previousScore = calculateHealthScore(previousData)

        val goalResults = buildGoalResults(currentData)
        val comparisons = buildComparisons(currentData, previousData)
        val factors = buildInfluenceFactors(currentScore)
        val suggestion = buildPersonalSuggestion(currentData, currentScore)

        _uiState.value = StatisticsUiState(
            selectedRange = selectedRange,
            selectedDateMillis = selectedDateMillis,
            periodTitle = formatPeriodTitle(selectedRange, currentRange),
            previousPeriodTitle = formatPreviousPeriodTitle(selectedRange),
            healthScore = currentScore.totalScore,
            healthScoreLabel = healthScoreLabel(currentScore.totalScore),
            healthScoreDiff = currentScore.totalScore - previousScore.totalScore,
            heartRateScore = currentScore.heartRateScore,
            spo2Score = currentScore.spo2Score,
            stepScore = currentScore.stepScore,
            sleepScore = currentScore.sleepScore,
            comparisons = comparisons,
            goalResults = goalResults,
            positiveFactors = factors.positiveFactors,
            improvementFactors = factors.improvementFactors,
            suggestionTitle = suggestion.title,
            suggestionContent = suggestion.content,
            suggestedStepGoal = suggestion.suggestedStepGoal,
            suggestedSleepGoalSeconds = suggestion.suggestedSleepGoalSeconds
        )
    }

    private fun buildPeriodData(range: PeriodRange): PeriodData {
        val dayStarts = buildDayStarts(range.startMillis, range.endMillis)
        val dayCount = dayStarts.size.coerceAtLeast(1)

        val periodMeasurements = measurements.filter { measurement ->
            val time = measurementTimeMillis(measurement)
            time in range.startMillis until range.endMillis
        }

        val heartRateValues = periodMeasurements
            .filter { it.isValidHeartRate() }
            .map { it.heartRateAvg }

        val spo2Values = periodMeasurements
            .filter { it.isValidSpo2() }
            .map { it.spo2Avg }

        val stepGoal = userProfile.stepGoal.takeIf { it > 0L } ?: 10000L
        val sleepGoal = userProfile.sleepGoalSeconds.takeIf { it > 0L } ?: 28800L
        val calorieGoal = userProfile.calorieGoal.takeIf { it > 0L } ?: 500L

        val dailySteps = dayStarts.map { dayStart ->
            val dateKey = formatDateKey(dayStart)
            getStepsForDate(dateKey)
        }

        val dailySleep = dayStarts.map { dayStart ->
            getSleepSecondsForDate(dayStart)
        }

        val dailyCalories = dailySteps.map { steps ->
            calculateCaloriesFromSteps(
                steps = steps,
                weightKg = userProfile.weight,
                heightCm = userProfile.height
            )
        }

        val totalSteps = dailySteps.sum()
        val totalSleepSeconds = dailySleep.sum()
        val totalCalories = dailyCalories.sum()

        val stepReachedDays = dailySteps.count { it >= stepGoal }
        val sleepReachedDays = dailySleep.count { it >= sleepGoal }
        val calorieReachedDays = dailyCalories.count { it >= calorieGoal }

        return PeriodData(
            range = range,
            daysCount = dayCount,
            heartRateValues = heartRateValues,
            spo2Values = spo2Values,
            totalSteps = totalSteps,
            totalSleepSeconds = totalSleepSeconds,
            totalCalories = totalCalories,
            averageHeartRate = heartRateValues.averageOrNull(),
            averageSpo2 = spo2Values.averageOrNull(),
            stepGoal = stepGoal,
            sleepGoalSeconds = sleepGoal,
            calorieGoal = calorieGoal,
            stepReachedDays = stepReachedDays,
            sleepReachedDays = sleepReachedDays,
            calorieReachedDays = calorieReachedDays
        )
    }

    private fun calculateHealthScore(data: PeriodData): HealthScoreBreakdown {
        val heartRateScore = if (data.heartRateValues.isEmpty()) {
            0
        } else {
            val normalCount = data.heartRateValues.count { it in 60.0..100.0 }
            ((normalCount.toFloat() / data.heartRateValues.size.toFloat()) * 25f)
                .roundToInt()
                .coerceIn(0, 25)
        }

        val spo2Score = if (data.spo2Values.isEmpty()) {
            0
        } else {
            val normalCount = data.spo2Values.count { it >= 95.0 }
            ((normalCount.toFloat() / data.spo2Values.size.toFloat()) * 25f)
                .roundToInt()
                .coerceIn(0, 25)
        }

        val stepScore = when (selectedRange) {
            StatisticsRange.DAY -> {
                val progress = data.totalSteps.toFloat() / data.stepGoal.toFloat()
                (progress.coerceIn(0f, 1f) * 25f).roundToInt()
            }

            StatisticsRange.WEEK,
            StatisticsRange.MONTH -> {
                val progress = data.stepReachedDays.toFloat() / data.daysCount.toFloat()
                (progress.coerceIn(0f, 1f) * 25f).roundToInt()
            }
        }

        val sleepScore = when (selectedRange) {
            StatisticsRange.DAY -> {
                val progress = data.totalSleepSeconds.toFloat() / data.sleepGoalSeconds.toFloat()
                (progress.coerceIn(0f, 1f) * 25f).roundToInt()
            }

            StatisticsRange.WEEK,
            StatisticsRange.MONTH -> {
                val progress = data.sleepReachedDays.toFloat() / data.daysCount.toFloat()
                (progress.coerceIn(0f, 1f) * 25f).roundToInt()
            }
        }

        return HealthScoreBreakdown(
            heartRateScore = heartRateScore.coerceIn(0, 25),
            spo2Score = spo2Score.coerceIn(0, 25),
            stepScore = stepScore.coerceIn(0, 25),
            sleepScore = sleepScore.coerceIn(0, 25)
        )
    }

    private fun buildComparisons(
        current: PeriodData,
        previous: PeriodData
    ): List<ComparisonItem> {
        return listOf(
            buildStepComparison(current, previous),
            buildSleepComparison(current, previous),
            buildHeartRateComparison(current, previous),
            buildSpo2Comparison(current, previous)
        )
    }

    private fun buildStepComparison(
        current: PeriodData,
        previous: PeriodData
    ): ComparisonItem {
        val currentValue = when (selectedRange) {
            StatisticsRange.DAY -> current.totalSteps
            StatisticsRange.WEEK,
            StatisticsRange.MONTH -> current.totalSteps / current.daysCount.coerceAtLeast(1)
        }

        val previousValue = when (selectedRange) {
            StatisticsRange.DAY -> previous.totalSteps
            StatisticsRange.WEEK,
            StatisticsRange.MONTH -> previous.totalSteps / previous.daysCount.coerceAtLeast(1)
        }

        val diff = currentValue - previousValue

        val trend = when {
            previousValue <= 0L -> ComparisonTrend.UNKNOWN
            diff > 0 -> ComparisonTrend.POSITIVE
            diff < 0 -> ComparisonTrend.NEGATIVE
            else -> ComparisonTrend.NEUTRAL
        }

        val title = when (selectedRange) {
            StatisticsRange.DAY -> "Bước chân"
            StatisticsRange.WEEK,
            StatisticsRange.MONTH -> "Bước chân TB/ngày"
        }

        return ComparisonItem(
            title = title,
            currentValue = formatNumber(currentValue),
            previousValue = formatNumber(previousValue),
            changeText = formatStepDiffText(diff),
            trend = trend,
            currentBarRatio = ratioForBar(currentValue.toDouble(), previousValue.toDouble()),
            previousBarRatio = ratioForBar(previousValue.toDouble(), currentValue.toDouble())
        )
    }

    private fun buildSleepComparison(
        current: PeriodData,
        previous: PeriodData
    ): ComparisonItem {
        val currentValue = when (selectedRange) {
            StatisticsRange.DAY -> current.totalSleepSeconds
            StatisticsRange.WEEK,
            StatisticsRange.MONTH -> current.totalSleepSeconds / current.daysCount.coerceAtLeast(1)
        }

        val previousValue = when (selectedRange) {
            StatisticsRange.DAY -> previous.totalSleepSeconds
            StatisticsRange.WEEK,
            StatisticsRange.MONTH -> previous.totalSleepSeconds / previous.daysCount.coerceAtLeast(1)
        }

        val diff = currentValue - previousValue

        val currentDistanceToGoal = abs(currentValue - current.sleepGoalSeconds)
        val previousDistanceToGoal = abs(previousValue - previous.sleepGoalSeconds)

        val trend = when {
            previousValue <= 0L -> ComparisonTrend.UNKNOWN
            currentDistanceToGoal < previousDistanceToGoal -> ComparisonTrend.POSITIVE
            currentDistanceToGoal > previousDistanceToGoal -> ComparisonTrend.NEGATIVE
            else -> ComparisonTrend.NEUTRAL
        }

        val title = when (selectedRange) {
            StatisticsRange.DAY -> "Giấc ngủ"
            StatisticsRange.WEEK,
            StatisticsRange.MONTH -> "Giấc ngủ TB/ngày"
        }

        return ComparisonItem(
            title = title,
            currentValue = formatSleepDuration(currentValue),
            previousValue = formatSleepDuration(previousValue),
            changeText = formatSleepDiffText(diff),
            trend = trend,
            currentBarRatio = ratioForBar(currentValue.toDouble(), previousValue.toDouble()),
            previousBarRatio = ratioForBar(previousValue.toDouble(), currentValue.toDouble())
        )
    }

    private fun buildHeartRateComparison(
        current: PeriodData,
        previous: PeriodData
    ): ComparisonItem {
        val currentAvg = current.averageHeartRate
        val previousAvg = previous.averageHeartRate

        if (currentAvg == null || previousAvg == null) {
            return ComparisonItem(
                title = "Nhịp tim TB",
                currentValue = currentAvg?.let { "${it.roundToInt()} bpm" } ?: "--",
                previousValue = previousAvg?.let { "${it.roundToInt()} bpm" } ?: "--",
                changeText = "Chưa đủ dữ liệu",
                trend = ComparisonTrend.UNKNOWN
            )
        }

        val diff = currentAvg - previousAvg

        val trend = when {
            abs(diff) <= 3.0 -> ComparisonTrend.NEUTRAL
            else -> ComparisonTrend.NEUTRAL
        }

        return ComparisonItem(
            title = "Nhịp tim TB",
            currentValue = "${currentAvg.roundToInt()} bpm",
            previousValue = "${previousAvg.roundToInt()} bpm",
            changeText = if (abs(diff) <= 3.0) "Ổn định" else formatDiffText(diff, "bpm"),
            trend = trend,
            currentBarRatio = ratioForBar(currentAvg, previousAvg),
            previousBarRatio = ratioForBar(previousAvg, currentAvg)
        )
    }

    private fun buildSpo2Comparison(
        current: PeriodData,
        previous: PeriodData
    ): ComparisonItem {
        val currentAvg = current.averageSpo2
        val previousAvg = previous.averageSpo2

        if (currentAvg == null || previousAvg == null) {
            return ComparisonItem(
                title = "SpO2 TB",
                currentValue = currentAvg?.let { "${it.roundToInt()}%" } ?: "--",
                previousValue = previousAvg?.let { "${it.roundToInt()}%" } ?: "--",
                changeText = "Chưa đủ dữ liệu",
                trend = ComparisonTrend.UNKNOWN
            )
        }

        val diff = currentAvg - previousAvg

        val trend = when {
            currentAvg >= 95.0 && diff >= -0.5 -> ComparisonTrend.POSITIVE
            currentAvg < 95.0 -> ComparisonTrend.NEGATIVE
            else -> ComparisonTrend.NEUTRAL
        }

        return ComparisonItem(
            title = "SpO2 TB",
            currentValue = "${currentAvg.roundToInt()}%",
            previousValue = "${previousAvg.roundToInt()}%",
            changeText = if (abs(diff) <= 0.5) "Ổn định" else formatDiffText(diff, "%"),
            trend = trend,
            currentBarRatio = ratioForBar(currentAvg, previousAvg),
            previousBarRatio = ratioForBar(previousAvg, currentAvg)
        )
    }

    private fun buildGoalResults(data: PeriodData): List<GoalResultItem> {
        return listOf(
            GoalResultItem(
                title = "Bước chân",
                resultText = "${data.stepReachedDays}/${data.daysCount} ngày",
                progress = (data.stepReachedDays.toFloat() / data.daysCount.toFloat()).coerceIn(0f, 1f)
            ),
            GoalResultItem(
                title = "Giấc ngủ",
                resultText = "${data.sleepReachedDays}/${data.daysCount} ngày",
                progress = (data.sleepReachedDays.toFloat() / data.daysCount.toFloat()).coerceIn(0f, 1f)
            ),
            GoalResultItem(
                title = "Calo",
                resultText = "${data.calorieReachedDays}/${data.daysCount} ngày",
                progress = (data.calorieReachedDays.toFloat() / data.daysCount.toFloat()).coerceIn(0f, 1f)
            )
        )
    }

    private fun buildInfluenceFactors(score: HealthScoreBreakdown): InfluenceFactors {
        val positive = mutableListOf<String>()
        val improvement = mutableListOf<String>()

        if (score.heartRateScore >= 20) positive.add("Nhịp tim ổn định")
        if (score.spo2Score >= 20) positive.add("SpO2 duy trì tốt")
        if (score.stepScore >= 18) positive.add("Vận động tốt")
        if (score.sleepScore >= 18) positive.add("Giấc ngủ gần đạt mục tiêu")

        if (score.heartRateScore < 15) improvement.add("Nhịp tim cần theo dõi thêm")
        if (score.spo2Score < 15) improvement.add("SpO2 chưa ổn định")
        if (score.stepScore < 15) improvement.add("Vận động chưa đều")
        if (score.sleepScore < 15) improvement.add("Giấc ngủ chưa đạt mục tiêu")

        if (positive.isEmpty()) {
            positive.add("Đã có dữ liệu để theo dõi xu hướng")
        }

        if (improvement.isEmpty()) {
            improvement.add("Chưa có yếu tố cần cải thiện rõ rệt")
        }

        return InfluenceFactors(
            positiveFactors = positive,
            improvementFactors = improvement
        )
    }

    private fun buildPersonalSuggestion(
        data: PeriodData,
        score: HealthScoreBreakdown
    ): PersonalSuggestion {
        val stepRatio = data.stepReachedDays.toFloat() / data.daysCount.toFloat()
        val sleepRatio = data.sleepReachedDays.toFloat() / data.daysCount.toFloat()

        if (score.stepScore < 15) {
            val suggestedGoal = roundToNearest500((data.stepGoal * 0.8).toLong())
                .coerceAtLeast(3000L)

            return PersonalSuggestion(
                title = "Gợi ý mục tiêu bước chân",
                content = "Mục tiêu bước chân hiện tại có thể hơi cao so với thói quen gần đây. Đề xuất thử mức ${formatNumber(suggestedGoal)} bước/ngày trong thời gian tới.",
                suggestedStepGoal = suggestedGoal
            )
        }

        if (stepRatio >= 0.85f && data.daysCount >= 7) {
            val suggestedGoal = roundToNearest500((data.stepGoal * 1.1).toLong())

            return PersonalSuggestion(
                title = "Gợi ý tăng mục tiêu bước chân",
                content = "Bạn đang duy trì mục tiêu bước chân khá tốt. Có thể tăng nhẹ lên ${formatNumber(suggestedGoal)} bước/ngày nếu muốn thử thách hơn.",
                suggestedStepGoal = suggestedGoal
            )
        }

        if (score.sleepScore < 15) {
            val suggestedSleepGoal = 25200L // 7h

            return PersonalSuggestion(
                title = "Gợi ý mục tiêu giấc ngủ",
                content = "Thời gian ngủ gần đây thấp hơn mục tiêu. Có thể đặt mục tiêu ngắn hạn 7h/ngày trước khi tăng dần.",
                suggestedSleepGoalSeconds = suggestedSleepGoal
            )
        }

        if (sleepRatio >= 0.7f) {
            return PersonalSuggestion(
                title = "Gợi ý duy trì thói quen",
                content = "Mục tiêu hiện tại tương đối phù hợp. Nên duy trì nhịp vận động và giờ ngủ ổn định trong các ngày tiếp theo."
            )
        }

        return PersonalSuggestion(
            title = "Gợi ý cá nhân hóa",
            content = "Tiếp tục theo dõi thêm dữ liệu để app đưa ra đề xuất phù hợp hơn với thói quen cá nhân."
        )
    }

    private fun getStepsForDate(
        dateKey: String
    ): Long {
        return stepsHistory[dateKey]?.totalSteps ?: 0L
    }

    private fun getSleepSecondsForDate(dayStartMillis: Long): Long {
        val dayEnd = addDays(dayStartMillis, 1)

        return sleepSessions
            .filter {
                val time = sessionGroupTimeMillis(it)
                time in dayStartMillis until dayEnd
            }
            .sumOf { it.totalSleepSeconds }
    }

    private fun buildCurrentRange(
        range: StatisticsRange,
        selectedDateMillis: Long
    ): PeriodRange {
        val start = when (range) {
            StatisticsRange.DAY -> startOfDayMillis(selectedDateMillis)
            StatisticsRange.WEEK -> startOfWeekMillis(selectedDateMillis)
            StatisticsRange.MONTH -> startOfMonthMillis(selectedDateMillis)
        }

        val end = when (range) {
            StatisticsRange.DAY -> addDays(start, 1)
            StatisticsRange.WEEK -> addDays(start, 7)
            StatisticsRange.MONTH -> addMonths(start, 1)
        }

        return PeriodRange(start, end)
    }

    private fun buildPreviousRange(
        range: StatisticsRange,
        currentRange: PeriodRange
    ): PeriodRange {
        return when (range) {
            StatisticsRange.DAY -> {
                val start = addDays(currentRange.startMillis, -1)
                PeriodRange(start, currentRange.startMillis)
            }

            StatisticsRange.WEEK -> {
                val start = addDays(currentRange.startMillis, -7)
                PeriodRange(start, currentRange.startMillis)
            }

            StatisticsRange.MONTH -> {
                val start = addMonths(currentRange.startMillis, -1)
                PeriodRange(start, currentRange.startMillis)
            }
        }
    }
}

enum class StatisticsRange {
    DAY,
    WEEK,
    MONTH
}

data class StatisticsUiState(
    val selectedRange: StatisticsRange = StatisticsRange.DAY,
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val periodTitle: String = "",
    val previousPeriodTitle: String = "",
    val healthScore: Int = 0,
    val healthScoreLabel: String = "Chưa có",
    val healthScoreDiff: Int = 0,
    val heartRateScore: Int = 0,
    val spo2Score: Int = 0,
    val stepScore: Int = 0,
    val sleepScore: Int = 0,
    val comparisons: List<ComparisonItem> = emptyList(),
    val goalResults: List<GoalResultItem> = emptyList(),
    val positiveFactors: List<String> = emptyList(),
    val improvementFactors: List<String> = emptyList(),
    val suggestionTitle: String = "",
    val suggestionContent: String = "",
    val suggestedStepGoal: Long? = null,
    val suggestedSleepGoalSeconds: Long? = null
)

data class ComparisonItem(
    val title: String,
    val currentValue: String,
    val previousValue: String,
    val changeText: String,
    val trend: ComparisonTrend,
    val currentBarRatio: Float = 0f,
    val previousBarRatio: Float = 0f
)

enum class ComparisonTrend {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
    UNKNOWN
}

data class GoalResultItem(
    val title: String,
    val resultText: String,
    val progress: Float
)

private data class PeriodRange(
    val startMillis: Long,
    val endMillis: Long
)

private data class PeriodData(
    val range: PeriodRange,
    val daysCount: Int,
    val heartRateValues: List<Double>,
    val spo2Values: List<Double>,
    val totalSteps: Long,
    val totalSleepSeconds: Long,
    val totalCalories: Long,
    val averageHeartRate: Double?,
    val averageSpo2: Double?,
    val stepGoal: Long,
    val sleepGoalSeconds: Long,
    val calorieGoal: Long,
    val stepReachedDays: Int,
    val sleepReachedDays: Int,
    val calorieReachedDays: Int
)

private data class HealthScoreBreakdown(
    val heartRateScore: Int,
    val spo2Score: Int,
    val stepScore: Int,
    val sleepScore: Int
) {
    val totalScore: Int
        get() = heartRateScore + spo2Score + stepScore + sleepScore
}

private data class InfluenceFactors(
    val positiveFactors: List<String>,
    val improvementFactors: List<String>
)

private data class PersonalSuggestion(
    val title: String,
    val content: String,
    val suggestedStepGoal: Long? = null,
    val suggestedSleepGoalSeconds: Long? = null
)

private fun MeasurementModel.isValidHeartRate(): Boolean {
    val validStatus = status.isBlank() || status.equals("completed", ignoreCase = true)
    return validStatus && heartRateAvg in 30.0..220.0
}

private fun MeasurementModel.isValidSpo2(): Boolean {
    val validStatus = status.isBlank() || status.equals("completed", ignoreCase = true)
    return validStatus && spo2Avg in 70.0..100.0
}

private fun measurementTimeMillis(measurement: MeasurementModel): Long {
    val raw = if (measurement.endTime > 0L) {
        measurement.endTime
    } else {
        measurement.startTime
    }

    return normalizeTimestampMillis(raw)
}

private fun sessionGroupTimeMillis(session: SleepSessionModel): Long {
    val raw = if (session.endTime > 0L) {
        session.endTime
    } else {
        session.startTime
    }

    return normalizeTimestampMillis(raw)
}

private fun buildDayStarts(
    startMillis: Long,
    endMillis: Long
): List<Long> {
    val result = mutableListOf<Long>()
    var current = startOfDayMillis(startMillis)

    while (current < endMillis) {
        result.add(current)
        current = addDays(current, 1)
    }

    return result
}

private fun calculateDistanceKm(
    steps: Long,
    heightCm: Int
): Double {
    if (steps <= 0L) return 0.0

    val safeHeight = if (heightCm > 0) heightCm.toDouble() else 165.0
    val stepLengthMeter = safeHeight * 0.414 / 100.0

    return steps * stepLengthMeter / 1000.0
}

private fun calculateCaloriesFromSteps(
    steps: Long,
    weightKg: Int,
    heightCm: Int
): Long {
    if (steps <= 0L) return 0L

    val safeWeight = if (weightKg > 0) weightKg.toDouble() else 60.0
    val distanceKm = calculateDistanceKm(steps, heightCm)

    return (safeWeight * distanceKm * 0.5).roundToInt().toLong()
}

private fun List<Double>.averageOrNull(): Double? {
    return if (isEmpty()) null else average()
}

private fun percentChange(
    current: Double,
    previous: Double
): Double? {
    if (previous <= 0.0) return null
    return ((current - previous) / previous) * 100.0
}

private fun formatPercentText(
    percent: Double?,
    diff: Long
): String {
    if (percent == null) return "Chưa đủ dữ liệu"

    val rounded = abs(percent).roundToInt()

    return when {
        diff > 0 -> "↑ +$rounded%"
        diff < 0 -> "↓ -$rounded%"
        else -> "Ổn định"
    }
}

private fun formatDiffText(
    diff: Double,
    unit: String
): String {
    val rounded = abs(diff).roundToInt()

    return when {
        diff > 0 -> "↑ +$rounded $unit"
        diff < 0 -> "↓ -$rounded $unit"
        else -> "Ổn định"
    }
}

private fun ratioForBar(
    value: Double,
    other: Double
): Float {
    val maxValue = maxOf(value, other)

    if (maxValue <= 0.0) return 0f

    return (value / maxValue).toFloat().coerceIn(0f, 1f)
}

private fun healthScoreLabel(score: Int): String {
    return when {
        score >= 85 -> "Rất tốt"
        score >= 70 -> "Tốt"
        score >= 50 -> "Cần cải thiện"
        else -> "Cần chú ý"
    }
}

private fun formatPeriodTitle(
    range: StatisticsRange,
    period: PeriodRange
): String {
    return when (range) {
        StatisticsRange.DAY -> {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(Date(period.startMillis))
        }

        StatisticsRange.WEEK -> {
            "${formatShortDate(period.startMillis)} - ${formatShortDate(addDays(period.endMillis, -1))}"
        }

        StatisticsRange.MONTH -> {
            SimpleDateFormat("'Tháng' MM/yyyy", Locale.getDefault())
                .format(Date(period.startMillis))
        }
    }
}

private fun formatPreviousPeriodTitle(range: StatisticsRange): String {
    return when (range) {
        StatisticsRange.DAY -> "hôm qua"
        StatisticsRange.WEEK -> "tuần trước"
        StatisticsRange.MONTH -> "tháng trước"
    }
}

private fun formatShortDate(timeMillis: Long): String {
    return SimpleDateFormat("dd/MM", Locale.getDefault())
        .format(Date(timeMillis))
}

private fun formatNumber(value: Long): String {
    return "%,d".format(value).replace(",", ".")
}

private fun formatSleepDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "0m"

    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun formatStepDiffText(diff: Long): String {
    return when {
        diff > 0 -> "↑ +${formatNumber(diff)} bước"
        diff < 0 -> "↓ -${formatNumber(abs(diff))} bước"
        else -> "Ổn định"
    }
}

private fun formatSleepDiffText(diffSeconds: Long): String {
    return when {
        diffSeconds > 0 -> "↑ +${formatSleepDuration(diffSeconds)}"
        diffSeconds < 0 -> "↓ -${formatSleepDuration(abs(diffSeconds))}"
        else -> "Ổn định"
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

private fun formatDateKey(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        .format(Date(timeMillis))
}



private fun roundToNearest500(value: Long): Long {
    return ((value + 250L) / 500L) * 500L
}