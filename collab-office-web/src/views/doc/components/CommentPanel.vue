<!-- 文档评论面板 -->
<template>
  <div class="comment-panel">
    <div class="comment-header">
      <span class="t-label-md">评论 ({{ comments.length }})</span>
      <el-select v-model="filter" size="small" style="width: 90px">
        <el-option :value="'all'" label="全部" />
        <el-option :value="'unresolved'" label="未解决" />
        <el-option :value="'resolved'" label="已解决" />
      </el-select>
    </div>
    <el-divider style="margin: 4px 0 10px" />

    <div v-if="filtered.length === 0" class="comment-empty t-body-md">
      暂无评论
    </div>

    <div
      v-for="c in filtered"
      :key="c.commentId || c.id"
      class="comment-card"
    >
      <div class="comment-user">
        <el-avatar :size="24" :style="{ backgroundColor: avatarColor(c.userName) }" class="comment-avatar">
          {{ c.userName?.charAt(0) || '?' }}
        </el-avatar>
        <div class="comment-meta">
          <span class="t-body-md" style="font-weight: 600">{{ c.userName }}</span>
          <span class="t-label-md" style="color: var(--c-text-ghost)">{{ formatTime(c.createTime || c.createdTime) }}</span>
        </div>
        <el-button
          v-if="!c.isResolved && !c.resolvedTime"
          size="small"
          text
          type="success"
          @click="$emit('resolve', c)"
        >
          解决
        </el-button>
      </div>
      <div class="comment-content t-body-md">{{ c.content }}</div>

      <!-- 回复列表 -->
      <div v-if="c.replies?.length" class="comment-replies">
        <div
          v-for="r in c.replies"
          :key="r.commentId || r.id"
          class="comment-reply t-body-md"
        >
          <strong>{{ r.userName }}</strong>: {{ r.content }}
        </div>
      </div>

      <!-- 回复输入 -->
      <div v-if="!c.isResolved && !c.resolvedTime" class="reply-input">
        <el-input
          :model-value="replyTexts[String(c.commentId || c.id || '')]"
          @update:model-value="(val: string) => replyTexts[String(c.commentId || c.id || '')] = val"
          size="small"
          placeholder="回复..."
          @keyup.enter="submitReply(c)"
        />
      </div>
    </div>

    <div class="comment-input-area">
      <el-input
        v-model="newComment"
        placeholder="添加评论，选中文本可定位..."
        type="textarea"
        :rows="2"
        @keyup.enter.ctrl="submitNew"
      />
      <el-button type="primary" size="small" @click="submitNew" style="margin-top: 6px" :disabled="!newComment.trim()">
        发表
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'

interface Comment {
  commentId?: number | string
  id?: number | string
  userName?: string
  content: string
  createTime?: string
  createdTime?: string
  isResolved?: boolean
  resolvedTime?: string
  replies?: Comment[]
}

const props = defineProps<{
  comments: Comment[]
}>()

const emit = defineEmits<{
  (e: 'add', content: string): void
  (e: 'reply', comment: Comment, content: string): void
  (e: 'resolve', comment: Comment): void
}>()

const filter = ref<'all' | 'unresolved' | 'resolved'>('all')
const newComment = ref('')
const replyTexts = reactive<Record<string, string>>({})

const filtered = computed(() => {
  if (filter.value === 'all') return props.comments
  if (filter.value === 'unresolved') return props.comments.filter(c => !c.isResolved && !c.resolvedTime)
  return props.comments.filter(c => c.isResolved || c.resolvedTime)
})

function submitNew() {
  const text = newComment.value.trim()
  if (!text) return
  emit('add', text)
  newComment.value = ''
}

function submitReply(c: Comment) {
  const id = String(c.commentId || c.id || '')
  const text = (replyTexts[id] || '').trim()
  if (!text) return
  emit('reply', c, text)
  replyTexts[id] = ''
}

function formatTime(time?: string) {
  if (!time) return ''
  try {
    const d = new Date(time)
    return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
  } catch {
    return time
  }
}

function avatarColor(name?: string) {
  const colors = ['#3370FF', '#00B578', '#FF8F1F', '#FF3B30', '#8B5CF6', '#6B7280']
  let hash = 0
  const s = name || '?'
  for (const ch of s) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
  return colors[hash % colors.length]
}
</script>

<style lang="scss" scoped>
.comment-panel {
  width: 280px;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 12px;
  border-left: 1px solid var(--c-border);
  background: var(--c-surface-dim);
  overflow-y: auto;
  flex-shrink: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;
  color: var(--c-text-secondary);
  font-weight: 600;
}

.comment-empty {
  padding: 20px 4px;
  color: var(--c-text-ghost);
  text-align: center;
}

.comment-card {
  padding: 8px;
  margin-bottom: 8px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg);
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.comment-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-content {
  color: var(--c-text);
  line-height: 1.5;
  white-space: pre-wrap;
  margin-left: 30px;
}

.comment-replies {
  margin-top: 6px;
  margin-left: 30px;
  padding-left: 8px;
  border-left: 2px solid var(--c-border);
}

.comment-reply {
  padding: 3px 0;
  color: var(--c-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.reply-input {
  margin-top: 6px;
  margin-left: 30px;
}

.comment-input-area {
  margin-top: auto;
  padding-top: 8px;
  border-top: 1px solid var(--c-border);
}
</style>
