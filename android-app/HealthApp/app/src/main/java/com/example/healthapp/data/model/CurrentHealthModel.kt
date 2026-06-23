package com.example.healthapp.data.model

data class CurrentHealthModel(
    val heartRate: Double = 0.0,
    val spo2: Double = 0.0,
    val steps: Long = 0L,
    val lastMeasureTime: Long = 0L,
    val lastStepUpdateTime: Long = 0L,
    val lastVitalMeasureTime: Long = 0L,
    val sleep: CurrentSleepModel? = null
)

data class CurrentSleepModel(
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val totalSleepSeconds: Long = 0L,
    val wakeCount: Int = 0,
    val status: String = ""
)