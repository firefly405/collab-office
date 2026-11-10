<template>
  <div v-if="visible" class="drawer-overlay" @click.self="$emit('update:visible', false)">
    <div class="drawer-backdrop"></div>
    <section class="drawer-panel">
      <!-- Header -->
      <header class="drawer-header">
        <div class="header-left">
          <div class="status-dropdown">
            <span class="material-symbols-outlined status-icon" style="font-variation-settings: 'FILL' 1;">play_circle</span>
            <span class="status-text">{{ statusLabel(task?.status) }}</span>
            <span class="material-symbols-outlined chevron">expand_more</span>
          </div>
          <div class="header-divider"></div>
          <span class="task-id">任务 ID: {{ task?.id || task?.taskId || '--' }}</span>
        </div>
        <div class="header-right">
          <button class="icon-btn" title="复制链接">
            <span class="material-symbols-outlined">content_copy</span>
          </button>
          <button class="icon-btn" title="归档">
            <span class="material-symbols-outlined">archive</span>
          </button>
          <button class="icon-btn danger" title="删除">
            <span class="material-symbols-outlined">delete</span>
          </button>
          <div class="header-divider"></div>
          <button class="icon-btn" @click="$emit('update:visible', false)" title="关闭">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </header>

      <!-- Content -->
      <div class="drawer-content" v-if="task">
        <div class="content-inner">
          <!-- Title -->
          <div class="title-section">
            <textarea
              class="title-input"
              :value="task.title"
              rows="1"
              readonly
              placeholder="任务标题"
            ></textarea>
          </div>

          <!-- Attributes Grid -->
          <div class="attrs-grid">
            <!-- Assignee -->
            <div class="attr-row">
              <span class="attr-label">负责人</span>
              <div class="attr-value assignee-row">
                <el-avatar :size="32" :style="{ backgroundColor: 'var(--c-brand)' }">
                  {{ (task.assignee || task.assigneeName || '?')[0] }}
                </el-avatar>
                <span class="assignee-name">{{ task.assignee || task.assigneeName || '未分配' }}</span>
                <span class="online-dot" v-if="task.assignee"></span>
              </div>
            </div>

            <!-- Due Date -->
            <div class="attr-row">
              <span class="attr-label">截止日期</span>
              <div class="attr-value date-value" :class="{ overdue: isOverdue }">
                <span class="material-symbols-outlined">calendar_today</span>
                <span>{{ task.dueDate || '无' }}</span>
              </div>
            </div>

            <!-- Priority -->
            <div class="attr-row">
              <span class="attr-label">优先级</span>
              <div class="attr-value">
                <span class="priority-tag" :class="'pri-' + (task.priority || 3)">
                  <span class="material-symbols-outlined">priority_high</span>
                  <span>{{ priorityLabel(task.priority) }}</span>
                </span>
              </div>
            </div>

            <!-- Project -->
            <div class="attr-row" v-if="task.project">
              <span class="attr-label">所属项目</span>
              <div class="attr-value project-link">
                <span class="material-symbols-outlined">folder</span>
                <span>{{ task.project }}</span>
              </div>
            </div>

            <!-- Tags -->
            <div class="attr-row full-width" v-if="task.tags?.length">
              <span class="attr-label">标签</span>
              <div class="attr-value">
                <div class="tags-row">
                  <span v-for="t in task.tags" :key="t" class="tag-item">{{ t }}</span>
                  <button class="add-tag-btn">
                    <span class="material-symbols-outlined">add</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Description -->
          <div class="section" v-if="task.description || task.desc">
            <div class="section-header">
              <h4 class="section-title">任务描述</h4>
              <button class="section-action">编辑</button>
            </div>
            <div class="description-box">
              <p>{{ task.description || task.desc }}</p>
            </div>
          </div>

          <!-- Subtasks -->
          <div class="section">
            <div class="section-header">
              <div class="section-title-row">
                <h4 class="section-title">子任务</h4>
                <span class="subtask-count">{{ doneSubtasks }}/{{ subtasks.length }}</span>
              </div>
              <button class="section-action add-action">
                <span class="material-symbols-outlined">add_task</span>
                <span>添加子项</span>
              </button>
            </div>

            <!-- Progress Bar -->
            <div class="subtask-progress" v-if="subtasks.length">
              <div class="progress-track">
                <div
                  class="progress-fill"
                  :style="{ width: subtasks.length ? (doneSubtasks / subtasks.length * 100) + '%' : '0%' }"
                ></div>
              </div>
            </div>

            <!-- Checklist -->
            <div class="checklist">
              <div
                v-for="st in subtasks"
                :key="st.id"
                class="checklist-item"
                :class="{ done: st.done }"
              >
                <span
                  class="material-symbols-outlined check-icon"
                  :style="{ fontVariationSettings: st.done ? `'FILL' 1` : `'FILL' 0` }"
                >
                  {{ st.done ? 'check_box' : 'check_box_outline_blank' }}
                </span>
                <span class="checklist-text">{{ st.title }}</span>
                <span class="material-symbols-outlined drag-handle">drag_indicator</span>
              </div>
              <div v-if="subtasks.length === 0" class="empty-subtasks">
                暂无子任务
              </div>
            </div>
          </div>

          <!-- Comments -->
          <div class="section">
            <h4 class="section-title">协作评论 ({{ comments.length }})</h4>

            <div class="comments-list">
              <div v-for="c in comments" :key="c.id" class="comment-item">
                <el-avatar :size="40" :style="{ backgroundColor: 'var(--c-brand)' }">
                  {{ c.user[0] }}
                </el-avatar>
                <div class="comment-body">
                  <div class="comment-meta">
                    <span class="comment-author">{{ c.user }}</span>
                    <span class="comment-time">{{ c.time }}</span>
                  </div>
                  <div class="comment-text">{{ c.text }}</div>
                </div>
              </div>
              <div v-if="comments.length === 0" class="empty-comments">
                暂无评论
              </div>
            </div>

            <!-- Add Comment -->
            <div class="add-comment">
              <el-avatar :size="40" :style="{ backgroundColor: 'var(--c-text-muted)' }">我</el-avatar>
              <div class="comment-input-wrap">
                <input
                  v-model="newComment"
                  class="comment-input"
                  placeholder="输入回复内容..."
                  @keyup.enter="addComment"
                />
                <div class="comment-input-actions">
                  <button class="input-icon-btn">
                    <span class="material-symbols-outlined">alternate_email</span>
                  </button>
                  <button class="input-icon-btn">
                    <span class="material-symbols-outlined">sentiment_satisfied</span>
                  </button>
                  <button class="input-icon-btn send-btn" @click="addComment">
                    <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">send</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <footer class="drawer-footer">
        <div class="footer-left">
          <span class="material-symbols-outlined">history</span>
          <span>最后修改于不久前</span>
        </div>
        <div class="footer-right">
          <div class="active-avatars">
            <div class="avatar-stack-extra">+2</div>
            <el-avatar :size="24" :style="{ backgroundColor: 'var(--c-text-muted)' }">A</el-avatar>
            <el-avatar :size="24" :style="{ backgroundColor: 'var(--c-brand)' }">B</el-avatar>
          </div>
        </div>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { getTaskDetail } from '@/api/task'

