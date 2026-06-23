package com.example.healthapp.data.model

data class StepHistoryModel(
    val date: String = "",
    val totalSteps: Long = 0L,
    val updatedAt: Long = 0L,
    val records: Map<String, StepRecordModel> = emptyMap()
)

data class StepRecordModel(
    val timestamp: Long = 0L,
    val steps: Long = 0L,
    val updatedAt: Long = 0L
)