<template>
  <div class="task-list-page">
    <!-- Toolbar Area -->
    <div class="toolbar-section">
      <div class="toolbar-top">
        <div class="toolbar-left">
          <div class="breadcrumb">
            <span class="breadcrumb-item">项目</span>
            <span class="material-symbols-outlined breadcrumb-sep">chevron_right</span>
            <span class="breadcrumb-item active">当前迭代</span>
          </div>
          <h2 class="page-title">任务管理看板</h2>
        </div>
        <div class="toolbar-right">
          <button class="action-btn" @click="filterVisible = !filterVisible">
            <span class="material-symbols-outlined">filter_list</span>
            <span>筛选</span>
          </button>
          <button class="action-btn">
            <span class="material-symbols-outlined">sort</span>
            <span>排序</span>
          </button>
          <button class="create-btn" @click="showCreate = true">
            <span class="material-symbols-outlined">add</span>
            <span>新建任务</span>
          </button>
        </div>
      </div>

      <!-- Filter Bar -->
      <div class="filter-bar" v-show="filterVisible">
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable size="small" style="width: 130px">
          <el-option label="全部状态" :value="null" />
          <el-option label="待办" :value="0" />
          <el-option label="进行中" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="已取消" :value="3" />
        </el-select>
        <el-select v-model="filterPriority" placeholder="优先级" clearable size="small" style="width: 120px">
          <el-option label="全部优先级" :value="null" />
          <el-option label="紧急" :value="1" />
          <el-option label="高" :value="2" />
          <el-option label="中" :value="3" />
          <el-option label="低" :value="4" />
        </el-select>
        <div class="search-wrap">
          <span class="material-symbols-outlined search-icon">search</span>
          <input
            v-model="searchKeyword"
            class="search-input"
            placeholder="搜索任务、文档或成员..."
          />
        </div>
      </div>

      <!-- Batch Actions Toolbar -->
      <div class="batch-toolbar" v-if="selectedRows.length > 0">
        <div class="batch-left">
          <span class="batch-count">{{ selectedRows.length }} 项已选择</span>
          <div class="batch-divider"></div>
          <button class="batch-action">
            <span class="material-symbols-outlined">edit</span>
            <span>修改状态</span>
          </button>
          <button class="batch-action">
            <span class="material-symbols-outlined">person_add</span>
            <span>分配负责人</span>
          </button>
          <button class="batch-action">
            <span class="material-symbols-outlined">calendar_today</span>
            <span>批量延期</span>
          </button>
        </div>
        <div class="batch-right">
          <button class="batch-action danger">
            <span class="material-symbols-outlined">delete</span>
            <span>删除</span>
          </button>
          <button class="batch-clear" @click="clearSelection">取消选择</button>
        </div>
      </div>
    </div>

    <!-- Data Table -->
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th class="col-check">
              <input
                type="checkbox"
                :checked="allSelected"
                :indeterminate="someSelected && !allSelected"
                @change="toggleAll"
              />
            </th>
            <th class="col-name">任务名称</th>
            <th class="col-status">状态</th>
            <th class="col-priority">优先级</th>
            <th class="col-assignee">负责人</th>
            <th class="col-date">截止日期</th>
            <th class="col-progress">进度</th>
            <th class="col-actions"></th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in filteredTasks"
            :key="row.id"
            class="data-row"
            :class="{ 'row-selected': selectedIds.has(row.id) }"
          >
            <td class="col-check">
              <input
                type="checkbox"
                :checked="selectedIds.has(row.id)"
                @change="toggleRow(row)"
              />
            </td>
            <td class="col-name" @click="openDetail(row)">
              <div class="name-cell">
                <span class="material-symbols-outlined name-icon">description</span>
                <span class="name-text" :class="{ completed: row.status === 2 }">{{ row.title }}</span>
              </div>
            </td>
            <td class="col-status">
              <span class="status-tag" :class="'status-' + row.status">
                {{ statusLabel(row.status) }}
              </span>
            </td>
            <td class="col-priority">
              <span class="priority-badge-sm" :class="'pri-' + row.priority">
                {{ priorityLabel(row.priority) }}
              </span>
            </td>
            <td class="col-assignee">
              <div class="assignee-cell">
                <el-avatar v-if="row.assigneeName" :size="24" :style="{ backgroundColor: 'var(--c-brand)' }">
                  {{ row.assigneeName[0] }}
                </el-avatar>
                <span v-else class="unassigned">未分配</span>
                <span v-if="row.assigneeName" class="assignee-name">{{ row.assigneeName }}</span>
              </div>
            </td>
            <td class="col-date" :class="{ overdue: isOverdue(row) }">
              {{ row.dueDate || '无' }}
            </td>
            <td class="col-progress">
              <div class="progress-cell">
                <span class="progress-pct">{{ row.progress || 0 }}%</span>
                <div class="progress-track">
                  <div
                    class="progress-fill"
                    :class="{ 'fill-done': row.status === 2 }"
                    :style="{ width: (row.progress || 0) + '%' }"
                  ></div>
                </div>
              </div>
            </td>
            <td class="col-actions">
              <button class="row-more-btn">
                <span class="material-symbols-outlined">more_horiz</span>
              </button>
            </td>
          </tr>
          <tr v-if="filteredTasks.length === 0">
            <td colspan="8" class="empty-row">暂无匹配的任务</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <footer class="pagination-footer">
      <div class="page-info">
        显示 {{ filteredTasks.length }} 个任务
      </div>
      <div class="page-controls">
        <button class="page-btn" disabled>
          <span class="material-symbols-outlined">chevron_left</span>
        </button>
        <button class="page-btn page-num active">1</button>
        <button class="page-btn page-num">2</button>
        <button class="page-btn page-num">3</button>
        <span class="page-ellipsis">...</span>
        <button class="page-btn page-num">25</button>
        <button class="page-btn">
          <span class="material-symbols-outlined">chevron_right</span>
        </button>
      </div>
    </footer>

    <!-- Collaborative Presence -->
    <div class="presence-bar">
      <div class="presence-avatars">
        <el-avatar :size="32" :style="{ backgroundColor: 'var(--c-brand)' }">A</el-avatar>
        <el-avatar :size="32" :style="{ backgroundColor: 'var(--c-online)' }">B</el-avatar>
        <div class="presence-extra">+3</div>
      </div>
      <div class="presence-text">
        <span class="presence-title">5人正在协作</span>
        <span class="presence-sub">实时更新中...</span>
      </div>
    </div>

    <!-- Detail Drawer -->
    <TaskDetailDrawer
      :visible="drawerVisible"
      :task="selectedTask"
      @update:visible="drawerVisible = $event"
    />

    <!-- Create Dialog -->
    <el-dialog v-model="showCreate" title="新建任务" width="440px">
      <el-form :model="newTask" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="newTask.title" placeholder="任务标题" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="newTask.priority" style="width: 100%">
            <el-option label="紧急" :value="1" />
            <el-option label="高" :value="2" />
            <el-option label="中" :value="3" />
            <el-option label="低" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="newTask.dueDate" type="date" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="createTaskFn">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTaskList, createTask } from '@/api/task'
