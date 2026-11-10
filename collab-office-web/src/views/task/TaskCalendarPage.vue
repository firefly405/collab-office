<template>
  <div class="calendar-page" v-loading="loading">
    <!-- Calendar Controller -->
    <div class="calendar-controller">
      <div class="controller-left">
        <h2 class="month-label">{{ monthLabel }}</h2>
        <div class="month-nav">
          <button class="nav-btn" @click="prevMonth">
            <span class="material-symbols-outlined">chevron_left</span>
          </button>
          <button class="today-btn" @click="goToday">今天</button>
          <button class="nav-btn" @click="nextMonth">
            <span class="material-symbols-outlined">chevron_right</span>
          </button>
        </div>
      </div>
      <div class="controller-right">
        <div class="view-switcher">
          <button class="view-btn" @click="$router.push('/tasks')">看板</button>
          <button class="view-btn" @click="$router.push('/tasks/list')">列表</button>
          <button class="view-btn active">日历</button>
        </div>
        <div class="team-avatars">
          <el-avatar :size="32" :style="{ backgroundColor: 'var(--c-brand)' }">A</el-avatar>
          <el-avatar :size="32" :style="{ backgroundColor: 'var(--c-online)' }">B</el-avatar>
          <div class="avatar-extra">+5</div>
        </div>
        <button class="filter-btn">
          <span class="material-symbols-outlined">filter_list</span>
          <span>筛选</span>
        </button>
        <button class="create-btn" @click="showCreate = true">
          <span class="material-symbols-outlined">add</span>
          <span>新建任务</span>
        </button>
      </div>
    </div>

    <!-- Calendar Grid -->
    <div class="calendar-body" v-if="view === 'month'">
      <div class="weekday-header">
        <span v-for="d in weekdays" :key="d" class="weekday">{{ d }}</span>
      </div>
      <div class="day-grid">
        <div
          v-for="(day, idx) in days"
          :key="idx"
          class="day-cell"
          :class="{
            'other-month': !day.isCurrent,
            'is-today': day.isToday,
            'is-selected': day.dateStr === selectedDate
          }"
          @click="selectedDate = day.dateStr"
        >
          <div class="day-num-wrap">
            <span v-if="day.isToday" class="today-badge">{{ day.day }}</span>
            <span v-else class="day-num" :class="{ muted: !day.isCurrent }">{{ day.day }}</span>
            <span v-if="day.isToday" class="today-label">今天</span>
          </div>
          <div class="day-events" v-if="day.tasks.length">
            <div
              v-for="t in day.tasks.slice(0, 3)"
              :key="t.id"
              class="day-event"
              :class="'event-pri-' + (t.priority || 3)"
            >
              {{ priorityLabel(t.priority) }}：{{ t.title }}
            </div>
            <span v-if="day.tasks.length > 3" class="more-events">+{{ day.tasks.length - 3 }} 更多</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Selected Day Task List -->
    <div class="day-task-list" v-if="selectedDate">
      <h3 class="list-title">
        <span class="material-symbols-outlined">event</span>
        {{ selectedDate }} 任务
      </h3>
      <div class="task-items">
        <div
          v-for="t in selectedTasks"
          :key="t.id"
          class="task-item"
          @click="openTask(t)"
        >
          <span class="priority-dot" :class="'dot-pri-' + (t.priority || 3)"></span>
          <div class="task-info">
            <span class="task-title">{{ t.title }}</span>
            <span class="task-assignee">{{ t.assignee || t.assigneeName || '未分配' }}</span>
          </div>
          <span class="task-status-tag" :class="'status-' + t.status">
            {{ statusLabel(t.status) }}
          </span>
        </div>
        <div v-if="selectedTasks.length === 0" class="empty-day">
          <span class="material-symbols-outlined">event_busy</span>
          <span>当天无任务</span>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <footer class="calendar-footer">
      <div class="footer-left">
        <div class="online-indicator">
          <span class="online-dot"></span>
          <span>12 名成员在线</span>
        </div>
        <div class="editing-indicator">
          <el-avatar :size="20" :style="{ backgroundColor: 'var(--c-brand)' }">A</el-avatar>
          <el-avatar :size="20" :style="{ backgroundColor: 'var(--c-text-muted)' }">B</el-avatar>
          <span>正在编辑...</span>
        </div>
      </div>
      <div class="footer-right">
        <span class="material-symbols-outlined">cloud_done</span>
        <span>所有更改已实时同步</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTaskList } from '@/api/task'

