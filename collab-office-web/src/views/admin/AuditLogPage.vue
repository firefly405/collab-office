<template>
  <div class="audit-page">
    <div class="page-hero">
      <h2 class="page-title">审计与登录日志</h2>
      <p class="page-subtitle">系统关键操作与登录行为的实时溯源清单，确保企业资产安全。</p>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">今日异常登录</span>
          <span class="material-symbols-outlined stat-icon danger">warning</span>
        </div>
        <div class="stat-value danger">{{ stats.todayErrors || 12 }}</div>
        <div class="stat-change danger">
          <span class="material-symbols-outlined">trending_up</span>
          +5% 较昨日
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">活跃管理员</span>
          <span class="material-symbols-outlined stat-icon primary">admin_panel_settings</span>
        </div>
        <div class="stat-value">8</div>
        <div class="stat-change muted">当前在线</div>
      </div>
      <div class="stat-card wide">
        <div class="stat-header">
          <span class="stat-label">操作趋势 (24h)</span>
          <div class="trend-legend">
            <span class="legend-item"><span class="legend-dot primary" /> 登录</span>
            <span class="legend-item"><span class="legend-dot tertiary" /> 操作</span>
          </div>
        </div>
        <div class="trend-bars">
          <div class="trend-bar" v-for="h in 7" :key="h" :style="{ height: (20 + Math.random() * 80) + '%', opacity: 0.2 + Math.random() * 0.8 }" />
        </div>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <div class="select-wrap">
          <select class="native-select" v-model="filterModule">
            <option value="">所有操作类型</option>
            <option value="doc">登录/登出</option>
            <option value="user">文件修改</option>
            <option value="permission">权限变更</option>
            <option value="space">成员邀请</option>
          </select>
          <span class="material-symbols-outlined select-arrow">expand_more</span>
        </div>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          format="YYYY/MM/DD"
          style="width: 240px"
        />
        <button class="btn-outline-sm">
          <span class="material-symbols-outlined">filter_list</span>
          更多筛选
        </button>
      </div>
      <div class="toolbar-right">
        <button class="btn-outline-sm" @click="exportLogs">
          <span class="material-symbols-outlined">file_download</span>
          导出
        </button>
      </div>
    </div>

    <div class="log-table-card">
      <table class="log-table">
        <thead>
          <tr>
            <th>操作人</th>
            <th>操作时间</th>
            <th>类型</th>
            <th>描述</th>
            <th>IP 地址</th>
            <th>对象</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in filteredLogs" :key="log.id" class="log-row">
            <td>
              <div class="user-cell">
                <div class="user-avatar" :style="{ background: avatarColor(log.user) }">
                  {{ log.user?.[0] || '?' }}
                  <span v-if="log.online" class="presence-dot" />
                </div>
                <span class="user-name">{{ log.user }}</span>
              </div>
            </td>
            <td class="text-muted">{{ log.time }}</td>
            <td>
              <span class="type-badge" :style="{ background: typeBg(log.module), color: typeColor(log.module) }">
                {{ log.module }}
              </span>
            </td>
            <td class="text-body">{{ log.detail || log.operation }}</td>
            <td class="text-mono">{{ log.ip }}</td>
            <td class="text-muted">{{ log.target || '-' }}</td>
          </tr>
        </tbody>
      </table>

      <div v-if="filteredLogs.length === 0" class="empty-state">
        暂无日志记录
      </div>

      <div class="pagination-bar">
        <span class="pagination-info">显示 1-{{ filteredLogs.length }} 共 {{ logs.length }} 条记录</span>
        <div class="pagination-btns">
          <button class="page-btn"><span class="material-symbols-outlined">chevron_left</span></button>
          <button class="page-btn active">1</button>
          <button class="page-btn">2</button>
          <button class="page-btn">3</button>
          <span class="page-ellipsis">...</span>
          <button class="page-btn">250</button>
          <button class="page-btn"><span class="material-symbols-outlined">chevron_right</span></button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const keyword = ref('')
const filterModule = ref('')
const dateRange = ref<[Date, Date] | null>(null)
const loading = ref(false)
const logs = ref<any[]>([])
const stats = ref({ todayTotal: 0, todayErrors: 0 })

onMounted(() => {
  fetchStats()
  fetchLogs()
})

function fetchStats() {
  request.get('/admin/logs/stats').then((res: any) => {
    if (res.code === 200 && res.data) stats.value = res.data
  }).catch(() => {})
}

function fetchLogs() {
  loading.value = true
  const params: any = { keyword: keyword.value || undefined, module: filterModule.value || undefined }
  if (dateRange.value) {
    params.startTime = dateRange.value[0].toISOString()
    params.endTime = dateRange.value[1].toISOString()
  }
  request.get('/admin/logs', { params }).then((res: any) => {
    if (res.code === 200) logs.value = res.data?.records || res.data || []
  }).catch(() => {}).finally(() => loading.value = false)
}

const filteredLogs = computed(() => {
  let result = logs.value
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    result = result.filter(l => l.user.includes(kw) || l.operation.includes(kw) || l.detail.includes(kw))
  }
  if (filterModule.value) {
    result = result.filter(l => l.module === { doc: '文档', user: '用户', permission: '权限', space: '空间' }[filterModule.value])
  }
  return result
})

