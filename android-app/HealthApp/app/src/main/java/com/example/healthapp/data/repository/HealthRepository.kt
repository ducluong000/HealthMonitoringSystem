package com.example.healthapp.data.repository

import com.example.healthapp.data.model.CurrentHealthModel
import com.example.healthapp.data.model.UserProfileModel
import com.example.healthapp.data.model.MeasurementModel
import com.example.healthapp.data.model.SleepSessionModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.example.healthapp.data.model.StepHistoryModel
import com.example.healthapp.data.model.StepRecordModel
import com.example.healthapp.data.model.NotificationModel

class HealthRepository {

    private val database = FirebaseDatabase.getInstance().reference

    private val userId: String
        get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "uid_001"

    fun observeCurrentHealth(): Flow<CurrentHealthModel?> = callbackFlow {
        val ref = database
            .child("users")
            .child(userId)
            .child("current")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(CurrentHealthModel::class.java)
                trySend(data)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            ref.removeEventListener(listener)
        }
    }
    fun observeUserProfile(): Flow<UserProfileModel?> = callbackFlow {
        val ref = database
            .child("users")
            .child(userId)
            .child("profile")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(UserProfileModel::class.java)
                trySend(data)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    fun saveUserProfile(profile: UserProfileModel) {
        database
            .child("users")
            .child(userId)
            .child("profile")
            .setValue(profile)
    }

    fun updateUserProfileFields(fields: Map<String, Any?>) {
        if (fields.isEmpty()) return

        database
            .child("users")
            .child(userId)
            .child("profile")
            .updateChildren(fields)
    }

    fun observeMeasurements(): Flow<List<MeasurementModel>> = callbackFlow {
        val ref = database
            .child("users")
            .child(userId)
            .child("measurements")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    MeasurementModel(
                        startTime = child.childLong("startTime"),
                        endTime = child.childLong("endTime"),
                        heartRateAvg = child.childDouble("heartRateAvg"),
                        spo2Avg = child.childDouble("spo2Avg"),
                        status = child.childString("status")
                    )
                }.sortedBy {
                    if (it.endTime > 0L) it.endTime else it.startTime
                }

                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            ref.removeEventListener(listener)
        }
    }
    fun observeStepsHistory(): Flow<Map<String, StepHistoryModel>> = callbackFlow {
        val ref = database
            .child("users")
            .child(userId)
            .child("steps_history")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val result = mutableMapOf<String, StepHistoryModel>()

                snapshot.children.forEach { daySnapshot ->
                    val dateKey = daySnapshot.key ?: return@forEach

                    // Bỏ qua dữ liệu lỗi do chưa đồng bộ NTP
                    if (dateKey == "1970-01-01") return@forEach

                    val records = mutableMapOf<String, StepRecordModel>()

                    daySnapshot.child("records").children.forEach { recordSnapshot ->
                        val recordKey = recordSnapshot.key ?: return@forEach
                        val timestampFromKey = recordKey.toLongOrNull() ?: 0L

                        val updatedAt = recordSnapshot.childLong("updatedAt")
                            .takeIf { it > 0L }
                            ?: timestampFromKey

                        records[recordKey] = StepRecordModel(
                            timestamp = timestampFromKey,
                            steps = recordSnapshot.childLong("steps"),
                            updatedAt = updatedAt
                        )
                    }

                    result[dateKey] = StepHistoryModel(
                        date = dateKey,
                        totalSteps = daySnapshot.childLong("totalSteps"),
                        updatedAt = daySnapshot.childLong("updatedAt"),
                        records = records.toSortedMap()
                    )
                }

                trySend(result.toSortedMap())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            ref.removeEventListener(listener)
        }
    }
    fun observeSleepSessions(): Flow<List<SleepSessionModel>> = callbackFlow {
        val ref = database
            .child("users")
            .child(userId)
            .child("sleep_sessions")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null

                    SleepSessionModel(
                        id = id,
                        startTime = child.childLong("startTime"),
                        endTime = child.childLong("endTime"),
                        totalSleepSeconds = child.childLong("totalSleepSeconds"),
                        wakeCount = child.childInt("wakeCount"),
                        status = child.childString("status")
                    )
                }.sortedByDescending {
                    if (it.endTime > 0L) it.endTime else it.startTime
                }

                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            ref.removeEventListener(listener)
        }
    }
    fun observeNotifications(): Flow<List<NotificationModel>> = callbackFlow {
        val ref = database
            .child("users")
            .child(userId)
            .child("notifications")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null

                    NotificationModel(
                        id = id,
                        type = child.childString("type"),
                        level = child.childString("level"),
                        title = child.childString("title"),
                        message = child.childString("message"),
                        createdAt = child.childLong("createdAt"),
                        isRead = child.childBoolean("isRead"),
                        targetScreen = child.childString("targetScreen"),
                        source = child.childString("source"),
                        sourceId = child.childString("sourceId")
                    )
                }.sortedByDescending {
                    it.createdAt
                }

                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            ref.removeEventListener(listener)
        }
    }
    fun markNotificationAsRead(notificationId: String) {
        if (notificationId.isBlank()) return

        database
            .child("users")
            .child(userId)
            .child("notifications")
            .child(notificationId)
            .child("isRead")
            .setValue(true)
    }

}
private fun DataSnapshot.childLong(name: String): Long {
    return when (val value = child(name).value) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Float -> value.toLong()
        is String -> value.toLongOrNull() ?: 0L
        else -> 0L
    }
}
private fun DataSnapshot.childInt(name: String): Int {
    return when (val value = child(name).value) {
        is Int -> value
        is Long -> value.toInt()
        is Double -> value.toInt()
        is Float -> value.toInt()
        is String -> value.toIntOrNull() ?: 0
        else -> 0
    }
}

private fun DataSnapshot.childDouble(name: String): Double {
    return when (val value = child(name).value) {
        is Double -> value
        is Float -> value.toDouble()
        is Long -> value.toDouble()
        is Int -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
}

private fun DataSnapshot.childString(name: String): String {
    return child(name).getValue(String::class.java) ?: ""
}
private fun DataSnapshot.childBoolean(name: String): Boolean {
    return when (val value = child(name).value) {
        is Boolean -> value
        is String -> value.toBooleanStrictOrNull() ?: false
        is Long -> value != 0L
        is Int -> value != 0
        else -> false
    }
}