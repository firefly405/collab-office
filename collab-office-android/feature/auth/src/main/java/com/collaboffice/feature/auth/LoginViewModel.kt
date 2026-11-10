package com.collaboffice.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.model.auth.LoginRequest
import com.collaboffice.core.network.api.AuthApi
import com.collaboffice.core.network.auth.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState(isLoggedIn = tokenManager.isLoggedIn))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(account: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = authApi.login(LoginRequest(account, password))
                if (response.isSuccess && response.data != null) {
                    tokenManager.saveTokens(
                        response.data!!.getEffectiveToken(),
                        response.data!!.refreshToken
                    )
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "登录失败") }
            }
        }
    }

    fun onBiometricSuccess() {
        if (tokenManager.isLoggedIn) {
            _uiState.update { it.copy(isLoggedIn = true) }
        } else {
            _uiState.update { it.copy(error = "无法进行生物识别登录：请先输入账号密码进行首次登录") }
        }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
}
