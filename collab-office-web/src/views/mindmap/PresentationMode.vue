<template>
  <div
    class="presentation"
    @keydown="onKey"
    tabindex="0"
    ref="container"
  >
    <!-- ===== Mesh Background ===== -->
    <div class="bg-mesh"></div>

    <!-- ===== Header ===== -->
    <header class="pres-header">
      <div class="header-left">
        <h1 class="header-title">思维导图演示模式</h1>
        <p class="header-subtitle">Presentation Mode / v2.4</p>
      </div>
    </header>

    <!-- ===== Top-Right Presence ===== -->
    <div class="presence-bar">
      <div class="avatar-stack">
        <div class="avatar" style="--avc: #3B82F6">
          <span>张</span>
        </div>
        <div class="avatar" style="--avc: #EF4444">
          <span>李</span>
        </div>
      </div>
      <span class="presence-label">2 人正在查看</span>
    </div>

    <!-- ===== Main Stage ===== -->
    <main class="pres-stage">
      <!-- SVG Connectors -->
      <svg class="stage-svg" viewBox="0 0 1200 800" preserveAspectRatio="xMidYMid meet">
        <path
          class="node-connection"
          d="M600 400 L850 250"
          fill="none"
          stroke="#0050d6"
          stroke-width="2"
        />
        <path
          class="node-connection dim"
          d="M600 400 L850 400"
          fill="none"
          stroke="#0050d6"
          stroke-width="2"
        />
        <path
          class="node-connection dim"
          d="M600 400 L850 550"
          fill="none"
          stroke="#0050d6"
          stroke-width="2"
        />
        <path
          class="node-connection dim"
          d="M600 400 L350 300"
          fill="none"
          stroke="#0050d6"
          stroke-width="2"
        />
        <path
          class="node-connection dim"
          d="M600 400 L350 500"
          fill="none"
          stroke="#0050d6"
          stroke-width="2"
        />
        <path
          class="node-connection"
          d="M850 250 L1050 180"
          fill="none"
          stroke="#0050d6"
          stroke-width="1.5"
        />
        <path
          class="node-connection"
          d="M850 250 L1050 320"
          fill="none"
          stroke="#0050d6"
          stroke-width="1.5"
        />
      </svg>

      <!-- Transition Wrapper -->
      <transition name="stage-fade">
        <div :key="currentStep" class="stage-content">
          <!-- Central Node (Current Step) - Pulse Glow -->
          <div class="central-node">
            <div class="mindmap-node pulse-glow node-primary">
              <span class="node-main-title">{{ steps[currentStep]?.title || title }}</span>
              <span class="node-sub-label">核心演示单元</span>
            </div>
          </div>

          <!-- Right Sub-node (Expanded) -->
          <div class="sub-node sub-node-right-top">
            <div class="mindmap-node node-glass">
              <div class="node-glass-header">
                <span class="node-glass-title">{{ steps[Math.min(currentStep + 1, steps.length - 1)]?.title || '核心模块' }}</span>
                <span class="material-symbols-outlined icon-20">expand_more</span>
              </div>
              <ul class="node-glass-list">
                <li
                  v-for="(item, i) in getSubItems(currentStep)"
                  :key="i"
                  class="node-bullet-item"
                >
                  <span class="bullet"></span>
                  <span class="bullet-text">{{ item }}</span>
                </li>
              </ul>
            </div>
          </div>

          <!-- Child node 1 -->
          <div class="sub-node sub-node-far-right-top">
            <div class="mindmap-node node-dim">
              <span>{{ steps[Math.min(currentStep + 1, steps.length - 1)]?.note || '架构性能基准' }}</span>
            </div>
          </div>

          <!-- Child node 2 -->
          <div class="sub-node sub-node-far-right-bottom">
            <div class="mindmap-node node-dim">
              <span>延迟优化策略</span>
            </div>
          </div>

          <!-- Left nodes (dimmed) -->
          <div
            v-for="(step, i) in steps.filter((_, idx) => idx !== currentStep).slice(0, 2)"
            :key="'left-' + i"
            class="sub-node"
            :class="i === 0 ? 'sub-node-left-top' : 'sub-node-left-bottom'"
          >
            <div class="mindmap-node node-ghost">
              <span>{{ step.title }}</span>
            </div>
          </div>
        </div>
      </transition>
    </main>

    <!-- ===== Bottom Navigation ===== -->
    <nav class="pres-nav">
      <div class="nav-pill">
        <button
          class="nav-btn"
          :disabled="currentStep === 0"
          title="上一步"
          @click="prev"
        >
          <span class="material-symbols-outlined">navigate_before</span>
        </button>

        <div class="nav-divider"></div>

        <button
          class="nav-btn play-btn"
          :class="{ playing: isPlaying }"
          title="播放/暂停"
          @click="togglePlay"
        >
          <span class="material-symbols-outlined icon-28">
            {{ isPlaying ? 'pause' : 'play_arrow' }}
          </span>
        </button>

        <div class="nav-divider"></div>

        <button
          class="nav-btn"
          :disabled="currentStep >= steps.length - 1"
          title="下一步"
          @click="next"
        >
          <span class="material-symbols-outlined">navigate_next</span>
        </button>

        <button
          class="nav-btn exit-btn"
          title="退出演示"
          @click="exit"
        >
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <!-- Slide Dots -->
      <div class="slide-dots">
        <span
          v-for="(_, i) in steps"
          :key="'dot-' + i"
          class="dot"
          :class="{ active: i === currentStep }"
          @click="currentStep = i"
        ></span>
      </div>
    </nav>

    <!-- ===== Bottom-Right Controls ===== -->
    <div class="corner-controls">
      <button class="corner-btn">
        <span class="material-symbols-outlined icon-20">zoom_in</span>
        <span class="corner-label">100%</span>
      </button>
      <button class="corner-btn">
        <span class="material-symbols-outlined icon-20">center_focus_strong</span>
        <span class="corner-label">重置视图</span>
      </button>
    </div>

    <!-- ===== Speaker Notes (if any) ===== -->
    <div v-if="steps[currentStep]?.speakerNote" class="speaker-notes-bar">
      <span class="notes-label">演讲者备注：</span>
      <span class="notes-text">{{ steps[currentStep].speakerNote }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps<{ nodes: any[]; title?: string }>()
const emit = defineEmits<{ (e: 'exit'): void }>()

const currentStep = ref(0)
const container = ref<HTMLElement>()
const isPlaying = ref(false)
let playTimer: ReturnType<typeof setInterval> | null = null

interface Step { title: string; note: string; speakerNote: string }

const steps = ref<Step[]>([])

onMounted(() => {
  buildSteps()
  container.value?.focus()
})

function buildSteps() {
  steps.value = (props.nodes || []).map((n: any) => ({
    title: n.content || n.title || '',
    note: n.note || '',
    speakerNote: n.note || '',
  }))
}

function getSubItems(_stepIdx: number): string[] {
  const items: string[] = []
  if (steps.value.length > 0) {
    items.push('实时同步引擎 (RSE)')
    items.push('冲突解决协议')
    items.push('分布式状态管理')
  }
  return items
}

function next() {
  if (currentStep.value < steps.value.length - 1) currentStep.value++
}
function prev() {
  if (currentStep.value > 0) currentStep.value--
}
function onKey(e: KeyboardEvent) {
  if (e.key === 'ArrowRight' || e.key === ' ') { e.preventDefault(); next() }
  else if (e.key === 'ArrowLeft') { e.preventDefault(); prev() }
  else if (e.key === 'Escape') exit()
}
function exit() { emit('exit') }

function togglePlay() {
  isPlaying.value = !isPlaying.value
  if (isPlaying.value) {
    playTimer = setInterval(() => {
      if (currentStep.value < steps.value.length - 1) {
        currentStep.value++
      } else {
        isPlaying.value = false
        if (playTimer) clearInterval(playTimer)
      }
    }, 3000)
  } else {
    if (playTimer) clearInterval(playTimer)
  }
}

onUnmounted(() => {
  if (playTimer) clearInterval(playTimer)
  document.exitFullscreen?.().catch(() => {})
})
</script>

<style lang="scss" scoped>
/* ===== CSS Variables ===== */
.presentation {
  --c-brand: #0050d6;
  --c-brand-container: #2a6af9;
  --c-text-on-brand: #ffffff;
  --c-text-primary: #f8f9fa;
  --c-text-secondary: #c3c6d8;
  --c-text-dim: rgba(255, 255, 255, 0.5);
  --c-bg-mesh: #191c1d;
  --c-surface-glass: rgba(255, 255, 255, 0.08);
  --c-border-glass: rgba(255, 255, 255, 0.1);
  --c-presence-online: #22c55e;
  --radius-sm: 0.125rem;
  --radius-md: 0.25rem;
  --radius-lg: 0.5rem;
  --radius-xl: 0.75rem;

  position: fixed;
  inset: 0;
  z-index: 9999;
  background: var(--c-bg-mesh);
  color: var(--c-text-primary);
  display: flex;
  flex-direction: column;
  outline: none;
  font-family: 'Inter Variable', -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 14px;
  line-height: 22px;
  overflow: hidden;
  user-select: none;

  ::selection {
    background: rgba(0, 80, 214, 0.3);
  }
}

/* ===== Material Symbols ===== */
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  display: inline-block;
  line-height: 1;
  text-transform: none;
  letter-spacing: normal;
  word-wrap: normal;
  white-space: nowrap;
  direction: ltr;
}
.icon-20 { font-size: 20px; font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24; }
.icon-28 { font-size: 28px; font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24; }

