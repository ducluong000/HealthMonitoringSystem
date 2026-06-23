package com.example.healthapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.model.CurrentHealthModel
import com.example.healthapp.data.model.MeasurementModel
import com.example.healthapp.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar

class Spo2ViewModel : ViewModel() {

    private val repository = HealthRepository()

    private val _currentHealth = MutableStateFlow(CurrentHealthModel())
    val currentHealth: StateFlow<CurrentHealthModel> = _currentHealth

    private val _measurements = MutableStateFlow<List<MeasurementModel>>(emptyList())
    val measurements: StateFlow<List<MeasurementModel>> = _measurements

    private val _selectedDateStartMillis = MutableStateFlow(startOfTodayMillis())
    val selectedDateStartMillis: StateFlow<Long> = _selectedDateStartMillis

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var currentHealthJob: kotlinx.coroutines.Job? = null
    private var measurementsJob: kotlinx.coroutines.Job? = null

    init {
        observeCurrentHealth()
        observeMeasurements()
    }

    fun refresh() {
        currentHealthJob?.cancel()
        measurementsJob?.cancel()
        _measurements.value = emptyList()
        _currentHealth.value = CurrentHealthModel()
        _selectedDateStartMillis.value = startOfTodayMillis()
        observeCurrentHealth()
        observeMeasurements()
    }

    fun selectDate(dayStartMillis: Long) {
        _selectedDateStartMillis.value = startOfDayMillis(dayStartMillis)
    }

    private fun observeCurrentHealth() {
        currentHealthJob = viewModelScope.launch {
            repository.observeCurrentHealth()
                .catch { e ->
                    _errorMessage.value = e.message
                }
                .collect { data ->
                    _currentHealth.value = data ?: CurrentHealthModel()
                }
        }
    }

    private fun observeMeasurements() {
        measurementsJob = viewModelScope.launch {
            repository.observeMeasurements()
                .catch { e ->
                    _errorMessage.value = e.message
                }
                .collect { data ->
                    _measurements.value = data
                }
        }
    }
}

private fun startOfTodayMillis(): Long {
    return startOfDayMillis(System.currentTimeMillis())
}

private fun startOfDayMillis(timeMillis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
