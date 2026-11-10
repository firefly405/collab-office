<template>
  <div class="chart-overlay" v-if="visible" @click.self="$emit('update:visible', false)">
    <aside class="chart-panel">
      <!-- Header -->
      <div class="panel-header">
        <div class="panel-title-row">
          <div class="panel-title-left">
            <span class="material-symbols-outlined title-icon">add_chart</span>
            <h2 class="panel-title">图表配置面板</h2>
          </div>
          <button class="panel-close" @click="$emit('update:visible', false)">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </div>

      <!-- Scrollable Content -->
      <div class="panel-body">
        <!-- Live Preview -->
        <section class="config-section">
          <div class="section-header">
            <h3 class="section-label">实时预览</h3>
            <span class="engine-badge">ECharts Engine</span>
          </div>
          <div class="preview-canvas">
            <div class="preview-title">{{ chartTitle || '图表预览' }}</div>
            <div class="preview-chart-area">
              <!-- Bar chart preview -->
              <div v-if="chartType === 'bar'" class="bar-preview">
                <div v-for="(h, i) in previewBars" :key="i" class="bar-col">
                  <div
                    class="bar-fill-preview"
                    :style="{ height: h + '%', background: previewColor }"
                  ></div>
                  <span class="bar-label-preview">月份{{ i + 1 }}</span>
                </div>
              </div>
              <!-- Line chart preview -->
              <div v-if="chartType === 'line'" class="line-preview">
                <svg viewBox="0 0 280 80" class="preview-svg">
                  <polyline
                    :points="linePoints"
                    fill="none"
                    :stroke="previewColor"
                    stroke-width="2.5"
                    stroke-linecap="round"
                  />
                  <circle v-for="(pt, i) in linePointArray" :key="i" :cx="pt[0]" :cy="pt[1]" r="4" :fill="previewColor" />
                </svg>
              </div>
              <!-- Pie chart preview -->
              <div v-if="chartType === 'pie'" class="pie-preview">
                <svg viewBox="0 0 100 100" class="preview-svg" style="width:100px;height:100px">
                  <path
                    v-for="(slice, i) in pieSlices"
                    :key="i"
                    :d="slice.path"
                    :fill="previewColor"
                    :opacity="0.35 + i * 0.2"
                  />
                </svg>
              </div>
              <!-- Scatter preview -->
              <div v-if="chartType === 'scatter'" class="scatter-preview">
                <svg viewBox="0 0 280 80" class="preview-svg">
                  <circle v-for="(pt, i) in scatterPoints" :key="i" :cx="pt[0]" :cy="pt[1]" r="4" :fill="previewColor" :opacity="0.7" />
                </svg>
              </div>
            </div>
          </div>
        </section>

        <!-- Data Range -->
        <section class="config-section">
          <h3 class="section-label">数据范围</h3>
          <div class="range-input-row">
            <span class="material-symbols-outlined range-icon">grid_on</span>
            <input
              v-model="dataRange"
              class="range-input"
              type="text"
              placeholder="例如: Sheet1!$B$2:$D$12"
            />
            <button class="range-edit-btn" @click="$emit('pick-range')">
              <span class="material-symbols-outlined">edit_square</span>
            </button>
          </div>
        </section>

        <!-- Chart Type Switcher -->
        <section class="config-section">
          <h3 class="section-label">图表类型</h3>
          <div class="type-grid">
            <button
              v-for="t in chartTypes"
              :key="t.value"
              class="type-card"
              :class="{ active: chartType === t.value }"
              @click="chartType = t.value"
            >
              <span class="material-symbols-outlined type-icon">{{ t.icon }}</span>
              <span class="type-name">{{ t.label }}</span>
            </button>
          </div>
        </section>

        <!-- Style Configuration -->
        <section class="config-section">
          <h3 class="section-label">样式配置</h3>

          <div class="style-field">
            <label class="style-label">图表标题</label>
            <input
              v-model="chartTitle"
              class="style-input"
              type="text"
              placeholder="输入图表标题..."
            />
          </div>

          <div class="style-field-row">
            <span class="style-label">显示图例</span>
            <label class="toggle">
              <input type="checkbox" v-model="showLegend" />
              <span class="toggle-slider"></span>
            </label>
          </div>

          <div class="style-field-row">
            <span class="style-label">显示坐标轴标签</span>
            <label class="toggle">
              <input type="checkbox" v-model="showAxisLabels" />
              <span class="toggle-slider"></span>
            </label>
          </div>

          <div class="style-field">
            <label class="style-label">颜色主题</label>
            <div class="color-themes">
              <button
                v-for="p in colorPresets"
                :key="p.name"
                class="color-dot-wrap"
                :class="{ active: colorTheme === p.name }"
                :title="p.name"
                @click="colorTheme = p.name"
              >
                <span class="color-dot" :style="{ background: p.color }"></span>
              </button>
            </div>
          </div>
        </section>
      </div>

      <!-- Footer Actions -->
      <div class="panel-footer">
        <button class="btn-cancel" @click="$emit('update:visible', false)">取消</button>
        <button class="btn-confirm" @click="insertChart">确定生成</button>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