import TaskDetailDrawer from './components/TaskDetailDrawer.vue'

const loading = ref(false)
const filterStatus = ref<number | null>(null)
const filterPriority = ref<number | null>(null)
const searchKeyword = ref('')
const drawerVisible = ref(false)
const showCreate = ref(false)
const selectedTask = ref<any>({})
const tasks = ref<any[]>([])
const filterVisible = ref(true)
const selectedIds = ref<Set<string>>(new Set())
const selectedRows = computed(() => filteredTasks.value.filter(t => selectedIds.value.has(t.id)))

const newTask = ref({ title: '', priority: 2, dueDate: '' })

const filteredTasks = computed(() => {
  let list = tasks.value
  if (filterStatus.value !== null) {
    list = list.filter(t => t.status === filterStatus.value)
  }
  if (filterPriority.value !== null) {
    list = list.filter(t => t.priority === filterPriority.value)
  }
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(t => (t.title || '').toLowerCase().includes(kw))
  }
  return list
})

const allSelected = computed(() => filteredTasks.value.length > 0 && filteredTasks.value.every(t => selectedIds.value.has(t.id)))
const someSelected = computed(() => filteredTasks.value.some(t => selectedIds.value.has(t.id)))

onMounted(() => loadTasks())

function loadTasks() {
  loading.value = true
  getTaskList(undefined, undefined).then((res: any) => {
    if (res.code === 200) {
      const list = res.data?.records || res.data || []
      tasks.value = list.map((t: any) => ({
        id: t.id ?? t.taskId,
        taskId: t.taskId ?? t.id,
        title: t.title || '',
        status: t.status ?? 0,
        priority: t.priority ?? 2,
        assigneeName: t.assigneeName ?? t.assignee ?? '',
        dueDate: t.dueDate ?? '',
        progress: t.progress ?? 0,
      }))
    }
  }).catch(() => {}).finally(() => loading.value = false)
}

