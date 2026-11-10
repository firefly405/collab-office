import request from '@/utils/request'

export function createDoc(title: string, type: number, spaceId?: number, folderId?: number) {
  return request.post('/document/create', { title, type, spaceId, folderId })
}

export function getDocList(params: { spaceId: number; type?: number; keyword?: string; pageNum?: number; pageSize?: number }) {
  return request.get('/document/list', { params })
}

export function getDocDetail(docId: string) {
  return request.get(`/document/${docId}`)
}

export function updateDoc(docId: string, data: any) {
  return request.put(`/document/${docId}`, data)
}

export function deleteDoc(docId: string) {
  return request.delete(`/document/${docId}`)
}

export function recoverDoc(docId: string) {
  return request.post(`/document/${docId}/recover`)
}

export function duplicateDoc(docId: string) {
  return request.post(`/document/${docId}/duplicate`)
}

export function favoriteDoc(docId: string, fav: boolean) {
  return request.post(`/document/${docId}/favorite`, null, { params: { fav } })
}

export function getDocContent(docId: string) {
  return request.get(`/document/${docId}/content`)
}

export function saveDocContent(docId: string, content: string) {
  return request.put(`/document/${docId}/content`, content, {
    headers: { 'Content-Type': 'text/html; charset=utf-8' },
  })
}

export function getVersions(docId: string, page = 1, size = 20) {
  return request.get(`/document/${docId}/versions`, { params: { page, size } })
}

export function rollbackVersion(docId: string, targetVersion: number) {
  return request.post(`/document/${docId}/versions/rollback`, null, { params: { targetVersion } })
}

export function createSnapshot(docId: string, name: string) {
  return request.post(`/document/${docId}/versions/snapshot`, null, { params: { name } })
}

export function getComments(docId: string, filter = 'all') {
  return request.get(`/document/${docId}/comments`, { params: { filter } })
}

export function createComment(docId: string, content: string, selectionRange?: string, parentId?: number) {
  return request.post(`/document/${docId}/comments`, { docId, content, selectionRange, parentId })
}

export function resolveComment(docId: string, commentId: number) {
  return request.put(`/document/${docId}/comments/${commentId}/resolve`)
}

export function deleteComment(docId: string, commentId: number) {
  return request.delete(`/document/${docId}/comments/${commentId}`)
}
