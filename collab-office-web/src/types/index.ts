export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface LoginRequest {
  account: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expireIn: number
  tokenType: string
  userInfo: UserInfo
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  phone: string
  email: string
  avatar: string
  gender: number
  status: number
  onlineStatus: number
  lastLoginIp: string
  lastLoginTime: string
}

export interface DocumentDetail {
  docId: string
  title: string
  type: number
  creatorName: string
  lastEditorName: string
  currentVersion: number
  wordCount: number
  editCount: number
  viewCount: number
  tags: string
  isFavorite: number
  createTime: string
  lastEditTime: string
  memberCount: number
  onlineMemberCount: number
}

export interface DocContent {
  docId: string
  title: string
  content: string
  version: number
  lastEditor: string
  lastEditTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export interface MessageItem {
  msgId: string
  senderId: string
  content: string
  msgType: number
  sendTime: string
}

export interface Conversation {
  sessionId: string
  name: string
  type: number
  lastMsg: string
  lastMsgTime: string
  unreadCount: number
}

export interface TaskItem {
  taskId: string
  title: string
  description: string
  projectId: string
  status: number
  priority: number
  progress: number
  assigneeId: number | null
  dueDate: string | null
}

export interface MeetingItem {
  meetingId: string
  title: string
  hostId: number
  status: number
  startTime: string
}

export interface SheetCell {
  sheetId: number
  cellKey: string
  content: string
  formatJson?: string
  formula?: string
}