/* ===== Mesh Background ===== */
.bg-mesh {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-color: var(--c-bg-mesh);
  background-image:
    radial-gradient(at 0% 0%, rgba(0, 80, 214, 0.15) 0px, transparent 50%),
    radial-gradient(at 100% 100%, rgba(0, 80, 214, 0.1) 0px, transparent 50%);
  pointer-events: none;
}

/* ===== Header ===== */
.pres-header {
  position: absolute;
  top: 32px;
  left: 32px;
  z-index: 20;
}

.header-title {
  font-size: 24px;
  font-weight: 600;
  line-height: 32px;
  letter-spacing: -0.01em;
  color: var(--c-text-primary);
  opacity: 0.9;
  margin: 0;
}

.header-subtitle {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: var(--c-text-secondary);
  margin: 4px 0 0;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

/* ===== Presence Bar ===== */
.presence-bar {
  position: absolute;
  top: 32px;
  right: 32px;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-stack {
  display: flex;
  align-items: center;

  > * + * {
    margin-left: -8px;
  }
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid var(--c-bg-mesh);
  background: var(--c-surface-glass);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: var(--c-text-primary);
}

.presence-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-primary);
  background: rgba(255, 255, 255, 0.08);
  padding: 4px 12px;
  border-radius: var(--radius-md);
}

