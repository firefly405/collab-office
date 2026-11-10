<template>
  <div class="quill-editor">
    <Toolbar :toolbar-id="toolbarId" />
    <ImageUpload
      ref="imageUploadRef"
      class="editor-upload"
      @uploaded="insertUploadedImage"
      @uploading-change="imageUploading = $event"
    >
      <div ref="editorEl" class="quill-host"></div>
      <div v-if="selectedImage" class="image-resize-panel" :style="imageResizePanelStyle">
        <button type="button" @click="setSelectedImageWidth('25%')">25%</button>
        <button type="button" @click="setSelectedImageWidth('50%')">50%</button>
        <button type="button" @click="setSelectedImageWidth('75%')">75%</button>
        <button type="button" @click="setSelectedImageWidth('100%')">100%</button>
        <button type="button" @click="setSelectedImageWidth(null)">原始</button>
      </div>
    </ImageUpload>
    <div v-if="!ready" class="sync-mask">
      {{ syncHint }}
    </div>
    <div v-if="imageUploading" class="image-upload-status">
      正在插入图片...
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import Quill from 'quill'
import QuillCursors from 'quill-cursors'
import 'quill/dist/quill.snow.css'
import * as Y from 'yjs'
import { Awareness, applyAwarenessUpdate, encodeAwarenessUpdate } from 'y-protocols/awareness'
import { QuillBinding } from 'y-quill'
import Toolbar from '@/components/editor/Toolbar.vue'
import ImageUpload from '@/components/editor/ImageUpload.vue'
import { getToken } from '@/utils/token'
import {ElMessage} from 'element-plus'

Quill.register('modules/cursors', QuillCursors, true)

const BlockEmbed = Quill.import('blots/block/embed') as any
const BaseImageBlot = Quill.import('formats/image') as any

class DividerBlot extends BlockEmbed {
  static blotName = 'divider'
  static tagName = 'hr'
}

class ResizableImageBlot extends BaseImageBlot {
  static blotName = 'image'

  static create(value: string | { url: string; alt?: string; title?: string; width?: string }) {
    const url = typeof value === 'string' ? value : value.url
    const node = super.create(url) as HTMLImageElement
    if (typeof value !== 'string') {
      if (value.alt) node.setAttribute('alt', value.alt)
      if (value.title) node.setAttribute('title', value.title)
      if (value.width) setImageNodeWidth(node, value.width)
    }
    node.setAttribute('draggable', 'false')
    return node
  }

  static formats(node: HTMLImageElement) {
    const formats = super.formats(node) || {}
    const width = node.style.width || node.getAttribute('width')
    if (width) formats.width = width
    return formats
  }

  format(name: string, value: string | null) {
    if (name === 'width') {
      if (value) {
        setImageNodeWidth(this.domNode as HTMLImageElement, value)
      } else {
        ;(this.domNode as HTMLImageElement).style.removeProperty('width')
        ;(this.domNode as HTMLImageElement).removeAttribute('width')
      }
      return
    }
    super.format(name, value)
  }
}

Quill.register(DividerBlot, true)
Quill.register(ResizableImageBlot, true)

type CollabState = 'offline' | 'connecting' | 'online'

type OnlineUser = {
  id: string
  name: string
  color: string
  lastSeen: number
}

type StatusPayload = {
  state: CollabState
  text: string
}

type CollabMessage = {
  type: string
  roomId?: string
  userId?: string
  seq?: number
  clientTimestamp?: number
  data?: Record<string, any>
}

type UploadedImage = {
  url: string
  file: File
  fileId?: string
  fileName: string
  fileSize: number
}

const props = defineProps<{
  docId: string
  initialContent?: string
  readOnly?: boolean
  standalone?: boolean
}>()

const emit = defineEmits<{
  (e: 'save', content: string): void
  (e: 'word-count', count: number): void
  (e: 'status-change', payload: StatusPayload): void
  (e: 'collaborators-change', users: OnlineUser[]): void
  (e: 'headings-change', headings: {index:number;level:number;text:string}[]):void
  (e: 'dirty'):void
}>()

