import request from '@/utils/request'

export function getNotificationList(params?: { pageNum?: number; pageSize?: number; type?: string }) {
  return request.get('/notification/list', { params })
}

export function markAsRead(id: number) {
  return request.put(`/notification/${id}/read`)
}

export function markAllRead() {
  return request.put('/notification/read-all')
}

export function getUnreadCount() {
  return request.get('/notification/unread-count')
}

export function deleteNotification(id: number) {
  return request.delete(`/notification/${id}`)
}