/* ===== Main Stage ===== */
.pres-stage {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 1;
}

.stage-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  opacity: 0.4;
  pointer-events: none;
}

/* ===== SVG Connector Animation ===== */
.node-connection {
  stroke-dasharray: 1000;
  stroke-dashoffset: 1000;
  animation: draw-line 1.5s ease-out forwards;

  &.dim {
    opacity: 0.3;
  }
}

@keyframes draw-line {
  to { stroke-dashoffset: 0; }
}

/* ===== Stage Content (Transition) ===== */
.stage-content {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 1050px;
  height: 100%;
}

/* ===== Central Node ===== */
.central-node {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 5;
}

.mindmap-node {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: scale(1.05) translateY(-5px);
  }
}

.node-primary {
  background: var(--c-brand);
  color: var(--c-text-on-brand);
  padding: 20px 32px;
  border-radius: var(--radius-xl);
  border: 1px solid rgba(180, 197, 255, 0.3);
  box-shadow: 0 0 40px rgba(0, 80, 214, 0.3);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.pulse-glow {
  animation: pulse-glow 2s infinite;
}

@keyframes pulse-glow {
  0% { box-shadow: 0 0 0 0 rgba(0, 80, 214, 0.4); }
  70% { box-shadow: 0 0 0 20px rgba(0, 80, 214, 0); }
  100% { box-shadow: 0 0 0 0 rgba(0, 80, 214, 0); }
}

.node-main-title {
  font-size: 24px;
  font-weight: 600;
  line-height: 32px;
  letter-spacing: -0.01em;
  margin-bottom: 4px;
}

.node-sub-label {
  font-size: 12px;
  font-weight: 500;
  opacity: 0.8;
}

/* ===== Sub Nodes ===== */
.sub-node {
  position: absolute;
  z-index: 3;
}

.sub-node-right-top {
  left: 70%;
  top: 31%;
  transform: translate(-50%, -50%);
}

.sub-node-far-right-top {
  left: 88%;
  top: 22%;
  transform: translate(-50%, -50%);
}

.sub-node-far-right-bottom {
  left: 88%;
  top: 40%;
  transform: translate(-50%, -50%);
}

.sub-node-left-top {
  left: 30%;
  top: 37%;
  transform: translate(-50%, -50%);
  opacity: 0.3;
  pointer-events: none;
}

.sub-node-left-bottom {
  left: 30%;
  top: 62%;
  transform: translate(-50%, -50%);
  opacity: 0.3;
  pointer-events: none;
}

/* ===== Glass Node (Expanded) ===== */
.node-glass {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(0, 80, 214, 0.4);
  border-radius: var(--radius-lg);
  padding: 20px;
  width: 256px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.3);
}