const editorEl = ref<HTMLDivElement>()
const imageUploadRef = ref<InstanceType<typeof ImageUpload>>()
const ready = ref(false)
const imageUploading = ref(false)
const selectedImage = ref<HTMLImageElement | null>(null)
const imageResizePanelStyle = ref<Record<string, string>>({})
const statusState = ref<CollabState>('offline')
const statusText = ref('协同离线')
const toolbarId = `quill-toolbar-${Math.random().toString(36).slice(2)}`
const syncHint = computed(() => {
  if (statusState.value === 'offline') return '正在加载编辑器...'
  if (statusState.value === 'connecting') return '正在同步协同状态...'
  return '正在准备编辑器...'
})

let quill: Quill | null = null
let lastSelection:{index:number;length:number}|null=null
let binding: QuillBinding | null = null
let ydoc: Y.Doc | null = null
let ytext: Y.Text | null = null
let awareness: Awareness | null = null
let ws: WebSocket | null = null
let saveTimer: number | undefined
let syncFallbackTimer: number | undefined
let reconnectTimer: number | undefined
let awarenessRefreshTimer: number | undefined
let heartbeatTimer: number | undefined
let manualClose = false
let synced = false
let suppressPersistence = false
let messageSeq = 0

const localUser = getLocalUser()

onMounted(async () => {
  await nextTick()
  setupEditor()
  connectCollaboration()
})

onUnmounted(() => {
  destroyEditor()
})

watch(
  () => props.docId,
  () => {
    destroyEditor()
    nextTick(() => {
      setupEditor()
      connectCollaboration()
    })
  }
)

function setupEditor() {
  if (!editorEl.value) return
  ready.value = false
  synced = false

  ydoc = new Y.Doc()
  ytext = ydoc.getText('quill')
  awareness = new Awareness(ydoc)
  awareness.setLocalStateField('user', {
    name: localUser.name,
    color: localUser.color,
  })

  quill = new Quill(editorEl.value, {
    theme: 'snow',
    placeholder: '开始协同编辑...',
    modules: {
      cursors: {
        transformOnTextChange: true,
      },
      toolbar: {
        container: `#${toolbarId}`,
        handlers: {
          undo: undoChange,
          redo: redoChange,
          divider: insertDivider,
          image: openImageUpload,
        },
      },
      history: {
        userOnly: true,
      },
    },
  })

  quill.enable(false)
  quill.root.addEventListener('click', handleEditorClick)
  binding = new QuillBinding(ytext, quill, awareness)

  ydoc.on('update', handleYjsUpdate)
  quill.on('text-change', handleQuillTextChange)
  quill.on('selection-change',range=>{if(range&&range.length)lastSelection={index:range.index,length:range.length}})
  quill.on('text-change',publishHeadings)
  quill.on('editor-change',publishHeadings)
  ;(awareness as any).on('update', handleAwarenessUpdate)
  ;(awareness as any).on('change', emitAwarenessUsers)
}

function destroyEditor() {
  if (saveTimer && quill && synced && !props.readOnly) emit('save', quill.root.innerHTML)
  manualClose = true
  clearTimers()

  if (awareness && ws?.readyState === WebSocket.OPEN) {
    awareness.setLocalState(null)
    sendCollabMessage({
      type: 'DOC_LEAVE',
      roomId: props.docId,
    })
  }
  ws?.close()
  ws = null

  if (ydoc) {
    ydoc.off('update', handleYjsUpdate)
  }
  if (quill) {
    quill.root.removeEventListener('click', handleEditorClick)
    quill.off('text-change', handleQuillTextChange)
  }
  binding?.destroy()
  ;(awareness as any)?.destroy?.()
  ydoc?.destroy()

  binding = null
  awareness = null
  ytext = null
  ydoc = null
  quill = null
  selectedImage.value = null
  ready.value = false
  emit('collaborators-change', [])
}

