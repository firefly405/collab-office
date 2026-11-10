<template>
  <div class="history-root">
    <!-- Sidebar -->
    <aside class="history-sidebar">
      <div class="sidebar-brand">
        <h1>Nexus Office</h1>
        <p>全球协作工作区</p>
      </div>
      <nav class="sidebar-nav">
        <a class="nav-item" href="#"><span class="material-symbols-outlined">description</span><span>文档</span></a>
        <a class="nav-item" href="#"><span class="material-symbols-outlined">table_chart</span><span>电子表格</span></a>
        <a class="nav-item" href="#"><span class="material-symbols-outlined">account_tree</span><span>思维导图</span></a>
        <a class="nav-item" href="#"><span class="material-symbols-outlined">draw</span><span>白板</span></a>
        <a class="nav-item" href="#"><span class="material-symbols-outlined">forum</span><span>消息</span></a>
        <a class="nav-item active" href="#"><span class="material-symbols-outlined">videocam</span><span>会议</span></a>
        <a class="nav-item" href="#"><span class="material-symbols-outlined">check_circle</span><span>任务</span></a>
      </nav>
      <div class="sidebar-footer">
        <a class="nav-item" href="#"><span class="material-symbols-outlined">settings</span><span>设置</span></a>
        <a class="nav-item" href="#"><span class="material-symbols-outlined">help</span><span>帮助中心</span></a>
      </div>
    </aside>

    <!-- Main -->
    <main class="history-main">
      <!-- Top Nav -->
      <header class="history-topbar">
        <div class="topbar-left">
          <button class="back-btn" @click="$router.back()">
            <span class="material-symbols-outlined">arrow_back</span>
          </button>
          <div>
            <h2>2023年Q4 产品路线图讨论会</h2>
            <p>2023年10月24日 · 60分钟</p>
          </div>
        </div>
        <div class="topbar-right">
          <div class="avatar-stack">
            <div class="stack-av a1"></div>
            <div class="stack-av a2"></div>
            <div class="stack-av a3"></div>
            <div class="stack-av count">+5</div>
          </div>
          <button class="btn-download">
            <span class="material-symbols-outlined">download</span>
            下载录像
          </button>
          <button class="icon-btn">
            <span class="material-symbols-outlined">share</span>
          </button>
        </div>
      </header>

      <!-- Content Area -->
      <div class="history-content">
        <!-- Left: Player + Bento Cards -->
        <div class="history-left">
          <!-- Video Player -->
          <div class="video-player">
            <div class="player-frame">
              <div class="player-placeholder">
                <span class="material-symbols-outlined">play_circle</span>
              </div>
              <!-- Cursor simulation -->
              <div class="player-cursor">
                <div class="cursor-line"></div>
                <div class="cursor-label">张伟 (产品经理)</div>
              </div>
              <!-- Controls Overlay -->
              <div class="player-controls-overlay">
                <!-- Progress Bar -->
                <div class="progress-bar">
                  <div class="progress-fill" style="width: 67%"></div>
                  <div class="progress-thumb" style="left: 67%"></div>
                </div>
                <div class="controls-row">
                  <div class="controls-left">
                    <button class="ctrl-btn"><span class="material-symbols-outlined">play_arrow</span></button>
                    <button class="ctrl-btn"><span class="material-symbols-outlined">replay_10</span></button>
                    <button class="ctrl-btn"><span class="material-symbols-outlined">forward_30</span></button>
                    <span class="time-display">42:15 / 60:00</span>
                    <button class="ctrl-btn"><span class="material-symbols-outlined">volume_up</span></button>
                  </div>
                  <div class="controls-right">
                    <button class="ctrl-btn"><span class="material-symbols-outlined">closed_caption</span></button>
                    <button class="ctrl-btn"><span class="material-symbols-outlined">settings</span></button>
                    <button class="ctrl-btn"><span class="material-symbols-outlined">fullscreen</span></button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Bento Cards -->
          <div class="bento-grid">
            <!-- Key Takeaways -->
            <div class="bento-card">
              <div class="bento-header">
                <span class="material-symbols-outlined bento-icon">stars</span>
                <h3>核心决议</h3>
              </div>
              <ul class="takeaway-list">
                <li>
                  <span class="bullet"></span>
                  <p>确认 Q4 将主打"实时协作引擎"作为核心卖点，优先保障文档同步毫秒级响应。</p>
                </li>
                <li>
                  <span class="bullet"></span>
                  <p>移动端重构版本延期至 11 月中旬发布，先进行内部灰度测试。</p>
                </li>
              </ul>
            </div>

            <!-- Action Items -->
            <div class="bento-card">
              <div class="bento-header">
                <span class="material-symbols-outlined bento-icon">assignment_turned_in</span>
                <h3>待办事项</h3>
              </div>
              <div class="todo-list">
                <label class="todo-item">
                  <input type="checkbox" checked />
                  <span class="todo-text done">更新技术规格文档</span>
                </label>
                <label class="todo-item">
                  <input type="checkbox" />
                  <span class="todo-text">与设计团队沟通 AI 助手 UI</span>
                </label>
                <label class="todo-item">
                  <input type="checkbox" />
                  <span class="todo-text">预约下周一的进度对齐会议</span>
                </label>
              </div>
            </div>
          </div>
        </div>

        <!-- Right: Tabs Panel -->
        <aside class="history-right">
          <!-- Tabs -->
          <div class="right-tabs">
            <button class="right-tab active">转写文本</button>
            <button class="right-tab">共享文件</button>
            <button class="right-tab">参会人</button>
          </div>

          <!-- Tab Content -->
          <div class="right-content">
            <!-- Transcript Tab -->
            <div class="tab-panel">
              <div class="transcript-item">
                <div class="ts-avatar a1"></div>
                <div>
                  <div class="ts-header">
                    <span class="ts-name">张伟</span>
                    <span class="ts-time">10:05</span>
                  </div>
                  <div class="ts-bubble other">
                    大家好，今天我们主要讨论第四季度的产品路线图。我们目前的重点是提升协同效率。
                  </div>
                </div>
              </div>
              <div class="transcript-item">
                <div class="ts-avatar a2"></div>
                <div>
                  <div class="ts-header">
                    <span class="ts-name">李华</span>
                    <span class="ts-time">10:07</span>
                  </div>
                  <div class="ts-bubble self">
                    技术架构方面我们已经准备好了新一轮的优化，特别是 WebSocket 的稳定性提升。
                  </div>
                </div>
              </div>
              <div class="transcript-item dimmed">
                <div class="ts-avatar a1"></div>
                <div>
                  <div class="ts-header">
                    <span class="ts-name">张伟</span>
                    <span class="ts-time">10:12</span>
                  </div>
                  <div class="ts-bubble other">
                    这非常好。那么对于 UI 层面的反馈，用户更倾向于哪种布局？
                  </div>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </div>

      <!-- AI Summary FAB -->
      <button class="ai-fab">
        <span class="material-symbols-outlined">bolt</span>
        <span>AI 摘要</span>
      </button>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMeetingList } from '@/api/meeting'

