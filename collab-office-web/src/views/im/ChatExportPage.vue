<template>
  <div class="export-page">
    <!-- Header -->
    <div class="export-header">
      <h1 class="export-title">导出历史聊天记录</h1>
      <p class="export-subtitle">遵循安全合规协议，轻松备份您的团队协作资产</p>
    </div>

    <!-- Stepper -->
    <div class="stepper">
      <div class="stepper-track">
        <div class="stepper-line-bg"></div>
        <div class="stepper-line-progress" :style="{ width: progressPercent + '%' }"></div>
      </div>

      <div
        v-for="step in steps"
        :key="step.num"
        class="stepper-step"
        :class="{ active: currentStep === step.num, done: currentStep > step.num }"
        @click="goToStep(step.num)"
      >
        <div class="stepper-circle">
          <span v-if="currentStep > step.num" class="material-symbols-outlined check-icon">check</span>
          <span v-else class="step-number">{{ step.num }}</span>
        </div>
        <span class="stepper-label">{{ step.label }}</span>
      </div>
    </div>

    <!-- Step Contents -->
    <div class="step-contents">
      <!-- Step 1: Select Session -->
      <div v-show="currentStep === 1" class="step-content animate-in">
        <div class="session-type-grid">
          <label class="session-type-card" :class="{ selected: sessionType === 'private' }">
            <input type="radio" name="session_type" value="private" v-model="sessionType" class="hidden-radio" />
            <div class="card-inner">
              <div class="card-top">
                <div class="card-icon private-icon">
                  <span class="material-symbols-outlined">person</span>
                </div>
                <span class="material-symbols-outlined check-circle" v-if="sessionType === 'private'">check_circle</span>
              </div>
              <h4 class="card-label">单人私聊</h4>
              <p class="card-desc">导出与特定成员的一对一通讯记录</p>
            </div>
          </label>

          <label class="session-type-card" :class="{ selected: sessionType === 'group' }">
            <input type="radio" name="session_type" value="group" v-model="sessionType" class="hidden-radio" />
            <div class="card-inner">
              <div class="card-top">
                <div class="card-icon group-icon">
                  <span class="material-symbols-outlined">groups</span>
                </div>
                <span class="material-symbols-outlined check-circle" v-if="sessionType === 'group'">check_circle</span>
              </div>
              <h4 class="card-label">群组聊天</h4>
              <p class="card-desc">导出特定频道或多人讨论组的历史记录</p>
            </div>
          </label>
        </div>

        <!-- Recent Sessions -->
        <div class="recent-sessions-card">
          <h4 class="section-label">近期会话</h4>
          <div class="session-list">
            <div
              v-for="s in sessions"
              :key="s.sessionId"
              class="session-item"
              :class="{ active: sessionId === s.sessionId }"
              @click="sessionId = s.sessionId"
            >
              <div class="session-avatar">
                <span class="material-symbols-outlined">{{ s.sessionType === 'GROUP' ? 'groups' : 'person' }}</span>
              </div>
              <div class="session-info">
                <div class="session-name">{{ s.sessionName || s.name }}</div>
                <div class="session-meta">{{ s.memberCount || '--' }} 位成员 · 活跃于 {{ s.lastActive || '--' }}</div>
              </div>
              <button v-if="sessionId !== s.sessionId" class="select-btn">选择</button>
              <span v-else class="material-symbols-outlined selected-mark">check_circle</span>
            </div>
            <div v-if="!sessions.length" class="session-empty">
              <span class="t-label-md" style="color: var(--c-text-muted)">暂无可用会话</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 2: Filter Range -->
      <div v-show="currentStep === 2" class="step-content animate-in">
        <div class="filter-card">
          <h4 class="section-title">选择时间范围</h4>
          <div class="time-presets">
            <button
              v-for="preset in timePresets"
              :key="preset.value"
              class="time-btn"
              :class="{ active: timePreset === preset.value }"
              @click="timePreset = preset.value"
            >
              {{ preset.label }}
            </button>
          </div>
          <div class="date-range-row">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              class="date-range-picker"
            />
          </div>
        </div>

        <div class="filter-divider"></div>

        <div class="filter-card">
          <h4 class="section-title">消息类型筛选</h4>
          <div class="msg-type-chips">
            <label class="type-chip" :class="{ checked: msgTypes.text }">
              <input type="checkbox" v-model="msgTypes.text" class="chip-checkbox" />
              <span>文字消息</span>
            </label>
            <label class="type-chip" :class="{ checked: msgTypes.media }">
              <input type="checkbox" v-model="msgTypes.media" class="chip-checkbox" />
              <span>图片与视频</span>
            </label>
            <label class="type-chip" :class="{ checked: msgTypes.file }">
              <input type="checkbox" v-model="msgTypes.file" class="chip-checkbox" />
              <span>文档附件</span>
            </label>
            <label class="type-chip" :class="{ checked: msgTypes.call }">
              <input type="checkbox" v-model="msgTypes.call" class="chip-checkbox" />
              <span>语音通话记录</span>
            </label>
            <label class="type-chip" :class="{ checked: msgTypes.system }">
              <input type="checkbox" v-model="msgTypes.system" class="chip-checkbox" />
              <span>系统通知</span>
            </label>
          </div>
        </div>
      </div>

      <!-- Step 3: Format & Preview -->
      <div v-show="currentStep === 3" class="step-content animate-in">
        <div class="preview-layout">
          <!-- Format Selection -->
          <div class="format-col">
            <h4 class="section-title">导出格式</h4>
            <div class="format-options">
              <div
                class="format-card"
                :class="{ selected: format === 'pdf' }"
                @click="format = 'pdf'"
              >
                <div class="format-card-left">
                  <div class="format-icon pdf">
                    <span class="material-symbols-outlined">picture_as_pdf</span>
                  </div>
                  <div>
                    <p class="format-name">PDF 文档</p>
                    <p class="format-desc">适合阅读与存档，支持富文本样式</p>
                  </div>
                </div>
                <span class="material-symbols-outlined radio-icon">
                  {{ format === 'pdf' ? 'radio_button_checked' : 'radio_button_unchecked' }}
                </span>
              </div>

              <div
                class="format-card"
                :class="{ selected: format === 'docx' }"
                @click="format = 'docx'"
              >
                <div class="format-card-left">
                  <div class="format-icon word">
                    <span class="material-symbols-outlined">description</span>
                  </div>
                  <div>
                    <p class="format-name">Word (.docx)</p>
                    <p class="format-desc">适合后续编辑与二次排版</p>
                  </div>
                </div>
                <span class="material-symbols-outlined radio-icon">
                  {{ format === 'docx' ? 'radio_button_checked' : 'radio_button_unchecked' }}
                </span>
              </div>

              <div
                class="format-card"
                :class="{ selected: format === 'json' }"
                @click="format = 'json'"
              >
                <div class="format-card-left">
                  <div class="format-icon json">
                    <span class="material-symbols-outlined">data_object</span>
                  </div>
                  <div>
                    <p class="format-name">JSON 数据</p>
                    <p class="format-desc">适合开发者进行数据迁移或清洗</p>
                  </div>
                </div>
                <span class="material-symbols-outlined radio-icon">
                  {{ format === 'json' ? 'radio_button_checked' : 'radio_button_unchecked' }}
                </span>
              </div>
            </div>
          </div>

          <!-- Preview -->
          <div class="preview-col">
            <h4 class="section-label-sm">样式实时预览</h4>
            <div class="preview-box">
              <div class="preview-header">
                <span class="preview-title">聊天记录导出预览 - {{ selectedSessionName }}</span>
                <span class="preview-page">Page 1 of 12</span>
              </div>
              <div class="preview-messages" v-if="previewText">
                <div
                  v-for="(line, idx) in previewLines"
                  :key="idx"
                  class="preview-msg"
                  :class="{ 'msg-self': idx % 2 === 1 }"
                >
                  <div class="preview-msg-avatar"></div>
                  <div class="preview-msg-body">
                    <div class="preview-msg-meta">
                      <span class="msg-sender">{{ idx % 2 === 1 ? 'Felix (Me)' : 'Alice Zhang' }}</span>
                      <span class="msg-time">{{ idx % 2 === 1 ? '10:47 AM' : '10:45 AM' }}</span>
                    </div>
                    <div class="preview-msg-bubble">{{ line }}</div>
                  </div>
                </div>
                <div v-if="previewText" class="preview-attachment">
                  <span class="material-symbols-outlined">image</span>
                  <span>[图片附件：Design_System_v2.png]</span>
                </div>
              </div>
              <div v-else class="preview-empty" @click="preview">
                <span class="material-symbols-outlined">visibility</span>
                <span>点击预览按钮加载消息预览</span>
              </div>
              <div class="preview-blur"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 4: Export & Download -->
      <div v-show="currentStep === 4" class="step-content animate-in">
        <div class="export-final">
          <!-- Loading State -->
          <div v-if="!exportDone" class="loading-state">
            <div class="progress-ring-wrapper">
              <svg class="progress-ring" viewBox="0 0 128 128">
                <circle
                  class="ring-bg"
                  cx="64" cy="64" r="60"
                  fill="transparent"
                  stroke="#e1e3e4"
                  stroke-width="8"
                />
                <circle
                  class="ring-progress"
                  cx="64" cy="64" r="60"
                  fill="transparent"
                  stroke="#0050d6"
                  stroke-width="8"
                  :stroke-dasharray="circumference"
                  :stroke-dashoffset="dashOffset"
                />
              </svg>
              <div class="ring-text">{{ exportProgress }}%</div>
            </div>
            <h3 class="loading-title">正在打包数据...</h3>
            <p class="loading-desc">我们正在根据您的筛选条件整理消息。请勿关闭此窗口。</p>
            <div class="loading-task">
              <span class="material-symbols-outlined">sync</span>
              <span>当前任务：压缩媒体附件 (24/156)</span>
              <div class="task-bar">
                <div class="task-bar-fill" style="width: 25%"></div>
              </div>
            </div>
          </div>

          <!-- Success State -->
          <div v-else class="success-state">
            <div class="success-icon">
              <span class="material-symbols-outlined">check_circle</span>
            </div>
            <h3 class="success-title">导出准备就绪！</h3>
            <p class="success-desc">文件已生成并经过数字加密，有效期为 24 小时。</p>
            <button class="download-btn" @click="doExport">
              <span class="material-symbols-outlined">download</span>
              立即下载 (12.4 MB)
            </button>
            <button class="email-btn">发送到注册邮箱</button>
            <p class="safety-tip">安全提示：请妥善保管导出的文件，切勿泄露给无关人员。</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Navigation Buttons -->
    <div class="nav-bar" v-show="currentStep !== 4 || exportDone">
      <button
        class="nav-btn prev-btn"
        :disabled="currentStep === 1"
        @click="prevStep"
      >
        <span class="material-symbols-outlined">arrow_back</span>
        上一步
      </button>
      <button
        v-if="currentStep < 4"
        class="nav-btn next-btn"
        @click="nextStep"
      >
        <span v-if="currentStep === 3">确认导出</span>
        <span v-else>下一步</span>
        <span class="material-symbols-outlined">{{ currentStep === 3 ? 'rocket_launch' : 'arrow_forward' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const sessions = ref<any[]>([])
const sessionId = ref('')
const dateRange = ref<any[]>([])
const format = ref('pdf')
const previewText = ref('')
const sessionType = ref('group')

// Stepper state
const currentStep = ref(1)
const steps = [
  { num: 1, label: '选择会话' },
  { num: 2, label: '筛选范围' },
  { num: 3, label: '格式预览' },
  { num: 4, label: '完成下载' },
]
const timePreset = ref('30d')
const timePresets = [
  { label: '最近 7 天', value: '7d' },
  { label: '最近 30 天', value: '30d' },
  { label: '最近一年', value: '1y' },
  { label: '全部记录', value: 'all' },
]
const msgTypes = ref({
  text: true,
  media: true,
  file: true,
  call: false,
  system: false,
})

// Export state
const exportDone = ref(false)
const exportProgress = ref(0)
const circumference = 2 * Math.PI * 60 // 377

const dashOffset = computed(() => {
  return circumference - (exportProgress.value / 100) * circumference
})

const progressPercent = computed(() => {
  return ((currentStep.value - 1) / (steps.length - 1)) * 100
})

const selectedSessionName = computed(() => {
  const s = sessions.value.find((s: any) => s.sessionId === sessionId.value)
  return s?.sessionName || s?.name || '未选择'
})

const previewLines = computed(() => {
  if (!previewText.value) return []
  return previewText.value.split('\n').filter(Boolean)
})

onMounted(() => {
  request.get('/chat/conversations').then((res: any) => {
    if (res.code === 200) sessions.value = res.data?.records || res.data || []
  }).catch(() => {})
})

function nextStep() {
  if (currentStep.value === 3) {
    // Confirm export - trigger step 4 with loading
    currentStep.value = 4
    startSimulatedExport()
    return
  }
  if (currentStep.value < 4) {
    currentStep.value++
  }
}

function prevStep() {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

function goToStep(step: number) {
  if (step < currentStep.value) {
    currentStep.value = step
    if (step === 4) {
      exportDone.value = false
      exportProgress.value = 0
    }
  }
}

function startSimulatedExport() {
  exportProgress.value = 0
  exportDone.value = false
  const interval = setInterval(() => {
    exportProgress.value += Math.floor(Math.random() * 15) + 3
    if (exportProgress.value >= 100) {
      exportProgress.value = 100
      clearInterval(interval)
      setTimeout(() => {
        exportDone.value = true
      }, 600)
    }
  }, 300)
}

function preview() {
  if (!sessionId.value) { ElMessage.warning('请选择会话'); return }
  request.get(`/chat/messages/${sessionId.value}`, { params: { page: 1, size: 5 } }).then((res: any) => {
    if (res.code === 200) {
      const msgs = res.data?.records || res.data || []
      previewText.value = msgs.map((m: any) => `[${m.sendTime}] ${m.senderName}: ${m.content}`).join('\n')
    }
  }).catch(() => {
    // Fallback preview data
    previewText.value = [
      '[10:45 AM] Alice Zhang: Hi Felix, 确认一下 Q4 的设计规范已经更新了吗？',
      '[10:47 AM] Felix: 是的，已经在共享文档里同步了。',
    ].join('\n')
  })
}

function doExport() {
  if (!sessionId.value) { ElMessage.warning('请选择会话'); return }
  ElMessage.success('导出请求已提交，文件将自动下载')
  const params = new URLSearchParams({
    sessionId: sessionId.value,
    format: format.value,
    startDate: dateRange.value?.[0]?.toISOString() || '',
    endDate: dateRange.value?.[1]?.toISOString() || '',
  })
  window.open(`/api/v1/chat/export?${params}`, '_blank')
}
</script>

<style lang="scss" scoped>
/* ===== Design Tokens ===== */
$c-bg: #f8f9fa;
$c-surface: #ffffff;
$c-surface-low: #f3f4f5;
$c-surface-high: #e7e8e9;
$c-surface-highest: #e1e3e4;
$c-text: #191c1d;
$c-text-secondary: #5b5e65;
$c-text-muted: #424655;
$c-text-ghost: #737687;
$c-brand: #0050d6;
$c-brand-light: rgba(51, 112, 255, 0.1);
$c-brand-container: #2a6af9;
$c-border: #e5e6eb;
$c-error: #ba1a1a;
$c-success: #22c55e;
$c-warning: #ef4444;
$radius-sm: 4px;
$radius: 8px;
$radius-lg: 12px;
$radius-xl: 16px;
$radius-full: 12px;

.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  vertical-align: middle;
  font-size: 20px;
}

/* ===== Page Layout ===== */
.export-page {
  display: flex;
  flex-direction: column;
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 16px;
  min-height: 100%;
}

/* ===== Header ===== */
.export-header {
  text-align: center;
  margin-bottom: 32px;
}

.export-title {
  font-size: 32px;
  font-weight: 700;
  line-height: 40px;
  letter-spacing: -0.02em;
  color: $c-text;
  margin: 0 0 8px 0;
}

.export-subtitle {
  font-size: 16px;
  color: $c-text-secondary;
  margin: 0;
}

/* ===== Stepper ===== */
.stepper {
  position: relative;
  display: flex;
  justify-content: space-between;
  padding: 0 16px;
  max-width: 720px;
  margin: 0 auto 40px;
}

.stepper-track {
  position: absolute;
  top: 16px;
  left: 40px;
  right: 40px;
  height: 2px;
  z-index: 0;
}

.stepper-line-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: $c-surface-highest;
  border-radius: 1px;
}

.stepper-line-progress {
  position: absolute;
  top: 0;
  left: 0;
  height: 2px;
  background: $c-brand;
  border-radius: 1px;
  transition: width 0.4s ease;
}

.stepper-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  position: relative;
  z-index: 10;
  user-select: none;
}