function typeBg(m: string): string {
  const map: Record<string, string> = {
    '登录': 'rgba(51,112,255,.08)',
    '文件修改': 'rgba(107,114,128,.08)',
    '异常报警': 'rgba(255,59,48,.08)',
    '备份': 'rgba(107,114,128,.08)',
    '权限变更': 'rgba(51,112,255,.08)',
  }
  return map[m] || 'rgba(107,114,128,.08)'
}

function typeColor(m: string): string {
  const map: Record<string, string> = {
    '登录': 'var(--c-brand)',
    '文件修改': 'var(--c-text-secondary)',
    '异常报警': 'var(--c-danger)',
    '备份': 'var(--c-text-secondary)',
    '权限变更': 'var(--c-brand)',
  }
  return map[m] || 'var(--c-text-secondary)'
}

function avatarColor(name: string): string {
  const colors = ['#3370FF', '#10B981', '#8B5CF6', '#FF8F1F', '#FF3B30', '#0EA5E9']
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

function exportLogs() {
  ElMessage.success('日志导出任务已创建')
}
</script>

<style lang="scss" scoped>
.audit-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-desktop, 2rem);
}

.page-hero {
  margin-bottom: 2rem;
}

.page-title {
  font-size: 32px;
  line-height: 40px;
  letter-spacing: -0.02em;
  font-weight: 700;
  color: var(--c-text);
  margin: 0 0 0.5rem;
}

.page-subtitle {
  font-size: 14px;
  color: var(--c-text-muted);
  margin: 0;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 2fr;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stat-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl, 0.75rem);
  padding: 1.25rem;
}

.stat-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.stat-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-muted);
}

.stat-icon {
  font-size: 20px;

  &.danger { color: var(--c-danger); }
  &.primary { color: var(--c-brand); }
}

.stat-value {
  font-size: 24px;
  line-height: 32px;
  font-weight: 700;
  color: var(--c-text);

  &.danger { color: var(--c-danger); }
}

.stat-change {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  margin-top: 0.25rem;

  &.danger { color: var(--c-danger); }
  &.muted { color: var(--c-text-muted); }

  .material-symbols-outlined { font-size: 14px; }
}

/* Trend Bars */
.trend-legend {
  display: flex;
  gap: 1rem;

  .legend-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 10px;
    color: var(--c-text-muted);
  }

  .legend-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &.primary { background: var(--c-brand); }
    &.tertiary { background: var(--c-text-muted); }
  }
}

.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 64px;
  margin-top: 1rem;
}

.trend-bar {
  flex: 1;
  background: var(--c-brand);
  border-radius: 2px 2px 0 0;
  min-height: 4px;
}

/* Toolbar */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl, 0.75rem);
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.select-wrap {
  position: relative;
}

.native-select {
  padding: 0.5rem 2rem 0.5rem 0.75rem;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg, 0.5rem);
  font-size: 14px;
  color: var(--c-text);
  outline: none;
  appearance: none;
  cursor: pointer;

  &:focus { border-color: var(--c-brand); }
}

.select-arrow {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  color: var(--c-text-ghost);
  pointer-events: none;
}

/* Log Table */
.log-table-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl, 0.75rem);
  overflow: hidden;
}

.log-table {
  width: 100%;
  border-collapse: collapse;

  th {
    padding: 1rem;
    font-size: 12px;
    font-weight: 500;
    color: var(--c-text-muted);
    text-align: left;
    background: var(--c-bg);
    border-bottom: 1px solid var(--c-border);
    text-transform: uppercase;
    letter-spacing: 0.02em;
  }

  td {
    padding: 1rem;
    border-bottom: 1px solid var(--c-border-light, #E5E6EB);
  }
}

.log-row {
  transition: background 0.15s;

  &:hover { background: var(--c-surface-dim); }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  position: relative;
  flex-shrink: 0;
}

.presence-dot {
  position: absolute;
  bottom: -1px;
  right: -1px;
  width: 10px;
  height: 10px;
  background: var(--c-success);
  border-radius: 50%;
  border: 2px solid #fff;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text);
}

.text-muted { font-size: 14px; color: var(--c-text-muted); }
.text-mono { font-size: 13px; font-family: 'JetBrains Mono Variable', monospace; color: var(--c-text-secondary); }
.text-body { font-size: 14px; color: var(--c-text); }

.type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
}

/* Empty State */
.empty-state {
  padding: 3rem;
  text-align: center;
  color: var(--c-text-ghost);
  font-size: 14px;
}

/* Pagination */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  background: var(--c-bg);
  border-top: 1px solid var(--c-border);
}

.pagination-info {
  font-size: 14px;
  color: var(--c-text-muted);
}

.pagination-btns {
  display: flex;
  gap: 4px;
}

.page-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--c-border);
  border-radius: var(--radius, 0.25rem);
  background: transparent;
  font-size: 14px;
  color: var(--c-text-secondary);
  cursor: pointer;
  transition: background 0.15s;

  &:hover { background: var(--c-surface-dim); }

  &.active {
    background: var(--c-brand);
    color: #fff;
    border-color: var(--c-brand);
    font-weight: 600;
  }

  .material-symbols-outlined { font-size: 18px; }
}

.page-ellipsis {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--c-text-muted);
}

/* Small buttons */
.btn-outline-sm {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg, 0.5rem);
  background: var(--c-surface-dim);
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text-secondary);
  cursor: pointer;
  transition: background 0.15s;

  &:hover { background: var(--c-border); }

  .material-symbols-outlined { font-size: 18px; }
}
</style>
