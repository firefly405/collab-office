package com.collaboffice.android.ui.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.model.user.SpaceInfo
import com.collaboffice.core.network.api.SpaceApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpaceUiState(
    val isLoading: Boolean = false,
    val spaces: List<SpaceInfo> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SpaceViewModel @Inject constructor(
    private val spaceApi: SpaceApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpaceUiState())
    val uiState: StateFlow<SpaceUiState> = _uiState.asStateFlow()

    fun loadSpaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = spaceApi.getMySpaces()
                if (response.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, spaces = response.data ?: emptyList()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun createSpace(name: String, description: String?) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf<String, String>("spaceName" to name)
                description?.let { body["description"] = it }
                val response = spaceApi.createSpace(body)
                if (response.isSuccess) {
                    loadSpaces()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
