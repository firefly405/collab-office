<template>
  <div class="storage-page">
    <!-- Main Content -->
    <div class="canvas-container">
          <!-- Page Header -->
          <div class="page-header">
            <div>
              <h1 class="page-title">存储配额与使用统计页</h1>
              <p class="page-subtitle">实时分析组织内的存储资源消耗与趋势</p>
            </div>
            <div class="page-actions">
              <button class="btn-outline">
                <span class="material-symbols-outlined icon-18">download</span>
                导出报告
              </button>
              <button class="btn-primary">
                <span class="material-symbols-outlined icon-18">add</span>
                扩容配额
              </button>
            </div>
          </div>

          <!-- Bento Grid -->
          <div class="bento-grid">
            <!-- Donut Chart -->
            <div class="card card-donut" v-loading="loading">
              <h3 class="card-title">总体使用率</h3>
              <div class="donut-wrap">
                <svg class="donut-svg" viewBox="0 0 36 36">
                  <circle
                    class="donut-bg"
                    cx="18" cy="18" r="16"
                    fill="none"
                    stroke-width="3"
                  />
                  <circle
                    class="donut-fill"
                    cx="18" cy="18" r="16"
                    fill="none"
                    stroke-width="3"
                    stroke-linecap="round"
                    :stroke-dasharray="`${usagePercent}, 100`"
                  />
                </svg>
                <div class="donut-center">
                  <span class="donut-pct">{{ usagePercent }}%</span>
                  <span class="donut-label">已使用容量</span>
                </div>
              </div>
              <div class="donut-stats">
                <div class="donut-stat">
                  <span class="donut-stat-val">{{ totalSize }}</span>
                  <span class="donut-stat-key">已使用空间</span>
                </div>
                <div class="donut-stat">
                  <span class="donut-stat-val">{{ quotaTotal }}</span>
                  <span class="donut-stat-key">总配额</span>
                </div>
              </div>
            </div>

            <!-- File Type Distribution -->
            <div class="card card-types">
              <div class="card-title-row">
                <h3 class="card-title">文件类型分布</h3>
                <span class="sync-badge">
                  <span class="sync-dot" />
                  实时同步中
                </span>
              </div>
              <div class="type-grid">
                <div class="type-card" v-for="t in fileTypes" :key="t.name">
                  <span class="material-symbols-outlined type-card-icon" :style="{ color: t.color }">
                    {{ getTypeIcon(t.name) }}
                  </span>
                  <h4 class="type-card-name">{{ t.name }}</h4>
                  <p class="type-card-size">{{ t.size }}</p>
                  <div class="type-card-bar">
                    <div class="type-card-fill" :style="{ width: t.pct + '%', background: t.color }" />
                  </div>
                  <span class="type-card-pct">占总比 {{ t.pct }}%</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Charts Row -->
          <div class="charts-row">
            <!-- Bar Chart: Growth Trend -->
            <div class="card card-chart">
              <h3 class="card-title">近6个月增长趋势 (TB)</h3>
              <div class="bar-chart">
                <div class="bar-col" v-for="(m, i) in trendMonths" :key="m">
                  <div class="bar-tooltip">{{ m }}</div>
                  <div
                    class="bar-fill"
                    :class="{ 'bar-current': i === trendMonths.length - 1 }"
                    :style="{ height: getBarHeight(i) }"
                  />
                  <span class="bar-label" :class="{ 'bar-label-current': i === trendMonths.length - 1 }">
                    {{ m }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Top Departments -->
            <div class="card card-top">
              <h3 class="card-title">高用量部门排名</h3>
              <div class="top-list">
                <div class="top-item" v-for="(u, i) in topUsers" :key="u.name">
                  <div class="top-rank" :class="`rank-${i + 1}`">{{ i + 1 }}</div>
                  <div class="top-info">
                    <p class="top-name">{{ u.name }}</p>
                    <p class="top-dept">{{ u.dept }}</p>
                  </div>
                  <div class="top-right">
                    <p class="top-usage">{{ u.usage }}</p>
                    <span class="top-trend">--</span>
                  </div>
                </div>
              </div>
              <button class="view-all-btn">查看全部部门</button>
            </div>
          </div>

          <!-- System Log Table -->
          <div class="card card-log">
            <div class="card-title-row">
              <h3 class="card-title">存储系统动态</h3>
              <select class="log-filter">
                <option>所有事件</option>
                <option>警告消息</option>
                <option>配额变更</option>
              </select>
            </div>
            <div class="log-table-wrap">
              <table class="log-table">
                <thead>
                  <tr>
                    <th>日期</th>
                    <th>类别</th>
                    <th>事件描述</th>
                    <th>执行人</th>
                    <th class="text-right">状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td class="log-date">2023-11-24 14:20</td>
                    <td><span class="log-badge badge-warn">配额警告</span></td>
                    <td>研发部存储使用量已达到 95% 警戒线</td>
                    <td>System</td>
                    <td class="text-right log-status-pending">待处理</td>
                  </tr>
                  <tr>
                    <td class="log-date">2023-11-23 09:15</td>
                    <td><span class="log-badge badge-info">系统优化</span></td>
                    <td>完成月度重复文件清理计划 (节省 120GB)</td>
                    <td>陈技术</td>
                    <td class="text-right log-status-done">已完成</td>
                  </tr>
                  <tr>
                    <td class="log-date">2023-11-22 17:45</td>
                    <td><span class="log-badge badge-change">配额变更</span></td>
                    <td>市场部存储总额上调至 1.0 TB</td>
                    <td>Admin</td>
                    <td class="text-right log-status-done">已生效</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Optimization Tips -->
          <div class="card card-tips">
            <h3 class="card-title">优化建议</h3>
            <ul class="tips-list">
              <li>回收站中有 284 个文件超过 30 天，建议清理</li>
              <li>大文件(>100MB)占用 1.2TB，建议压缩或归档</li>
              <li>版本历史占用 320GB，可设置自动清理策略</li>
            </ul>
          </div>
        </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const loading = ref(false)
const totalFiles = ref(0)
const totalFolders = ref(0)
const totalVersions = ref(0)
const totalSize = ref('0 B')
const quotaTotal = ref('--')
const usagePercent = ref(0)

onMounted(() => {
  loading.value = true
  request.get('/file/storage/stats').then((res: any) => {
    if (res.code === 200 && res.data) {
      const d = res.data
      totalFiles.value = d.totalFiles ?? 0
      totalFolders.value = d.totalFolders ?? 0
      totalVersions.value = d.totalVersions ?? d.versionCount ?? 0
      totalSize.value = d.totalSizeReadable ?? d.usedSize ?? '0 B'
      quotaTotal.value = d.quotaTotalReadable ?? d.quotaTotal ?? '--'
      usagePercent.value = d.usagePercent ?? 0

      if (d.fileTypes && Array.isArray(d.fileTypes)) {
        d.fileTypes.forEach((ft: any, i: number) => {
          if (fileTypes.value[i]) {
            fileTypes.value[i].pct = ft.pct ?? ft.percent ?? 0
            fileTypes.value[i].size = ft.size ?? ft.sizeReadable ?? '--'
          }
        })
      }
      if (d.trend && Array.isArray(d.trend)) {
        updateTrend(d.trend)
      }
      if (d.topUsers && Array.isArray(d.topUsers)) {
        topUsers.value = d.topUsers.map((u: any) => ({
          name: u.name ?? u.userName ?? '--',
          dept: u.dept ?? u.department ?? '--',
          usage: u.usage ?? u.usageReadable ?? '--',
          pct: u.pct ?? u.percent ?? 0,
          color: u.color ?? 'var(--c-brand)',
        }))
      }
    }
  }).catch(() => {}).finally(() => loading.value = false)
})

const fileTypes = ref([
  { name: '文档', pct: 0, size: '--', color: 'var(--c-brand)' },
  { name: '媒体(图片/视频)', pct: 0, size: '--', color: 'var(--c-success)' },
  { name: '其他', pct: 0, size: '--', color: 'var(--c-text-muted)' },
])

const trendMonths = ref(['12月', '1月', '2月', '3月', '4月', '5月'])
const trendPoints = ref('0,90 60,72 120,58 180,42 240,28 300,18')
const trendArea = ref('0,120 0,90 60,72 120,58 180,42 240,28 300,18 300,120')
const trendValues = ref<number[]>([])

function updateTrend(data: number[]) {
  trendValues.value = data
  const w = 300
  const h = 120
  const n = data.length
  const max = Math.max(...data, 1)
  const points = data.map((v, i) => `${Math.round(i * w / (n - 1))},${Math.round(h - (v / max) * h)}`)
  trendPoints.value = points.join(' ')
  trendArea.value = `0,${h} ${points.join(' ')} ${w},${h}`
}

function getBarHeight(index: number): string {
  const heights = ['40%', '50%', '58%', '65%', '78%', '90%']
  return heights[index] || '50%'
}

function getTypeIcon(name: string): string {
  if (name.includes('文档')) return 'description'
  if (name.includes('媒体')) return 'movie'
  return 'archive'
}

const topUsers = ref([
  { name: '--', dept: '--', usage: '--', pct: 0, color: 'var(--c-brand)' },
])
</script>

<style lang="scss" scoped>
/* ===== CSS Variables ===== */
$c-bg: #f8f9fa;
$c-surface: #f8f9fa;
$c-surface-low: #f3f4f5;
$c-surface-lowest: #ffffff;
$c-surface-high: #e7e8e9;
$c-surface-dim: #d9dadb;
$c-border: #E5E6EB;
$c-border-variant: #c3c6d8;
$c-primary: #0050d6;
$c-primary-container: #2a6af9;
$c-on-primary: #ffffff;
$c-on-primary-container: #fefcff;
$c-text: #191c1d;
$c-text-secondary: #5b5e65;
$c-text-muted: #424655;
$c-text-ghost: #737687;
$c-selection: rgba(51, 112, 255, 0.1);
$c-error: #ba1a1a;
$c-error-container: #ffdad6;
$c-on-error-container: #93000a;
$c-presence: #22C55E;
$c-success: #22C55E;
$c-warning: #EAB308;
$c-secondary-container: #dfe2eb;
$c-on-secondary-container: #61646c;
$c-inverse-surface: #2e3132;
$c-inverse-on-surface: #f0f1f2;
$radius-sm: 4px;
$radius-md: 8px;
$radius-lg: 12px;
$radius-xl: 16px;
$radius-full: 999px;
$sidebar-width: 280px;

.storage-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: $c-bg;
  color: $c-text;
  font-family: 'Inter Variable', sans-serif;
  overflow: hidden;
}