function connectCollaboration() {
  if (props.standalone) { bootstrapFromInitialContent(); return }
  const token = getToken()
  if (!token) {
    updateStatus('offline', '协同离线')
    bootstrapFromInitialContent()
    return
  }
  if (ws?.readyState === WebSocket.OPEN || ws?.readyState === WebSocket.CONNECTING) return

  manualClose = false
  updateStatus('connecting', '协同连接中')
  ws = new WebSocket(buildWsUrl(token))

  ws.onopen = () => {
    updateStatus('connecting', '正在同步协同状态')
    sendCollabMessage({ type: 'CONNECT' })
    sendCollabMessage({ type: 'DOC_JOIN', roomId: props.docId })
    requestRemoteState()
    sendLocalAwareness()
    startMaintenanceLoops()
    syncFallbackTimer = window.setTimeout(() => {
      bootstrapFromInitialContent()
    }, 800)
  }

  ws.onmessage = (event) => {
    handleCollabMessage(event.data)
  }

  ws.onerror = () => {
    updateStatus('offline', '协同连接异常')
  }

  ws.onclose = () => {
    stopMaintenanceLoops()
    if (!synced) bootstrapFromInitialContent()
    updateStatus('offline', '协同离线')
    if (!manualClose) {
      reconnectTimer = window.setTimeout(connectCollaboration, 2000)
    }
  }
}

function bootstrapFromInitialContent() {
  if (synced || !quill) return
  if (syncFallbackTimer) window.clearTimeout(syncFallbackTimer)

  suppressPersistence = true
  const content = normalizeInitialContent(props.initialContent)
  if (content) {
    const delta = quill.clipboard.convert({ html: content })
    quill.setContents(delta, 'api')
  } else {
    quill.setText('', 'api')
  }
  suppressPersistence = false

  finishSync()
  broadcastFullState()
}

function finishSync() {
  if (syncFallbackTimer) window.clearTimeout(syncFallbackTimer)
  synced = true
  ready.value = true
  quill?.enable(!props.readOnly)
  updateWordCount()
  updateStatus(ws?.readyState === WebSocket.OPEN ? 'online' : 'offline', ws?.readyState === WebSocket.OPEN ? '协同已连接' : '协同离线')
}

function requestRemoteState() {
  sendCollabMessage({
    type: 'DOC_OPERATION',
    roomId: props.docId,
    data: {
      kind: 'yjs_sync_request',
      clientId: String(ydoc?.clientID || ''),
      userName: localUser.name,
      color: localUser.color,
    },
  })
}

function broadcastFullState() {
  if (!ydoc) return
  sendCollabMessage({
    type: 'DOC_OPERATION',
    roomId: props.docId,
    data: {
      kind: 'yjs_state',
      clientId: String(ydoc.clientID),
      update: bytesToBase64(Y.encodeStateAsUpdate(ydoc)),
      userName: localUser.name,
      color: localUser.color,
    },
  })
}

function handleYjsUpdate(update: Uint8Array, origin: unknown) {
  if (suppressPersistence) return
  if (origin !== 'remote') {
    sendCollabMessage({
      type: 'DOC_OPERATION',
      roomId: props.docId,
      data: {
        kind: 'yjs_update',
        clientId: String(ydoc?.clientID || ''),
        update: bytesToBase64(update),
        userName: localUser.name,
        color: localUser.color,
      },
    })
  }
  queueContentPersist()
}

function undoChange(this: { quill: Quill }) {
  const history = this.quill.getModule('history') as { undo?: () => void }
  history.undo?.()
}

function redoChange(this: { quill: Quill }) {
  const history = this.quill.getModule('history') as { redo?: () => void }
  history.redo?.()
}

function insertDivider(this: { quill: Quill }) {
  const editor = this.quill
  const range = editor.getSelection(true)
  const index = range?.index ?? editor.getLength()
  editor.insertText(index, '\n', 'user')
  editor.insertEmbed(index + 1, 'divider', true, 'user')
  editor.insertText(index + 2, '\n', 'user')
  editor.setSelection(index + 3, 0, 'silent')
}

function openImageUpload() {
  if (props.readOnly) return
  imageUploadRef.value?.selectImage()
}

function insertUploadedImage(image: UploadedImage) {
  if (!quill) return
  const range = quill.getSelection(true)
  const index = range?.index ?? quill.getLength()
  quill.insertEmbed(index, 'image', {
    url: image.url,
    alt: image.fileName,
    title: image.fileName,
  }, 'user')
  quill.setSelection(index + 1, 0, 'silent')
  window.requestAnimationFrame(() => {
    const inserted = Array.from(quill?.root.querySelectorAll('img') || [])
      .find((img) => img.getAttribute('src') === image.url) as HTMLImageElement | undefined
    if (inserted) {
      selectedImage.value = inserted
      updateImageResizePanel()
    }
  })
}