const props = defineProps<{ visible: boolean; task: any }>()
defineEmits<{ (e: 'update:visible', v: boolean): void }>()

const newComment = ref('')
const loadingComments = ref(false)

const subtasks = ref<any[]>([])
const comments = ref<any[]>([])

watch(() => props.visible, (v) => {
  if (v && props.task?.id) {
    loadTaskDetail()
  }
})

function loadTaskDetail() {
  loadingComments.value = true
  getTaskDetail(props.task.id).then((res: any) => {
    if (res.code === 200 && res.data) {
      const d = res.data
      if (d.subtasks && Array.isArray(d.subtasks)) {
        subtasks.value = d.subtasks.map((st: any) => ({
          id: st.id ?? st.taskId ?? st.subtaskId,
          title: st.title ?? st.name ?? '',
          done: st.status === 2 || st.status === 'completed' || !!st.done,
        }))
      }
      if (d.comments && Array.isArray(d.comments)) {
        comments.value = d.comments.map((c: any) => ({
          id: c.id ?? c.commentId,
          user: c.userName ?? c.user ?? c.creatorName ?? '--',
          time: c.createTime ?? c.time ?? '--',
          text: c.content ?? c.text ?? '',
        }))
      }
    }
  }).catch(() => {}).finally(() => loadingComments.value = false)
}

const doneSubtasks = computed(() => subtasks.value.filter(s => s.done).length)

const isOverdue = computed(() => {
  if (!props.task?.dueDate) return false
  return new Date(props.task.dueDate) < new Date()
})