const loading = ref(false)
const meetings = ref<any[]>([])

onMounted(() => { loadMeetings() })

function loadMeetings() {
  loading.value = true
  getMeetingList({ status: 'ended', pageSize: 50 }).then((res: any) => {
    if (res.code === 200) {
      meetings.value = (res.data?.records || res.data || []).map((m: any) => ({
        ...m,
        participantCount: m.participantCount || m.participants?.length || 0,
      }))
    }
  }).catch(() => {}).finally(() => loading.value = false)
}

</script>

<style lang="scss" scoped>
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  vertical-align: middle;
}

.history-root {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--c-bg);
  color: var(--c-text);
  font-family: var(--font-sans);
}

// ---- Sidebar ----
.history-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #f3f4f5;
  border-right: 1px solid var(--c-border);
  padding: 1rem 0.5rem;
}
.sidebar-brand {
  padding: 0 1rem;
  margin-bottom: 2rem;
  h1 { font-size: 24px; font-weight: 700; color: var(--c-brand); }
  p { font-size: 12px; color: var(--c-text-muted); margin-top: 4px; }
}
.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: var(--radius-lg);
  color: var(--c-text-secondary);
  text-decoration: none;
  font-size: 14px;
  transition: background .15s, color .15s;
  &:hover { background: #e7e8e9; }
  &.active { background: rgba(51,112,255,.1); color: var(--c-brand); border-left: 4px solid var(--c-brand); font-weight: 600; }
  .material-symbols-outlined { font-size: 20px; }
}
.sidebar-footer {
  margin-top: auto;
  border-top: 1px solid var(--c-border);
  padding-top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

// ---- Main ----
.history-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--c-surface);
  position: relative;
}