function handleEditorClick(event: MouseEvent) {
  const target = event.target
  if (target instanceof HTMLImageElement && quill?.root.contains(target)) {
    selectedImage.value = target
    updateImageResizePanel()
    return
  }
  selectedImage.value = null
}

function setSelectedImageWidth(width: string | null) {
  if (!quill || !selectedImage.value) return
  const blot = Quill.find(selectedImage.value)
  if (!blot) return
  const index = quill.getIndex(blot as any)
  quill.formatText(index, 1, 'width', width, 'user')
  quill.setSelection(index + 1, 0, 'silent')
  window.requestAnimationFrame(updateImageResizePanel)
}

function updateImageResizePanel() {
  const image = selectedImage.value
  const container = editorEl.value?.parentElement
  if (!image || !container) return
  const imageRect = image.getBoundingClientRect()
  const containerRect = container.getBoundingClientRect()
  const panelWidth = 250
  const left = Math.max(8, Math.min(imageRect.left - containerRect.left, containerRect.width - panelWidth - 8))
  const preferredTop = imageRect.top - containerRect.top - 42
  const top = preferredTop > 8 ? preferredTop : imageRect.bottom - containerRect.top + 8
  imageResizePanelStyle.value = {
    left: `${left}px`,
    top: `${top}px`,
  }
}

function handleQuillTextChange() {
  window.requestAnimationFrame(updateWordCount)
}

function handleAwarenessUpdate(changes: { added: number[]; updated: number[]; removed: number[] }, origin: unknown) {
  if (origin === 'remote' || !awareness) return
  const changedClients = [...changes.added, ...changes.updated, ...changes.removed]
  if (changedClients.length === 0) return
  sendCollabMessage({
    type: 'DOC_CURSOR',
    roomId: props.docId,
    data: {
      kind: 'awareness_update',
      clientId: String(ydoc?.clientID || ''),
      update: bytesToBase64(encodeAwarenessUpdate(awareness, changedClients)),
      userName: localUser.name,
      color: localUser.color,
    },
  })
}

function handleCollabMessage(raw: string) {
  let message: CollabMessage
  try {
    message = JSON.parse(raw)
  } catch {
    return
  }

  const data = message.data || {}
  if (data.clientId && String(data.clientId) === String(ydoc?.clientID || '')) return
  const type = normalizeMessageType(message.type)

  if (type === 'DOC_OPERATION' || type === 'DOC_OPERATION_BROADCAST') {
    handleDocOperation(data)
    return
  }

  if (type === 'DOC_CURSOR' || type === 'DOC_CURSOR_BROADCAST') {
    handleDocCursor(data)
  }
}

function handleDocOperation(data: Record<string, any>) {
  if (data.kind === 'yjs_sync_request') {
    if (synced) broadcastFullState()
    return
  }

  if ((data.kind === 'yjs_state' || data.kind === 'yjs_update') && typeof data.update === 'string') {
    applyRemoteYjsUpdate(data.update, data.userName)
    return
  }

  if (data.kind === 'content_snapshot' && typeof data.content === 'string') {
    applyLegacySnapshot(data.content, data.userName)
  }
}

function handleDocCursor(data: Record<string, any>) {
  if (data.kind === 'awareness_update' && typeof data.update === 'string' && awareness) {
    applyAwarenessUpdate(awareness, base64ToBytes(data.update), 'remote')
  }
}

function applyRemoteYjsUpdate(encodedUpdate: string, userName?: string) {
  if (!ydoc) return
  Y.applyUpdate(ydoc, base64ToBytes(encodedUpdate), 'remote')
  if (!synced) finishSync()
  if (userName) updateStatus('online', `已同步 ${userName} 的修改`)
}

function applyLegacySnapshot(content: string, userName?: string) {
  if (!quill) return
  suppressPersistence = true
  const delta = quill.clipboard.convert({ html: normalizeInitialContent(content) })
  quill.setContents(delta, 'api')
  suppressPersistence = false
  if (!synced) finishSync()
  if (userName) updateStatus('online', `已同步 ${userName} 的修改`)
}

