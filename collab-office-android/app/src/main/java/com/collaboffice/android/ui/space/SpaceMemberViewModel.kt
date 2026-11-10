package com.collaboffice.android.ui.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.network.api.SpaceApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpaceMemberViewModel @Inject constructor(
    private val spaceApi: SpaceApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpaceMemberUiState())
    val uiState: StateFlow<SpaceMemberUiState> = _uiState.asStateFlow()

    fun loadMembers(spaceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = spaceApi.getMembers(spaceId)
                if (response.isSuccess) {
                    val members = (response.data as? List<*>)?.mapNotNull { item ->
                        val map = item as? Map<*, *> ?: return@mapNotNull null
                        SpaceMember(
                            userId = map["userId"]?.toString() ?: "",
                            username = map["username"]?.toString() ?: "",
                            nickname = map["nickname"]?.toString() ?: "",
                            avatar = map["avatar"]?.toString(),
                            role = map["role"]?.toString() ?: "member",
                            joinTime = map["joinTime"]?.toString() ?: ""
                        )
                    } ?: emptyList()
                    _uiState.update { it.copy(isLoading = false, members = members) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun inviteMember(spaceId: String, username: String) {
        viewModelScope.launch {
            try {
                spaceApi.inviteMember(spaceId, mapOf("username" to username))
                loadMembers(spaceId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "邀请失败: ${e.message}") }
            }
        }
    }

    fun changeMemberRole(spaceId: String, userId: String, newRole: String) {
        viewModelScope.launch {
            try {
                spaceApi.updateMemberRole(spaceId, userId, mapOf("role" to newRole))
                loadMembers(spaceId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "修改角色失败: ${e.message}") }
            }
        }
    }

    fun removeMember(spaceId: String, userId: String) {
        viewModelScope.launch {
            try {
                spaceApi.removeMember(spaceId, userId)
                _uiState.update { it.copy(members = it.members.filter { m -> m.userId != userId }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "移除失败: ${e.message}") }
            }
        }
    }
}
