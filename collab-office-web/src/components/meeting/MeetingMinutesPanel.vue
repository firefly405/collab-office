<template>
  <div>
    <!-- Backdrop -->
    <div class="minutes-backdrop" :class="{ show: visible }" @click="$emit('update:visible', false)"></div>

    <!-- Main Panel Container -->
    <div class="minutes-panel-root" :class="{ show: visible }">
      <!-- Header -->
      <header class="minutes-header">
        <div class="header-left">
          <span class="header-title">会议纪要与实时转录</span>
          <span class="recording-badge">
            <span class="recording-dot"></span>
            正在录制
          </span>
        </div>
        <div class="header-right">
          <button class="icon-btn">
            <span class="material-symbols-outlined">history</span>
          </button>
          <button class="icon-btn">
            <span class="material-symbols-outlined">more_vert</span>
          </button>
          <button class="close-btn" @click="$emit('update:visible', false)">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </header>

      <!-- Body: Split Panel -->
      <div class="minutes-body">
        <!-- Left Panel: Transcript -->
        <section class="transcript-panel">
          <div class="transcript-scroll">
            <div class="transcript-inner">
              <!-- Meeting Info Header -->
              <header class="meeting-info-head">
                <h1>Q3 季度产品路线图讨论</h1>
                <div class="meeting-info-meta">
                  <span><span class="material-symbols-outlined">calendar_today</span> 2023年10月24日</span>
                  <span><span class="material-symbols-outlined">schedule</span> 14:00 - 15:30</span>
                </div>
              </header>

              <!-- Transcript Blocks -->
              <div class="transcript-blocks">
                <div class="transcript-block">
                  <div class="block-avatar" style="background: #dbe1ff; color: #0050d6;">张</div>
                  <div class="block-content">
                    <div class="block-header">
                      <span class="block-speaker">张伟 (产品经理)</span>
                      <span class="block-time">14:02</span>
                    </div>
                    <p class="block-text">
                      大家好，今天我们的主要议题是确定 Q3 的核心功能优先级。目前我们的重点依然放在"实时协作"和"AI生产力"这两个维度上。
                    </p>
                  </div>
                </div>

                <div class="transcript-block">
                  <div class="block-avatar" style="background: #dde3ed; color: #565c65;">李</div>
                  <div class="block-content">
                    <div class="block-header">
                      <span class="block-speaker">李娜 (技术主管)</span>
                      <span class="block-time">14:05</span>
                    </div>
                    <p class="block-text">
                      关于实时协作，我们需要在 11 月中旬之前完成底层协同算法的优化，以支持超过 100 人的并发编辑。这部分我会让开发团队在下周出一份详细的技术方案。
                    </p>
                    <div class="ai-task-hint">
                      <span class="ai-label">AI 识别出潜在任务:</span>
                      <p>下周提交底层协同算法优化的技术方案。</p>
                    </div>
                  </div>
                </div>

                <div class="transcript-block">
                  <div class="block-avatar" style="background: #dfe2eb; color: #5b5e65;">王</div>
                  <div class="block-content">
                    <div class="block-header">
                      <span class="block-speaker">王强 (设计总监)</span>
                      <span class="block-time">14:12</span>
                    </div>
                    <p class="block-text">
                      UI 方面，我们需要重新设计任务看板的视觉层级。现在的阴影太重了，不符合我们"轻量化、高性能"的定位。我建议在 10 月底完成初版原型。
                    </p>
                  </div>
                </div>

                <!-- AI Typing Indicator -->
                <div class="ai-typing">
                  <span class="ai-dot"></span>
                  <span class="ai-dot"></span>
                  <span class="ai-dot"></span>
                  <span class="ai-text">AI 正在转录并分析后续对话...</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Input Bar -->
          <div class="transcript-input-bar">
            <div class="input-row">
              <input type="text" placeholder="输入笔记或指令..." />
              <button class="send-btn">
                <span class="material-symbols-outlined">send</span>
              </button>
            </div>
          </div>
        </section>

        <!-- Right Panel: AI Tasks -->
        <section class="tasks-panel">
          <div class="tasks-header">
            <div class="tasks-header-left">
              <span class="material-symbols-outlined">auto_awesome</span>
              <span class="tasks-title">AI 智能提取任务</span>
            </div>
            <span class="beta-tag">Beta</span>
          </div>

          <div class="tasks-scroll">
            <!-- Task Cards -->
            <div class="task-card">
              <div class="task-row">
                <el-checkbox v-model="task1Done" size="small" />
                <div class="task-body">
                  <div class="task-name">底层协同算法优化方案</div>
                  <div class="task-tags">
                    <span class="task-assignee">
                      <span class="assignee-av" style="background: #b4c5ff;">李</span>
                      李娜
                    </span>
                    <span class="task-due overdue">
                      <span class="material-symbols-outlined">event</span>
                      10月31日
                    </span>
                  </div>
                  <div class="task-quote">
                    "这部分我会让开发团队在下周出一份详细的技术方案。"
                  </div>
                </div>
              </div>
            </div>

            <div class="task-card">
              <div class="task-row">
                <el-checkbox v-model="task2Done" size="small" />
                <div class="task-body">
                  <div class="task-name">任务看板 UI 视觉迭代原型</div>
                  <div class="task-tags">
                    <span class="task-assignee">
                      <span class="assignee-av" style="background: #dfe2eb;">王</span>
                      王强
                    </span>
                    <span class="task-due">
                      <span class="material-symbols-outlined">event</span>
                      10月30日
                    </span>
                  </div>
                  <div class="task-quote">
                    "我建议在 10 月底完成初版原型。"
                  </div>
                </div>
              </div>
            </div>

            <!-- Newly extracted task (highlighted) -->
            <div class="task-card new-task">
              <div class="new-badge">New</div>
              <div class="task-row">
                <el-checkbox v-model="task3Done" size="small" />
                <div class="task-body">
                  <div class="task-name">确定 Q3 核心功能优先级</div>
                  <div class="task-tags">
                    <span class="task-assignee">
                      <span class="assignee-av" style="background: #c3c6ce;">张</span>
                      张伟
                    </span>
                    <span class="task-due pending">
                      <span class="material-symbols-outlined">event</span>
                      待定
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Dynamic todos from script -->
            <div v-for="(t, i) in todos" :key="'todo-' + i" class="task-card">
              <div class="task-row">
                <el-checkbox v-model="t.done" size="small" />
                <div class="task-body">
                  <div class="task-name">{{ t.title }}</div>
                  <div class="task-tags">
                    <span v-if="t.assignee" class="task-assignee">
                      <span class="assignee-av" style="background: #c3c6ce;">{{ t.assignee[0] }}</span>
                      {{ t.assignee }}
                    </span>
                    <span v-if="t.dueDate" class="task-due">
                      <span class="material-symbols-outlined">event</span>
                      {{ t.dueDate }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Add todo -->
            <button class="add-todo-btn" @click="todos.push({ title: '', done: false, assignee: '', dueDate: '' })">
              <span class="material-symbols-outlined">add</span>
              添加待办
            </button>
          </div>

          <!-- Save Footer -->
          <div class="tasks-footer">
            <button class="save-btn" @click="saveAll">
              <span class="material-symbols-outlined">save</span>
              保存至任务中心
            </button>
            <p class="sync-hint">同步到：<strong>技术团队看板</strong></p>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const props = defineProps<{ visible: boolean; meetingId: string }>()
defineEmits<{ (e: 'update:visible', v: boolean): void }>()

const minutes = ref('')
const todos = ref<any[]>([])

// UI state for demo task cards
const task1Done = ref(true)
const task2Done = ref(false)
const task3Done = ref(false)

watch(() => props.visible, (v) => { if (v) loadMinutes() })

function loadMinutes() {
  request.get(`/meeting/${props.meetingId}/minutes`).then((res: any) => {
    if (res.code === 200 && res.data) {
      minutes.value = res.data.minuteContent || res.data.content || ''
      todos.value = res.data.todoListJson || res.data.todos || []
    }
  }).catch(() => {})
}

function saveAll() {
  request.put(`/meeting/${props.meetingId}/minutes`, {
    minuteContent: minutes.value,
    todoListJson: todos.value,
  }).then((res: any) => {
    if (res.code === 200) ElMessage.success('已保存')
  }).catch(() => ElMessage.error('保存失败'))
}

</script>

<style lang="scss" scoped>
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  vertical-align: middle;
  font-size: 20px;
}

