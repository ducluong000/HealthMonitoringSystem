package com.example.healthapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import com.example.healthapp.ui.navigation.AppNavigation
import com.example.healthapp.ui.theme.HealthAppTheme
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()
        saveCurrentFcmToken()

        enableEdgeToEdge()

        setContent {
            HealthAppTheme {
                AppNavigation()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun saveCurrentFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                saveFcmTokenToFirebase(token)
            }
    }

    private fun saveFcmTokenToFirebase(token: String) {
        // Tạm thời dùng uid_001 vì chưa làm đăng nhập
        // Sau này đổi thành FirebaseAuth.currentUser.uid
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
}