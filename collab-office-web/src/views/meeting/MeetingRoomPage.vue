<template>
  <div class="meeting-room-root">
    <!-- Top Header -->
    <header class="room-header">
      <div class="header-left">
        <h1 class="brand-text">Nexus Office</h1>
        <div class="header-divider"></div>
        <div class="meeting-meta">
          <span class="meeting-meta-title">{{ meetingTitle }}</span>
          <span class="meeting-meta-sub">{{ elapsed }} · {{ participants.length }} 位参与者</span>
        </div>
      </div>
      <div class="header-right">
        <div class="avatar-stack">
          <div class="stack-av a1"></div>
          <div class="stack-av a2"></div>
          <div class="stack-av count">+{{ Math.max(0, participants.length - 2) }}</div>
        </div>
        <button class="btn-invite">
          <span class="material-symbols-outlined">group_add</span>
          邀请
        </button>
        <button class="icon-btn" title="应用">
          <span class="material-symbols-outlined">apps</span>
        </button>
      </div>
    </header>

    <!-- Main Content -->
    <main class="room-main">
      <!-- Video Grid -->
      <section class="video-canvas" :class="layout">
        <div
          v-for="(p, idx) in participants"
          :key="p.id"
          class="video-tile"
          :class="{
            speaking: p.isSpeaking,
            spotlight: layout === 'spotlight' && idx === 0,
            'screen-share': p.isScreenSharing,
            'no-video': !p.isVideoOn
          }"
        >
          <!-- Local camera feed -->
          <div v-if="p.id === 'local' && videoReady" class="tile-media">
            <video
              ref="localVideoRef"
              autoplay
              muted
              playsinline
              class="tile-video-el"
            />
          </div>
          <!-- Remote participant with stream -->
          <div v-else-if="p.stream" class="tile-media">
            <video
              :ref="(el) => { if (el) remoteVideoRefs[p.id] = el as HTMLVideoElement }"
              autoplay
              playsinline
              class="tile-video-el"
            />
          </div>
          <!-- Placeholder for no camera -->
          <div v-else class="tile-media tile-placeholder">
            <div class="placeholder-avatar" :style="{ background: p.color || '#c3c6ce' }">
              <span class="material-symbols-outlined">person</span>
            </div>
          </div>
          <!-- Overlay label -->
          <div class="tile-label">
            <span class="material-symbols-outlined mic-icon" :class="{ muted: p.isMuted }">
              {{ p.isMuted ? 'mic_off' : 'mic' }}
            </span>
            {{ p.name }}
            <span v-if="p.isHost" class="host-tag">主持人</span>
          </div>
          <!-- Speaking indicator -->
          <div v-if="p.isSpeaking" class="speaking-indicator">正在发言</div>
        </div>
      </section>

      <!-- Right Sidebar -->
      <aside class="room-sidebar">
        <!-- Sidebar Tabs -->
        <div class="sidebar-tabs">
          <button class="sidebar-tab active">
            <span class="material-symbols-outlined">forum</span>
            聊天
          </button>
          <button class="sidebar-tab">
            <span class="material-symbols-outlined">groups</span>
            成员
          </button>
        </div>

        <!-- Chat Content -->
        <div class="chat-content" v-if="showChat">
          <div class="chat-system-msg">
            <span>会议开始于 14:00</span>
          </div>
          <div v-for="m in chatMessages" :key="m.id" class="chat-bubble" :class="{ self: m.user === '我' }">
            <div class="bubble-header">
              <span class="bubble-user">{{ m.user }}</span>
              <span class="bubble-time">14:22</span>
            </div>
            <div class="bubble-body">{{ m.text }}</div>
          </div>
          <!-- Typing indicator -->
          <div class="typing-indicator">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-text">陈经理 正在输入...</span>
          </div>
        </div>

        <!-- Chat Input -->
        <div class="chat-input-area">
          <div class="chat-input-wrap">
            <textarea
              v-model="chatInput"
              placeholder="发送消息..."
              rows="3"
              @keyup.enter="sendChat"
            ></textarea>
            <div class="input-actions-left">
              <button class="input-action-btn">
                <span class="material-symbols-outlined">sentiment_satisfied</span>
              </button>
              <button class="input-action-btn">
                <span class="material-symbols-outlined">attach_file</span>
              </button>
            </div>
            <button class="send-btn" @click="sendChat">
              <span class="material-symbols-outlined">send</span>
            </button>
          </div>
        </div>
      </aside>
    </main>

    <!-- Control Bar -->
    <footer class="control-bar">
      <div class="control-info">
        <span class="control-info-title">会议详情</span>
        <span class="control-info-url">nexus-office.com/v/{{ roomId }}</span>
      </div>

      <div class="control-center">
        <button class="control-btn" :class="{ active: isMuted }" @click="toggleMute">
          <div class="control-icon-circle">
            <span class="material-symbols-outlined">{{ isMuted ? 'mic_off' : 'mic' }}</span>
          </div>
          <span class="control-label">静音</span>
        </button>
        <button class="control-btn" :class="{ active: !isVideoOn }" @click="toggleVideo">
          <div class="control-icon-circle">
            <span class="material-symbols-outlined">{{ isVideoOn ? 'videocam' : 'videocam_off' }}</span>
          </div>
          <span class="control-label">摄像头</span>
        </button>
        <button class="control-btn" :class="{ active: isScreenSharing }" @click="isScreenSharing = !isScreenSharing">
          <div class="control-icon-circle">
            <span class="material-symbols-outlined">present_to_all</span>
          </div>
          <span class="control-label">屏幕共享</span>
        </button>
        <button class="control-btn" :class="{ active: isHandRaised }" @click="isHandRaised = !isHandRaised">
          <div class="control-icon-circle">
            <span class="material-symbols-outlined">front_hand</span>
          </div>
          <span class="control-label">举手</span>
        </button>
        <button class="control-btn">
          <div class="control-icon-circle">
            <span class="material-symbols-outlined">more_vert</span>
          </div>
          <span class="control-label">更多</span>
        </button>
        <div class="control-divider"></div>
        <button class="end-call-btn" @click="leaveMeeting">
          <span class="material-symbols-outlined">call_end</span>
          结束会议
        </button>
      </div>

      <div class="control-utils">
        <button class="icon-btn util-btn">
          <span class="material-symbols-outlined">notifications</span>
        </button>
        <button class="icon-btn util-btn">
          <span class="material-symbols-outlined">settings</span>
        </button>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { wsClient } from '@/utils/wsClient'
