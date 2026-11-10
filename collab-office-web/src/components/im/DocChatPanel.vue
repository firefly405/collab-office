<template>
  <div class="doc-chat-panel" :class="{ collapsed }">
    <!-- Panel header (always visible) -->
    <div class="panel-header" @click="collapsed = !collapsed">
      <div class="header-left">
        <span class="material-symbols-outlined header-icon">chat</span>
        <span class="header-title" v-if="!collapsed">文档讨论</span>
      </div>
      <div class="header-right" v-if="!collapsed">
        <span v-if="unreadCount" class="unread-badge">{{ unreadCount }}</span>
        <span class="material-symbols-outlined toggle-icon">
          {{ collapsed ? 'chevron_left' : 'chevron_right' }}
        </span>
      </div>
      <span v-else class="material-symbols-outlined toggle-icon collapsed-icon">chevron_left</span>
    </div>

    <!-- Panel body (hidden when collapsed) -->
    <template v-if="!collapsed">
      <!-- Online users -->
      <div class="online-row" v-if="onlineUsers.length">
        <div class="online-avatars">
          <div
            v-for="u in onlineUsers"
            :key="u.userId"
            class="online-avatar"
            :style="{ backgroundColor: u.color }"
            :title="u.name"
          >
            {{ u.name[0] }}
          </div>
        </div>
        <span class="t-label-md online-count">
          <span class="status-dot online" />
          {{ onlineUsers.length }} 人在线
        </span>
      </div>

      <!-- Message list -->
      <div class="msg-list" ref="msgList">
        <div v-if="messages.length === 0" class="empty-chat">
          <span class="material-symbols-outlined empty-icon">chat_bubble_outline</span>
          <span class="t-label-md" style="color: var(--c-text-muted)">暂无讨论</span>
          <span class="t-label-md" style="color: var(--c-text-ghost)">开始第一条消息吧</span>
        </div>
        <div
          v-for="msg in messages"
          :key="msg.msgId"
          class="msg-item"
          :class="{ own: msg.isMine }"
        >
          <template v-if="msg.isMine">
            <div class="msg-bubble own-bubble">{{ msg.content }}</div>
          </template>
          <template v-else>
            <div class="msg-avatar" :style="{ backgroundColor: msg.avatarColor || '#6B7280' }">
              {{ msg.senderName[0] }}
            </div>
            <div class="msg-content">
              <span class="sender-name t-label-md">{{ msg.senderName }}</span>
              <div class="msg-bubble other-bubble">{{ msg.content }}</div>
            </div>
          </template>
        </div>
      </div>

      <!-- Input row -->
      <div class="input-row">
        <div class="input-wrapper">
          <input
            v-model="newMsg"
            placeholder="输入消息..."
            class="msg-text-input"
            @keyup.enter="sendMessage"
          />
          <button
            class="send-btn"
            :disabled="!newMsg.trim()"
            @click="sendMessage"
            title="发送"
          >
            <span class="material-symbols-outlined">send</span>
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onUnmounted } from 'vue'

const props = defineProps<{ docId: string }>()

const collapsed = ref(false)
const newMsg = ref('')
const unreadCount = ref(0)
const msgList = ref<HTMLElement>()
const currentUserId = ref('')
const currentUserName = ref('我')

const messages = ref<any[]>([])
const onlineUsers = ref<any[]>([])
let simTimer: ReturnType<typeof setInterval> | null = null

watch(() => props.docId, (id) => {
  if (id) loadMessages()
})

function loadMessages() {
  try {
    const saved = localStorage.getItem(`doc_chat_${props.docId}`)
    if (saved) messages.value = JSON.parse(saved)
  } catch { messages.value = [] }

  try {
    const token = localStorage.getItem('access_token')
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]))
      currentUserId.value = payload.userId || payload.sub || ''
    }
  } catch { /* ignore */ }

  // Simulate online users for demo
  onlineUsers.value = [{
    userId: '1',
    name: currentUserName.value,
    color: '#3370FF',
  }]
}

function sendMessage() {
  if (!newMsg.value.trim()) return
  const msg = {
    msgId: Date.now().toString(),
    senderId: currentUserId.value,
    senderName: currentUserName.value,
    content: newMsg.value.trim(),
    sendTime: new Date().toISOString(),
    isMine: true,
  }
  messages.value.push(msg)
  newMsg.value = ''
  saveMessages()
  scrollBottom()
}