/* ===== Top Header ===== */
.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 64px;
  background: $c-surface;
  border-bottom: 1px solid $c-border;
  flex-shrink: 0;
  z-index: 50;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-logo {
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
  color: $c-primary;
}

.search-box {
  display: none;
  align-items: center;
  background: $c-surface-low;
  padding: 6px 12px;
  border-radius: $radius-md;
  border: 1px solid $c-border;
  transition: box-shadow 0.15s;

  &:focus-within {
    box-shadow: 0 0 0 2px rgba($c-primary-container, 0.2);
  }

  @media (min-width: 768px) {
    display: flex;
  }
}

.search-icon {
  color: $c-text-secondary;
  font-size: 20px;
  margin-right: 8px;
}

.search-input {
  width: 256px;
  background: transparent;
  border: none;
  outline: none;
  font-size: 14px;
  line-height: 22px;
  color: $c-text;

  &::placeholder { color: $c-text-ghost; }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: $c-text-muted;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: color 0.15s;

  &:hover { color: $c-primary; }
}

.header-divider {
  width: 1px;
  height: 24px;
  background: $c-border;
  margin: 0 8px;
}

.share-btn {
  background: $c-primary;
  color: $c-on-primary;
  border: none;
  padding: 8px 16px;
  border-radius: $radius-sm;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition: background-color 0.15s;

  &:hover { background: darken($c-primary, 5%); }
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: $c-border-variant;
  border: 1px solid $c-border;
  flex-shrink: 0;
}

