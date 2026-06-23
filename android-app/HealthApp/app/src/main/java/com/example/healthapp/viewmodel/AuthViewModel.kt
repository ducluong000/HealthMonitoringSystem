package com.example.healthapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.healthapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AuthTarget {
    LOGIN,
    HOME
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val target: AuthTarget? = null
)

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun checkAuthState() {
        val uid = repository.currentUid()

        if (uid == null) {
            _uiState.value = AuthUiState(target = AuthTarget.LOGIN)
            return
        }

        _uiState.value = AuthUiState(target = AuthTarget.HOME)
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Vui lòng nhập email và mật khẩu"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        repository.login(email, password) { result ->
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Đăng nhập thất bại"
                )
            }
        }
    }

    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Vui lòng nhập đầy đủ thông tin"
            )
            return
        }

        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Mật khẩu xác nhận không khớp"
            )
            return
        }

        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Mật khẩu cần ít nhất 6 ký tự"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        repository.register(email, password) { result ->
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Đăng ký thất bại"
                )
            }
        }
    }


    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState(target = AuthTarget.LOGIN)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}