package com.example.healthapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.model.CurrentHealthModel
import com.example.healthapp.data.model.StepHistoryModel
import com.example.healthapp.data.model.UserProfileModel
import com.example.healthapp.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepsViewModel : ViewModel() {

    private val repository = HealthRepository()

    private val _currentHealth = MutableStateFlow(CurrentHealthModel())
    val currentHealth: StateFlow<CurrentHealthModel> = _currentHealth

    private val _userProfile = MutableStateFlow(UserProfileModel())
    val userProfile: StateFlow<UserProfileModel> = _userProfile

    private val _stepsHistory = MutableStateFlow<Map<String, StepHistoryModel>>(emptyMap())
    val stepsHistory: StateFlow<Map<String, StepHistoryModel>> = _stepsHistory

    private val _selectedTab = MutableStateFlow("Ngày")
    val selectedTab: StateFlow<String> = _selectedTab

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        observeCurrentHealth()
        observeUserProfile()
        observeStepsHistory()
    }

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

    fun selectDate(timeMillis: Long) {
        _selectedDateMillis.value = timeMillis
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

    private fun observeStepsHistory() {
        viewModelScope.launch {
            repository.observeStepsHistory()
                .catch { e ->
                    _errorMessage.value = e.message
                }
                .collect { data ->
                    _stepsHistory.value = data
                }
        }
    }

    fun getStepGoal(): Long {
        return userProfile.value.stepGoal.takeIf { it > 0L } ?: 10000L
    }

    fun getTodaySteps(): Long {
        return currentHealth.value.steps
    }

    fun getSelectedDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(selectedDateMillis.value))
    }

    fun getSelectedDayHistory(): StepHistoryModel? {
        return stepsHistory.value[getSelectedDateKey()]
    }
}