function saveMessages() {
  try {
    localStorage.setItem(`doc_chat_${props.docId}`, JSON.stringify(messages.value.slice(-200)))
  } catch { /* ignore */ }
}

function scrollBottom() {
  nextTick(() => {
    if (msgList.value) msgList.value.scrollTop = msgList.value.scrollHeight
  })
}

onUnmounted(() => {
  if (simTimer) clearInterval(simTimer)
})
</script>

<style lang="scss" scoped>
.doc-chat-panel {
  display: flex; flex-direction: column;
  background: var(--c-surface);
  border-left: 1px solid var(--c-border);
  width: 280px; height: 100%;
  transition: width .2s var(--ease-out);
  &.collapsed { width: 48px; }
}

// -- Header --
.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--c-border);
  cursor: pointer; user-select: none; flex-shrink: 0;
  transition: background .15s var(--ease-out);
  &:hover { background: var(--c-surface-hover); }
}
.header-left { display: flex; align-items: center; gap: 8px; }
.header-icon { font-size: 20px; color: var(--c-brand); }
.header-title { font-size: 14px; font-weight: 600; color: var(--c-text); }
.header-right { display: flex; align-items: center; gap: 8px; }

.unread-badge {
  background: var(--c-danger); color: #fff;
  font-size: 10px; font-weight: 600; padding: 1px 6px; border-radius: var(--radius-full); min-width: 18px; text-align: center;
}

.toggle-icon { font-size: 18px; color: var(--c-text-muted); transition: transform .2s; }
.collapsed-icon { font-size: 18px; color: var(--c-text-muted); }

// -- Online row --
.online-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid var(--c-border-light);
  flex-shrink: 0;
}
.online-avatars { display: flex; gap: -6px; }
.online-avatar {
  width: 24px; height: 24px;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 11px; font-weight: 600;
  border: 2px solid var(--c-surface); margin-right: -8px;
  &:last-child { margin-right: 0; }
}
.online-count { display: flex; align-items: center; gap: 6px; color: var(--c-text-muted); }

// -- Message list --
.msg-list {
  flex: 1; overflow-y: auto; padding: 12px;
  display: flex; flex-direction: column; gap: 10px;
}

.empty-chat {
  display: flex; flex-direction: column; align-items: center;
  padding: 32px 0; gap: 4px;
}
.empty-icon { font-size: 32px; color: var(--c-text-ghost); margin-bottom: 2px; }

.msg-item {
  display: flex; gap: 8px;
  &.own { justify-content: flex-end; }
}

.msg-avatar {
  width: 24px; height: 24px;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 11px; font-weight: 600; flex-shrink: 0;
}

.msg-content { display: flex; flex-direction: column; gap: 2px; }
.sender-name { color: var(--c-text-muted); padding-left: 2px; }

.msg-bubble {
  padding: 8px 12px;
  border-radius: var(--radius-lg);
  font-size: 13px; line-height: 18px;
  max-width: 190px; word-break: break-word;
}

.own-bubble {
  background: var(--c-brand); color: #fff;
  border-bottom-right-radius: var(--radius-sm);
}

.other-bubble {
  background: var(--c-surface-dim); color: var(--c-text);
  border-bottom-left-radius: var(--radius-sm);
}

// -- Input row --
.input-row {
  padding: 10px 12px;
  border-top: 1px solid var(--c-border);
  flex-shrink: 0;
}

.input-wrapper {
  display: flex; align-items: center; gap: 8px;
  background: var(--c-bg); border: 1px solid var(--c-border);
  border-radius: var(--radius-lg); padding: 0 4px 0 12px;
  transition: border-color .15s var(--ease-out);
  &:focus-within { border-color: var(--c-brand); }
}

.msg-text-input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 13px; padding: 8px 0; color: var(--c-text); font-family: var(--font-sans);
  &::placeholder { color: var(--c-text-muted); }
}

.send-btn {
  width: 32px; height: 32px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border: none; border-radius: var(--radius);
  background: var(--c-brand); color: #fff; cursor: pointer;
  transition: all .15s var(--ease-out);
  &:hover:not(:disabled) { background: var(--c-brand-hover); }
  &:disabled { opacity: .4; cursor: not-allowed; }
  .material-symbols-outlined { font-size: 16px; }
}
</style>
