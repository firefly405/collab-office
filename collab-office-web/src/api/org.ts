import request from '@/utils/request'

// Department
export function getDeptTree(spaceId: number) {
  return request.get('/org/dept/tree', { params: { spaceId } })
}

export function createDept(data: { parentId?: number; name: string; leaderId?: number }) {
  return request.post('/org/dept', data)
}

export function updateDept(deptId: number, data: { name?: string; leaderId?: number }) {
  return request.put(`/org/dept/${deptId}`, data)
}

export function deleteDept(deptId: number) {
  return request.delete(`/org/dept/${deptId}`)
}

// Permissions
export function getPermissionMatrix() {
  return request.get('/org/permission/matrix')
}

export function updatePermission(data: { role: string; module: string; level: string }) {
  return request.put('/org/permission/matrix', data)
}

export function getRoleTemplates() {
  return request.get('/org/permission/templates')
}