// ---- Top Bar ----
.history-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 1rem;
  background: var(--c-surface);
  border-bottom: 1px solid var(--c-border);
  flex-shrink: 0;
  z-index: 10;
}
.topbar-left { display: flex; align-items: center; gap: 16px; }
.back-btn {
  padding: 8px;
  background: none;
  border: none;
  border-radius: 50%;
  color: var(--c-text-muted);
  cursor: pointer;
  &:hover { background: #e7e8e9; }
}
.topbar-left h2 { font-size: 20px; font-weight: 700; color: var(--c-brand); }
.topbar-left p { font-size: 12px; color: var(--c-text-muted); }
.topbar-right { display: flex; align-items: center; gap: 12px; }
.avatar-stack { display: flex; margin-right: 16px; }
.stack-av {
  width: 32px; height: 32px;
  border-radius: 50%;
  border: 2px solid var(--c-surface);
  margin-left: -8px;
  &:first-child { margin-left: 0; }
  &.a1 { background: #b4c5ff; }
  &.a2 { background: #c3c6ce; }
  &.a3 { background: #dfe2eb; }
  &.count {
    background: #dde3ed;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: 600;
  }
}
.btn-download {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--c-brand);
  color: #fff;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background .15s;
  &:hover { background: #2a6af9; }
  &:active { transform: scale(.98); }
  .material-symbols-outlined { font-size: 18px; }
}
.icon-btn {
  padding: 8px;
  background: none;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: var(--c-text-muted);
  &:hover { background: #e7e8e9; }
}

// ---- Content ----
.history-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

// ---- Left Side ----
.history-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 1.5rem;
  gap: 1.5rem;
  overflow-y: auto;
}

// ---- Video Player ----
.video-player { flex-shrink: 0; }
.player-frame {
  position: relative;
  aspect-ratio: 16/9;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  &:hover .player-controls-overlay { opacity: 1; }
}
.player-placeholder {
  width: 100%; height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: .9;
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  .material-symbols-outlined { font-size: 64px; color: rgba(255,255,255,.4); }
}
.player-cursor {
  position: absolute;
  top: 25%;
  left: 33%;
  display: flex;
  align-items: flex-start;
  pointer-events: none;
  .cursor-line { width: 2px; height: 24px; background: var(--c-brand); }
  .cursor-label {
    background: var(--c-brand);
    color: #fff;
    font-size: 10px;
    padding: 2px 6px;
    border-radius: var(--radius);
    white-space: nowrap;
  }
}
.player-controls-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(0deg, rgba(0,0,0,.7) 0%, transparent 40%);
  opacity: 0;
  transition: opacity .2s;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 1rem;
}
.progress-bar {
  width: 100%;
  height: 4px;
  background: rgba(255,255,255,.3);
  border-radius: 9999px;
  margin-bottom: 1rem;
  position: relative;
  cursor: pointer;
}
.progress-fill {
  position: absolute; left: 0; top: 0;
  height: 100%;
  background: var(--c-brand);
  border-radius: 9999px;
}
.progress-thumb {
  position: absolute;
  top: 50%; transform: translate(-50%, -50%);
  width: 12px; height: 12px;
  background: #fff;
  border-radius: 50%;
  box-shadow: var(--shadow-sm);
}
.controls-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
}
.controls-left, .controls-right { display: flex; align-items: center; gap: 16px; }
.ctrl-btn {
  background: none;
  border: none;
  color: #fff;
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius);
  &:hover { background: rgba(255,255,255,.1); }
  .material-symbols-outlined { font-size: 24px; }
}
.time-display { font-size: 12px; font-family: 'JetBrains Mono Variable', monospace; }

// ---- Bento Grid ----
.bento-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  @media (max-width: 768px) { grid-template-columns: 1fr; }
}
.bento-card {
  background: #fff;
  border: 1px solid var(--c-border);
  border-radius: 12px;
  padding: 1.25rem;
}
.bento-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 1rem;
  h3 { font-size: 20px; font-weight: 600; }
  .bento-icon {
    color: var(--c-brand);
    font-variation-settings: 'FILL' 1;
  }
}
.takeaway-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
  li {
    display: flex;
    gap: 12px;
    p { font-size: 14px; }
  }
}
.bullet {
  width: 6px; height: 6px;
  background: var(--c-brand);
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 7px;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: background .15s;
  &:hover { background: #f3f4f5; }
  input[type="checkbox"] {
    width: 16px; height: 16px;
    accent-color: var(--c-brand);
  }
  .todo-text { font-size: 14px; }
  .todo-text.done { color: var(--c-text-muted); text-decoration: line-through; }
}

// ---- Right Side ----
.history-right {
  width: 384px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-left: 1px solid var(--c-border);
  @media (max-width: 1024px) { display: none; }
}
.right-tabs {
  display: flex;
  border-bottom: 1px solid var(--c-border);
}
.right-tab {
  flex: 1;
  padding: 16px;
  background: none;
  border: none;
  font-size: 14px;
  color: var(--c-text-muted);
  cursor: pointer;
  transition: color .15s;
  &:hover { color: var(--c-brand); }
  &.active { color: var(--c-brand); font-weight: 600; border-bottom: 2px solid var(--c-brand); }
}
.right-content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}
.tab-panel {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}
.transcript-item {
  display: flex;
  gap: 16px;
  &.dimmed { opacity: .6; }
}
.ts-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  &.a1 { background: #b4c5ff; }
  &.a2 { background: #c3c6ce; }
}
.ts-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.ts-name { font-size: 12px; font-weight: 600; }
.ts-time { font-size: 12px; color: var(--c-text-muted); }
.ts-bubble {
  padding: 12px;
  border-radius: 12px;
  font-size: 14px;
  &.other { background: #f3f4f5; border-top-left-radius: 0; }
  &.self { background: var(--c-brand); color: #fff; border-top-right-radius: 0; box-shadow: var(--shadow-sm); }
}

// ---- AI FAB ----
.ai-fab {
  position: absolute;
  bottom: 2rem;
  right: 2rem;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 1rem 1.5rem;
  background: var(--c-brand);
  color: #fff;
  border: none;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 8px 24px rgba(0,0,0,.2);
  cursor: pointer;
  z-index: 20;
  transition: background .15s, transform .15s;
  &:hover { background: #2a6af9; }
  &:active { transform: scale(.95); }
  .material-symbols-outlined { font-variation-settings: 'FILL' 1; font-size: 20px; }
}
</style>
