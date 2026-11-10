<template>
  <div class="project-page" v-loading="loading">
    <!-- Project Header -->
    <div class="project-header">
      <div class="header-info">
        <div class="project-badge">
          <span class="material-symbols-outlined">rocket_launch</span>
          <span>战略性项目</span>
        </div>
        <h1 class="project-title">{{ project.name || '项目概览' }}</h1>
        <p class="project-desc">
          下一代实时协作平台的全面升级。本项目涵盖了从底层协议优化到前端UI组件库重构的所有核心任务。
        </p>
      </div>
      <div class="header-actions">
        <button class="action-btn secondary">
          <span class="material-symbols-outlined">download</span>
          <span>导出报告</span>
        </button>
        <button class="action-btn primary">
          <span class="material-symbols-outlined">edit</span>
          <span>编辑计划</span>
        </button>
      </div>
    </div>

    <!-- Bento Grid -->
    <div class="bento-grid">
      <!-- Progress Gauge Card -->
      <div class="card card-progress">
        <h4 class="card-label">总体进度百分比</h4>
        <div class="gauge-wrap">
          <svg class="gauge-svg" viewBox="0 0 192 192">
            <circle
              cx="96" cy="96" r="80"
              fill="transparent"
              stroke="var(--c-border)"
              stroke-width="12"
            />
            <circle
              cx="96" cy="96" r="80"
              fill="transparent"
              stroke="var(--c-brand)"
              stroke-width="12"
              stroke-linecap="round"
              stroke-dasharray="502.6"
              :stroke-dashoffset="502.6 - (502.6 * (project.progress || 0) / 100)"
              transform="rotate(-90 96 96)"
              style="transition: stroke-dashoffset 0.6s var(--ease-out);"
            />
          </svg>
          <div class="gauge-center">
            <span class="gauge-pct">{{ project.progress || 0 }}%</span>
            <span class="gauge-sub">已完成</span>
          </div>
        </div>
        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-label">剩余天数</span>
            <span class="stat-value">{{ project.remainDays ?? '--' }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">总任务数</span>
            <span class="stat-value">{{ project.taskCount || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- Burn-down Chart Card -->
      <div class="card card-burndown">
        <div class="card-header-row">
          <h4 class="card-label">任务燃尽图 (Burn-down)</h4>
          <div class="legend">
            <div class="legend-item">
              <span class="legend-dot dashed"></span>
              <span>计划</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot solid"></span>
              <span>实际</span>
            </div>
          </div>
        </div>
        <div class="burndown-chart">
          <svg viewBox="0 0 800 200" preserveAspectRatio="none" class="chart-svg">
            <!-- Ideal line -->
            <polyline
              points="0,10 200,40 400,60 600,120 800,180"
              fill="none"
              stroke="var(--c-border)"
              stroke-width="2"
              stroke-dasharray="6,4"
            />
            <!-- Actual line -->
            <polyline
              points="0,10 150,50 350,90 550,140 750,200"
              fill="none"
              stroke="var(--c-brand)"
              stroke-width="3"
              stroke-linecap="round"
            />
          </svg>
          <div class="chart-weeks">
            <span>W1</span>
            <span>W2</span>
            <span>W3</span>
            <span>W4</span>
          </div>
        </div>
      </div>

      <!-- Team Workload Card -->
      <div class="card card-workload">
        <h4 class="card-label">各成员任务负载情况</h4>
        <div class="workload-list">
          <div v-for="u in contributors" :key="u.name" class="workload-item">
            <span class="workload-name">{{ u.name }}</span>
            <div class="workload-bar-wrap">
              <div class="workload-bar" :style="{ width: u.pct + '%', background: u.color }"></div>
              <div v-if="u.pct < 90" class="workload-bar-rest" :style="{ width: (100 - u.pct) + '%' }"></div>
            </div>
            <span class="workload-count">{{ u.tasks }}</span>
          </div>
        </div>
        <div class="workload-legend">
          <div class="legend-item">
            <span class="legend-dot solid"></span>
            <span>进行中任务</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot dim"></span>
            <span>待评审任务</span>
          </div>
        </div>
      </div>

      <!-- Milestones Card -->
      <div class="card card-milestones">
        <h4 class="card-label">关键里程碑</h4>
        <div class="timeline">
          <div class="timeline-line"></div>
          <div
            v-for="m in milestones"
            :key="m.name"
            class="timeline-item"
            :class="{ done: m.done }"
          >
            <div class="timeline-dot" :class="{ done: m.done }"></div>
            <div class="timeline-content">
              <span class="timeline-date" :class="{ done: m.done }">
                {{ m.done ? '已完成' : '待开始' }} - {{ m.date }}
              </span>
              <span class="timeline-name">{{ m.name }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Team Grid Card -->
      <div class="card card-team full-width">
        <div class="card-header-row">
          <h4 class="card-label">核心项目团队 ({{ project.memberCount || 0 }})</h4>
          <button class="view-all">查看所有成员</button>
        </div>
        <div class="team-grid">
          <div
            v-for="u in contributors"
            :key="u.name"
            class="team-member"
          >
            <div class="member-avatar-wrap">
              <el-avatar :size="48" :style="{ backgroundColor: u.color || 'var(--c-brand)' }">
                {{ u.name[0] }}
              </el-avatar>
              <span class="presence-dot" :class="u.online !== false ? 'online' : 'offline'"></span>
            </div>
            <span class="member-name">{{ u.name }}</span>
            <span class="member-role">成员</span>
          </div>
          <div class="team-member add-member">
            <span class="material-symbols-outlined">add</span>
            <span class="add-label">添加成员</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getProjectList } from '@/api/task'

const route = useRoute()
const projectId = ref(route.params.projectId as string || '')
const loading = ref(false)

const project = ref<any>({ name: '加载中...', progress: 0, taskCount: 0, doneCount: 0, memberCount: 0, remainDays: '--' })

const contributors = ref([
  { name: '--', tasks: 0, pct: 0, color: 'var(--c-brand)', online: false },
])

const milestones = ref([
  { name: '--', date: '--', done: false },
])

onMounted(() => {
  if (projectId.value) {
    loading.value = true
    getProjectList(1).then((res: any) => {
      const list = res.data?.records || res.data || []
      const found = list.find((p: any) => p.projectId === projectId.value || p.id === projectId.value)
      if (found) {
        project.value = {
          name: found.name || found.title || '项目概览',
          progress: found.progress ?? 0,
          taskCount: found.taskCount ?? found.totalTasks ?? 0,
          doneCount: found.doneCount ?? found.completedTasks ?? 0,
          memberCount: found.memberCount ?? 0,
          remainDays: found.remainDays ?? found.daysLeft ?? '--',
        }
        if (found.contributors && Array.isArray(found.contributors)) {
          contributors.value = found.contributors.map((c: any, i: number) => ({
            name: c.name ?? c.userName ?? '--',
            tasks: c.tasks ?? c.taskCount ?? 0,
            pct: c.pct ?? c.percent ?? 0,
            color: c.color ?? ['var(--c-brand)', 'var(--c-online)', '#8B5CF6', '#F59E0B'][i % 4],
            online: c.online,
          }))
        }
        if (found.milestones && Array.isArray(found.milestones)) {
          milestones.value = found.milestones.map((m: any) => ({
            name: m.name ?? m.title ?? '--',
            date: m.date ?? m.deadline ?? '--',
            done: !!m.done || m.status === 'completed',
          }))
        }
      }
    }).catch(() => {}).finally(() => loading.value = false)
  }
})
</script>

<style lang="scss" scoped>
.project-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
  background: var(--c-bg);

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

.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  display: inline-block;
  line-height: 1;
}

/* ── Header ── */
.project-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 32px 24px;
  flex-shrink: 0;
}

.header-info {
  flex: 1;
  max-width: 640px;
}

.project-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--c-brand);
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 8px;

  .material-symbols-outlined {
    font-size: 16px;
  }
}

