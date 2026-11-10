<!-- Kinetic Sync §5: Message Bubbles -->
<template>
  <div class="msg-group" :class="{ 'msg-self': isSelf, 'msg-other': !isSelf }">
    <div v-if="showAvatar" class="msg-avatar">
      <el-avatar :size="28" :src="avatar" />
    </div>
    <div class="msg-body" :class="{ 'msg-self-body': isSelf, 'msg-other-body': !isSelf }">
      <div v-if="showName" class="msg-name t-label-md">{{ senderName }}</div>
      <div class="msg-bubble" :class="{ 'bubble-self': isSelf, 'bubble-other': !isSelf }">
        <!-- Quoted message -->
        <div v-if="quote" class="msg-quote t-body-md">
          <div class="quote-bar" />
          <span class="quote-text">{{ quote }}</span>
        </div>
        <!-- Text content -->
        <div class="msg-content t-body-md">{{ content }}</div>
        <!-- Image -->
        <img v-if="imageUrl" :src="imageUrl" class="msg-image" @click="$emit('imageClick')" />
        <!-- File attachment -->
        <div v-if="file" class="msg-file">
          <el-icon><Document /></el-icon>
          <span class="t-body-md">{{ file.name }}</span>
          <span class="t-label-md file-size">{{ formatSize(file.size) }}</span>
        </div>
      </div>
      <div class="msg-meta t-label-md">
        <span>{{ time }}</span>
        <span v-if="isSelf && isRead" class="msg-read">已读</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'

withDefaults(defineProps<{
  isSelf?: boolean
  senderName?: string
  avatar?: string
  content: string
  time?: string
  isRead?: boolean
  showAvatar?: boolean
  showName?: boolean
  quote?: string
  imageUrl?: string
  file?: { name: string; size: number } | null
}>(), {
  isSelf: false,
  showAvatar: true,
  showName: true,
})

defineEmits(['imageClick'])

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}
</script>

<style lang="scss" scoped>
.msg-group {
  display: flex;
  gap: 8px;
  padding: 2px 0;
  max-width: 75%;
}

.msg-self {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-other {
  align-self: flex-start;
}

.msg-avatar { flex-shrink: 0; }

.msg-body {
  display: flex;
  flex-direction: column;
}

.msg-name {
  color: var(--c-text-muted);
  padding: 0 4px 2px;
}

// Bubbles per Kinetic Sync:
// Other: white background, slate border, left aligned
// Self: primary blue background, white text, right aligned
.msg-bubble {
  padding: 8px 12px;
  max-width: 100%;
  word-break: break-word;
}

.bubble-other {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 2px var(--radius-lg) var(--radius-lg) var(--radius-lg);
  color: var(--c-text);
}

.bubble-self {
  background: var(--c-brand);
  color: #fff;
  border-radius: var(--radius-lg) 2px var(--radius-lg) var(--radius-lg);
}

.msg-content { white-space: pre-wrap; }

.msg-quote {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
  padding: 6px 8px;
  background: rgba(0,0,0,.06);
  border-radius: var(--radius);
  font-size: 13px;
  color: var(--c-text-secondary);
}

.quote-bar {
  width: 2px;
  background: var(--c-text-muted);
  border-radius: 1px;
  flex-shrink: 0;
}

.msg-image {
  max-width: 240px;
  border-radius: var(--radius);
  cursor: pointer;
  margin-top: 4px;
}

.msg-file {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  background: rgba(0,0,0,.06);
  border-radius: var(--radius);
  margin-top: 4px;
}

.file-size { color: var(--c-text-muted); }

.msg-meta {
  display: flex;
  gap: 6px;
  margin-top: 4px;
  padding: 0 4px;
  color: var(--c-text-ghost);
}

.msg-self-body .msg-meta {
  flex-direction: row-reverse;
}

.msg-read { color: var(--c-brand); }
</style>
