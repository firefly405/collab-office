package com.collaboffice.feature.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.model.doc.Document
import com.collaboffice.core.network.api.DocumentApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DocListUiState(
    val isLoading: Boolean = false,
    val documents: List<Document> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DocListViewModel @Inject constructor(
    private val documentApi: DocumentApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocListUiState())
    val uiState: StateFlow<DocListUiState> = _uiState.asStateFlow()

    fun loadDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = documentApi.getDocuments()
                if (response.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, documents = response.data ?: emptyList()) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun createDocument(type: Int, title: String) {
        viewModelScope.launch {
            try {
                documentApi.createDocument(mapOf("title" to title, "type" to type))
                loadDocuments()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
