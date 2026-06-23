package com.example.healthapp.data.model

data class NotificationModel(
    val id: String = "",
    val type: String = "",
    val level: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
    val isRead: Boolean = false,
    val targetScreen: String = "",
    val source: String = "",
    val sourceId: String = ""
)