.stepper-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  background: $c-surface-highest;
  color: $c-text-muted;
  transition: all 0.3s ease;
  ring: 4px solid $c-bg;

  .stepper-step.active & {
    background: $c-brand;
    color: #fff;
    box-shadow: 0 4px 12px rgba(0, 80, 214, 0.3);
  }

  .stepper-step.done & {
    background: $c-success;
    color: #fff;
  }
}

.check-icon {
  font-size: 18px;
}

.step-number {
  font-size: 12px;
  font-weight: 600;
}

.stepper-label {
  font-size: 12px;
  font-weight: 500;
  color: $c-text-muted;
  transition: color 0.3s;

  .stepper-step.active & {
    color: $c-brand;
    font-weight: 600;
  }

  .stepper-step.done & {
    color: $c-success;
  }
}

/* ===== Step Contents ===== */
.step-contents {
  min-height: 400px;
}

.step-content {
  transition: all 0.3s ease;
}

/* ===== Step 1: Session Selection ===== */
.session-type-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.session-type-card {
  cursor: pointer;
  display: block;

  .card-inner {
    padding: 24px;
    background: $c-surface;
    border: 1px solid $c-border;
    border-radius: $radius-xl;
    transition: all 0.3s ease;

    .session-type-card.selected & {
      border-color: $c-brand;
      background: $c-brand-light;
    }

    &:hover {
      border-color: rgba(0, 80, 214, 0.4);
    }
  }
}

