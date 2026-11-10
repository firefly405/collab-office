package com.collaboffice.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.model.auth.RegisterRequest
import com.collaboffice.core.model.auth.SmsCodeRequest
import com.collaboffice.core.network.api.AuthApi
import com.collaboffice.core.network.auth.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isSendingCode: Boolean = false,
    val isRegistered: Boolean = false,
    val countdown: Int = 0,
    val error: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun sendSmsCode(phone: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCode = true, error = null) }
            try {
                val response = authApi.sendSmsCode(SmsCodeRequest(phone))
                if (response.isSuccess) {
                    _uiState.update { it.copy(isSendingCode = false, countdown = 60) }
                    // Start countdown
                    startCountdown()
                } else {
                    _uiState.update { it.copy(isSendingCode = false, error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSendingCode = false, error = e.message ?: "发送失败") }
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            for (i in 60 downTo 0) {
                _uiState.update { it.copy(countdown = i) }
                delay(1000)
            }
        }
    }

    fun register(username: String, phone: String, password: String, smsCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = authApi.register(RegisterRequest(username, phone, password, smsCode))
                if (response.isSuccess && response.data != null) {
                    tokenManager.saveTokens(
                        response.data!!.getEffectiveToken(),
                        response.data!!.refreshToken
                    )
                    _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "注册失败") }
            }
        }
    }
}
