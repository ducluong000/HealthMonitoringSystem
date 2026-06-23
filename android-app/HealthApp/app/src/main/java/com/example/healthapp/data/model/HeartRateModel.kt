package com.example.healthapp.data.model

enum class HeartRateType { HIGH, NORMAL, LOW }

data class HeartRateEntry(
    val id: Int,
    val bpm: Int,
    val status: String,
    val time: String,
    val type: HeartRateType
)

val heartRateData = listOf(
    HeartRateEntry(1, 112, "Cao", "14:20, Hôm nay", HeartRateType.HIGH),
    HeartRateEntry(2, 82, "Bình thường", "08:45, Hôm nay", HeartRateType.NORMAL),
    HeartRateEntry(3, 74, "Bình thường", "23:10, Hôm qua", HeartRateType.NORMAL),
    HeartRateEntry(4, 58, "Thấp", "04:30, Hôm qua", HeartRateType.LOW),
    HeartRateEntry(5, 79, "Bình thường", "18:15, 22 Tháng 05", HeartRateType.NORMAL)
)