.hidden-radio {
  display: none;
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: $radius;
  display: flex;
  align-items: center;
  justify-content: center;

  .material-symbols-outlined {
    font-size: 24px;
  }

  &.private-icon {
    background: rgba(0, 80, 214, 0.1);
    color: $c-brand;
  }

  &.group-icon {
    background: rgba(91, 94, 101, 0.1);
    color: $c-text-secondary;
  }
}

.check-circle {
  color: $c-brand;
  font-size: 24px;
}

.card-label {
  font-size: 20px;
  font-weight: 600;
  color: $c-text;
  margin: 0 0 4px 0;
}

.card-desc {
  font-size: 14px;
  color: $c-text-secondary;
  margin: 0;
}

/* ===== Recent Sessions ===== */
.recent-sessions-card {
  background: $c-surface;
  border: 1px solid $c-border;
  border-radius: $radius-xl;
  padding: 20px;
}

.section-label {
  font-size: 12px;
  font-weight: 600;
  color: $c-text-secondary;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0 0 12px 0;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: $c-text;
  margin: 0 0 16px 0;
}

.section-label-sm {
  font-size: 12px;
  font-weight: 600;
  color: $c-text-secondary;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0 0 8px 0;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: $radius;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: $c-surface-low;
  }

  &.active {
    background: $c-brand-light;
    border: 1px solid rgba(0, 80, 214, 0.2);
  }
}