import { getToken } from '@/utils/token'

const router = useRouter()
const route = useRoute()
const roomId = route.params.roomId as string || ''
const meetingTitle = ref('Q3 战略同步会议')
const isMuted = ref(false)
const isVideoOn = ref(true)
const isScreenSharing = ref(false)
const isHandRaised = ref(false)
const layout = ref<'grid' | 'spotlight'>('grid')
const showChat = ref(true)
const chatInput = ref('')
const elapsed = ref('00:00:00')

let timer: number | undefined
let localStream: MediaStream | null = null
const videoReady = ref(false)
const localVideoRef = ref<HTMLVideoElement>()

const participants = ref<any[]>([])
const chatMessages = ref<{ id: number; user: string; text: string }[]>([
  { id: 1, user: '张明', text: '大家先看一下这个季度的数据' },
])

// === WebRTC mesh 拓扑：每对参与者一个 RTCPeerConnection ===
// 适合 3-5 人小型会议；超过 5 人需 SFU（留待后续）
const myUserId = parseUserId(getToken())
const peerConnections = new Map<string, RTCPeerConnection>()
const remoteStreams = new Map<string, MediaStream>()
const remoteVideoRefs = ref<Record<string, HTMLVideoElement | undefined>>({})
const unsubSignal = ref<(() => void) | null>(null)
const unsubPresence = ref<(() => void) | null>(null)

const RTC_CONFIG: RTCConfiguration = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
  ],
}

function parseUserId(token: string | null): string {
  if (!token) return 'me'
  try {
    const parts = token.split('.')
    if (parts.length === 3) {
      const payload = JSON.parse(atob(parts[1]))
      return String(payload.userId || 'me')
    }
  } catch { /* ignore */ }
  return 'me'
}