function openDetail(row: any) {
  selectedTask.value = { ...row, id: row.taskId || row.id }
  drawerVisible.value = true
}

function createTaskFn() {
  if (!newTask.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  createTask(newTask.value.title, undefined).then((res: any) => {
    if (res.code === 200) { ElMessage.success('已创建'); showCreate.value = false; loadTasks() }
  }).catch(() => ElMessage.error('创建失败'))
}

function isOverdue(row: any) {
  if (!row.dueDate) return false
  return new Date(row.dueDate) < new Date()
}

function toggleAll(e: Event) {
  const checked = (e.target as HTMLInputElement).checked
  filteredTasks.value.forEach(t => {
    if (checked) selectedIds.value.add(t.id)
    else selectedIds.value.delete(t.id)
  })
}

function toggleRow(row: any) {
  if (selectedIds.value.has(row.id)) selectedIds.value.delete(row.id)
  else selectedIds.value.add(row.id)
}

function clearSelection() {
  selectedIds.value.clear()
}

function priorityLabel(p: number) { return ['', '紧急', '高', '中', '低'][p] || '中' }
function statusLabel(s: number) { return ['待办', '进行中', '已完成', '已取消'][s] || '待办' }
</script>

<style lang="scss" scoped>
.task-list-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--c-bg);
  overflow: hidden;
}

/* ── Toolbar ── */
.toolbar-section {
  flex-shrink: 0;
  border-bottom: 1px solid var(--c-border);
  background: #fff;
}

.toolbar-top {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 16px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
}

.breadcrumb-item {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-muted);

  &.active {
    color: var(--c-text);
    font-weight: 600;
  }
}

.breadcrumb-sep {
  font-size: 14px;
  color: var(--c-text-muted);
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0;
  letter-spacing: -0.02em;
  line-height: 1.25;
}

.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  display: inline-block;
  line-height: 1;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg);
  background: var(--c-surface);
  color: var(--c-text);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s var(--ease-out);
  font-family: var(--font-sans);

  .material-symbols-outlined {
    font-size: 18px;
  }

  &:hover {
    background: var(--c-bg);
  }
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: var(--radius-lg);
  background: var(--c-brand);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s var(--ease-out);
  font-family: var(--font-sans);

  .material-symbols-outlined {
    font-size: 18px;
  }

  &:hover {
    opacity: 0.9;
  }
}

/* ── Filter Bar ── */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px 12px;
}

.search-wrap {
  position: relative;
  flex: 1;
  max-width: 320px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  color: var(--c-text-muted);
}

