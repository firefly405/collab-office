package com.collaboffice.feature.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.model.doc.DocVersion
import com.collaboffice.core.network.api.DocumentApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VersionHistoryUiState(
    val isLoading: Boolean = false,
    val versions: List<DocVersion> = emptyList()
)

@HiltViewModel
class VersionHistoryViewModel @Inject constructor(
    private val documentApi: DocumentApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(VersionHistoryUiState())
    val uiState: StateFlow<VersionHistoryUiState> = _uiState.asStateFlow()

    fun loadVersions(docId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = documentApi.getVersions(docId)
                if (response.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, versions = response.data ?: emptyList()) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