/* ===== Body ===== */
.storage-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ===== Sidebar ===== */
.sidebar {
  display: none;
  flex-direction: column;
  width: $sidebar-width;
  background: $c-surface-low;
  border-right: 1px solid $c-border;
  padding: 16px 8px;
  flex-shrink: 0;
  overflow-y: auto;

  @media (min-width: 768px) {
    display: flex;
  }
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-section-label {
  padding: 8px 16px 4px;
  font-size: 10px;
  font-weight: 500;
  color: $c-text-secondary;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  color: $c-text-secondary;
  text-decoration: none;
  border-radius: $radius-sm;
  font-size: 14px;
  line-height: 22px;
  transition: background-color 0.15s;
  cursor: pointer;

  &:hover { background: $c-surface-high; }

  &.active {
    background: $c-selection;
    color: $c-primary;
    border-left: 4px solid $c-primary;
    font-weight: 700;

    .material-symbols-outlined {
      font-variation-settings: 'FILL' 1;
    }
  }
}

.nav-divider {
  margin: 24px 0 8px;
  border-top: 1px solid $c-border;
}

.quota-card {
  margin-top: auto;
  padding: 16px;
  background: $c-surface-high;
  border-radius: $radius-xl;
  margin: 16px 8px 16px;
}

.quota-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.quota-card-icon {
  color: $c-primary;
  font-size: 18px;
}

.quota-card-label {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: $c-text;
}

.quota-card-bar {
  width: 100%;
  height: 6px;
  background: $c-surface-dim;
  border-radius: $radius-full;
  overflow: hidden;
}

.quota-card-fill {
  width: 78%;
  height: 100%;
  background: $c-primary;
  border-radius: $radius-full;
  transition: width 0.5s ease;
}

.quota-card-text {
  font-size: 11px;
  color: $c-text-secondary;
  margin-top: 8px;
  letter-spacing: 0.01em;
}

/* ===== Main Content ===== */
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 32px 16px 48px;
}

