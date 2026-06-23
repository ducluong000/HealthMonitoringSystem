package com.example.healthapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.model.NotificationModel
import com.example.healthapp.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    private val repository = HealthRepository()

    private val _notifications = MutableStateFlow<List<NotificationModel>>(emptyList())
    val notifications: StateFlow<List<NotificationModel>> = _notifications

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            repository.observeNotifications()
                .catch { e ->
                    _errorMessage.value = e.message
                }
                .collect { data ->
                    _notifications.value = data
                }
        }
    }

    fun markAsRead(notificationId: String) {
        repository.markNotificationAsRead(notificationId)
    }
}