// ---- Backdrop ----
.minutes-backdrop {
  position: fixed; inset: 0;
  background: rgba(0,0,0,.2);
  backdrop-filter: blur(4px);
  z-index: 54;
  opacity: 0; visibility: hidden;
  transition: opacity .3s, visibility .3s;
  &.show { opacity: 1; visibility: visible; }
}

// ---- Panel Root ----
.minutes-panel-root {
  position: fixed; top: 0; right: 0; bottom: 0;
  width: calc(100vw - 280px);
  background: var(--c-bg);
  z-index: 55;
  transform: translateX(100%);
  transition: transform .3s var(--ease-out);
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px rgba(0,0,0,.15);
  &.show { transform: translateX(0); }
}

// ---- Header ----
.minutes-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  background: var(--c-surface);
  border-bottom: 1px solid var(--c-border);
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-title { font-size: 20px; font-weight: 700; }
.recording-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  background: #ffdad6;
  color: #ba1a1a;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
}
.recording-dot {
  width: 6px; height: 6px;
  background: #ba1a1a;
  border-radius: 50%;
  animation: pulse-dot 1.5s infinite;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: .4; }
}
.header-right { display: flex; align-items: center; gap: 8px; }
.icon-btn {
  padding: 8px;
  background: none;
  border: none;
  border-radius: 50%;
  color: var(--c-text-secondary);
  cursor: pointer;
  &:hover { background: #f3f4f5; }
}
.close-btn {
  padding: 8px;
  background: none;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  &:hover { background: #f3f4f5; }
}

// ---- Body ----
.minutes-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

// ---- Transcript Panel ----
.transcript-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--c-surface);
  border-right: 1px solid var(--c-border);
}
.transcript-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 2rem;
}
.transcript-inner {
  max-width: 672px;
  margin: 0 auto;
}

