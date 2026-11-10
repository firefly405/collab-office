import request from '@/utils/request'

export function getDashboardStats() {
  return request.get('/admin/dashboard/stats')
}

export function getAuditLogs(params?: {
  keyword?: string
  module?: string
  startTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get('/admin/logs', { params })
}

export function getAuditStats() {
  return request.get('/admin/logs/stats')
}

export function exportLogs(params?: Record<string, any>) {
  return request.post('/admin/logs/export', params)
}