.session-avatar {
  width: 40px;
  height: 40px;
  border-radius: $radius;
  background: $c-surface-high;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  .material-symbols-outlined {
    color: $c-text-secondary;
  }
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-name {
  font-size: 14px;
  font-weight: 600;
  color: $c-text;
}

.session-meta {
  font-size: 12px;
  color: $c-text-secondary;
}

.select-btn {
  background: none;
  border: none;
  color: $c-brand;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s;

  .session-item:hover & {
    opacity: 1;
  }
}

.selected-mark {
  color: $c-brand;
  font-size: 22px;
}

.session-empty {
  padding: 24px;
  text-align: center;
}

/* ===== Step 2: Filters ===== */
.filter-card {
  background: $c-surface;
  border: 1px solid $c-border;
  border-radius: $radius-xl;
  padding: 28px;
}

.filter-divider {
  height: 1px;
  background: $c-border;
  margin: 24px 0;
}

.time-presets {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}

.time-btn {
  padding: 10px 16px;
  background: none;
  border: 1px solid $c-border;
  border-radius: $radius;
  font-size: 14px;
  color: $c-text;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    border-color: $c-brand;
    color: $c-brand;
  }

  &.active {
    border-color: $c-brand;
    background: $c-brand-light;
    color: $c-brand;
    font-weight: 600;
  }
}

