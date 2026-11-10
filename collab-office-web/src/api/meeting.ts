import request from '@/utils/request'

export function createMeeting(data: {
  title: string
  agenda?: string
  startTime?: string
  endTime?: string
  password?: string
  participantIds?: number[]
}) {
  return request.post('/meeting/create', data)
}

export function getMeetingList(params?: { status?: string; pageNum?: number; pageSize?: number }) {
  return request.get('/meeting/list', { params })
}

export function getMeetingDetail(meetingId: string) {
  return request.get(`/meeting/${meetingId}`)
}

export function joinMeeting(meetingId: string) {
  return request.post(`/meeting/${meetingId}/join`)
}

export function leaveMeeting(meetingId: string) {
  return request.post(`/meeting/${meetingId}/leave`)
}

export function cancelMeeting(meetingId: string) {
  return request.delete(`/meeting/${meetingId}`)
}