function queueContentPersist() {
  if (!synced || !quill) return
  updateWordCount()
  if(!props.readOnly)emit('dirty')
  if (saveTimer) window.clearTimeout(saveTimer)
  saveTimer = window.setTimeout(() => {
    if (!quill) return
    emit('save', quill.root.innerHTML)
  }, 2000)
}

function updateWordCount() {
  if (!quill) {
    emit('word-count', 0)
    return
  }
  emit('word-count', quill.getText().replace(/\s+$/g, '').length)
}

function emitAwarenessUsers() {
  if (!awareness || !ydoc) return
  const users: OnlineUser[] = []
  awareness.getStates().forEach((state, clientId) => {
    if (clientId === ydoc?.clientID) return
    const user = state.user || {}
    users.push({
      id: String(clientId),
      name: String(user.name || '协作者'),
      color: String(user.color || colorFromId(String(clientId))),
      lastSeen: Date.now(),
    })
  })
  emit('collaborators-change', users)
}

function sendLocalAwareness() {
  if (!awareness) return
  awareness.setLocalStateField('user', {
    name: localUser.name,
    color: localUser.color,
    activeAt: Date.now(),
  })
}

function startMaintenanceLoops() {
  stopMaintenanceLoops()
  awarenessRefreshTimer = window.setInterval(sendLocalAwareness, 15000)
  heartbeatTimer = window.setInterval(() => {
    sendCollabMessage({ type: 'HEARTBEAT' })
  }, 20000)
}

function stopMaintenanceLoops() {
  if (awarenessRefreshTimer) window.clearInterval(awarenessRefreshTimer)
  if (heartbeatTimer) window.clearInterval(heartbeatTimer)
  awarenessRefreshTimer = undefined
  heartbeatTimer = undefined
}

function clearTimers() {
  if (saveTimer) window.clearTimeout(saveTimer)
  if (syncFallbackTimer) window.clearTimeout(syncFallbackTimer)
  if (reconnectTimer) window.clearTimeout(reconnectTimer)
  stopMaintenanceLoops()
  saveTimer = undefined
  syncFallbackTimer = undefined
  reconnectTimer = undefined
}

function sendCollabMessage(message: CollabMessage) {
  if (ws?.readyState !== WebSocket.OPEN) return
  ws.send(JSON.stringify({
    ...message,
    roomId: message.roomId || props.docId,
    seq: message.seq || Date.now() * 1000 + (++messageSeq),
    clientTimestamp: Date.now(),
  }))
}

function updateStatus(state: CollabState, text: string) {
  statusState.value = state
  statusText.value = text
  emit('status-change', { state, text })
}