.project-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0 0 8px;
  letter-spacing: -0.02em;
  line-height: 1.25;
}

.project-desc {
  font-size: 16px;
  color: var(--c-text-muted);
  margin: 0;
  line-height: 1.6;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: var(--radius-lg);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s var(--ease-out);
  font-family: var(--font-sans);
  border: none;

  .material-symbols-outlined {
    font-size: 18px;
  }

  &.secondary {
    background: var(--c-surface);
    color: var(--c-text);
    border: 1px solid var(--c-border);

    &:hover {
      background: var(--c-bg);
    }
  }

  &.primary {
    background: var(--c-brand);
    color: #fff;
    box-shadow: var(--shadow-sm);

    &:hover {
      opacity: 0.9;
    }
  }
}

/* ── Bento Grid ── */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 20px;
  padding: 0 24px 40px;
}

/* ── Card ── */
.card {
  background: #fff;
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl);
  padding: 24px;
  transition: all 0.2s var(--ease-out);

  &:hover {
    background: #fafbfc;
  }
}

.card-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-muted);
  margin: 0 0 24px;
  letter-spacing: 0.01em;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;

  .card-label {
    margin-bottom: 0;
  }
}

/* ── Progress Card (col 1-5) ── */
.card-progress {
  grid-column: span 5;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.gauge-wrap {
  position: relative;
  width: 192px;
  height: 192px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.gauge-svg {
  width: 100%;
  height: 100%;
  position: absolute;
}

.gauge-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 1;
}

.gauge-pct {
  font-size: 32px;
  font-weight: 700;
  color: var(--c-text);
  letter-spacing: -0.02em;
}

.gauge-sub {
  font-size: 12px;
  color: var(--c-text-muted);
}

.stats-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  width: 100%;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: var(--c-bg);
  border-radius: var(--radius-lg);
}

