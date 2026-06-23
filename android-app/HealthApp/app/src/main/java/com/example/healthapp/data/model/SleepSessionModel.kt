package com.example.healthapp.data.model

data class SleepSessionModel(
    val id: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val totalSleepSeconds: Long = 0L,
    val wakeCount: Int = 0,
    val status: String = ""
)