const router = useRouter()
const view = ref<'month' | 'week' | 'day'>('month')
const currentMonth = ref(new Date(2026, 4, 1))
const selectedDate = ref('')
const showCreate = ref(false)
const loading = ref(false)
const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const tasks = ref<any[]>([])

onMounted(() => fetchTasks())

function fetchTasks() {
  loading.value = true
  getTaskList().then((res: any) => {
    if (res.code === 200) tasks.value = res.data?.records || res.data || []
  }).catch(() => {}).finally(() => loading.value = false)
}

const monthLabel = computed(() => {
  return currentMonth.value.toLocaleString('zh-CN', { year: 'numeric', month: 'long' })
})

const todayDate = new Date()
const todayStr = `${todayDate.getFullYear()}-${String(todayDate.getMonth() + 1).padStart(2, '0')}-${String(todayDate.getDate()).padStart(2, '0')}`

const days = computed(() => {
  const y = currentMonth.value.getFullYear()
  const m = currentMonth.value.getMonth()
  const firstDay = new Date(y, m, 1)
  const lastDay = new Date(y, m + 1, 0)
  const startOffset = (firstDay.getDay() + 6) % 7
  const cells: { day: number; dateStr: string; isCurrent: boolean; isToday: boolean; tasks: typeof tasks.value }[] = []

  for (let i = 0; i < startOffset; i++) {
    const d = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), -(startOffset - i - 1))
    cells.push({ day: d.getDate(), dateStr: formatDate(d), isCurrent: false, isToday: false, tasks: [] })
  }
  for (let d = 1; d <= lastDay.getDate(); d++) {
    const date = new Date(y, m, d)
    const dateStr = formatDate(date)
    cells.push({
      day: d, dateStr, isCurrent: true,
      isToday: dateStr === todayStr,
      tasks: tasks.value.filter(t => t.dueDate === dateStr),
    })
  }
  while (cells.length % 7 !== 0) {
    const d = cells.length - startOffset - lastDay.getDate() + 1
    const date = new Date(y, m + 1, d)
    cells.push({ day: d, dateStr: formatDate(date), isCurrent: false, isToday: false, tasks: [] })
  }
  return cells
})

const selectedTasks = computed(() => {
  if (!selectedDate.value) return []
  return tasks.value.filter(t => t.dueDate === selectedDate.value)
})

function formatDate(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function prevMonth() {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() - 1, 1)
}

function nextMonth() {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 1)
}

function goToday() { currentMonth.value = new Date(); selectedDate.value = todayStr }

function statusLabel(s: number) { return ['待办', '进行中', '已完成'][s] || '待办' }
function priorityLabel(p: number) { return ['', '紧急', '高', '中', '低'][p] || '中' }

function openTask(_t: typeof tasks.value[0]) {
  router.push('/tasks')
}
</script>

<style lang="scss" scoped>
.calendar-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--c-bg);
  overflow: hidden;
}

.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  display: inline-block;
  line-height: 1;
}

/* ── Controller ── */
.calendar-controller {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid var(--c-border);
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 12px;
}

.controller-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.month-label {
  font-size: 24px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0;
  letter-spacing: -0.01em;
}

.month-nav {
  display: flex;
  align-items: center;
  background: var(--c-bg);
  border-radius: var(--radius-lg);
  padding: 2px;
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--c-text-muted);
  cursor: pointer;
  border-radius: var(--radius-lg);
  transition: all 0.15s var(--ease-out);

  .material-symbols-outlined {
    font-size: 20px;
  }

  &:hover {
    background: var(--c-surface);
    color: var(--c-text);
  }
}

.today-btn {
  padding: 6px 16px;
  border: none;
  background: transparent;
  color: var(--c-text);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-lg);
  transition: all 0.15s var(--ease-out);
  font-family: var(--font-sans);

  &:hover {
    background: var(--c-surface);
  }
}

.controller-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.view-switcher {
  display: flex;
  align-items: center;
  gap: 4px;
}

.view-btn {
  padding: 6px 12px;
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
    color: var(--c-brand);
  }

  &.active {
    color: var(--c-brand);
    border-bottom: 2px solid var(--c-brand);
    border-radius: 0;
    padding-bottom: 4px;
    font-weight: 700;
  }
}

.team-avatars {
  display: flex;
  align-items: center;

  > * {
    margin-right: -8px;
    border: 2px solid #fff;
  }
}

