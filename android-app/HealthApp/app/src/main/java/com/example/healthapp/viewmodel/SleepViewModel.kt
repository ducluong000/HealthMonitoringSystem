package com.example.healthapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.model.CurrentHealthModel
import com.example.healthapp.data.model.SleepSessionModel
import com.example.healthapp.data.model.UserProfileModel
import com.example.healthapp.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SleepViewModel : ViewModel() {

    private val repository = HealthRepository()

    private val _currentHealth = MutableStateFlow(CurrentHealthModel())
    val currentHealth: StateFlow<CurrentHealthModel> = _currentHealth

    private val _userProfile = MutableStateFlow(UserProfileModel())
    val userProfile: StateFlow<UserProfileModel> = _userProfile

    private val _sleepSessions = MutableStateFlow<List<SleepSessionModel>>(emptyList())
    val sleepSessions: StateFlow<List<SleepSessionModel>> = _sleepSessions

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        observeCurrentHealth()
        observeUserProfile()
        observeSleepSessions()
    }

    private fun observeCurrentHealth() {
        viewModelScope.launch {
            repository.observeCurrentHealth()
                .catch { e ->
                    _errorMessage.value = e.message
                }
                .collect { data ->
                    _currentHealth.value = data ?: CurrentHealthModel()
                }
        }
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            repository.observeUserProfile()
                .catch { e ->
                    _errorMessage.value = e.message
                }
                .collect { data ->
                    _userProfile.value = data ?: UserProfileModel()
                }
        }
    }

    private fun observeSleepSessions() {
        viewModelScope.launch {
            repository.observeSleepSessions()
                .catch { e ->
                    _errorMessage.value = e.message
                }
                .collect { data ->
                    _sleepSessions.value = data
                }
        }
    }
}