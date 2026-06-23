package com.example.healthapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.healthapp.MainActivity
import com.example.healthapp.R
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "health_alerts"
        private const val CHANNEL_NAME = "Thông báo sức khỏe"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "HealthApp"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
            ?: "Bạn có thông báo mới."

        showNotification(title, body)
    }

    private fun saveFcmToken(token: String) {
        // Tạm thời dùng uid_001 vì app của bạn chưa làm đăng nhập
        // Sau này đổi thành FirebaseAuth.getInstance().currentUser?.uid
        val uid = "uid_001"

        val deviceId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"

        val data = mapOf(
            "token" to token,
            "updatedAt" to System.currentTimeMillis() / 1000
        )

        FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(uid)
            .child("fcmTokens")
            .child(deviceId)
            .setValue(data)
    }

    private fun showNotification(
        title: String,
        body: String
    ) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Random.nextInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}