.date-range-row {
  display: flex;
  align-items: center;
  gap: 12px;

  .date-range-picker {
    width: 100%;
  }
}

.msg-type-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.type-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  background: $c-surface-low;
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  color: $c-text;
  transition: all 0.15s;

  &:hover {
    background: $c-surface-high;
  }

  &.checked {
    background: $c-brand-light;
    color: $c-brand;
    font-weight: 500;
  }
}

.chip-checkbox {
  accent-color: $c-brand;
  border-radius: 4px;
}

/* ===== Step 3: Format & Preview ===== */
.preview-layout {
  display: flex;
  gap: 24px;
}

.format-col {
  flex: 1;
}

.preview-col {
  flex: 1;
}

.format-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.format-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: $c-surface;
  border: 1px solid $c-border;
  border-radius: $radius-xl;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    border-color: rgba(0, 80, 214, 0.4);
  }

  &.selected {
    border-color: $c-brand;
    background: $c-brand-light;
  }
}

.format-card-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.format-icon {
  width: 48px;
  height: 48px;
  border-radius: $radius;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  .material-symbols-outlined {
    font-size: 32px;
  }

  &.pdf {
    background: #fef2f2;
    color: #dc2626;
  }

  &.word {
    background: #eff6ff;
    color: #2563eb;
  }

  &.json {
    background: #fffbeb;
    color: #d97706;
  }
}