defineProps<{ visible: boolean }>()
const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'pick-range'): void
  (e: 'insert', config: any): void
}>()

const dataRange = ref('Sheet1!$B$2:$D$12')
const chartType = ref('bar')
const chartTitle = ref('销售数据趋势分析')
const colorTheme = ref('blue')
const showLegend = ref(true)
const showAxisLabels = ref(true)

const chartTypes = [
  { value: 'bar', label: '柱状图', icon: 'bar_chart' },
  { value: 'line', label: '折线图', icon: 'show_chart' },
  { value: 'pie', label: '饼图', icon: 'pie_chart' },
  { value: 'scatter', label: '散点图', icon: 'scatter_plot' },
]

const colorPresets = [
  { name: 'blue', color: '#0050d6' },
  { name: 'green', color: '#22C55E' },
  { name: 'amber', color: '#6f757e' },
  { name: 'purple', color: '#2a6af9' },
]

const colorValues: Record<string, string> = {
  blue: '#0050d6',
  green: '#22C55E',
  amber: '#6f757e',
  purple: '#2a6af9',
}

const previewColor = computed(() => colorValues[colorTheme.value] || '#0050d6')

const previewBars = computed(() => {
  if (chartType.value === 'bar') return [60, 85, 70, 55, 78, 50]
  return []
})

const linePoints = computed(() => {
  if (chartType.value !== 'line') return ''
  const pts = [[15, 50], [80, 30], [145, 55], [210, 25], [270, 40]]
  return pts.map(p => p.join(',')).join(' ')
})

const linePointArray = computed(() => {
  if (chartType.value !== 'line') return []
  return [[15, 50], [80, 30], [145, 55], [210, 25], [270, 40]]
})

const pieSlices = computed(() => {
  if (chartType.value !== 'pie') return []
  const segments = [[0, 45], [45, 100], [100, 160], [160, 250], [250, 360]]
  return segments.map(([start, end]) => {
    const cx = 50, cy = 50, r = 40
    const sRad = start * Math.PI / 180
    const eRad = end * Math.PI / 180
    const x1 = cx + r * Math.cos(sRad)
    const y1 = cy + r * Math.sin(sRad)
    const x2 = cx + r * Math.cos(eRad)
    const y2 = cy + r * Math.sin(eRad)
    const largeArc = (end - start) > 180 ? 1 : 0
    return { path: `M${cx},${cy} L${x1},${y1} A${r},${r} 0 ${largeArc},1 ${x2},${y2} Z` }
  })
})

const scatterPoints = computed(() => {
  if (chartType.value !== 'scatter') return []
  return [[30, 50], [80, 30], [130, 60], [170, 20], [220, 45], [260, 35]]
})

