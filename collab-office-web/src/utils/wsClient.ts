type WSCallback = (data: any) => void

class WsClient {
  private socket: WebSocket | null = null
  private listeners: Record<string, Set<WSCallback>> = {}
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private statusListeners: Set<(connected: boolean) => void> = new Set()
  private _connected = false
  private url = ''
  private connecting = false
  private token = ''
  private reconnectAttempts = 0
  private manuallyClosed = false
  private readonly maxReconnectDelay = 30_000
  private readonly baseReconnectDelay = 1_000

  constructor() {
    this.url = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`
  }

  connect(token?: string) {
    if (this._connected || this.connecting) return
    this.connecting = true
    this.manuallyClosed = false
    if (token) this.token = token

    try {
      const url = this.token ? `${this.url}?token=${encodeURIComponent(this.token)}` : this.url
      this.socket = new WebSocket(url)

      this.socket.onopen = () => {
        this._connected = true
        this.connecting = false
        this.reconnectAttempts = 0
        this.socket?.send(JSON.stringify({ type: 'CONNECT' }))
        this.statusListeners.forEach(fn => fn(true))
      }

      this.socket.onclose = () => {
        this._connected = false
        this.connecting = false
        this.statusListeners.forEach(fn => fn(false))
        if (!this.manuallyClosed) this.scheduleReconnect()
      }

      this.socket.onerror = () => {
        this.socket?.close()
      }

      this.socket.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data)
          if (msg?.type) {
            // 把顶层 roomId/userId 透传到 data，方便回调统一读取
            const data = msg.data ?? {}
            if (msg.roomId != null && typeof data === 'object' && !Array.isArray(data)) {
              data.roomId = msg.roomId
            }
            if (msg.userId != null && typeof data === 'object' && !Array.isArray(data)) {
              data.userId = msg.userId
            }
            this.listeners[String(msg.type).toLowerCase()]?.forEach(cb => cb(data))
          }
        } catch { /* ignore malformed messages */ }
      }
    } catch {
      this.scheduleReconnect()
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    const delay = Math.min(
      this.baseReconnectDelay * (1 << this.reconnectAttempts),
      this.maxReconnectDelay
    )
    this.reconnectAttempts++
    this.reconnectTimer = setTimeout(() => this.connect(), delay)
  }

  disconnect() {
    this.manuallyClosed = true
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    this.reconnectTimer = null
    this.reconnectAttempts = 0
    this.socket?.close()
    this.socket = null
    this._connected = false
  }

  /**
   * Send to server; if offline, still trigger local listeners (optimistic).
   * 当传入 roomId 时，会作为顶层字段发送（服务端 WebSocketMessage.roomId 是顶层字段）。
   */
  send(type: string, data: any, roomId?: string) {
    const payload: Record<string, unknown> = { type, data }
    if (roomId) payload.roomId = roomId
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(payload))
    } else {
      // Optimistic: fire local listeners immediately
      this.listeners[type]?.forEach(cb => cb(data))
    }
  }

  /** Listen to incoming messages of a type. Returns unsubscribe function. */
  on(type: string, cb: WSCallback): () => void {
    type = type.toLowerCase()
    if (!this.listeners[type]) this.listeners[type] = new Set()
    this.listeners[type].add(cb)
    return () => this.listeners[type]?.delete(cb)
  }

  onStatusChange(cb: (connected: boolean) => void) {
    this.statusListeners.add(cb)
    // Immediately notify current status
    cb(this._connected)
    return () => this.statusListeners.delete(cb)
  }

  get connected() { return this._connected }
}

export const wsClient = new WsClient()
