package com.example.healthapp.data.model

data class UserProfileModel(
    val createdAt: Long = 0L,
    val gender: String = "",
    val height: Int = 0,
    val name: String = "",
    val weight: Int = 0,
    val yearOfBirth: Int = 0,
    val stepGoal: Long = 10000L,
    val calorieGoal: Long = 500L,
    val sleepGoalSeconds: Long = 28800L,
    val linkedDeviceId: String = "",
    val profileImageUri: String = ""
)