.search-input {
  width: 100%;
  padding: 8px 12px 8px 40px;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg);
  font-size: 12px;
  outline: none;
  transition: border-color 0.15s var(--ease-out);
  font-family: var(--font-sans);
  color: var(--c-text);

  &:focus {
    border-color: var(--c-brand);
  }

  &::placeholder {
    color: var(--c-text-muted);
  }
}

/* ── Batch Toolbar ── */
.batch-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: rgba(0, 80, 214, 0.06);
  border-top: 1px solid rgba(0, 80, 214, 0.15);
  animation: fadeSlideIn 0.2s var(--ease-out);
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.batch-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.batch-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-count {
  font-size: 12px;
  font-weight: 700;
  color: var(--c-brand);
}

.batch-divider {
  width: 1px;
  height: 16px;
  background: rgba(0, 80, 214, 0.2);
}

.batch-action {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: var(--c-text-secondary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-lg);
  transition: all 0.15s var(--ease-out);
  font-family: var(--font-sans);

  .material-symbols-outlined {
    font-size: 18px;
  }

  &:hover {
    color: var(--c-brand);
    background: rgba(0, 80, 214, 0.08);
  }

  &.danger:hover {
    color: var(--c-danger);
    background: rgba(186, 26, 26, 0.08);
  }
}

.batch-clear {
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: var(--c-text-secondary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-lg);
  transition: all 0.15s var(--ease-out);
  font-family: var(--font-sans);

  &:hover {
    background: var(--c-bg);
  }
}

/* ── Table ── */
.table-container {
  flex: 1;
  overflow: auto;

  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: var(--c-border);
    border-radius: 10px;
  }
}

.data-table {
  width: 100%;
  min-width: 1000px;
  border-collapse: collapse;
  table-layout: auto;

  thead {
    position: sticky;
    top: 0;
    z-index: 5;

    tr {
      background: #fff;
      border-bottom: 1px solid var(--c-border);
    }

    th {
      padding: 14px 16px;
      font-size: 12px;
      font-weight: 500;
      color: var(--c-text-muted);
      text-transform: uppercase;
      letter-spacing: 0.04em;
      text-align: left;
      white-space: nowrap;

      &.col-check {
        width: 48px;
        text-align: center;
        padding-left: 16px;
      }

      &.col-actions {
        width: 48px;
        padding-right: 16px;
      }

      &.col-priority {
        text-align: center;
      }

      &.col-status {
        width: 120px;
      }

      &.col-date {
        width: 130px;
      }

      &.col-progress {
        width: 160px;
      }

      &.col-assignee {
        width: 160px;
      }
    }
  }

  tbody {
    tr.data-row {
      border-bottom: 1px solid var(--c-border);
      transition: background 0.1s var(--ease-out);

      &:hover {
        background: rgba(248, 249, 250, 0.6);

        .row-more-btn {
          opacity: 1;
        }
      }

      &.row-selected {
        background: var(--c-selection);
      }
    }

    td {
      padding: 14px 16px;
      font-size: 14px;
      color: var(--c-text);
      vertical-align: middle;

      &.col-check {
        text-align: center;
        padding-left: 16px;
      }

      &.col-priority {
        text-align: center;
      }

      &.col-actions {
        text-align: right;
        padding-right: 16px;
      }

      &.col-date {
        font-family: 'JetBrains Mono Variable', monospace;
        font-size: 13px;
        color: var(--c-text-muted);

        &.overdue {
          color: var(--c-danger);
        }
      }
    }
  }
}

input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: var(--c-brand);
  cursor: pointer;
}

/* ── Name Cell ── */
.name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name-icon {
  font-size: 20px;
  color: var(--c-brand);
  flex-shrink: 0;
}

.name-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  cursor: pointer;
  transition: color 0.15s var(--ease-out);

  &:hover {
    color: var(--c-brand);
  }

  &.completed {
    text-decoration: line-through;
    color: var(--c-text-muted);
  }
}