function addComment() {
  if (!newComment.value.trim()) return
  const text = newComment.value.trim()
  request.post(`/task/${props.task.id}/comment`, { content: text }).then((res: any) => {
    if (res.code === 200) {
      comments.value.push({ id: res.data?.id ?? Date.now(), user: '我', time: '刚刚', text })
      newComment.value = ''
    } else {
      ElMessage.error(res.message || '评论失败')
    }
  }).catch(() => ElMessage.error('网络错误'))
}

function statusLabel(s: number) { return ['待办', '进行中', '已完成', '已取消'][s] || '待办' }
function priorityLabel(p: number) { return ['', '紧急', '高', '中', '低'][p] || '中' }
</script>

<style lang="scss" scoped>
/* ── Overlay ── */
.drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  justify-content: flex-end;
}

.drawer-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(2px);
}

/* ── Panel ── */
.drawer-panel {
  position: relative;
  width: 100%;
  max-width: 640px;
  height: 100vh;
  background: #fff;
  box-shadow: -8px 0 30px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  z-index: 1;
  animation: slideIn 0.3s var(--ease-out);
}

@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

/* ── Header ── */
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  border-bottom: 1px solid var(--c-border);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-dropdown {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: var(--c-bg);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: background 0.15s var(--ease-out);

  &:hover {
    background: var(--c-surface);
  }
}

.status-icon {
  font-size: 18px;
  color: var(--c-brand);
  font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}

.status-text {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text);
}

.chevron {
  font-size: 16px;
  color: var(--c-text-muted);
}

.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  display: inline-block;
  line-height: 1;
}

.task-id {
  font-size: 12px;
  color: var(--c-text-secondary);
  font-weight: 500;
}

.header-divider {
  width: 1px;
  height: 20px;
  background: var(--c-border);
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--c-text-secondary);
  cursor: pointer;
  border-radius: var(--radius-lg);
  transition: all 0.15s var(--ease-out);

  .material-symbols-outlined {
    font-size: 20px;
  }

  &:hover {
    background: var(--c-bg);
    color: var(--c-text);
  }

  &.danger:hover {
    color: var(--c-danger);
    background: #ffdad6;
  }
}

/* ── Content ── */
.drawer-content {
  flex: 1;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: var(--c-border);
    border-radius: 10px;
  }
}

.content-inner {
  padding: 32px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* ── Title ── */
.title-section {
  margin-bottom: -8px;
}

.title-input {
  width: 100%;
  font-size: 24px;
  font-weight: 600;
  color: var(--c-text);
  background: transparent;
  border: none;
  resize: none;
  padding: 0;
  outline: none;
  font-family: var(--font-sans);
  line-height: 1.3;
  letter-spacing: -0.01em;

  &:focus {
    ring: none;
  }
}

/* ── Attributes Grid ── */
.attrs-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px 48px;
}

.attr-row {
  display: flex;
  align-items: center;
  gap: 16px;

  &.full-width {
    grid-column: 1 / -1;
  }
}

.attr-label {
  width: 96px;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-secondary);
  letter-spacing: 0.01em;
}

.attr-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.assignee-row {
  gap: 8px;
  cursor: pointer;

  &:hover .assignee-name {
    color: var(--c-brand);
  }
}

.assignee-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text);
  transition: color 0.15s var(--ease-out);
}

.online-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--c-online);
  border: 2px solid #fff;
  margin-left: -4px;
}

.date-value {
  padding: 6px 12px;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-size: 14px;
  color: var(--c-text);
  transition: border-color 0.15s var(--ease-out);

  .material-symbols-outlined {
    font-size: 18px;
    color: var(--c-text-secondary);
  }

  &:hover {
    border-color: var(--c-brand);
  }

  &.overdue {
    color: var(--c-danger);
  }
}

.priority-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: var(--radius-lg);
  font-size: 12px;
  font-weight: 500;

  .material-symbols-outlined {
    font-size: 14px;
  }

  &.pri-1 {
    background: #ffdad6;
    color: #93000a;
  }
  &.pri-2 {
    background: #fff3e0;
    color: #e65100;
  }
  &.pri-3 {
    background: var(--c-selection);
    color: var(--c-brand);
  }
  &.pri-4 {
    background: var(--c-bg);
    color: var(--c-text-muted);
  }
}