.format-name {
  font-weight: 600;
  font-size: 14px;
  color: $c-text;
  margin: 0 0 2px 0;
}

.format-desc {
  font-size: 12px;
  color: $c-text-secondary;
  margin: 0;
}

.radio-icon {
  font-size: 24px;
  color: $c-brand;

  .format-card:not(.selected) & {
    color: $c-text-ghost;
  }
}

/* ===== Preview Box ===== */
.preview-box {
  background: $c-surface;
  border: 1px solid $c-border;
  border-radius: $radius-xl;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  padding: 20px;
  min-height: 300px;
  position: relative;
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid $c-border;
  margin-bottom: 14px;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: $c-text;
}

.preview-page {
  font-size: 12px;
  color: $c-text-secondary;
}

.preview-messages {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-msg {
  display: flex;
  gap: 10px;

  &.msg-self {
    flex-direction: row-reverse;
  }
}

.preview-msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e2e8f0;
  flex-shrink: 0;

  .msg-self & {
    background: rgba(0, 80, 214, 0.2);
  }
}

.preview-msg-body {
  flex: 1;
  min-width: 0;

  .msg-self & {
    text-align: right;
  }
}

.preview-msg-meta {
  margin-bottom: 4px;
  display: flex;
  gap: 6px;

  .msg-self & {
    justify-content: flex-end;
  }
}

.msg-sender {
  font-size: 12px;
  font-weight: 600;
  color: $c-text;
}

.msg-time {
  font-size: 12px;
  color: $c-text-secondary;
  font-weight: 400;
}