function insertChart() {
  emit('insert', {
    type: chartType.value,
    title: chartTitle.value || '图表',
    dataRange: dataRange.value,
    colorTheme: colorTheme.value,
    showLegend: showLegend.value,
    showAxisLabels: showAxisLabels.value,
  })
  emit('update:visible', false)
}
</script>

<style lang="scss" scoped>
// ---------- CSS Variables ----------
$c-surface: #f8f9fa;
$c-surface-container-lowest: #ffffff;
$c-surface-container-low: #f3f4f5;
$c-surface-container: #edeeef;
$c-surface-container-high: #e7e8e9;
$c-surface-dim: #d9dadb;
$c-border-subtle: #E5E6EB;
$c-outline: #737687;
$c-outline-variant: #c3c6d8;
$c-primary: #0050d6;
$c-primary-container: #2a6af9;
$c-selection-blue: rgba(51, 112, 255, 0.1);
$c-on-surface: #191c1d;
$c-on-surface-variant: #424655;
$c-secondary: #5b5e65;
$c-on-primary: #ffffff;
$c-on-tertiary: #ffffff;
$c-tertiary-container: #6f757e;
$c-success: #22C55E;
$c-text: #191c1d;
$c-text-secondary: #424655;
$c-text-muted: #737687;
$c-bg: #f8f9fa;
$c-border: #E5E6EB;
$c-brand: #0050d6;
$radius: 4px;
$radius-lg: 8px;
$radius-xl: 12px;
$radius-full: 24px;
$gutter: 16px;
$ease-out: cubic-bezier(0.16, 1, 0.3, 1);

// ---------- Overlay ----------
.chart-overlay {
  position: fixed;
  inset: 0;
  z-index: 95;
  display: flex;
  justify-content: flex-end;
  background: rgba(0, 0, 0, 0.15);
}

// ---------- Panel ----------
.chart-panel {
  width: 400px;
  height: 100vh;
  background: $c-surface-container-lowest;
  border-left: 1px solid $c-border-subtle;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 12px rgba(0,0,0,0.03);
  font-family: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  color: $c-on-surface;
}

// ---------- Header ----------
.panel-header {
  padding: 16px 24px;
  border-bottom: 1px solid $c-border-subtle;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 22px;
  color: $c-primary;
}

.panel-title {
  font-size: 20px;
  font-weight: 600;
  color: $c-on-surface;
  margin: 0;
}

.panel-close {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border: none;
  background: transparent;
  color: $c-outline;
  cursor: pointer;
  border-radius: $radius;
  &:hover { color: $c-on-surface; }
  .material-symbols-outlined { font-size: 20px; }
}

// ---------- Panel Body ----------
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: $c-border-subtle; border-radius: 2px; }
}

// ---------- Config Section ----------
.config-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-label {
  font-size: 12px;
  font-weight: 500;
  color: $c-outline;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin: 0;
}

.engine-badge {
  font-size: 10px;
  font-weight: 500;
  background: $c-tertiary-container;
  color: $c-on-tertiary;
  padding: 2px 6px;
  border-radius: $radius;
  text-transform: uppercase;
}

// ---------- Preview ----------
.preview-canvas {
  background: linear-gradient(180deg, $c-surface-container-lowest 0%, $c-bg 100%);
  border: 1px solid $c-border-subtle;
  border-radius: $radius-lg;
  padding: 16px;
  aspect-ratio: 16/9;
  display: flex;
  flex-direction: column;
}

.preview-title {
  text-align: center;
  font-size: 14px;
  font-weight: 700;
  color: $c-on-surface;
  margin-bottom: 12px;
}

