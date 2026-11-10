<template>
  <div class="admin-page">
    <div class="page-hero">
      <h2 class="page-title">管理控制台</h2>
      <p class="page-subtitle">系统运行数据与协作效率分析</p>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-header">
          <div class="stat-icon">
            <span class="material-symbols-outlined">group</span>
          </div>
          <span v-if="stats.activeUsersChange !== 0" class="stat-trend" :class="stats.activeUsersChange > 0 ? 'up' : 'down'">
            <span class="material-symbols-outlined">{{ stats.activeUsersChange > 0 ? 'trending_up' : 'trending_down' }}</span>
            {{ formatChange(stats.activeUsersChange) }}
          </span>
        </div>
        <h4 class="stat-label">活跃用户总数</h4>
        <p class="stat-value">{{ stats.activeUsers.toLocaleString() }}</p>
        <p class="stat-meta">较上月变化</p>
      </div>

      <div class="stat-card">
        <div class="stat-header">
          <div class="stat-icon">
            <span class="material-symbols-outlined">description</span>
          </div>
          <span v-if="stats.totalDocsChange !== 0" class="stat-trend" :class="stats.totalDocsChange > 0 ? 'up' : 'down'">
            <span class="material-symbols-outlined">{{ stats.totalDocsChange > 0 ? 'trending_up' : 'trending_down' }}</span>
            {{ formatChange(stats.totalDocsChange) }}
          </span>
        </div>
        <h4 class="stat-label">本月新增文档</h4>
        <p class="stat-value">{{ stats.totalDocs.toLocaleString() }}</p>
        <p class="stat-meta">文档创建总量</p>
      </div>

      <div class="stat-card">
        <div class="stat-header">
          <div class="stat-icon">
            <span class="material-symbols-outlined">schedule</span>
          </div>
          <span v-if="stats.avgSessionChange !== 0" class="stat-trend" :class="stats.avgSessionChange > 0 ? 'up' : 'down'">
            <span class="material-symbols-outlined">{{ stats.avgSessionChange > 0 ? 'trending_up' : 'trending_down' }}</span>
            {{ formatChange(stats.avgSessionChange) }}
          </span>
        </div>
        <h4 class="stat-label">平均会话时长</h4>
        <p class="stat-value">{{ stats.avgSession }}</p>
        <p class="stat-meta">协作效率提升显著</p>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card chart-wide">
        <div class="chart-header">
          <div>
            <h3 class="chart-title">活跃用户与互动趋势 (DAU)</h3>
            <p class="chart-subtitle">过去 7 天的每日活跃用户及平台交互总量</p>
          </div>
          <div class="chart-actions">
            <button class="btn-outline-sm">导出数据</button>
            <button class="btn-primary-sm">筛选器</button>
          </div>
        </div>
        <div class="bar-chart">
          <div v-for="bar in dauData" :key="bar.label" class="bar-col">
            <div class="bar-fill" :style="{ height: bar.pct + '%' }" />
            <span class="bar-value">{{ bar.value }}</span>
            <span class="bar-label">{{ bar.label }}</span>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="chart-title">文档类型分布</h3>
        <div class="donut-section">
          <div v-for="item in docTypes" :key="item.name" class="donut-row">
            <div class="donut-info">
              <span class="donut-dot" :style="{ background: item.color }" />
              <span class="donut-name">{{ item.name }}</span>
            </div>
            <span class="donut-pct">{{ item.pct }}%</span>
            <div class="donut-track">
              <div class="donut-fill" :style="{ width: item.pct + '%', background: item.color }" />
            </div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="chart-title">协作效率分析</h3>
        <div class="efficiency-list">
          <div v-for="item in efficiency" :key="item.label" class="eff-row">
            <span class="eff-label">{{ item.label }}</span>
            <div class="eff-track">
              <div class="eff-fill" :style="{ width: item.pct + '%', background: item.color }" />
            </div>
            <span class="eff-value">{{ item.value }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-grid">
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">优秀贡献者</h3>
          <span class="material-symbols-outlined more-icon">more_vert</span>
        </div>
        <div class="contributors-list">
          <div v-for="(c, i) in topContributors" :key="i" class="contributor-row">
            <div class="contributor-user">
              <div class="avatar-placeholder" :style="{ background: c.color }">
                {{ c.name[0] }}
              </div>
              <div>
                <p class="contributor-name">{{ c.name }}</p>
                <p class="contributor-role">{{ c.role }}</p>
              </div>
            </div>
            <div class="contributor-score">
              <p class="score-value">{{ c.score }}</p>
              <p class="score-label">互动分数</p>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="chart-title">活跃协作热力图</h3>
        <div class="heatmap-grid">
          <div v-for="day in ['一','二','三','四','五','六','日']" :key="day" class="heatmap-label">{{ day }}</div>
          <div v-for="i in 35" :key="i" class="heatmap-cell" :style="{ opacity: 0.1 + Math.random() * 0.9 }" />
        </div>
        <div class="heatmap-legend">
          <span>低活跃</span>
          <div class="legend-dots">
            <span class="legend-dot" style="opacity:0.2" />
            <span class="legend-dot" style="opacity:0.4" />
            <span class="legend-dot" style="opacity:0.7" />
            <span class="legend-dot" style="opacity:1" />
          </div>
          <span>高活跃</span>
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">实时协作概览</h3>
          <div class="online-badge">
            <span class="pulse-dot" />
            {{ stats.peakConcurrency }} 名成员在线编辑中
          </div>
        </div>
        <div class="realtime-preview">
          <span class="material-symbols-outlined realtime-bg-icon">edit_document</span>
          <p class="realtime-hint">实时更新中...</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
// ECharts available when installed: import * as echarts from 'echarts'
import request from '@/utils/request'

const loading = ref(false)

const stats = reactive({
  activeUsers: 0,
  activeUsersChange: 0,
  totalDocs: 0,
  totalDocsChange: 0,
  avgSession: '--',
  avgSessionChange: 0,
  peakConcurrency: 0,
})

const dauData = ref([
  { label: '一', value: 0, pct: 0 }, { label: '二', value: 0, pct: 0 },
  { label: '三', value: 0, pct: 0 }, { label: '四', value: 0, pct: 0 },
  { label: '五', value: 0, pct: 0 }, { label: '六', value: 0, pct: 0 },
  { label: '日', value: 0, pct: 0 },
])

const docTypes = ref([
  { name: '文档', pct: 0, color: 'var(--c-brand)' },
  { name: '表格', pct: 0, color: 'var(--c-success)' },
  { name: '思维导图', pct: 0, color: '#8B5CF6' },
  { name: '白板', pct: 0, color: 'var(--c-warning)' },
  { name: '其他', pct: 0, color: 'var(--c-text-muted)' },
])

const efficiency = ref([
  { label: '协同编辑响应', value: '--', pct: 0, color: 'var(--c-brand)' },
  { label: '文档加载速度', value: '--', pct: 0, color: 'var(--c-success)' },
  { label: '消息到达率', value: '--', pct: 0, color: '#8B5CF6' },
  { label: '冲突解决率', value: '--', pct: 0, color: 'var(--c-warning)' },
])

const topContributors = ref([
  { name: '李建国', role: 'Product Manager', score: 842, color: '#3370FF' },
  { name: '张晓雅', role: 'UX Designer', score: 715, color: '#10B981' },
  { name: '王志强', role: 'Dev Lead', score: 690, color: '#8B5CF6' },
])

function formatChange(val: number): string {
  const sign = val > 0 ? '+' : ''
  return `${sign}${val}%`
}


onMounted(() => {
  loading.value = true
  request.get('/admin/dashboard/stats').then((res: any) => {
    if (res.code === 200 && res.data) {
      const d = res.data
      stats.activeUsers = d.activeUsers ?? d.totalUsers ?? 0
      stats.activeUsersChange = d.activeUsersChange ?? d.userGrowth ?? 0
      stats.totalDocs = d.totalDocs ?? d.docCount ?? 0
      stats.totalDocsChange = d.totalDocsChange ?? d.docGrowth ?? 0
      stats.avgSession = d.avgSession ?? d.avgSessionDuration ?? '--'
      stats.avgSessionChange = d.avgSessionChange ?? 0
      stats.peakConcurrency = d.peakConcurrency ?? d.maxConcurrency ?? 0

      if (d.trend && Array.isArray(d.trend)) {
        d.trend.forEach((t: any, i: number) => {
          if (dauData.value[i]) dauData.value[i].value = t.count || 0
        })
        const max = Math.max(...dauData.value.map((x: any) => x.value), 1)
        dauData.value.forEach((x: any) => x.pct = Math.round(x.value / max * 100))
      }

      if (d.docTypes && Array.isArray(d.docTypes)) {
        d.docTypes.forEach((dt: any, i: number) => {
          if (docTypes.value[i]) docTypes.value[i].pct = dt.pct ?? dt.percent ?? 0
        })
      }

      if (d.efficiency && Array.isArray(d.efficiency)) {
        d.efficiency.forEach((e: any, i: number) => {
          if (efficiency.value[i]) {
            efficiency.value[i].value = e.value ?? '--'
            efficiency.value[i].pct = e.pct ?? e.percent ?? 0
          }
        })
      }
    }
  }).catch(() => {}).finally(() => loading.value = false)
})
</script>

<style lang="scss" scoped>
.admin-page {
  padding: var(--spacing-desktop, 2rem);
  max-width: 1200px;
  margin: 0 auto;
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
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.stat-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl, 0.75rem);
  padding: 1.5rem;
  transition: box-shadow 0.2s;

  &:hover { box-shadow: 0 4px 12px rgba(0,0,0,.06); }
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.stat-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(51,112,255,.08);
  border-radius: var(--radius-lg, 0.5rem);
  color: var(--c-brand);

  .material-symbols-outlined { font-size: 20px; }
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--radius, 0.25rem);

  .material-symbols-outlined { font-size: 14px; }

  &.up { color: var(--c-success); background: rgba(16,185,129,.1); }
  &.down { color: var(--c-danger); background: rgba(255,59,48,.08); }
}