function buildWsUrl(token: string) {
  const configuredUrl = import.meta.env.VITE_WS_URL as string | undefined
  if (configuredUrl) return `${configuredUrl}?token=${encodeURIComponent(token)}`
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}/ws?token=${encodeURIComponent(token)}`
}

function getLocalUser() {
  const token = getToken()
  const payload = token ? decodeJwtPayload(token) : null
  const userId = String(payload?.userId || payload?.sub || createClientId().slice(0, 6))
  return {
    id: userId,
    name: String(payload?.nickname || payload?.username || `用户${userId}`),
    color: colorFromId(userId),
  }
}

function decodeJwtPayload(token: string): Record<string, any> | null {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(payload))
  } catch {
    return null
  }
}

function createClientId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function colorFromId(id: string) {
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#7c3aed', '#0891b2']
  let hash = 0
  for (const char of id) hash = (hash * 31 + char.charCodeAt(0)) >>> 0
  return colors[hash % colors.length]
}

function normalizeMessageType(type?: string) {
  return String(type || '').toUpperCase()
}

function normalizeInitialContent(content?: string) {
  if (!content || content === '{}') return ''
  return content
}

function setImageNodeWidth(node: HTMLImageElement, width: string) {
  node.style.width = width
  node.style.maxWidth = '100%'
  if (/^\d+$/.test(width)) {
    node.setAttribute('width', width)
  } else {
    node.removeAttribute('width')
  }
}

function bytesToBase64(bytes: Uint8Array) {
  let binary = ''
  for (const byte of bytes) binary += String.fromCharCode(byte)
  return btoa(binary)
}

function base64ToBytes(base64: string) {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes
}

function scrollToPosition(position: number) {
  quill?.root.scrollTo({ top: position, behavior: 'smooth' })
}

function publishHeadings(){
  if(!quill)return
  emit('headings-change',Array.from(quill.root.querySelectorAll('h1,h2,h3,h4,h5,h6')).map(el=>({index:quill!.getIndex(Quill.find(el) as any),level:Number(el.tagName.slice(1)),text:el.textContent||''})))
}

function locateHeading(index:number){if(!quill)return;const [line]=quill.getLine(index);line?.domNode.scrollIntoView({block:'center',behavior:'smooth'});quill.setSelection(index,0,'api')}

defineExpose({
  scrollToPosition,
  locateHeading,
  flushContent(){if(!quill||props.readOnly)return undefined;if(saveTimer)window.clearTimeout(saveTimer);saveTimer=undefined;return quill.root.innerHTML},
  getCommentAnchor(){if(!quill)return null;const range=quill.getSelection()||lastSelection;if(!range?.length)return null;return {index:range.index,length:Math.min(range.length,160),text:quill.getText(range.index,Math.min(range.length,160))}},
  locateAnchor(anchor:{index:number;length:number;text:string}){
    if(!quill||!anchor.text)return false
    const text=quill.getText();let index=anchor.index
    if(text.slice(index,index+anchor.length)!==anchor.text){index=text.indexOf(anchor.text);if(index<0||text.indexOf(anchor.text,index+1)>=0)return false}
    quill.focus();quill.setSelection(index,anchor.text.length,'api');const [line]=quill.getLine(index);line?.domNode.scrollIntoView({block:'center',behavior:'smooth'});return true
  },
  moveSection(from:number,to:number){
    if(!quill||props.readOnly||from===to)return
    const headings=Array.from(quill.root.querySelectorAll('h1,h2,h3,h4,h5,h6')).map(el=>({index:quill!.getIndex(Quill.find(el) as any),level:Number(el.tagName.slice(1))}));const current=headings.find(h=>h.index===from);if(!current)return
    const end=headings.find(h=>h.index>from&&h.level<=current.level)?.index??quill.getLength()
    if(to>from&&to<end){ElMessage.info('不能将章节移入自身');return}
    const length=end-from,content=quill.getContents(from,length),Delta=Quill.import('delta') as any
    const removal=new Delta().retain(from).delete(length)
    quill.updateContents(removal,'user');quill.updateContents(new Delta().retain(to>from?to-length:to).concat(content),'user');publishHeadings()
  },
  restoreContent(content: string) {
    if (!quill || props.readOnly) return
    quill.setContents(quill.clipboard.convert({html: normalizeInitialContent(content)}), 'user')
  },
})
</script>

<style scoped lang="scss">
.quill-editor {
  position: relative;
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  background: #fff;
}

.quill-host {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.editor-upload {
  flex: 1;
  min-height: 0;
}

.sync-mask {
  position: absolute;
  inset: 42px 0 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: rgba(255, 255, 255, 0.82);
  font-size: 14px;
  z-index: 3;
}

.image-upload-status {
  position: absolute;
  right: 16px;
  bottom: 16px;
  z-index: 7;
  padding: 6px 12px;
  border-radius: 6px;
  color: #606266;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
  font-size: 13px;
}

.image-resize-panel {
  position: absolute;
  z-index: 8;
  display: inline-flex;
  gap: 4px;
  padding: 6px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.14);
}

.image-resize-panel button {
  min-width: 34px;
  height: 26px;
  padding: 0 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #606266;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
}

.image-resize-panel button:hover {
  border-color: #409eff;
  color: #409eff;
}

:deep(.ql-toolbar.ql-snow) {
  border: 0;
  border-bottom: 1px solid #e8e8e8;
  background: #fafafa;
}

:deep(.ql-container.ql-snow) {
  flex: 1;
  min-height: 0;
  border: 0;
  font-size: 15px;
}

:deep(.ql-editor) {
  min-height: 100%;
  padding: 28px 44px;
  line-height: 1.8;
}

:deep(.ql-editor img) {
  max-width: 100%;
  cursor: pointer;
}
</style>