.canvas-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* Page Header */
.page-header {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 32px;

  @media (min-width: 768px) {
    flex-direction: row;
    align-items: flex-end;
    justify-content: space-between;
  }
}

.page-title {
  font-size: 32px;
  line-height: 40px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: $c-text;
  margin: 0;
}

.page-subtitle {
  font-size: 16px;
  line-height: 24px;
  color: $c-text-secondary;
  margin: 8px 0 0;
}

.page-actions {
  display: flex;
  gap: 8px;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: $c-primary;
  color: $c-on-primary;
  border: none;
  border-radius: $radius-md;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition: background-color 0.15s, box-shadow 0.15s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);

  &:hover { background: darken($c-primary, 5%); }
}

.btn-outline {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: $c-surface;
  color: $c-text-secondary;
  border: 1px solid $c-border;
  border-radius: $radius-md;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition: background-color 0.15s;

  &:hover { background: $c-surface-high; }
}

.icon-18 { font-size: 18px; }

/* ===== Cards ===== */
.card {
  background: $c-surface-lowest;
  border: 1px solid $c-border;
  border-radius: $radius-xl;
  padding: 24px;
  transition: border-color 0.15s, transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);

  &:hover {
    transform: translateY(-2px);
    border-color: $c-primary;
  }
}

.card-title {
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
  color: $c-text;
  margin: 0 0 24px;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .card-title { margin: 0; }
}

/* ===== Bento Grid ===== */
.bento-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  margin-bottom: 24px;

  @media (min-width: 768px) {
    grid-template-columns: 4fr 8fr;
  }
}

/* Donut Card */
.card-donut {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.donut-wrap {
  position: relative;
  width: 192px;
  height: 192px;
  margin-bottom: 32px;
}

.donut-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.donut-bg {
  stroke: $c-surface-high;
}

.donut-fill {
  stroke: $c-primary;
  transition: stroke-dasharray 1s ease-out;
}

.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.donut-pct {
  font-size: 32px;
  line-height: 40px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: $c-primary;
}

.donut-label {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: $c-text-secondary;
}

.donut-stats {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.donut-stat {
  display: flex;
  flex-direction: column;
  align-items: center;

  &:first-child {
    border-right: 1px solid $c-border;
  }
}

.donut-stat-val {
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
}

.donut-stat-key {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: $c-text-secondary;
}

/* File Type Card */
.sync-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: $c-text-secondary;
}

.sync-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $c-primary;
}

.type-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;

  @media (min-width: 640px) {
    grid-template-columns: repeat(3, 1fr);
  }
}

.type-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: $c-surface-low;
  border-radius: $radius-md;
  border: 1px solid $c-border;
}

.type-card-icon {
  margin-bottom: 8px;
}

.type-card-name {
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 4px;
}

.type-card-size {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 16px;
}

.type-card-bar {
  width: 100%;
  height: 4px;
  background: $c-surface-dim;
  border-radius: $radius-full;
  overflow: hidden;
  margin-bottom: 4px;
}

.type-card-fill {
  height: 100%;
  border-radius: $radius-full;
  transition: width 0.5s ease;
}

.type-card-pct {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: $c-text-secondary;
}

/* ===== Charts Row ===== */
.charts-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  margin-bottom: 24px;

  @media (min-width: 768px) {
    grid-template-columns: 8fr 4fr;
  }
}

/* Bar Chart */
.bar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 192px;
  gap: 12px;
  padding: 0 8px;
}

