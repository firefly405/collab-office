import request from '@/utils/request'

export function createSheet(title: string, spaceId?: number) {
  return request.post('/sheet/manage/create', null, { params: { title, spaceId } })
}

export function getSheetDetail(sheetId: string) {
  return request.get(`/sheet/manage/${sheetId}`)
}

export function updateCell(sheetId: number, cellKey: string, content?: string, formula?: string, formatJson?: string) {
  return request.put('/sheet/cell', { sheetId, cellKey, content, formula, formatJson })
}

export function batchUpdateCell(sheetId: number, cells: Array<{ sheetId: number; cellKey: string; content?: string }>) {
  return request.put('/sheet/cell/batch', { sheetId, cells })
}

export function getCellRange(sheetId: number, range: string) {
  return request.get('/sheet/cell/range', { params: { sheetId, range } })
}

export function addSheet(sheetId: string, name: string) {
  return request.post(`/sheet/manage/${sheetId}/sheet`, null, { params: { name } })
}

export function deleteSheet(sheetId: string, index: number) {
  return request.delete(`/sheet/manage/${sheetId}/sheet/${index}`)
}

export function renameSheet(sheetId: string, index: number, name: string) {
  return request.put(`/sheet/manage/${sheetId}/sheet/${index}/rename`, null, { params: { name } })
}