.stat-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-muted);
  margin: 0 0 0.25rem;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.stat-value {
  font-size: 32px;
  line-height: 40px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0;
}

.stat-meta {
  font-size: 12px;
  color: var(--c-text-ghost);
  margin: 0.5rem 0 0;
}

/* Charts */
.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.chart-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl, 0.75rem);
  padding: 1.5rem;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.chart-title {
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0 0 0.25rem;
}

.chart-subtitle {
  font-size: 13px;
  color: var(--c-text-muted);
  margin: 0;
}

.chart-actions {
  display: flex;
  gap: 0.5rem;
}

/* Bar Chart */
.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 200px;
  padding: 0 4px;
}

.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  height: 100%;
  justify-content: flex-end;
}

.bar-fill {
  width: 100%;
  background: var(--c-brand);
  border-radius: 4px 4px 0 0;
  min-height: 4px;
  transition: height 0.5s ease;
}

.bar-value {
  font-size: 11px;
  color: var(--c-text-secondary);
  font-weight: 600;
}

.bar-label {
  font-size: 11px;
  color: var(--c-text-ghost);
}

/* Donut / Doc Types */
.donut-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.donut-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.donut-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.donut-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.donut-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text);
  flex: 1;
  margin-left: 8px;
}

.donut-pct {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-muted);
}