.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  height: 100%;
  justify-content: flex-end;
}

.bar-tooltip {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-bottom: 8px;
  padding: 4px 8px;
  background: $c-inverse-surface;
  color: $c-inverse-on-surface;
  font-size: 10px;
  border-radius: $radius-sm;
  opacity: 0;
  transition: opacity 0.15s;
  white-space: nowrap;
  pointer-events: none;

  .bar-col:hover & { opacity: 1; }
}

.bar-fill {
  width: 100%;
  background: rgba($c-primary-container, 0.3);
  border-radius: 2px 2px 0 0;
  transition: background-color 0.15s, height 0.8s cubic-bezier(0.16, 1, 0.3, 1);

  .bar-col:hover & { background: rgba($c-primary-container, 0.6); }

  &.bar-current {
    background: $c-primary;
  }
}

.bar-label {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: $c-text-secondary;
  margin-top: 12px;

  &.bar-label-current {
    font-weight: 700;
    color: $c-primary;
  }
}

/* Top Departments */
.top-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 220px;
  overflow-y: auto;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: $c-border; border-radius: 10px; }
}

.top-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: $c-surface-low;
  border-radius: $radius-md;
  border: 1px solid transparent;
  transition: border-color 0.15s;

  &:hover { border-color: rgba($c-primary-container, 0.2); }
}

.top-rank {
  width: 40px;
  height: 40px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
  margin-right: 12px;

  &.rank-1 { background: rgba($c-primary, 0.1); color: $c-primary; }
  &.rank-2 { background: rgba($c-text-secondary, 0.1); color: $c-text-secondary; }
  &.rank-3 { background: rgba($c-text-ghost, 0.1); color: $c-text-ghost; }
}

.top-info {
  flex: 1;
}

.top-name {
  font-size: 14px;
  font-weight: 700;
  margin: 0;
}

.top-dept {
  font-size: 12px;
  color: $c-text-secondary;
  letter-spacing: 0.01em;
  margin: 0;
}

.top-right {
  text-align: right;
}

.top-usage {
  font-size: 14px;
  font-weight: 700;
  margin: 0;
}

.top-trend {
  font-size: 10px;
  color: $c-text-secondary;
  letter-spacing: 0.01em;
}

.view-all-btn {
  width: 100%;
  margin-top: 24px;
  padding: 8px 0;
  background: transparent;
  color: $c-primary;
  border: 1px solid rgba($c-primary, 0.2);
  border-radius: $radius-md;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition: background-color 0.15s;

  &:hover { background: $c-selection; }
}

/* Log Table */
.card-log {
  margin-bottom: 24px;
}

.log-filter {
  font-size: 12px;
  font-weight: 500;
  padding: 4px 12px;
  border: 1px solid $c-border;
  border-radius: $radius-sm;
  background: $c-surface-low;
  color: $c-text;
  outline: none;
  cursor: pointer;

  &:focus { border-color: $c-primary; }
}

.log-table-wrap {
  overflow-x: auto;
}

.log-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 14px;
  line-height: 22px;

  th {
    padding-bottom: 12px;
    font-size: 12px;
    font-weight: 500;
    color: $c-text-secondary;
    border-bottom: 1px solid $c-border;
    letter-spacing: 0.01em;
  }

  td {
    padding: 16px 0;
    border-bottom: 1px solid $c-border;
  }

  tr:last-child td { border-bottom: none; }

  tr:hover td { background: rgba($c-surface-low, 0.5); }

  .text-right { text-align: right; }
}

.log-date { color: $c-text-muted; }

.log-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 700;
  border-radius: $radius-sm;
  white-space: nowrap;

  &.badge-warn { background: $c-error-container; color: $c-on-error-container; }
  &.badge-info { background: $c-secondary-container; color: $c-on-secondary-container; }
  &.badge-change { background: rgba($c-primary, 0.1); color: $c-primary; }
}

.log-status-pending { color: $c-error; font-weight: 500; }
.log-status-done { color: $c-presence; font-weight: 500; }

/* Tips Card */
.card-tips {
  background: rgba(255, 143, 31, 0.05);
  border-color: rgba(255, 143, 31, 0.3);

  .card-title {
    color: #d97706;
    margin-bottom: 16px;
  }
}

.tips-list {
  padding-left: 20px;
  margin: 0;
  color: $c-text-secondary;
  line-height: 2;
  font-size: 14px;
}
</style>