// === WebRTC: acquire local media ===
async function startLocalStream() {
  try {
    localStream = await navigator.mediaDevices.getUserMedia({
      video: isVideoOn.value,
      audio: !isMuted.value,
    })
    if (localVideoRef.value) {
      localVideoRef.value.srcObject = localStream
      videoReady.value = true
    }
    participants.value.unshift({
      id: 'local', name: '我', color: '#3370FF',
      isHost: true, isSpeaking: false, isMuted: isMuted.value,
      isVideoOn: isVideoOn.value, isScreenSharing: false,
      stream: localStream,
    })
  } catch {
    ElMessage.warning('无法访问摄像头/麦克风')
    participants.value.unshift({
      id: 'local', name: '我', color: '#3370FF',
      isHost: true, isSpeaking: false, isMuted: true,
      isVideoOn: false, isScreenSharing: false,
    })
  }
}

function toggleMute() {
  isMuted.value = !isMuted.value
  if (localStream) {
    localStream.getAudioTracks().forEach(t => t.enabled = !isMuted.value)
  }
  const self = participants.value.find(p => p.id === 'local')
  if (self) self.isMuted = isMuted.value
}

function toggleVideo() {
  isVideoOn.value = !isVideoOn.value
  if (localStream) {
    localStream.getVideoTracks().forEach(t => t.enabled = isVideoOn.value)
  }
  const self = participants.value.find(p => p.id === 'local')
  if (self) self.isVideoOn = isVideoOn.value
}

function stopLocalStream() {
  if (localStream) {
    localStream.getTracks().forEach(t => t.stop())
    localStream = null
  }
}

function sendChat() {
  if (!chatInput.value.trim()) return
  chatMessages.value.push({ id: Date.now(), user: '我', text: chatInput.value })
  chatInput.value = ''
}

function leaveMeeting() {
  if (roomId) {
    request.post(`/meeting/${roomId}/leave`).catch(() => {})
    // 通知房间内其他成员自己离开，便于他们关闭对应的 peer connection
    wsClient.send('MEETING_LEAVE', { userId: myUserId }, roomId)
  }
  ElMessage.info('已离开会议')
  router.back()
}

// === WebRTC 信令 ===

function ensurePeerConnection(remoteUserId: string): RTCPeerConnection {
  let pc = peerConnections.get(remoteUserId)
  if (pc) return pc

  pc = new RTCPeerConnection(RTC_CONFIG)
  peerConnections.set(remoteUserId, pc)

  // 添加本地 tracks
  if (localStream) {
    localStream.getTracks().forEach(track => {
      pc!.addTrack(track, localStream!)
    })
  }

  // 接收远端 track
  const remoteStream = new MediaStream()
  remoteStreams.set(remoteUserId, remoteStream)
  pc.ontrack = (event) => {
    event.streams[0]?.getTracks().forEach(t => remoteStream.addTrack(t))
    // 挂到 UI
    const existing = participants.value.find(p => p.id === remoteUserId)
    if (existing) {
      existing.stream = remoteStream
      existing.isVideoOn = remoteStream.getVideoTracks().length > 0
    } else {
      participants.value.push({
        id: remoteUserId,
        name: `用户 ${remoteUserId.slice(-4)}`,
        color: '#8B5CF6',
        isHost: false,
        isSpeaking: false,
        isMuted: false,
        isVideoOn: remoteStream.getVideoTracks().length > 0,
        isScreenSharing: false,
        stream: remoteStream,
      })
    }
    // 触发 video 元素绑定
    nextTick(() => {
      const el = remoteVideoRefs.value[remoteUserId]
      if (el) el.srcObject = remoteStream
    })
  }

  // ICE candidate 通过 WS 转发
  pc.onicecandidate = (event) => {
    if (event.candidate) {
      wsClient.send('MEETING_SIGNAL', {
        targetUserId: remoteUserId,
        fromUserId: myUserId,
        signalType: 'ice',
        data: event.candidate.toJSON(),
      }, roomId)
    }
  }

  pc.onconnectionstatechange = () => {
    if (pc!.connectionState === 'failed' || pc!.connectionState === 'closed') {
      cleanupPeer(remoteUserId)
    }
  }

  return pc
}

function cleanupPeer(remoteUserId: string) {
  const pc = peerConnections.get(remoteUserId)
  if (pc) {
    pc.close()
    peerConnections.delete(remoteUserId)
  }
  remoteStreams.delete(remoteUserId)
  participants.value = participants.value.filter(p => p.id !== remoteUserId)
}