.donut-track {
  width: 100%;
  height: 8px;
  background: var(--c-bg);
  border-radius: 4px;
  overflow: hidden;
}

.donut-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

/* Efficiency */
.efficiency-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.eff-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.eff-label {
  font-size: 13px;
  color: var(--c-text);
  flex: 1;
  min-width: 0;
}

.eff-track {
  flex: 2;
  height: 6px;
  background: var(--c-border);
  border-radius: 3px;
  overflow: hidden;
}

.eff-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

.eff-value {
  font-size: 12px;
  color: var(--c-text-secondary);
  width: 36px;
  text-align: right;
}

/* Bottom Grid */
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1.5rem;
}

/* Contributors */
.contributors-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.contributor-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem;
  border-radius: var(--radius-lg, 0.5rem);
  transition: background 0.15s;

  &:hover { background: var(--c-surface-dim); }
}

.contributor-user {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  border: 2px solid var(--c-border-light, #E5E6EB);
}

.contributor-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0;
}

.contributor-role {
  font-size: 10px;
  color: var(--c-text-ghost);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin: 1px 0 0;
}

.contributor-score {
  text-align: right;
}

.score-value {
  font-size: 14px;
  font-weight: 700;
  color: var(--c-brand);
  font-family: 'JetBrains Mono Variable', monospace;
  margin: 0;
}

.score-label {
  font-size: 10px;
  color: var(--c-text-ghost);
  margin: 1px 0 0;
}

/* Heatmap */
.heatmap-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.heatmap-label {
  font-size: 10px;
  text-align: center;
  color: var(--c-text-ghost);
  margin-bottom: 4px;
}

.heatmap-cell {
  aspect-ratio: 1;
  background: var(--c-brand);
  border-radius: 2px;
}

.heatmap-legend {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 1rem;
  font-size: 10px;
  color: var(--c-text-ghost);
}

.legend-dots {
  display: flex;
  gap: 2px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  background: var(--c-brand);
  border-radius: 2px;
}

/* Realtime Preview */
.online-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: rgba(34,197,94,.08);
  color: var(--c-success);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--c-success);
  position: relative;
}

.realtime-preview {
  height: 120px;
  background: var(--c-bg);
  border: 1px dashed var(--c-border);
  border-radius: var(--radius-lg, 0.5rem);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-top: 1rem;
}

.realtime-bg-icon {
  font-size: 48px;
  color: var(--c-border);
  margin-bottom: 0.5rem;
}

.realtime-hint {
  font-size: 12px;
  color: var(--c-text-ghost);
  font-style: italic;
  margin: 0;
}

.more-icon {
  font-size: 20px;
  color: var(--c-text-ghost);
  cursor: pointer;
}

/* Buttons */
.btn-outline-sm {
  padding: 4px 12px;
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg, 0.5rem);
  background: transparent;
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-secondary);
  cursor: pointer;

  &:hover { background: var(--c-surface-dim); }
}

.btn-primary-sm {
  padding: 4px 12px;
  background: var(--c-brand);
  color: #fff;
  border: none;
  border-radius: var(--radius-lg, 0.5rem);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;

  &:hover { opacity: 0.9; }
}
</style>
