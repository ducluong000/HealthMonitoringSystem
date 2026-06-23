package com.example.healthapp.ui.utils

import com.example.healthapp.data.model.MeasurementModel
import java.util.Calendar

fun MeasurementModel.isValidHeartRateMeasurement(): Boolean {
    val completed = status.isBlank() || status.equals("completed", ignoreCase = true)
    return completed &&
        heartRateAvg >= 40.0 &&
        heartRateAvg <= 180.0 &&
        measurementTimeMillis(this) > 0L
}

fun MeasurementModel.isValidSpO2Measurement(): Boolean {
    val completed = status.isBlank() || status.equals("completed", ignoreCase = true)
    return completed &&
        spo2Avg in 70.0..100.0 &&
        measurementTimeMillis(this) > 0L
}

fun isMeasurementInDay(measurement: MeasurementModel, selectedDateStartMillis: Long): Boolean {
    val time = measurementTimeMillis(measurement)
    val start = selectedDateStartMillis
    val end = addDays(start, 1)
    return time in start until end
}

fun measurementTimeMillis(measurement: MeasurementModel): Long {
    val rawTime = if (measurement.endTime > 0L) {
        measurement.endTime
    } else {
        measurement.startTime
    }

    return normalizeTimestampMillis(rawTime)
}

fun normalizeTimestampMillis(timestamp: Long): Long {
    if (timestamp <= 0L) return 0L

    return if (timestamp in 1..9_999_999_999L) {
        timestamp * 1000L
    } else {
        timestamp
    }
}

fun addDays(timeMillis: Long, days: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.add(Calendar.DAY_OF_MONTH, days)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