.stat-label {
  display: block;
  font-size: 12px;
  color: var(--c-text-muted);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--c-text);
}

/* ── Burndown Card (col 6-12) ── */
.card-burndown {
  grid-column: span 7;
}

.legend {
  display: flex;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--c-text-muted);
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;

  &.dashed {
    border: 2px dashed var(--c-border);
    background: transparent;
  }
  &.solid {
    background: var(--c-brand);
  }
  &.dim {
    background: var(--c-border);
  }
}

.burndown-chart {
  height: 200px;
  display: flex;
  flex-direction: column;
}

.chart-svg {
  flex: 1;
  width: 100%;
}

.chart-weeks {
  display: flex;
  justify-content: space-between;
  padding: 4px 8px 0;

  span {
    font-size: 10px;
    color: var(--c-text-muted);
  }
}

/* ── Workload Card (col 1-7) ── */
.card-workload {
  grid-column: span 7;
}

.workload-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-bottom: 16px;
}

.workload-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.workload-name {
  width: 120px;
  font-size: 14px;
  color: var(--c-text);
  font-weight: 500;
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.workload-bar-wrap {
  flex: 1;
  height: 32px;
  background: var(--c-bg);
  border-radius: 999px;
  overflow: hidden;
  display: flex;
}

.workload-bar {
  height: 100%;
  border-radius: 999px;
  transition: width 0.5s var(--ease-out);
}

.workload-bar-rest {
  height: 100%;
  background: var(--c-border);
  transition: width 0.5s var(--ease-out);
}

.workload-count {
  width: 40px;
  text-align: right;
  font-size: 12px;
  font-weight: 700;
  color: var(--c-text);
}

.workload-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
}

/* ── Milestones Card (col 8-12) ── */
.card-milestones {
  grid-column: span 5;
  overflow: hidden;
}

.timeline {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.timeline-line {
  position: absolute;
  left: 7px;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: var(--c-border);
}

.timeline-item {
  position: relative;
  padding-left: 30px;

  &.done {
    .timeline-dot {
      background: var(--c-online);
      border-color: var(--c-online);
    }
  }
}

.timeline-dot {
  position: absolute;
  left: 0;
  top: 4px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 4px solid #fff;
  background: var(--c-border);
  box-shadow: 0 0 0 1px var(--c-border);

  &.done {
    background: var(--c-online);
    box-shadow: 0 0 0 1px var(--c-online);
  }
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 2px;

  &.done {
    opacity: 0.6;
  }
}

.timeline-date {
  font-size: 12px;
  font-weight: 700;
  color: var(--c-text-muted);

  &.done {
    color: var(--c-online);
  }
}

.timeline-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
}

/* ── Team Card (col 1-12) ── */
.card-team {
  grid-column: span 12;
}

.view-all {
  font-size: 12px;
  font-weight: 700;
  color: var(--c-brand);
  background: none;
  border: none;
  cursor: pointer;
  font-family: var(--font-sans);

  &:hover {
    opacity: 0.8;
  }
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.team-member {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: var(--c-bg);
  border-radius: var(--radius-xl);
  transition: all 0.15s var(--ease-out);

  &:hover {
    background: var(--c-surface);
  }
}

.member-avatar-wrap {
  position: relative;
  margin-bottom: 10px;
}

.presence-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #fff;

  &.online {
    background: var(--c-online);
  }
  &.offline {
    background: var(--c-text-muted);
  }
}

.member-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 2px;
}

.member-role {
  font-size: 12px;
  color: var(--c-text-muted);
}

.add-member {
  border: 2px dashed var(--c-border);
  cursor: pointer;
  color: var(--c-text-muted);

  .material-symbols-outlined {
    font-size: 28px;
    margin-bottom: 4px;
  }

  .add-label {
    font-size: 12px;
  }

  &:hover {
    border-color: var(--c-brand);
    color: var(--c-brand);
    background: transparent;
  }
}

.full-width {
  grid-column: 1 / -1;
}

/* ── Responsive adjustments ── */
@media (max-width: 1200px) {
  .bento-grid {
    grid-template-columns: repeat(6, 1fr);
  }
  .card-progress { grid-column: span 3; }
  .card-burndown { grid-column: span 3; }
  .card-workload { grid-column: span 3; }
  .card-milestones { grid-column: span 3; }
  .team-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 768px) {
  .bento-grid {
    grid-template-columns: 1fr;
  }
  .card-progress,
  .card-burndown,
  .card-workload,
  .card-milestones {
    grid-column: span 1;
  }
  .team-grid { grid-template-columns: repeat(2, 1fr); }
  .project-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
