import request from '@/utils/request'

export function getConversations() {
  return request.get('/chat/conversations')
}

export function getMessages(sessionId: string, page = 1, size = 50) {
  return request.get(`/chat/messages/${sessionId}`, { params: { page, size } })
}

export function sendMessage(sessionId: string, content: string, msgType = 1) {
  return request.post('/chat/send', null, { params: { sessionId, content, msgType } })
}

export function recallMessage(msgId: string) {
  return request.post(`/chat/recall/${msgId}`)
}
