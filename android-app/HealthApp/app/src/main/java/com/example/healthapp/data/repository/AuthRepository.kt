
package com.example.healthapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    fun currentUid(): String? {
        return auth.currentUser?.uid
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun login(
        email: String,
        password: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(
                        Result.failure(
                            task.exception ?: Exception("Đăng nhập thất bại")
                        )
                    )
                }
            }
    }

    fun register(
        email: String,
        password: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(
                        Result.failure(
                            task.exception ?: Exception("Đăng ký thất bại")
                        )
                    )
                }
            }
    }

    fun pairDevice(
        deviceId: String,
        pairCode: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val uid = currentUid()

        if (uid == null) {
            onResult(Result.failure(Exception("Bạn chưa đăng nhập")))
            return
        }

        val cleanDeviceId = deviceId.trim()
        val cleanPairCode = pairCode.trim()

        if (cleanDeviceId.isBlank() || cleanPairCode.isBlank()) {
            onResult(Result.failure(Exception("Vui lòng nhập ID thiết bị và mã ghép")))
            return
        }

        val deviceRef = database
            .child("devices")
            .child(cleanDeviceId)

        deviceRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    onResult(Result.failure(Exception("Không tìm thấy thiết bị")))
                    return@addOnSuccessListener
                }

                val firebasePairCode = snapshot.child("pairCode").value
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val currentOwnerUid = snapshot.child("ownerUid").value
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                if (firebasePairCode != cleanPairCode) {
                    onResult(Result.failure(Exception("Mã ghép không đúng")))
                    return@addOnSuccessListener
                }

                if (currentOwnerUid.isNotBlank() && currentOwnerUid != uid) {
                    onResult(Result.failure(Exception("Thiết bị đã được liên kết với tài khoản khác")))
                    return@addOnSuccessListener
                }

                val now = System.currentTimeMillis() / 1000
                val newPairCode = generatePairCode()

                val updates = mapOf<String, Any>(
                    "devices/$cleanDeviceId/ownerUid" to uid,
                    "devices/$cleanDeviceId/pairedAt" to now,
                    "devices/$cleanDeviceId/pairCode" to newPairCode,
                    "devices/$cleanDeviceId/pairCodeUpdatedAt" to now,
                    "users/$uid/profile/linkedDeviceId" to cleanDeviceId
                )

                database.updateChildren(updates)
                    .addOnSuccessListener {
                        onResult(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        onResult(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    fun unlinkDevice(
        deviceId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val uid = currentUid()

        if (uid == null) {
            onResult(Result.failure(Exception("Bạn chưa đăng nhập")))
            return
        }

        val cleanDeviceId = deviceId.trim()

        if (cleanDeviceId.isBlank()) {
            onResult(Result.failure(Exception("Không tìm thấy thiết bị để hủy liên kết")))
            return
        }

        val updates = mapOf<String, Any>(
            "devices/$cleanDeviceId/ownerUid" to "",
            "devices/$cleanDeviceId/pairedAt" to 0,
            "users/$uid/profile/linkedDeviceId" to ""
        )

        database.updateChildren(updates)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    private fun generatePairCode(): String {
        return (100000..999999).random().toString()
    }

    fun logout() {
        auth.signOut()
    }
}