/**
 * 作为 offerer 主动发起呼叫（用于已有成员发现新成员加入时）
 */
async function makeOffer(remoteUserId: string) {
  const pc = ensurePeerConnection(remoteUserId)
  try {
    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)
    wsClient.send('MEETING_SIGNAL', {
      targetUserId: remoteUserId,
      fromUserId: myUserId,
      signalType: 'offer',
      data: { type: offer.type, sdp: offer.sdp },
    }, roomId)
  } catch (e) {
    console.error('makeOffer failed', e)
  }
}

async function handleSignal(payload: any) {
  if (!payload || payload.roomId !== roomId) return
  // 只处理发给自己的信号
  if (payload.targetUserId && payload.targetUserId !== myUserId) return

  const fromUserId = payload.fromUserId
  if (!fromUserId || fromUserId === myUserId) return

  const pc = ensurePeerConnection(fromUserId)

  try {
    if (payload.signalType === 'offer') {
      await pc.setRemoteDescription(new RTCSessionDescription(payload.data))
      const answer = await pc.createAnswer()
      await pc.setLocalDescription(answer)
      wsClient.send('MEETING_SIGNAL', {
        targetUserId: fromUserId,
        fromUserId: myUserId,
        signalType: 'answer',
        data: { type: answer.type, sdp: answer.sdp },
      }, roomId)
    } else if (payload.signalType === 'answer') {
      await pc.setRemoteDescription(new RTCSessionDescription(payload.data))
    } else if (payload.signalType === 'ice') {
      if (pc.remoteDescription) {
        await pc.addIceCandidate(new RTCIceCandidate(payload.data))
      } else {
        // 缓存到队列，等 remoteDescription 设置后再处理（简化：直接丢弃，ICE 重试会补）
        console.warn('ICE candidate arrived before remoteDescription, dropping', fromUserId)
      }
    }
  } catch (e) {
    console.error('handleSignal failed', e)
  }
}

/**
 * 处理房间内其他成员的加入/离开通知。
 * 收到其他成员加入信号后，作为 offerer 主动发起呼叫。
 */
function handlePresence(payload: any) {
  if (!payload || payload.roomId !== roomId) return
  const otherUserId = payload.userId
  if (!otherUserId || otherUserId === myUserId) return

  if (payload.kind === 'join') {
    // 为避免双方同时发起 offer 导致冲突，约定 userId 较小的一方作为 offerer
    if (myUserId < otherUserId) {
      makeOffer(otherUserId)
    }
  } else if (payload.kind === 'leave') {
    cleanupPeer(otherUserId)
  }
}

onMounted(async () => {
  if (roomId) {
    request.get(`/meeting/${roomId}`).then((res: any) => {
      if (res.code === 200 && res.data) {
        meetingTitle.value = res.data.title || meetingTitle.value
      }
    }).catch(() => {})
    request.post(`/meeting/${roomId}/join`).catch(() => {})
  }

  // 建立 WebSocket 连接
  const token = getToken()
  if (token) wsClient.connect(token)

  // 订阅信令与存在感消息
  // 服务端 handleMeetingMedia 广播时保留原 type（MEETING_SIGNAL / MEETING_JOIN / MEETING_LEAVE）
  unsubSignal.value = wsClient.on('MEETING_SIGNAL', handleSignal)
  unsubPresence.value = wsClient.on('MEETING_JOIN', (data: any) => {
    handlePresence({ ...data, kind: 'join' })
  })
  // 退订用 MEETING_LEAVE 广播
  const unsubLeave = wsClient.on('MEETING_LEAVE', (data: any) => {
    handlePresence({ ...data, kind: 'leave' })
  })
  unsubPresence.value = () => {
    unsubPresence.value?.()
    unsubLeave()
  }

  await nextTick()
  await startLocalStream()

  // localStream 就绪后再通知房间内其他成员自己加入（避免 makeOffer 时无本地 tracks）
  wsClient.send('MEETING_JOIN', { userId: myUserId }, roomId)

  let sec = 0
  timer = window.setInterval(() => {
    sec++
    const h = Math.floor(sec / 3600).toString().padStart(2, '0')
    const m = Math.floor(sec / 60 % 60).toString().padStart(2, '0')
    const s = (sec % 60).toString().padStart(2, '0')
    elapsed.value = `${h}:${m}:${s}`
  }, 1000)
})

