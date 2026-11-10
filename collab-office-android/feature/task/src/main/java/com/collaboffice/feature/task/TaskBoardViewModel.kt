package com.collaboffice.feature.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.collaboffice.core.model.task.KanbanColumn
import com.collaboffice.core.network.api.TaskApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskBoardUiState(
    val isLoading: Boolean = false,
    val columns: List<KanbanColumn> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TaskBoardViewModel @Inject constructor(
    private val taskApi: TaskApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskBoardUiState())
    val uiState: StateFlow<TaskBoardUiState> = _uiState.asStateFlow()

    fun loadKanban(projectId: String = "default") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = taskApi.getKanban(projectId)
                if (response.isSuccess && response.data != null) {
                    val map = response.data!!
                    val columns = listOf(
                        KanbanColumn(columnId = "todo", name = "待办", tasks = map["todo"] ?: emptyList()),
                        KanbanColumn(columnId = "in_progress", name = "进行中", tasks = map["in_progress"] ?: emptyList()),
                        KanbanColumn(columnId = "done", name = "已完成", tasks = map["done"] ?: emptyList())
                    )
                    _uiState.update { it.copy(isLoading = false, columns = columns, error = null) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateTaskStatus(taskId: String, statusInt: Int, projectId: String = "default") {
        val currentColumns = _uiState.value.columns
        var movedTask: com.collaboffice.core.model.task.Task? = null
        val updatedColumns = currentColumns.map { col ->
            val tasks = col.tasks.filter {
                if (it.taskId == taskId) {
                    movedTask = it.copy(status = statusInt)
                    false
                } else {
                    true
                }
            }
            col.copy(tasks = tasks)
        }.map { col ->
            val targetColumnId = when (statusInt) {
                0 -> "todo"
                1 -> "in_progress"
                2 -> "done"
                else -> "todo"
            }
            if (col.columnId == targetColumnId && movedTask != null) {
                col.copy(tasks = col.tasks + movedTask!!)
            } else {
                col
            }
        }
        _uiState.update { it.copy(columns = updatedColumns) }

        viewModelScope.launch {
            try {
                taskApi.updateStatus(taskId, statusInt)
                loadKanban(projectId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                loadKanban(projectId)
            }
        }
    }

    fun createTask(title: String, projectId: String = "default") {
        viewModelScope.launch {
            try {
                val response = taskApi.createTask(title, projectId)
                if (response.isSuccess) {
                    loadKanban(projectId)
                } else {
                    _uiState.update { it.copy(error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
