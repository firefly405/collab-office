package com.collaboffice.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.network.api.SearchApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchApi: SearchApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        // Debounced search
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(300)
                search()
            }
        } else {
            _uiState.update {
                it.copy(documents = emptyList(), messages = emptyList(),
                    files = emptyList(), users = emptyList())
            }
        }
    }

    fun setTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun search() {
        val query = _uiState.value.query
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            try {
                val response = searchApi.globalSearch(query)
                if (response.isSuccess) {
                    val data = response.data as? Map<*, *>

                    val docs = (data?.get("documents") as? List<*>)?.map { item ->
                        val m = item as Map<*, *>
                        SearchResult.DocumentResult(
                            docId = m["docId"]?.toString() ?: "",
                            title = m["title"]?.toString() ?: "",
                            snippet = m["snippet"]?.toString() ?: "",
                            updatedAt = m["updatedAt"]?.toString() ?: "",
                            docType = (m["docType"] as? Number)?.toInt() ?: 1
                        )
                    } ?: emptyList()

                    val msgs = (data?.get("messages") as? List<*>)?.map { item ->
                        val m = item as Map<*, *>
                        SearchResult.MessageResult(
                            messageId = m["messageId"]?.toString() ?: "",
                            content = m["content"]?.toString() ?: "",
                            senderName = m["senderName"]?.toString() ?: "",
                            channelName = m["channelName"]?.toString() ?: "",
                            timestamp = m["timestamp"]?.toString() ?: ""
                        )
                    } ?: emptyList()

                    val files = (data?.get("files") as? List<*>)?.map { item ->
                        val m = item as Map<*, *>
                        SearchResult.FileResult(
                            fileId = m["fileId"]?.toString() ?: "",
                            fileName = m["fileName"]?.toString() ?: "",
                            fileSize = (m["fileSize"] as? Number)?.toLong() ?: 0L,
                            filePath = m["filePath"]?.toString() ?: "",
                            mimeType = m["mimeType"]?.toString() ?: ""
                        )
                    } ?: emptyList()

                    val users = (data?.get("users") as? List<*>)?.map { item ->
                        val m = item as Map<*, *>
                        SearchResult.UserResult(
                            userId = m["userId"]?.toString() ?: "",
                            username = m["username"]?.toString() ?: "",
                            nickname = m["nickname"]?.toString() ?: "",
                            avatar = m["avatar"]?.toString()
                        )
                    } ?: emptyList()

                    // Save to recent searches
                    val recent = (_uiState.value.recentSearches + query).distinct().takeLast(10)

                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            documents = docs, messages = msgs,
                            files = files, users = users,
                            recentSearches = recent
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    fun clearRecentSearches() {
        _uiState.update { it.copy(recentSearches = emptyList()) }
    }
}