onUnmounted(() => {
  stopLocalStream()
  if (timer) window.clearInterval(timer)
  // 关闭所有 peer connection
  for (const uid of Array.from(peerConnections.keys())) {
    cleanupPeer(uid)
  }
  // 通知离开
  if (roomId) {
    wsClient.send('MEETING_LEAVE', { userId: myUserId }, roomId)
  }
  unsubSignal.value?.()
  unsubPresence.value?.()
})
</script>

<style lang="scss" scoped>
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  vertical-align: middle;
}

.meeting-room-root {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--c-bg);
  color: var(--c-text);
  font-family: var(--font-sans);
}

// ---- Header ----
.room-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 1rem;
  background: var(--c-surface);
  border-bottom: 1px solid var(--c-border);
  flex-shrink: 0;
  z-index: 30;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.brand-text { font-size: 20px; font-weight: 700; color: var(--c-brand); }
.header-divider { width: 1px; height: 24px; background: var(--c-border); }
.meeting-meta { display: flex; flex-direction: column; }
.meeting-meta-title { font-size: 12px; font-weight: 600; }
.meeting-meta-sub { font-size: 10px; color: var(--c-text-muted); }

.header-right { display: flex; align-items: center; gap: 12px; }
.avatar-stack { display: flex; margin-right: 16px; }
.stack-av {
  width: 32px; height: 32px;
  border-radius: 50%;
  border: 2px solid var(--c-surface);
  margin-left: -8px;
  &:first-child { margin-left: 0; }
  &.a1 { background: #b4c5ff; }
  &.a2 { background: #c3c6ce; }
  &.count {
    background: #e7e8e9;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: 600;
  }
}
.btn-invite {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 16px;
  background: var(--c-brand);
  color: #fff;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  &:hover { opacity: .9; }
  span { font-size: 18px; }
}
.icon-btn {
  padding: 8px;
  background: none;
  border: none;
  border-radius: 50%;
  color: var(--c-text-muted);
  cursor: pointer;
  &:hover { color: var(--c-brand); }
}

// ---- Main ----
.room-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

// ---- Video Canvas ----
.video-canvas {
  flex: 1;
  padding: 1rem;
  background: #f3f4f5;
  display: grid;
  gap: 1rem;
  overflow-y: auto;
  &.grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
  &.spotlight {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr auto;
  }
}
.video-tile {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: #000;
  aspect-ratio: 16 / 9;
  box-shadow: var(--shadow-sm);
  transition: transform .2s ease;
  &:hover { transform: scale(1.01); }
  &.speaking { outline: 3px solid #22c55e; outline-offset: -3px; }
  &.spotlight { grid-column: 1 / -1; grid-row: span 2; }
  &.screen-share { grid-column: 1 / -1; min-height: 400px; }
}
.tile-media { width: 100%; height: 100%; }
.tile-video-el { width: 100%; height: 100%; object-fit: cover; }
.tile-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e1e3e4;
}
.placeholder-avatar {
  width: 64px; height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  span { font-size: 32px; color: #fff; }
}
.tile-label {
  position: absolute;
  bottom: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: rgba(0,0,0,.4);
  backdrop-filter: blur(8px);
  border-radius: 9999px;
  color: #fff;
  font-size: 12px;
  .mic-icon { font-size: 16px; }
  .mic-icon.muted { color: #ef4444; }
  .host-tag { font-size: 10px; background: #22c55e; padding: 1px 6px; border-radius: 4px; margin-left: 4px; }
}
.speaking-indicator {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 4px 8px;
  background: rgba(34,197,94,.2);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(34,197,94,.4);
  border-radius: var(--radius);
  color: #22c55e;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: .05em;
  text-transform: uppercase;
}

// ---- Sidebar ----
.room-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--c-surface);
  border-left: 1px solid var(--c-border);
}
.sidebar-tabs {
  display: flex;
  border-bottom: 1px solid var(--c-border);
}
.sidebar-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  background: none;
  border: none;
  font-size: 14px;
  color: var(--c-text-secondary);
  cursor: pointer;
  transition: background .15s;
  &:hover { background: #f3f4f5; }
  &.active { color: var(--c-brand); font-weight: 600; border-bottom: 2px solid var(--c-brand); }
  span { font-size: 20px; }
}
.chat-content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background: #f8f9fa;
}
.chat-system-msg {
  text-align: center;
  span {
    font-size: 10px;
    background: #e7e8e9;
    padding: 2px 8px;
    border-radius: 9999px;
    color: var(--c-text-secondary);
  }
}
.chat-bubble {
  display: flex;
  flex-direction: column;
  gap: 4px;
  &.self { align-items: flex-end; }
}
.bubble-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px;
  .chat-bubble.self & { flex-direction: row-reverse; }
}
.bubble-user { font-size: 12px; font-weight: 600; }
.bubble-time { font-size: 10px; color: var(--c-text-muted); }
.bubble-body {
  padding: 12px;
  border-radius: var(--radius-lg);
  font-size: 14px;
  .chat-bubble:not(.self) & { background: #fff; border: 1px solid var(--c-border); border-top-left-radius: 0; }
  .chat-bubble.self & { background: var(--c-brand); color: #fff; border-top-right-radius: 0; }
}
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: auto;
  padding-bottom: 8px;
}
.typing-dot {
  width: 6px; height: 6px;
  background: rgba(91,94,101,.4);
  border-radius: 50%;
  animation: bounce 1.2s infinite;
  &:nth-child(2) { animation-delay: .2s; }
  &:nth-child(3) { animation-delay: .4s; }
}
.typing-text { font-size: 10px; color: var(--c-text-secondary); margin-left: 4px; }

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.chat-input-area {
  padding: 1rem;
  border-top: 1px solid var(--c-border);
  background: var(--c-surface);
}
.chat-input-wrap {
  position: relative;
  textarea {
    width: 100%;
    padding: 12px 12px 40px 12px;
    background: #f3f4f5;
    border: none;
    border-radius: 12px;
    font-size: 14px;
    resize: none;
    outline: none;
    height: 96px;
    &:focus { box-shadow: 0 0 0 2px var(--c-brand); }
  }
}
.input-actions-left {
  position: absolute;
  bottom: 8px;
  left: 8px;
  display: flex;
  gap: 8px;
}
.input-action-btn {
  padding: 4px;
  background: none;
  border: none;
  border-radius: 50%;
  color: var(--c-text-secondary);
  cursor: pointer;
  &:hover { background: #e7e8e9; }
  span { font-size: 20px; }
}
.send-btn {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 6px;
  background: var(--c-brand);
  color: #fff;
  border: none;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  &:hover { opacity: .9; }
  span { font-size: 20px; }
}

// ---- Control Bar ----
.control-bar {
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem;
  background: #fff;
  border-top: 1px solid var(--c-border);
  flex-shrink: 0;
  z-index: 40;
}
.control-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  @media (max-width: 768px) { display: none; }
}
.control-info-title { font-weight: 600; }
.control-info-url { font-size: 12px; color: var(--c-text-muted); }

.control-center { display: flex; align-items: center; gap: 16px; }
.control-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  cursor: pointer;
  .control-icon-circle {
    width: 48px; height: 48px;
    border-radius: 50%;
    background: #e1e3e4;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--c-text);
    transition: background .15s, color .15s;
    span { font-size: 24px; }
  }
  .control-label { font-size: 10px; font-weight: 600; color: var(--c-text-muted); }
  &:hover .control-icon-circle { background: #e7e8e9; }
  &.active .control-icon-circle { background: rgba(0,80,214,.1); color: var(--c-brand); }
}
.control-divider { width: 1px; height: 40px; background: var(--c-border); margin: 0 8px; }
.end-call-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: #ba1a1a;
  color: #fff;
  border: none;
  border-radius: 9999px;
  font-weight: 600;
  font-size: 14px;
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: filter .15s, transform .15s;
  &:hover { filter: brightness(1.1); }
  &:active { transform: scale(.95); }
  span { font-size: 24px; }
}
.control-utils {
  display: flex;
  align-items: center;
  gap: 12px;
  @media (max-width: 768px) { display: none; }
  .util-btn {
    padding: 8px;
    color: var(--c-text-secondary);
    &:hover { background: #e7e8e9; }
  }
}
</style>