/* ── Status Tag ── */
.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;

  &.status-0 {
    background: var(--c-bg);
    color: var(--c-text-muted);
  }
  &.status-1 {
    background: rgba(0, 80, 214, 0.08);
    color: var(--c-brand);
    border: 1px solid rgba(0, 80, 214, 0.2);
  }
  &.status-2 {
    background: rgba(34, 197, 94, 0.08);
    color: var(--c-online);
    border: 1px solid rgba(34, 197, 94, 0.2);
  }
  &.status-3 {
    background: var(--c-bg);
    color: var(--c-text-muted);
    border: 1px solid var(--c-border);
  }
}

/* ── Priority Badge ── */
.priority-badge-sm {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--radius-lg);
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;

  &.pri-1 {
    background: #ffdad6;
    color: #93000a;
  }
  &.pri-2 {
    background: #fff3e0;
    color: #e65100;
  }
  &.pri-3 {
    background: var(--c-selection);
    color: var(--c-brand);
  }
  &.pri-4 {
    background: var(--c-bg);
    color: var(--c-text-muted);
  }
}

/* ── Assignee Cell ── */
.assignee-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.assignee-name {
  font-size: 14px;
  color: var(--c-text);
}

.unassigned {
  font-size: 12px;
  color: var(--c-text-muted);
  font-style: italic;
}

/* ── Progress Cell ── */
.progress-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-pct {
  font-size: 10px;
  color: var(--c-text-muted);
  flex-shrink: 0;
  width: 32px;
}

.progress-track {
  flex: 1;
  height: 6px;
  background: var(--c-bg);
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--c-brand);
  border-radius: 999px;
  transition: width 0.5s var(--ease-out);

  &.fill-done {
    background: var(--c-online);
  }
}

/* ── Row More ── */
.row-more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--c-text-muted);
  cursor: pointer;
  border-radius: var(--radius-lg);
  opacity: 0;
  transition: all 0.15s var(--ease-out);

  .material-symbols-outlined {
    font-size: 20px;
  }

  &:hover {
    background: var(--c-bg);
    color: var(--c-text);
  }
}

/* ── Empty ── */
.empty-row {
  text-align: center;
  padding: 48px 16px !important;
  color: var(--c-text-muted);
  font-size: 14px;
}

/* ── Pagination ── */
.pagination-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 56px;
  border-top: 1px solid var(--c-border);
  background: #fff;
  flex-shrink: 0;
}

.page-info {
  font-size: 12px;
  color: var(--c-text-muted);
  font-weight: 500;
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border: none;
  background: transparent;
  color: var(--c-text-muted);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-lg);
  transition: all 0.15s var(--ease-out);
  font-family: var(--font-sans);

  .material-symbols-outlined {
    font-size: 18px;
  }

  &:hover:not(:disabled) {
    background: var(--c-bg);
    color: var(--c-text);
  }

  &:disabled {
    opacity: 0.3;
    cursor: default;
  }

  &.page-num.active {
    background: var(--c-brand);
    color: #fff;
    font-weight: 700;
  }
}

.page-ellipsis {
  padding: 0 4px;
  color: var(--c-text-muted);
  font-size: 12px;
}

/* ── Presence Bar ── */
.presence-bar {
  position: fixed;
  bottom: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid var(--c-border);
  border-radius: 999px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.presence-avatars {
  display: flex;
  align-items: center;

  > * {
    margin-right: -8px;
  }
}

.presence-extra {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: var(--c-brand);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  z-index: 5;
  margin-right: 0 !important;
}

.presence-text {
  display: flex;
  flex-direction: column;
  padding-right: 8px;
}

.presence-title {
  font-size: 11px;
  font-weight: 700;
  color: var(--c-text);
  line-height: 1.2;
}

.presence-sub {
  font-size: 9px;
  color: var(--c-text-muted);
}
</style>