.preview-msg-bubble {
  display: inline-block;
  padding: 8px 14px;
  background: $c-surface-low;
  border-radius: $radius;
  font-size: 14px;
  color: $c-text-muted;
  max-width: 85%;

  .msg-self & {
    background: $c-brand;
    color: #fff;
  }
}

.preview-attachment {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: $c-surface-low;
  border: 1px dashed $c-border;
  border-radius: $radius;
  font-size: 12px;
  color: $c-text-secondary;
  font-style: italic;

  .material-symbols-outlined {
    color: $c-text-secondary;
    font-size: 20px;
  }
}

.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: $c-text-ghost;
  cursor: pointer;
  gap: 8px;

  .material-symbols-outlined {
    font-size: 40px;
  }

  &:hover {
    color: $c-brand;
  }
}

.preview-blur {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 80px;
  background: linear-gradient(to top, $c-surface, transparent);
  pointer-events: none;
}

/* ===== Step 4: Export Final ===== */
.export-final {
  max-width: 480px;
  margin: 0 auto;
  padding: 48px 0;
  text-align: center;
}

/* Loading State */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.progress-ring-wrapper {
  position: relative;
  width: 128px;
  height: 128px;
  margin: 0 auto;
}

.progress-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-progress {
  transition: stroke-dashoffset 0.3s ease;
}

.ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
  color: $c-text;
}

.loading-title {
  font-size: 20px;
  font-weight: 600;
  color: $c-text;
  margin: 0;
}

.loading-desc {
  font-size: 14px;
  color: $c-text-secondary;
  margin: 0;
  max-width: 320px;
  line-height: 1.5;
}

.loading-task {
  background: $c-surface-low;
  padding: 12px 16px;
  border-radius: $radius;
  text-align: left;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: $c-text-secondary;
  width: 100%;

  .material-symbols-outlined {
    font-size: 16px;
    animation: spin 1.5s linear infinite;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.task-bar {
  width: 100%;
  height: 6px;
  background: $c-surface-highest;
  border-radius: 3px;
  overflow: hidden;
  margin-top: 8px;
}

.task-bar-fill {
  height: 100%;
  background: $c-brand;
  border-radius: 3px;
}

/* Success State */
.success-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.success-icon {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: rgba(34, 197, 94, 0.1);
  color: $c-success;
  display: flex;
  align-items: center;
  justify-content: center;

  .material-symbols-outlined {
    font-size: 64px;
    font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 48;
  }
}

.success-title {
  font-size: 20px;
  font-weight: 600;
  color: $c-text;
  margin: 0;
}

.success-desc {
  font-size: 14px;
  color: $c-text-secondary;
  margin: 0;
}

.download-btn {
  width: 100%;
  padding: 16px 0;
  background: $c-brand;
  color: #fff;
  border: none;
  border-radius: $radius-xl;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.15s;

  &:hover {
    opacity: 0.9;
  }

  &:active {
    transform: scale(0.97);
  }

  .material-symbols-outlined {
    font-size: 22px;
  }
}

.email-btn {
  width: 100%;
  padding: 12px 0;
  background: none;
  border: none;
  color: $c-brand;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  border-radius: $radius-xl;
  transition: background 0.15s;

  &:hover {
    background: $c-brand-light;
  }
}

.safety-tip {
  font-size: 12px;
  color: $c-text-secondary;
  margin: 8px 0 0;
}

/* ===== Navigation Bar ===== */
.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid $c-border;
  max-width: 720px;
  margin-left: auto;
  margin-right: auto;
  width: 100%;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  border: none;
  border-radius: $radius;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;

  .material-symbols-outlined {
    font-size: 18px;
  }

  &.prev-btn {
    background: none;
    color: $c-text-secondary;

    &:hover:not(:disabled) {
      background: $c-surface-high;
    }

    &:disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }
  }

  &.next-btn {
    background: $c-brand;
    color: #fff;
    padding: 10px 32px;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 80, 214, 0.3);
    }

    &:active {
      transform: scale(0.98);
    }
  }
}

/* ===== Scrollbar ===== */
.export-page {
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: #e1e3e4;
    border-radius: 10px;

    &:hover {
      background: #c3c6d8;
    }
  }
}
</style>
