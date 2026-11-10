package com.collaboffice.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collaboffice.task.entity.KanbanColumn;
import com.collaboffice.task.entity.TaskDependency;
import com.collaboffice.task.entity.TaskTask;
import com.collaboffice.task.exception.WipLimitExceededException;
import com.collaboffice.task.mapper.KanbanColumnMapper;
import com.collaboffice.task.mapper.TaskDependencyMapper;
import com.collaboffice.task.mapper.TaskTaskMapper;
import com.collaboffice.task.service.impl.KanbanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Gap-3 KanbanService 层单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KanbanService 单元测试 - WIP/子任务/依赖")
class KanbanServiceTest {

    @InjectMocks
    private KanbanServiceImpl kanbanService;

    @Mock
    private KanbanColumnMapper columnMapper;

    @Mock
    private TaskTaskMapper taskMapper;

    @Mock
    private TaskDependencyMapper dependencyMapper;
    @Mock
    private TaskService taskService;

    private KanbanColumn mockColumn;

    @BeforeEach
    void setUp() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),""),TaskTask.class);
        mockColumn = new KanbanColumn();
        mockColumn.setId(1L);
        mockColumn.setProjectId("proj-001");
        mockColumn.setName("进行中");
        mockColumn.setWipLimit(3);
        mockColumn.setIsDoneColumn(0);
    }

    // =================== WIP 限制测试 ===================

    @Test
    @DisplayName("moveTaskToColumn - 当列未达 WIP 上限时移动成功")
    void testMoveTask_WithinWipLimit_Success() {
        when(columnMapper.selectById(1L)).thenReturn(mockColumn);
        when(columnMapper.countActiveTasksInColumn(1L)).thenReturn(2); // 当前2个 < 上限3

        TaskTask mockTask = new TaskTask();
        mockTask.setTaskId("task-001");
        mockTask.setProjectId("proj-001");
        when(taskMapper.selectOne(any())).thenReturn(mockTask);

        assertThatNoException().isThrownBy(() -> kanbanService.moveTaskToColumn("task-001", 1L));
        verify(taskService).updateTaskStatus("task-001",1);
        verify(taskMapper).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("moveTaskToColumn - 当列达到 WIP 上限时抛出 WipLimitExceededException")
    void testMoveTask_WipLimitExceeded_ThrowsException() {
        when(columnMapper.selectById(1L)).thenReturn(mockColumn);
        when(columnMapper.countActiveTasksInColumn(1L)).thenReturn(3); // 当前3个 == 上限3
        TaskTask task=new TaskTask();task.setProjectId("proj-001");when(taskMapper.selectOne(any())).thenReturn(task);

        assertThatThrownBy(() -> kanbanService.moveTaskToColumn("task-001", 1L))
                .isInstanceOf(WipLimitExceededException.class)
                .hasMessageContaining("WIP_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("moveTaskToColumn - WIP 为 0（不限）时不检查上限")
    void testMoveTask_WipLimitZeroMeansUnlimited() {
        mockColumn.setWipLimit(0); // 0 = 不限
        when(columnMapper.selectById(1L)).thenReturn(mockColumn);

        TaskTask mockTask = new TaskTask();
        mockTask.setTaskId("task-001");
        mockTask.setProjectId("proj-001");
        when(taskMapper.selectOne(any())).thenReturn(mockTask);

        assertThatNoException().isThrownBy(() -> kanbanService.moveTaskToColumn("task-001", 1L));
        // WipLimit=0时不调用 countActiveTasksInColumn
        verify(columnMapper, never()).countActiveTasksInColumn(anyLong());
    }

    @Test
    @DisplayName("moveTaskToColumn - 移动到完成列时检查前置依赖")
    void testMoveTask_ToDoneColumn_ChecksDependencies() {
        KanbanColumn doneColumn = new KanbanColumn();
        doneColumn.setId(2L);
        doneColumn.setWipLimit(0);
        doneColumn.setIsDoneColumn(1); // 完成列
        when(columnMapper.selectById(2L)).thenReturn(doneColumn);
        TaskTask task=new TaskTask();when(taskMapper.selectOne(any())).thenReturn(task);
        when(dependencyMapper.countUnfinishedDependencies("task-001")).thenReturn(1); // 有1个未完成依赖

        assertThatThrownBy(() -> kanbanService.moveTaskToColumn("task-001", 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DEPENDENCY_NOT_FULFILLED");
    }

    // =================== 子任务测试 ===================

    @Test
    @DisplayName("getSubtasks - 返回指定父任务的子任务列表")
    void testGetSubtasks_ReturnsList() {
        TaskTask subtask1 = new TaskTask();
        subtask1.setTaskId("sub-001");
        subtask1.setTitle("子任务1");
        subtask1.setParentId("parent-001");
        subtask1.setStatus(0);

        when(taskMapper.selectList(any())).thenReturn(List.of(subtask1));

        List<Map<String, Object>> subtasks = kanbanService.getSubtasks("parent-001");
        assertThat(subtasks).hasSize(1);
        assertThat(subtasks.get(0).get("taskId")).isEqualTo("sub-001");
        assertThat(subtasks.get(0).get("parentId")).isEqualTo("parent-001");
    }

    @Test
    @DisplayName("getSubtasks - 无子任务时返回空列表")
    void testGetSubtasks_Empty() {
        when(taskMapper.selectList(any())).thenReturn(Collections.emptyList());
        List<Map<String, Object>> subtasks = kanbanService.getSubtasks("parent-001");
        assertThat(subtasks).isEmpty();
    }

    // =================== 任务依赖测试 ===================

    @Test
    @DisplayName("addDependency - 正常添加依赖关系")
    void testAddDependency_Success() {
        when(dependencyMapper.selectList(any())).thenReturn(Collections.emptyList()); // 无循环依赖
        when(dependencyMapper.selectCount(any())).thenReturn(0L); // 不存在

        assertThatNoException().isThrownBy(
                () -> kanbanService.addDependency("task-A", "task-B", 1001L));
        verify(dependencyMapper, times(1)).insert(any(TaskDependency.class));
    }

    @Test
    @DisplayName("addDependency - 检测到循环依赖时抛出异常")
    void testAddDependency_CircularDependency() {
        TaskDependency circular = new TaskDependency();
        circular.setTaskId("task-B");
        circular.setDependsOnTaskId("task-A");
        when(dependencyMapper.selectList(any())).thenReturn(List.of(circular));

        assertThatThrownBy(() -> kanbanService.addDependency("task-A", "task-B", 1001L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CIRCULAR_DEPENDENCY");
    }

    @Test
    @DisplayName("getDependencies - 返回依赖任务信息和完成状态")
    void testGetDependencies_WithStatus() {
        TaskDependency dep = new TaskDependency();
        dep.setTaskId("task-A");
        dep.setDependsOnTaskId("task-B");
        when(dependencyMapper.selectByTaskId("task-A")).thenReturn(List.of(dep));

        TaskTask depTask = new TaskTask();
        depTask.setTaskId("task-B");
        depTask.setTitle("前置任务B");
        depTask.setStatus(2); // 已完成
        when(taskMapper.selectOne(any())).thenReturn(depTask);

        List<Map<String, Object>> deps = kanbanService.getDependencies("task-A");
        assertThat(deps).hasSize(1);
        assertThat(deps.get(0).get("dependsOnTitle")).isEqualTo("前置任务B");
        assertThat(deps.get(0).get("isCompleted")).isEqualTo(true);
    }

    @Test void sameColumnMoveIsIdempotentEvenAtWipLimit() {
        when(columnMapper.selectById(1L)).thenReturn(mockColumn);
        TaskTask task=new TaskTask();task.setProjectId("proj-001");task.setColumnId(1L);
        when(taskMapper.selectOne(any())).thenReturn(task);
        kanbanService.moveTaskToColumn("task-001",1L);
        verifyNoInteractions(taskService);verify(columnMapper,never()).countActiveTasksInColumn(anyLong());
    }
    @Test void crossProjectMoveIsRejected() {
        when(columnMapper.selectById(1L)).thenReturn(mockColumn);
        TaskTask task=new TaskTask();task.setProjectId("another-project");when(taskMapper.selectOne(any())).thenReturn(task);
        assertThatThrownBy(()->kanbanService.moveTaskToColumn("task-001",1L)).hasMessageContaining("同一项目");
        verifyNoInteractions(taskService);
    }
    @Test void transitiveCycleIsRejected() {
        when(dependencyMapper.selectList(any())).thenReturn(List.of());
        TaskDependency bc=new TaskDependency();bc.setTaskId("B");bc.setDependsOnTaskId("C");
        TaskDependency ca=new TaskDependency();ca.setTaskId("C");ca.setDependsOnTaskId("A");
        when(dependencyMapper.selectByTaskId("B")).thenReturn(List.of(bc));when(dependencyMapper.selectByTaskId("C")).thenReturn(List.of(ca));
        assertThatThrownBy(()->kanbanService.addDependency("A","B",1L)).hasMessageContaining("CIRCULAR_DEPENDENCY");
        verify(dependencyMapper,never()).insert(any(TaskDependency.class));
    }
}
