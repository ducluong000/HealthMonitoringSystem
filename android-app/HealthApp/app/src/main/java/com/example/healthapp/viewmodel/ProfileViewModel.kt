
package com.example.healthapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.model.UserProfileModel
import com.example.healthapp.data.repository.AuthRepository
import com.example.healthapp.data.repository.HealthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class DevicePairUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel : ViewModel() {

    private val repository = HealthRepository()
    private val authRepository = AuthRepository()

    private val _userProfile = MutableStateFlow(UserProfileModel())
    val userProfile: StateFlow<UserProfileModel> = _userProfile

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _devicePairState = MutableStateFlow(DevicePairUiState())
    val devicePairState: StateFlow<DevicePairUiState> = _devicePairState

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage

    init {
        observeUserProfile()
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            repository.observeUserProfile()
                .catch { e -> _errorMessage.value = e.message }
                .collect { data ->
                    _userProfile.value = data ?: UserProfileModel()
                }
        }
    }

    fun saveUserProfile(profile: UserProfileModel) {
        repository.saveUserProfile(profile)
    }

    fun updateUserProfileFields(fields: Map<String, Any?>) {
        repository.updateUserProfileFields(fields)
    }

    /**
     * Copies the selected image from gallery/camera to internal storage,
     * then saves the file path to Firebase profile.
     */
    fun updateProfileImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            try {
                val savedPath = withContext(Dispatchers.IO) {
                    saveImageToInternalStorage(context, uri)
                }
                if (savedPath != null) {
                    updateUserProfileFields(mapOf("profileImageUri" to savedPath))
                } else {
                    _errorMessage.value = "Không thể lưu ảnh đại diện"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Lỗi khi cập nhật ảnh đại diện"
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    /**
     * Copies image content from a content:// URI to app-private internal storage.
     * Returns the absolute path of the saved file.
     */
    private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val profileImagesDir = File(context.filesDir, "profile_images")
            if (!profileImagesDir.exists()) {
                profileImagesDir.mkdirs()
            }
            val imageFile = File(profileImagesDir, "avatar_${System.currentTimeMillis()}.jpg")

            // Clean up old profile images to save space
            profileImagesDir.listFiles()?.forEach { old ->
                if (old.name != imageFile.name) old.delete()
            }

            FileOutputStream(imageFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            imageFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun pairDevice(
        deviceId: String,
        pairCode: String,
        onSuccess: () -> Unit
    ) {
        _devicePairState.value = DevicePairUiState(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        authRepository.pairDevice(
            deviceId = deviceId,
            pairCode = pairCode
        ) { result ->
            if (result.isSuccess) {
                _devicePairState.value = DevicePairUiState(
                    successMessage = "Kết nối thiết bị thành công"
                )
                onSuccess()
            } else {
                _devicePairState.value = DevicePairUiState(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                        ?: "Ghép thiết bị thất bại"
                )
            }
        }
    }

    fun unlinkDevice(
        deviceId: String,
        onSuccess: () -> Unit
    ) {
        _devicePairState.value = DevicePairUiState(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        authRepository.unlinkDevice(deviceId) { result ->
            if (result.isSuccess) {
                _devicePairState.value = DevicePairUiState(
                    successMessage = "Đã hủy liên kết thiết bị"
                )
                onSuccess()
            } else {
                _devicePairState.value = DevicePairUiState(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                        ?: "Hủy liên kết thất bại"
                )
            }
        }
    }

    fun clearDevicePairState() {
        _devicePairState.value = DevicePairUiState()
    }

    fun clearDevicePairMessage() {
        _devicePairState.value = _devicePairState.value.copy(successMessage = null)
    }
}