.meeting-info-head {
  margin-bottom: 2.5rem;
  h1 { font-size: 32px; font-weight: 700; margin-bottom: 8px; letter-spacing: -.02em; }
  .meeting-info-meta {
    display: flex;
    gap: 16px;
    color: var(--c-text-secondary);
    font-size: 14px;
    span { display: flex; align-items: center; gap: 4px;
      .material-symbols-outlined { font-size: 18px; }
    }
  }
}

.transcript-blocks {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}
.transcript-block {
  display: flex;
  gap: 16px;
}
.block-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}
.block-content { flex: 1; }
.block-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.block-speaker { font-weight: 600; font-size: 14px; }
.block-time { font-size: 11px; color: var(--c-text-secondary); }
.block-text { color: var(--c-text-muted); line-height: 1.6; }

.ai-task-hint {
  margin-top: 8px;
  background: rgba(51,112,255,.1);
  border-left: 4px solid var(--c-brand);
  padding: 12px;
  border-radius: 0 var(--radius-lg) var(--radius-lg) 0;
  .ai-label { font-size: 12px; font-weight: 700; color: var(--c-brand); display: block; margin-bottom: 4px; }
  p { font-size: 14px; }
}

.ai-typing {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ai-dot {
  width: 8px; height: 8px;
  background: var(--c-brand);
  border-radius: 50%;
  animation: bounce-dot 1.2s infinite;
  &:nth-child(2) { animation-delay: .15s; }
  &:nth-child(3) { animation-delay: .3s; }
}
.ai-text { font-size: 12px; color: var(--c-text-secondary); font-style: italic; }

@keyframes bounce-dot {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.transcript-input-bar {
  height: 80px;
  padding: 1rem;
  border-top: 1px solid var(--c-border);
  background: #fff;
  display: flex;
  align-items: center;
}
.input-row {
  position: relative;
  width: 100%;
  input {
    width: 100%;
    padding: 12px 48px 12px 24px;
    background: #edeeef;
    border: none;
    border-radius: 12px;
    font-size: 14px;
    outline: none;
    &:focus { box-shadow: 0 0 0 2px var(--c-brand); }
  }
}
.send-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  padding: 6px;
  background: var(--c-brand);
  color: #fff;
  border: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  &:hover { opacity: .9; }
}

// ---- Tasks Panel ----
.tasks-panel {
  width: 400px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #f3f4f5;
}
.tasks-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  background: var(--c-surface);
  border-bottom: 1px solid var(--c-border);
  flex-shrink: 0;
}
.tasks-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  .material-symbols-outlined { color: var(--c-brand); }
}
.tasks-title { font-weight: 700; }
.beta-tag {
  font-size: 12px;
  padding: 2px 8px;
  background: #dbe1ff;
  color: #00174b;
  border-radius: var(--radius);
}
.tasks-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-card {
  background: var(--c-surface);
  padding: 1rem;
  border-radius: 12px;
  border: 1px solid var(--c-border);
  position: relative;
  transition: background .15s;
  &:hover { background: #f8f9fa; }
  &.new-task {
    background: rgba(219,225,255,.3);
    border: 1px dashed var(--c-brand);
  }
}
.new-badge {
  position: absolute;
  top: 0; right: 0;
  padding: 2px 8px;
  background: var(--c-brand);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
}
.task-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.task-body { flex: 1; }
.task-name { font-weight: 600; margin-bottom: 8px; }
.task-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.task-assignee {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: #e7e8e9;
  border-radius: 9999px;
  font-size: 12px;
  .assignee-av {
    width: 16px; height: 16px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: 600;
  }
}
.task-due {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #e7e8e9;
  color: var(--c-text-muted);
  border-radius: 9999px;
  font-size: 12px;
  .material-symbols-outlined { font-size: 14px; }
  &.overdue { background: #ffdad6; color: #ba1a1a; }
  &.pending { background: #c1c7d1; color: #161c23; }
}
.task-quote {
  font-size: 12px;
  color: var(--c-text-secondary);
  font-style: italic;
  border-left: 2px solid #dbe1ff;
  padding-left: 8px;
}

.add-todo-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  background: none;
  border: 1px dashed var(--c-border);
  border-radius: var(--radius-lg);
  color: var(--c-text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: border-color .15s, color .15s;
  &:hover { border-color: var(--c-brand); color: var(--c-brand); }
  .material-symbols-outlined { font-size: 18px; }
}

.tasks-footer {
  padding: 1.5rem;
  background: var(--c-surface);
  border-top: 1px solid var(--c-border);
  flex-shrink: 0;
}
.save-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: var(--c-brand);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
  &:hover { background: #2a6af9; }
  &:active { transform: scale(.98); }
}
.sync-hint {
  font-size: 11px;
  color: var(--c-text-secondary);
  text-align: center;
  margin-top: 12px;
}
</style>