.node-glass-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.node-glass-title {
  font-size: 20px;
  font-weight: 600;
  line-height: 28px;
  color: var(--c-text-primary);
}

.node-glass-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.node-bullet-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.bullet {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--c-brand);
  flex-shrink: 0;
}

.bullet-text {
  font-size: 14px;
  font-weight: 400;
  color: var(--c-text-primary);
  transition: color 0.15s;

  .node-bullet-item:hover & {
    color: var(--c-brand);
  }
}

/* ===== Dim Node ===== */
.node-dim {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-lg);
  padding: 12px 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.2);

  span {
    font-size: 14px;
    color: var(--c-text-secondary);
  }
}

/* ===== Ghost Node (far dim) ===== */
.node-ghost {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: var(--radius-lg);
  padding: 16px 24px;
  width: 256px;

  span {
    font-size: 20px;
    font-weight: 600;
    color: var(--c-text-primary);
    opacity: 0.5;
  }
}

/* ===== Stage Transition ===== */
.stage-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.stage-fade-leave-active {
  transition: all 0.2s ease;
}
.stage-fade-enter-from {
  opacity: 0;
  transform: translateY(24px);
}
.stage-fade-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

/* ===== Bottom Nav ===== */
.pres-nav {
  position: absolute;
  bottom: 48px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 30;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.nav-pill {
  display: flex;
  align-items: center;
  gap: 2px;
  background: rgba(46, 49, 50, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 8px 8px;
  border-radius: 999px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.4);
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--c-text-primary);
  cursor: pointer;
  transition: all 0.15s;

  &:hover:not(:disabled) {
    background: rgba(255, 255, 255, 0.1);
  }
  &:active:not(:disabled) {
    transform: scale(0.95);
  }
  &:disabled {
    opacity: 0.3;
    cursor: not-allowed;
  }
}

.play-btn {
  width: 56px;
  height: 56px;
  background: var(--c-brand);
  color: var(--c-text-on-brand);
  box-shadow: 0 4px 16px rgba(0, 80, 214, 0.4);
  transition: all 0.25s;

  &:hover {
    background: var(--c-brand-container);
    .material-symbols-outlined {
      transform: scale(1.1);
    }
  }

  &.playing {
    background: rgba(255, 255, 255, 0.1);
    color: var(--c-text-primary);
    box-shadow: none;
  }
}

.exit-btn {
  margin-left: 8px;

  &:hover {
    color: #ef4444;
    background: rgba(239, 68, 68, 0.1);
  }
}

.nav-divider {
  width: 1px;
  height: 24px;
  background: rgba(255, 255, 255, 0.2);
  margin: 0 4px;
}

/* ===== Slide Dots ===== */
.slide-dots {
  display: flex;
  gap: 8px;
  padding: 6px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.4);
  }

  &.active {
    background: var(--c-brand);
    transform: scale(1.25);
  }
}

/* ===== Corner Controls ===== */
.corner-controls {
  position: absolute;
  bottom: 32px;
  right: 32px;
  z-index: 20;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.corner-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: none;
  border-radius: var(--radius-lg);
  background: transparent;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: color 0.15s;

  &:hover {
    color: var(--c-text-primary);
  }
}

.corner-label {
  font-size: 12px;
  font-weight: 500;
}

/* ===== Speaker Notes ===== */
.speaker-notes-bar {
  position: absolute;
  bottom: 140px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 15;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--c-brand);
  color: rgba(255, 255, 255, 0.6);
  max-width: 500px;
  max-height: 80px;
  overflow-y: auto;
  font-size: 14px;
}

.notes-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-dim);
  margin-right: 4px;
}

.notes-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}
</style>