.preview-chart-area {
  flex: 1;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.bar-preview {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 8px;
  width: 100%;
  height: 100%;
  padding: 0 8px;
}

.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.bar-fill-preview {
  width: 32px;
  border-radius: 2px 2px 0 0;
  transition: height .5s, opacity .2s;
  &:hover { opacity: 0.8; }
}

.bar-label-preview {
  font-size: 10px;
  color: $c-outline;
  margin-top: 4px;
}

.line-preview, .scatter-preview {
  width: 100%;
  height: 100%;
}

.preview-svg {
  width: 100%;
  height: 100%;
}

.pie-preview {
  display: flex;
  align-items: center;
  justify-content: center;
}

// ---------- Data Range ----------
.range-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
  background: $c-surface-container-low;
  border: 1px solid $c-border-subtle;
  border-radius: $radius;
  padding: 8px 12px;
  transition: border-color .15s;
  &:focus-within { border-color: $c-primary; }
}

.range-icon {
  font-size: 20px;
  color: $c-primary;
}

.range-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 14px;
  font-family: 'JetBrains Mono Variable', monospace;
  color: $c-on-surface;
  &::placeholder { color: $c-outline-variant; }
}

.range-edit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2px;
  border: none;
  background: transparent;
  color: $c-outline;
  cursor: pointer;
  border-radius: $radius;
  &:hover { color: $c-primary; }
  .material-symbols-outlined { font-size: 18px; }
}

// ---------- Chart Type Grid ----------
.type-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  border: 2px solid $c-border-subtle;
  border-radius: $radius;
  background: transparent;
  cursor: pointer;
  transition: all .15s $ease-out;
  color: $c-on-surface-variant;
  &:hover { border-color: $c-outline; }
  &.active {
    border-color: $c-primary;
    background: $c-selection-blue;
    color: $c-primary;
  }
}

.type-icon {
  font-size: 28px;
}

.type-name {
  font-size: 10px;
  font-weight: 500;
}

// ---------- Style Config ----------
.style-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.style-field-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
}

.style-label {
  font-size: 14px;
  font-weight: 500;
  color: $c-on-surface;
}

.style-input {
  width: 100%;
  background: $c-surface;
  border: 1px solid $c-border-subtle;
  border-radius: $radius;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
  color: $c-on-surface;
  transition: border-color .15s;
  box-sizing: border-box;
  &:focus { border-color: $c-primary; box-shadow: 0 0 0 1px $c-primary; }
  &::placeholder { color: $c-outline-variant; }
}

// ---------- Toggle Switch ----------
.toggle {
  position: relative;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  input { position: absolute; opacity: 0; width: 0; height: 0; }
}

.toggle-slider {
  width: 40px;
  height: 20px;
  background: $c-surface-container-high;
  border-radius: 10px;
  position: relative;
  transition: background .2s;
  &::after {
    content: '';
    position: absolute;
    top: 2px;
    left: 2px;
    width: 16px;
    height: 16px;
    background: $c-surface-container-lowest;
    border-radius: 50%;
    border: 1px solid $c-border;
    transition: transform .2s;
  }
  input:checked + & {
    background: $c-primary;
    &::after { transform: translateX(20px); border-color: $c-surface-container-lowest; }
  }
}

// ---------- Color Themes ----------
.color-themes {
  display: flex;
  gap: 12px;
}

.color-dot-wrap {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid transparent;
  padding: 2px;
  background: transparent;
  cursor: pointer;
  transition: all .15s $ease-out;
  &:hover { border-color: $c-outline; }
  &.active { border-color: $c-primary; }
}

.color-dot {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

// ---------- Footer ----------
.panel-footer {
  display: flex;
  gap: 12px;
  padding: 24px;
  border-top: 1px solid $c-border-subtle;
}

.btn-cancel {
  flex: 1;
  padding: 10px;
  background: $c-surface-container-high;
  color: $c-on-surface;
  border: none;
  border-radius: $radius;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background .15s;
  &:hover { background: $c-surface-dim; }
}

.btn-confirm {
  flex: 1;
  padding: 10px;
  background: $c-primary;
  color: $c-on-primary;
  border: none;
  border-radius: $radius;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 80, 214, 0.2);
  transition: all .15s $ease-out;
  &:hover { box-shadow: 0 4px 12px rgba(0, 80, 214, 0.25); }
  &:active { transform: scale(0.98); }
}
</style>
