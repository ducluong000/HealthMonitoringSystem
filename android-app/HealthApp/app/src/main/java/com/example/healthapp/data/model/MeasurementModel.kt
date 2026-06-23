package com.example.healthapp.data.model

data class MeasurementModel(
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val heartRateAvg: Double = 0.0,
    val spo2Avg: Double = 0.0,
    val status: String = ""
)