.project-link {
  font-size: 14px;
  color: var(--c-brand);
  cursor: pointer;

  .material-symbols-outlined {
    font-size: 18px;
  }

  &:hover {
    text-decoration: underline;
  }
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.tag-item {
  padding: 2px 8px;
  background: var(--c-bg);
  color: var(--c-text-secondary);
  border-radius: var(--radius-lg);
  font-size: 12px;
}

.add-tag-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 1px dashed var(--c-border);
  border-radius: var(--radius-lg);
  background: transparent;
  color: var(--c-text-secondary);
  cursor: pointer;
  transition: all 0.15s var(--ease-out);

  .material-symbols-outlined {
    font-size: 14px;
  }

  &:hover {
    border-color: var(--c-brand);
    color: var(--c-brand);
  }
}

/* ── Section ── */
.section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0;
}

.subtask-count {
  padding: 2px 8px;
  background: var(--c-bg);
  color: var(--c-text-secondary);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.section-action {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-brand);
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: var(--font-sans);

  .material-symbols-outlined {
    font-size: 16px;
  }

  &:hover {
    opacity: 0.8;
  }
}

/* ── Description ── */
.description-box {
  padding: 16px;
  background: #fff;
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl);
  font-size: 14px;
  color: var(--c-text-secondary);
  line-height: 1.7;

  p {
    margin: 0;
  }
}

/* ── Subtasks ── */
.subtask-progress {
  margin-bottom: 4px;
}

.progress-track {
  width: 100%;
  height: 6px;
  background: var(--c-bg);
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--c-brand);
  border-radius: 999px;
  transition: width 0.5s var(--ease-out);
}

.checklist {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.checklist-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: background 0.15s var(--ease-out);

  &:hover {
    background: var(--c-bg);

    .drag-handle {
      opacity: 1;
    }
  }

  &.done .checklist-text {
    text-decoration: line-through;
    opacity: 0.5;
  }
}

.check-icon {
  font-size: 20px;
  color: var(--c-brand);
  flex-shrink: 0;
}

.checklist-text {
  flex: 1;
  font-size: 14px;
  color: var(--c-text);
}

.drag-handle {
  font-size: 18px;
  color: var(--c-text-secondary);
  opacity: 0;
  transition: opacity 0.15s var(--ease-out);
  cursor: grab;
}

.empty-subtasks {
  padding: 16px;
  text-align: center;
  color: var(--c-text-muted);
  font-size: 13px;
}

/* ── Comments ── */
.comments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.comment-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.comment-author {
  font-size: 12px;
  font-weight: 700;
  color: var(--c-text);
}

.comment-time {
  font-size: 11px;
  color: var(--c-text-secondary);
}

.comment-text {
  padding: 10px 14px;
  background: var(--c-bg);
  border-radius: 0 var(--radius-xl) var(--radius-xl) var(--radius-xl);
  font-size: 14px;
  color: var(--c-text-secondary);
  line-height: 1.5;
}

.empty-comments {
  padding: 16px;
  text-align: center;
  color: var(--c-text-muted);
  font-size: 13px;
}

/* ── Add Comment ── */
.add-comment {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--c-border);
}

.comment-input-wrap {
  flex: 1;
  position: relative;
}

.comment-input {
  width: 100%;
  padding: 10px 100px 10px 16px;
  background: #fff;
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl);
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s var(--ease-out);
  font-family: var(--font-sans);
  color: var(--c-text);

  &:focus {
    border-color: var(--c-brand);
  }

  &::placeholder {
    color: var(--c-text-muted);
  }
}

.comment-input-actions {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 2px;
}

.input-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--c-text-secondary);
  cursor: pointer;
  border-radius: var(--radius-lg);
  transition: all 0.15s var(--ease-out);

  .material-symbols-outlined {
    font-size: 20px;
  }

  &:hover {
    color: var(--c-brand);
    background: var(--c-bg);
  }

  &.send-btn {
    color: var(--c-brand);
  }
}

/* ── Footer ── */
.drawer-footer {
  height: 48px;
  padding: 0 24px;
  border-top: 1px solid var(--c-border);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  font-size: 11px;
  color: var(--c-text-secondary);
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 6px;

  .material-symbols-outlined {
    font-size: 14px;
  }
}

.footer-right {
  display: flex;
  align-items: center;
}

.active-avatars {
  display: flex;
  align-items: center;
  gap: -6px;
}

.avatar-stack-extra {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: var(--c-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 8px;
  font-weight: 700;
  color: var(--c-text-secondary);
  z-index: 5;
  margin-right: -8px;
}
</style>