.avatar-extra {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: var(--c-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  color: var(--c-text-muted);
  z-index: 5;
  margin-right: 0 !important;
}

.filter-btn {
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

/* ── Calendar Body ── */
.calendar-body {
  flex: 1;
  overflow: auto;
  display: flex;
  flex-direction: column;

  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: var(--c-border);
    border-radius: 10px;
  }
}

.weekday-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: var(--c-bg);
  border-bottom: 1px solid var(--c-border);
  position: sticky;
  top: 0;
  z-index: 5;
}

.weekday {
  padding: 10px 0;
  text-align: center;
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-muted);
  border-right: 1px solid var(--c-border);

  &:last-child {
    border-right: none;
  }
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  flex: 1;
}

.day-cell {
  min-height: 120px;
  padding: 8px;
  border-right: 1px solid var(--c-border);
  border-bottom: 1px solid var(--c-border);
  cursor: pointer;
  transition: background 0.15s var(--ease-out);
  position: relative;

  &:hover {
    background: var(--c-bg);
  }

  &.other-month {
    background: var(--c-bg);
    opacity: 0.5;
    cursor: default;
  }

  &.is-today {
    background: rgba(0, 80, 214, 0.03);
    border: 2px solid rgba(0, 80, 214, 0.15);
    margin: -1px;
  }

  &.is-selected {
    box-shadow: inset 0 0 0 2px var(--c-brand);
    border-radius: var(--radius-lg);
  }
}

.day-num-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.day-num {
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text);

  &.muted {
    color: var(--c-text-muted);
  }
}

.today-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--c-brand);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.today-label {
  font-size: 10px;
  font-weight: 700;
  color: var(--c-brand);
}

/* ── Day Events ── */
.day-events {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.day-event {
  padding: 2px 8px;
  border-radius: var(--radius-lg);
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  border-left: 4px solid transparent;
  cursor: pointer;
  transition: opacity 0.15s var(--ease-out);

  &:hover {
    opacity: 0.85;
  }

  &.event-pri-1 {
    background: #ffdad6;
    color: #93000a;
    border-left-color: #ba1a1a;
  }

  &.event-pri-2 {
    background: #dbe1ff;
    color: #003ea8;
    border-left-color: #0050d6;
  }

  &.event-pri-3 {
    background: #dfe2eb;
    color: #43474e;
    border-left-color: #5b5e65;
  }

  &.event-pri-4 {
    background: #dde3ed;
    color: #414750;
    border-left-color: #565c65;
  }
}

.more-events {
  font-size: 10px;
  color: var(--c-text-muted);
  padding: 2px 4px;
  font-weight: 500;
}

/* ── Day Task List ── */
.day-task-list {
  flex-shrink: 0;
  max-height: 260px;
  overflow-y: auto;
  border-top: 1px solid var(--c-border);
  background: #fff;
  padding: 16px;
}

.list-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0 0 12px;

  .material-symbols-outlined {
    font-size: 20px;
    color: var(--c-brand);
  }
}

.task-items {
  display: flex;
  flex-direction: column;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--c-border);
  cursor: pointer;
  transition: all 0.15s var(--ease-out);

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    color: var(--c-brand);

    .task-title {
      color: var(--c-brand);
    }
  }
}

.priority-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  &.dot-pri-1 { background: #ba1a1a; }
  &.dot-pri-2 { background: #e65100; }
  &.dot-pri-3 { background: var(--c-brand); }
  &.dot-pri-4 { background: var(--c-text-muted); }
}

.task-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.task-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  transition: color 0.15s var(--ease-out);
}

.task-assignee {
  font-size: 12px;
  color: var(--c-text-muted);
}

.task-status-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--radius-lg);
  flex-shrink: 0;

  &.status-0 {
    background: var(--c-bg);
    color: var(--c-text-muted);
  }
  &.status-1 {
    background: rgba(0, 80, 214, 0.08);
    color: var(--c-brand);
  }
  &.status-2 {
    background: rgba(34, 197, 94, 0.08);
    color: var(--c-online);
  }
}

.empty-day {
  padding: 24px;
  text-align: center;
  color: var(--c-text-muted);
  font-size: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;

  .material-symbols-outlined {
    font-size: 32px;
  }
}

/* ── Footer ── */
.calendar-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 40px;
  border-top: 1px solid var(--c-border);
  background: var(--c-surface);
  flex-shrink: 0;
  font-size: 10px;
  color: var(--c-text-muted);

  .material-symbols-outlined {
    font-size: 12px;
  }
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.online-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
}

.online-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--c-online);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.editing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;

  > * {
    margin-right: -4px;
    border: 1px solid #fff;
  }

  span:last-child {
    margin-left: 8px;
    margin-right: 0;
  }
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
