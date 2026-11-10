package com.collaboffice.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.model.user.UserInfo
import com.collaboffice.core.network.api.AuthApi
import com.collaboffice.core.network.api.UserApi
import com.collaboffice.core.network.auth.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val userInfo: UserInfo? = null,
    val storageUsed: String = "0 MB",
    val storageQuota: String = "10 GB",
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userApi: UserApi,
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = userApi.getUserInfo()
                if (response.isSuccess && response.data != null) {
                    _uiState.update { it.copy(isLoading = false, userInfo = response.data) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { authApi.logout() } catch (_: Exception) {}
            tokenManager.clear()
        }
    }
}
